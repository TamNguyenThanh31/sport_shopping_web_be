package com.runner.shopping.repository;

import com.runner.shopping.entity.Promotions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotions, Long> {

    Optional<Promotions> findByIdAndIsActiveTrue(Long id);

    boolean existsByCode(String code);

    @Query(value = "SELECT * FROM Promotions p WHERE " +
            "(:code IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
            "(:isActive IS NULL OR p.is_active = :isActive) AND " +
            "(:dateFrom IS NULL OR :dateTo IS NULL OR " +
            "(p.start_date <= :dateTo AND p.end_date >= :dateFrom))",
            countQuery = "SELECT COUNT(*) FROM Promotions p WHERE " +
                    "(:code IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
                    "(:isActive IS NULL OR p.is_active = :isActive) AND " +
                    "(:dateFrom IS NULL OR :dateTo IS NULL OR " +
                    "(p.start_date <= :dateTo AND p.end_date >= :dateFrom))",
            nativeQuery = true)
    Page<Promotions> findPromotions(
            @Param("code") String code,
            @Param("isActive") Boolean isActive,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    @Query(
            value =
                    "SELECT p.* " +
                            "  FROM promotions p " +
                            "  LEFT JOIN strava_coupon sc   " +
                            "    ON sc.promotion_id = p.id  " +
                            "   AND sc.user_id      = :customerId     -- chỉ join coupon của đúng user  \n" +
                            " WHERE (sc.user_id = :customerId                                          \n" +
                            "        OR NOT EXISTS (                                                    \n" +
                            "             SELECT 1                                                     \n" +
                            "               FROM strava_coupon sc2                                     \n" +
                            "              WHERE sc2.promotion_id = p.id                               \n" +
                            "        )                                                                  \n" +
                            "       )                                                                  \n" +
                            "   AND (:code     IS NULL OR LOWER(p.code)   LIKE LOWER(CONCAT('%',:code,'%')))  \n" +
                            "   AND (:isActive IS NULL OR p.is_active     = :isActive)                       \n" +
                            "   AND (:dateFrom IS NULL OR :dateTo IS NULL                                   \n" +
                            "        OR (p.start_date <= :dateTo AND p.end_date >= :dateFrom))",
            nativeQuery = true
    )
    Page<Promotions> findPromotionsForCustomerWithCoupon(
            @Param("customerId") Long customerId,
            @Param("code")       String code,
            @Param("isActive")   Boolean isActive,
            @Param("dateFrom")   LocalDateTime dateFrom,
            @Param("dateTo")     LocalDateTime dateTo,
            Pageable pageable
    );
}
