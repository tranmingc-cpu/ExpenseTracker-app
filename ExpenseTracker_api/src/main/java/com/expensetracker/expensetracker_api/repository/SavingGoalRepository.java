package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.SavingGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingGoalRepository extends JpaRepository<SavingGoalEntity, Long> {
    List<SavingGoalEntity> findByUserId(Long userId);
}
