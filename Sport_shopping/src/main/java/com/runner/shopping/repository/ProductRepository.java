package com.runner.shopping.repository;

import com.runner.shopping.entity.Product;
import com.runner.shopping.model.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.deleted = 0 AND p.active = true")
    Page<Product> findAllNotDeleted(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (p.name LIKE %:keyword% OR p.brand LIKE %:keyword%) AND p.deleted = 0")
    Page<Product> searchByNameOrBrand(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = 0")
    Optional<Product> findByIdNotDeleted(@Param("id") Long id);

//    Tìm sản phẩm, sử dụng trong report, sản phẩm -> biển thể
    @Query("SELECT new com.runner.shopping.model.dto.ProductDTO(p.id, p.name) " +
            "FROM Product p " +
            "WHERE p.deleted = 0")
    List<ProductDTO> findAllActiveProducts();

}