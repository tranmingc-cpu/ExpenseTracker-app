package com.expensetracker.expensetracker_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recurring_transactions")
public class RecurringTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String type; // INCOME, EXPENSE

    @Column(nullable = false)
    private String frequency;

    @Column(nullable = false)
    private LocalDateTime nextExecutionDate;

    @Column(name = "last_paid_year")
    private Integer lastPaidYear;

    @Column(name = "last_paid_month")
    private Integer lastPaidMonth;

    @Column(name = "last_paid_at")
    private LocalDateTime lastPaidAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private CategoryEntity category;
}
