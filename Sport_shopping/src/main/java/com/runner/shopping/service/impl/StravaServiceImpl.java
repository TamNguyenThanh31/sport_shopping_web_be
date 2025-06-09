package com.runner.shopping.service.impl;

import com.runner.shopping.config.StravaProperties;
import com.runner.shopping.entity.Promotions;
import com.runner.shopping.entity.StravaCoupon;
import com.runner.shopping.entity.StravaToken;
import com.runner.shopping.entity.User;
import com.runner.shopping.mapper.PromotionMapper;
import com.runner.shopping.model.dto.PromotionDTO;
import com.runner.shopping.model.dto.StravaStatusDTO;
import com.runner.shopping.repository.PromotionRepository;
import com.runner.shopping.repository.StravaCouponRepository;
import com.runner.shopping.repository.StravaTokenRepository;
import com.runner.shopping.repository.UserRepository;
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
    private final UserRepository userRepo;
    private final RestTemplate rt = new RestTemplate();

    public StravaServiceImpl(StravaProperties props,
                             StravaTokenRepository tokenRepo,
                             StravaCouponRepository couponRepo,
                             PromotionRepository promoRepo,
                             PromotionMapper promoMapper,
                             UserRepository userRepo) {
        this.props = props;
        this.tokenRepo = tokenRepo;
        this.couponRepo = couponRepo;
        this.promoRepo = promoRepo;
        this.promoMapper = promoMapper;
        this.userRepo = userRepo;
    }

    @Override
    public void connect(Long userId, String code) {
        // 1. Exchange code → tokens
        Map<String, Object> req = Map.of(
                "client_id", props.getClientId(),
                "client_secret", props.getClientSecret(),
                "code", code,
                "grant_type", "authorization_code"
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = rt.postForObject(
                props.getApiBaseUrl() + "/oauth/token", req, Map.class);

        // 2. Lưu StravaToken
        StravaToken t = tokenRepo.findByUserId(userId).orElse(new StravaToken());
        t.setUserId(userId);
        t.setAccessToken((String) resp.get("access_token"));
        t.setRefreshToken((String) resp.get("refresh_token"));
        t.setExpiresAt(Instant.ofEpochSecond(
                ((Number) resp.get("expires_at")).longValue()));
        tokenRepo.save(t);

        // 3. (Tuỳ chọn) Lấy athlete và lưu stravaId lên User
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(t.getAccessToken());
        @SuppressWarnings("unchecked")
        Map<String, Object> athlete = rt.exchange(
                props.getApiBaseUrl() + "/athlete",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();

        String stravaAthleteId = String.valueOf(athlete.get("id"));
        User u = userRepo.findById(userId).orElseThrow();
        u.setStravaId(stravaAthleteId);
        userRepo.save(u);
    }

//    Cứ 1km là đổi mã giamr giá
//    @Override
//    public StravaStatusDTO getStatus(Long userId, int days) {
//        StravaToken t = tokenRepo.findByUserId(userId)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.BAD_REQUEST, "Chưa kết nối Strava"));
//
//        // Refresh nếu expired
//        if (t.getExpiresAt().isBefore(Instant.now())) {
//            refreshToken(t);
//        }
//
//        // Tính timestamp 'after'
//        long after = Instant.now()
//                .minus(days, ChronoUnit.DAYS)
//                .getEpochSecond();
//
//        // Phân trang để gom hết activities
//        List<Map<String,Object>> all = new ArrayList<>();
//        int page = 1;
//        while (true) {
//            String url = props.getApiBaseUrl()
//                    + "/athlete/activities?after=" + after
//                    + "&per_page=100&page=" + page;
//            HttpHeaders headers = new HttpHeaders();
//            headers.setBearerAuth(t.getAccessToken());
//            List<Map<String,Object>> pageList = rt.exchange(
//                    url, HttpMethod.GET,
//                    new HttpEntity<>(headers),
//                    new ParameterizedTypeReference<List<Map<String,Object>>>() {}
//            ).getBody();
//            if (pageList == null || pageList.isEmpty()) break;
//            all.addAll(pageList);
//            if (pageList.size() < 100) break;
//            page++;
//        }
//
//        // Tính tổng chỉ cho type="Run"
//        double totalMeters = 0;
//        long totalMovingSec = 0;
//        for (var a : all) {
//            if ("Run".equals(a.get("type"))) {
//                totalMeters    += ((Number) a.get("distance")).doubleValue();
//                totalMovingSec += ((Number) a.get("moving_time")).longValue();
//            }
//        }
//        double totalKm = totalMeters / 1000.0;
//
//        // Lấy usedKm từ DB
//        int usedKm = couponRepo.sumUsedKmByUser(userId);
//        int availKm = Math.max(0, (int) Math.floor(totalKm) - usedKm);
//        int discount = (availKm / 5) * 10;
//
//        // Tính pace trung bình
//        long avgSec = totalKm > 0
//                ? Math.round(totalMovingSec / totalKm)
//                : 0;
//        String pace = String.format("%02d:%02d", avgSec / 60, avgSec % 60);
//
//        // Build DTO
//        StravaStatusDTO dto = new StravaStatusDTO();
//        dto.setTotalDistanceKm(totalKm);
//        dto.setAveragePace(pace);
//        dto.setAvailableKm(availKm);
//        dto.setCurrentDiscount(discount);
//        return dto;
//    }
//
//    @Override
//    public PromotionDTO redeemCoupon(Long userId) {
//        StravaStatusDTO s = getStatus(userId);
//        if (s.getCurrentDiscount() == 0) {
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST, "Chưa đủ km để đổi coupon");
//        }
//
//        // Tạo Promotion
//        Promotions p = new Promotions();
//        p.setCode("STRAVA-" +
//                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
//        p.setDiscountPercentage(BigDecimal.valueOf(s.getCurrentDiscount()));
//        p.setMinimumOrderValue(BigDecimal.ZERO);
//        p.setIsActive(true);
//        p.setCreatedAt(LocalDateTime.now());
//        promoRepo.save(p);
//
//        // Ghi nhận usedKm
//        StravaCoupon c = new StravaCoupon();
//        c.setUserId(userId);
//        c.setPromotionId(p.getId());
//        c.setUsedKm((s.getCurrentDiscount() / 10) * 5);
//        c.setCreatedAt(LocalDateTime.now());
//        couponRepo.save(c);
//
//        return promoMapper.toDTO(p);
//    }

    //    CỨ 100m là đổi mã gỉamr giá
    @Override
    public StravaStatusDTO getStatus(Long userId, int days) {
        StravaToken t = tokenRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Chưa kết nối Strava"));

        // Refresh nếu expired
        if (t.getExpiresAt().isBefore(Instant.now())) {
            refreshToken(t);
        }

        // Tính timestamp 'after'
        long after = Instant.now()
                .minus(days, ChronoUnit.DAYS)
                .getEpochSecond();

        // Phân trang để gom hết activities
        List<Map<String, Object>> all = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = props.getApiBaseUrl()
                    + "/athlete/activities?after=" + after
                    + "&per_page=100&page=" + page;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(t.getAccessToken());
            List<Map<String, Object>> pageList = rt.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            ).getBody();
            if (pageList == null || pageList.isEmpty()) break;
            all.addAll(pageList);
            if (pageList.size() < 100) break;
            page++;
        }

        // 1) Tính tổng mét cho type="Run"
        double totalMeters = 0;
        long totalMovingSec = 0;
        for (var a : all) {
            if ("Run".equals(a.get("type"))) {
                totalMeters += ((Number) a.get("distance")).doubleValue();
                totalMovingSec += ((Number) a.get("moving_time")).longValue();
            }
        }

        // 2) Lấy tổng usedMeters của user từ DB
        int usedMeters = couponRepo.sumUsedMetersByUser(userId);

        // 3) Tính availableMeters
        int availMeters = Math.max(0, (int) Math.floor(totalMeters) - usedMeters);

        // 4) Tính discount: mỗi 100m = 5%
        int discount = (availMeters / 100) * 5;

        // 5) Tính pace
        long avgSec = totalMeters > 0
                ? Math.round(totalMovingSec / totalMeters * 1000)  // moving_time per meter → sec per meter
                : 0;
        // chuyển sec/m ra mm:ss per km
        long secPerKm = avgSec * 1000;
        String pace = String.format("%02d:%02d", secPerKm / 60, secPerKm % 60);

        // 6) Build DTO (bạn có thể mở rộng DTO để trả về cả availableMeters)
        StravaStatusDTO dto = new StravaStatusDTO();
        dto.setTotalDistanceKm(totalMeters / 1000.0);
        dto.setAveragePace(pace);
        dto.setAvailableKm(availMeters);      // tái sử dụng availableKm cho mét
        dto.setCurrentDiscount(discount);
        return dto;
    }

    @Override
    public PromotionDTO redeemCoupon(Long userId, int requestedMeters) {
        StravaStatusDTO s = getStatus(userId);
        int avail = s.getAvailableKm();  // mét còn lại

        if (requestedMeters < 100 || requestedMeters > avail) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    requestedMeters < 100
                            ? "Phải nhập tối thiểu 100 m"
                            : "Không đủ số mét để redeem"
            );
        }

        int used = (requestedMeters / 100) * 100;
        // Tính tiền giảm: 1.000₫ cho mỗi 100 m
        BigDecimal discountAmt = BigDecimal.valueOf(used / 100L * 1000L);

        // Tạo Promotion
        Promotions p = new Promotions();
        p.setCode("STRAVA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setDiscountPercentage(null);       // không áp %
        p.setDiscountAmount(discountAmt);    // áp tiền
        p.setMinimumOrderValue(BigDecimal.ZERO);
        p.setIsActive(true);
        p.setMaxUsage(1);
        p.setCreatedAt(LocalDateTime.now());
        promoRepo.save(p);

        // Ghi StravaCoupon
        StravaCoupon c = new StravaCoupon();
        c.setUserId(userId);
        c.setPromotionId(p.getId());
        c.setUsedMeters(used);
        couponRepo.save(c);

        return promoMapper.toDTO(p);
    }


    private void refreshToken(StravaToken t) {
        Map<String, Object> req = Map.of(
                "client_id", props.getClientId(),
                "client_secret", props.getClientSecret(),
                "grant_type", "refresh_token",
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

