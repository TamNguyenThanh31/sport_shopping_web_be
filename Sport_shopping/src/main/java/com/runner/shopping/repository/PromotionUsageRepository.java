package com.runner.shopping.repository;

import com.runner.shopping.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    boolean existsByPromotionIdAndUserId(Long promotionId, Long userId);

    long countByPromotionId(Long promotionId);

    void deleteByOrderId(Long orderId);
}
