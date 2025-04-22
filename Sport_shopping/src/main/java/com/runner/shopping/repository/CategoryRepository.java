package com.runner.shopping.repository;

import com.runner.shopping.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.deleted = 0")
    List<Category> findAllNotDeleted();

    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% AND c.deleted = 0")
    List<Category> searchByName(String keyword);
}
