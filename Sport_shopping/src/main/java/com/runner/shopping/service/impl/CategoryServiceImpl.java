package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Category;
import com.runner.shopping.model.dto.CategoryDTO;
import com.runner.shopping.repository.CategoryRepository;
import com.runner.shopping.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setDeleted(0);
        category = categoryRepository.save(category);
        categoryDTO.setId(category.getId());
        return categoryDTO;
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllNotDeleted().stream().map(category -> {
            CategoryDTO dto = new CategoryDTO();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setDescription(category.getDescription());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Category not found or deleted"));
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Category not found or deleted"));
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category = categoryRepository.save(category);
        return categoryDTO;
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Category not found or deleted"));
        category.setDeleted(1);
        categoryRepository.save(category);
    }

    @Override
    public List<CategoryDTO> searchCategories(String keyword) {
        return categoryRepository.searchByName(keyword).stream().map(category -> {
            CategoryDTO dto = new CategoryDTO();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setDescription(category.getDescription());
            return dto;
        }).collect(Collectors.toList());
    }
}
