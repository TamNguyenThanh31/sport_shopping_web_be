package com.runner.shopping.repository;

import com.runner.shopping.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Products, Long> {
    List<Products> findByCategoryId(Long categoryId);
    List<Products> findByIsActiveTrue();
}