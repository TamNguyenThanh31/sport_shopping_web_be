package com.runner.shopping.service;

import com.runner.shopping.entity.Products;

import java.util.List;

public interface ProductsService {
    Products createProduct(Products product);

    List<Products> getProductsByCategory(Long categoryId);

    List<Products> getActiveProducts();
}
