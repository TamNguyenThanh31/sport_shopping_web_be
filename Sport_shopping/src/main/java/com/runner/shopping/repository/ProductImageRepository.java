package com.runner.shopping.repository;

import com.runner.shopping.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.deleted = 0")
    List<ProductImage> findByProductIdNotDeleted(Long productId);

    @Query("SELECT i FROM ProductImage i WHERE i.productId IN :productIds AND i.deleted = 0")
    List<ProductImage> findByProductIdInNotDeleted(@Param("productIds") List<Long> productIds);
}
