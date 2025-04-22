package com.runner.shopping.repository;

import com.runner.shopping.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.deleted = 0")
    List<Product> findAllNotDeleted();

    @Query("SELECT p FROM Product p WHERE (p.name LIKE %:keyword% OR p.brand LIKE %:keyword%) AND p.deleted = 0")
    List<Product> searchByNameOrBrand(String keyword);
}