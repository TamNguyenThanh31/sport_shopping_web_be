package com.runner.shopping.repository;

import com.runner.shopping.entity.StravaCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StravaCouponRepository extends JpaRepository<StravaCoupon, Long> {
//    @Query("SELECT COALESCE(SUM(c.usedKm),0) FROM StravaCoupon c WHERE c.userId = :userId")
//    int sumUsedKmByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.usedMeters),0) FROM StravaCoupon c WHERE c.userId = :userId")
    int sumUsedMetersByUser(@Param("userId") Long userId);

    /** Lấy toàn bộ records StravaCoupon của 1 user */
    List<StravaCoupon> findByUserId(Long userId);
}
