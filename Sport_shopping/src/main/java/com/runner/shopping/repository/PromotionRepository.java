package com.runner.shopping.repository;

import com.runner.shopping.entity.Promotions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotions, Long> {

    Optional<Promotions> findByIdAndIsActiveTrue(Long id);
}
