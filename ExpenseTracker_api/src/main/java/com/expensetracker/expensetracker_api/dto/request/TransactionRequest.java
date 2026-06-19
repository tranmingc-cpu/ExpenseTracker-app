package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    @NotBlank(message = "Transaction type is required")
    private String type;

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}