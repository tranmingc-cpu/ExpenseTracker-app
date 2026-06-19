package com.expensetracker.expensetracker_api.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {

    private Long id;

    private BigDecimal amount;

    private String description;

    private LocalDateTime transactionDate;

    private String type;

    private String categoryName;

}