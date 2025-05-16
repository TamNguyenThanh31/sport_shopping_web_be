package com.runner.shopping.repository;

import com.runner.shopping.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.deleted = 0")
    List<ProductVariant> findByProductIdNotDeleted(@Param("productId") Long productId);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId IN :productIds AND pv.deleted = 0")
    List<ProductVariant> findByProductIdsNotDeleted(@Param("productIds") List<Long> productIds);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId")
    List<ProductVariant> findByProductId(@Param("productId") Long productId);

    // Khoá Lock này giúp tình trạng nhiều nguười dùng cùng mua một sản phẩm gây xung đột dữ liệu
    // -> Khoá Lock này có nhiệm vụ ngăn xung đột như vậy (cập nhật số hàng trong kho)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id AND pv.deleted = 0")
    Optional<ProductVariant> findByIdNotDeleted(@Param("id") Long id);
}
