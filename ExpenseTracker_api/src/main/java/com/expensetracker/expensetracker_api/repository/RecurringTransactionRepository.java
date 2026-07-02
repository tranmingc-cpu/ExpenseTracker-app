package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.RecurringTransactionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, Long> {

    List<RecurringTransactionEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select recurring from RecurringTransactionEntity recurring where recurring.id = :id")
    Optional<RecurringTransactionEntity> findByIdForUpdate(@Param("id") Long id);
}
