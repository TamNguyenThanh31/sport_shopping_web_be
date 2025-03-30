package com.runner.shopping.service;

import com.runner.shopping.entity.Categories;

import java.util.List;

public interface CategoriesService {
    Categories createCategory(Categories category);

    List<Categories> getAllCategories();

    Categories getCategoryById(Long id);
}
