package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01",
            message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Month is required")
    private Integer month;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}