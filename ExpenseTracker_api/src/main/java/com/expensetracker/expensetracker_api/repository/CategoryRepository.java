package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository
        extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByUserId(Long userId);
}