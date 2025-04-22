package com.runner.shopping.repository;

import com.runner.shopping.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.deleted = 0")
    List<ProductVariant> findByProductIdNotDeleted(Long productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.productId IN :productIds AND v.deleted = 0")
    List<ProductVariant> findByProductIdNotDeleted(@Param("productIds") List<Long> productIds);
}
