package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetRepository
        extends JpaRepository<BudgetEntity, Long> {

    List<BudgetEntity> findByUserId(Long userId);
}