package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByUserId(Long userId);
    List<TransactionEntity> findByUserIdAndTransactionDateBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Query(value = "SELECT TO_CHAR(t.transaction_date, 'YYYY-MM') as month, " +
            "SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END) as income, " +
            "SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END) as expense " +
            "FROM transactions t " +
            "WHERE t.user_id = :userId " +
            "GROUP BY TO_CHAR(t.transaction_date, 'YYYY-MM') " +
            "ORDER BY TO_CHAR(t.transaction_date, 'YYYY-MM') ASC", nativeQuery = true)
    List<Object[]> getMonthlyOverview(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT c.name as categoryName, " +
            "SUM(t.amount) as amount, " +
            "t.type as type " +
            "FROM transactions t " +
            "JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = :userId AND t.type = :type " +
            "GROUP BY c.name, t.type " +
            "ORDER BY amount DESC", nativeQuery = true)
    List<Object[]> getCategoryDistribution(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("type") String type);


}