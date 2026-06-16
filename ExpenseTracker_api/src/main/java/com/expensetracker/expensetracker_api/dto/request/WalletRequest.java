package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletRequest {
    @NotBlank(message = "Wallet name is required")
    private String name;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00")
    private BigDecimal balance;

    @NotNull(message = "User id is required")
    private Long userId;
    private String description;
    private String type;

}