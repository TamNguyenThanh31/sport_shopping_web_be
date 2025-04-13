package com.runner.shopping.repository;

import com.runner.shopping.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.deleted = 0")
    List<ProductVariant> findByProductIdNotDeleted(Long productId);
}
