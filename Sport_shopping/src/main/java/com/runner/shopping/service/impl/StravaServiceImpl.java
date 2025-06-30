package com.runner.shopping.service.impl;

import com.runner.shopping.config.StravaProperties;
import com.runner.shopping.entity.Promotions;
import com.runner.shopping.entity.StravaCoupon;
import com.runner.shopping.entity.StravaToken;
import com.runner.shopping.mapper.PromotionMapper;
import com.runner.shopping.model.dto.PromotionDTO;
import com.runner.shopping.model.dto.StravaStatusDTO;
import com.runner.shopping.repository.PromotionRepository;
import com.runner.shopping.repository.StravaCouponRepository;
import com.runner.shopping.repository.StravaTokenRepository;
import com.runner.shopping.service.StravaService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class StravaServiceImpl implements StravaService {
    private final StravaProperties props;
    private final StravaTokenRepository tokenRepo;
    private final StravaCouponRepository couponRepo;
    private final PromotionRepository promoRepo;
    private final PromotionMapper promoMapper;
    private final RestTemplate rt = new RestTemplate();

    public StravaServiceImpl(
            StravaProperties props,
            StravaTokenRepository tokenRepo,
            StravaCouponRepository couponRepo,
            PromotionRepository promoRepo,
            PromotionMapper promoMapper) {
        this.props = props;
        this.tokenRepo = tokenRepo;
        this.couponRepo = couponRepo;
        this.promoRepo = promoRepo;
        this.promoMapper = promoMapper;
    }

    @Override
    public void connect(Long userId, String code) {
        // 1. Đổi code lấy access + refresh token
        Map<String, Object> req = Map.of(
                "client_id",     props.getClientId(),
                "client_secret", props.getClientSecret(),
                "code",          code,
                "grant_type",    "authorization_code"
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = rt.postForObject(
                props.getApiBaseUrl() + "/oauth/token",
                req, Map.class);

        // 2. Lưu hoặc cập nhật StravaToken
        StravaToken t = tokenRepo.findByUserId(userId).orElse(new StravaToken());
        t.setUserId(userId);
        t.setAccessToken((String) resp.get("access_token"));
        t.setRefreshToken((String) resp.get("refresh_token"));
        t.setExpiresAt(Instant.ofEpochSecond(
                ((Number) resp.get("expires_at")).longValue()));
        tokenRepo.save(t);
    }

    @Override
    public StravaStatusDTO getStatus(Long userId, int days) {
        // --- 1) Lấy token, refresh nếu đã hết hạn ---
        StravaToken t = tokenRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Chưa kết nối Strava"));
        if (t.getExpiresAt().isBefore(Instant.now())) {
            refreshToken(t);
        }

        // --- 2) Tính ngưỡng thời gian 'after' (giây) ---
        long after = Instant.now()
                .minus(days, ChronoUnit.DAYS)
                .getEpochSecond();

        // --- 3) Lấy tất cả activities qua Strava API (phân trang) ---
        List<Map<String, Object>> all = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = String.format(
                    "%s/athlete/activities?after=%d&per_page=100&page=%d",
                    props.getApiBaseUrl(), after, page);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(t.getAccessToken());
            List<Map<String, Object>> pageList = rt.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (pageList == null || pageList.isEmpty()) break;
            all.addAll(pageList);
            if (pageList.size() < 100) break;
            page++;
        }

        // --- 4) Tính tổng mét & tổng thời gian, lọc theo pace hợp lệ ---
        double totalMeters = 0;
        long totalMovingSec = 0;
        for (var a : all) {
            if (!"Run".equals(a.get("type"))) continue;  // chỉ tính chạy bộ

            double dist = ((Number) a.get("distance")).doubleValue();     // mét
            long movingSec = ((Number) a.get("moving_time")).longValue(); // giây

            // tính pace = giây/km
            long secPerKm = Math.round(movingSec * 1000.0 / dist);
            // bỏ qua nếu pace < min hoặc > max (giả mạo/GPS lỗi)
            if (secPerKm < props.getMinPaceSecPerKm()
                    || secPerKm > props.getMaxPaceSecPerKm()) {
                continue;
            }

            totalMeters    += dist;
            totalMovingSec += movingSec;
        }

        // --- 5) Lấy số mét đã redeem hôm nay & trong tháng ---
        int usedToday = couponRepo.sumUsedMetersByPeriod(
                userId,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59, 59));
        int usedThisMonth = couponRepo.sumUsedMetersByPeriod(
                userId,
                LocalDate.now().withDayOfMonth(1).atStartOfDay(),
                LocalDate.now().atTime(23, 59, 59));

        // --- 6) Tính raw & áp giới hạn ngày/tháng ---
        int rawToday   = (int) Math.floor(totalMeters) - usedToday;
        int rawMonth   = (int) Math.floor(totalMeters) - usedThisMonth;
        int availToday = Math.min(rawToday, props.getMaxDailyMeters());
        int availMonth = Math.min(rawMonth, props.getMaxMonthlyMeters());
        int availMeters= Math.max(0, Math.min(availToday, availMonth));

        // --- 7) Tính số tiền giảm và pace trung bình ---
        int discount = (availMeters / 100) * 1000; // 1.000₫ mỗi 100m
        long secPerKmAvg = totalMeters > 0
                ? Math.round(totalMovingSec * 1000.0 / totalMeters)
                : 0;
        String pace = String.format("%02d:%02d",
                secPerKmAvg / 60, secPerKmAvg % 60);

        // --- 8) Build DTO & cảnh báo nếu gần đầy hạn mức tháng ---
        StravaStatusDTO dto = new StravaStatusDTO();
        dto.setTotalDistanceKm(totalMeters / 1000.0);
        dto.setAveragePace(pace);
        dto.setAvailableKm(availMeters);
        dto.setCurrentDiscount(discount);

        double usedPercent = 100.0 * (
                ((Math.floor(totalMeters) - availMeters))
                        / props.getMaxMonthlyMeters());
        if (usedPercent >= props.getWarnThresholdPercent()) {
            dto.setNearLimit(true);
            dto.setWarningMessage(
                    "Bạn đã sử dụng " + (int) usedPercent + "% hạn mức tháng, vui lòng kiểm tra.");
        }
        return dto;
    }

    @Override
    public PromotionDTO redeemCoupon(Long userId, int requestedMeters) {
        // 1) Giới hạn số lần redeem trong tháng dựa trên StravaCoupon
        int redeemsThisMonth = couponRepo.countByUserIdAndCreatedAtBetween(
                userId,
                LocalDate.now().withDayOfMonth(1).atStartOfDay(),
                LocalDate.now().atTime(23,59,59)
        );
        if (redeemsThisMonth >= props.getMaxRedeemsPerMonth()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bạn đã đạt giới hạn " + props.getMaxRedeemsPerMonth() + " coupon/tháng."
            );
        }

        // 2) Kiểm requestedMeters
        StravaStatusDTO status = getStatus(userId, 30);
        if (requestedMeters < 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Phải nhập tối thiểu 100 m");
        }
        if (requestedMeters > status.getAvailableKm()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Không đủ mét để redeem");
        }

        // 3) Tính tiền giảm & tạo Promotion
        int used = (requestedMeters / 100) * 100;
        BigDecimal discountAmt = BigDecimal.valueOf(used / 100L * 1000L);

        Promotions p = new Promotions();
        p.setCode("STRAVA-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setDiscountAmount(discountAmt);
        p.setMinimumOrderValue(BigDecimal.ZERO);
        p.setIsActive(true);
        p.setMaxUsage(1);
        p.setCreatedAt(LocalDateTime.now());
        promoRepo.save(p);

        // 4) Ghi nhận StravaCoupon
        StravaCoupon c = new StravaCoupon();
        c.setUserId(userId);
        c.setPromotionId(p.getId());
        c.setUsedMeters(used);
        c.setCreatedAt(LocalDateTime.now());
        couponRepo.save(c);

        return promoMapper.toDTO(p);
    }

    /** Nếu token hết hạn, gọi Strava để lấy mới */
    private void refreshToken(StravaToken t) {
        Map<String, Object> req = Map.of(
                "client_id",     props.getClientId(),
                "client_secret", props.getClientSecret(),
                "grant_type",    "refresh_token",
                "refresh_token", t.getRefreshToken()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = rt.postForObject(
                props.getApiBaseUrl() + "/oauth/token", req, Map.class);
        t.setAccessToken((String) resp.get("access_token"));
        t.setRefreshToken((String) resp.get("refresh_token"));
        t.setExpiresAt(Instant.ofEpochSecond(
                ((Number) resp.get("expires_at")).longValue()));
        tokenRepo.save(t);
    }
}
