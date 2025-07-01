package com.runner.shopping.controller;

import com.runner.shopping.entity.Promotions;
import com.runner.shopping.mapper.PromotionMapper;
import com.runner.shopping.model.dto.PromotionDTO;
import com.runner.shopping.model.dto.StravaStatusDTO;
import com.runner.shopping.model.request.StravaRedeemRequest;
import com.runner.shopping.repository.PromotionRepository;
import com.runner.shopping.repository.StravaCouponRepository;
import com.runner.shopping.repository.UserRepository;
import com.runner.shopping.service.StravaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/strava")
@AllArgsConstructor
public class StravaController {
    private final StravaService strava;
    private final StravaCouponRepository couponRepo;
    private final PromotionRepository promoRepo;
    private final PromotionMapper promoMapper;
    private final UserRepository userRepo;

    // OAuth callback
    @GetMapping("/callback")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public void callback(@RequestParam String code,
                         Authentication auth,
                         HttpServletResponse resp) throws IOException {
        Long userId = userRepo.findByUsername(auth.getName())
                .orElseThrow().getId();
        strava.connect(userId, code);
        resp.sendRedirect("/");  // hoặc URL frontend
    }

    // Lấy status, days mặc định 7
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public StravaStatusDTO status(Authentication auth,
                                  @RequestParam(defaultValue = "7") int days) {
        Long userId = userRepo.findByUsername(auth.getName())
                .orElseThrow().getId();
        return strava.getStatus(userId, days);
    }

    // Redeem coupon
    @PostMapping("/redeem-coupon")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public PromotionDTO redeem(
            @RequestBody @Valid StravaRedeemRequest req,
            Authentication auth) {

        Long userId = userRepo.findByUsername(auth.getName())
                .orElseThrow().getId();

        // chuyển sang metres
        int metres = "km".equalsIgnoreCase(req.getUnit())
                ? (int)Math.round(req.getAmount() * 1000)
                : req.getAmount().intValue();

        return strava.redeemCoupon(userId, metres);
    }

    // MỚI: Lấy danh sách Strava coupons của chính user
    @GetMapping("/coupons")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public List<PromotionDTO> myCoupons(Authentication auth) {
        Long userId = userRepo.findByUsername(auth.getName()).orElseThrow().getId();

        return couponRepo.findByUserId(userId).stream()
                .map(coupon -> {
                    // coupon.getPromotionId() trả về id của Promotions
                    Promotions promo = promoRepo.findById(coupon.getPromotionId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    return promoMapper.toDTO(promo);
                })
                .collect(Collectors.toList());
    }

}
