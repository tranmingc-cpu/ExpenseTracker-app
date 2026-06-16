package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SavingGoalRequest {
    @NotNull(message = "Name is required")
    private String name;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
    private BigDecimal targetAmount;

    private BigDecimal currentAmount = BigDecimal.ZERO;

    private LocalDateTime targetDate;

    @NotNull(message = "User ID is required")
    private Long userId;
}
