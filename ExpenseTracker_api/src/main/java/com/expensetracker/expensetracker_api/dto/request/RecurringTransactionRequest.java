package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecurringTransactionRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Description is required")
    private String description;

    @NotNull(message = "Type is required")
    private String type;

    @NotNull(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Next execution date is required")
    private LocalDateTime nextExecutionDate;

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long categoryId;
}
