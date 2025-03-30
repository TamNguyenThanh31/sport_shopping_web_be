package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Products;
import com.runner.shopping.repository.ProductRepository;
import com.runner.shopping.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductsServiceImpl implements ProductsService {
    @Autowired
    private ProductRepository productsRepository;

    @Override
    public Products createProduct(Products product) {
        return productsRepository.save(product);
    }

    @Override
    public List<Products> getProductsByCategory(Long categoryId) {
        return productsRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Products> getActiveProducts() {
        return productsRepository.findByIsActiveTrue();
    }
}
