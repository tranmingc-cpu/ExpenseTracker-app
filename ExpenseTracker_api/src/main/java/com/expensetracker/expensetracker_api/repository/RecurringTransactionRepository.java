package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.RecurringTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, Long> {
    List<RecurringTransactionEntity> findByUserId(Long userId);
}
