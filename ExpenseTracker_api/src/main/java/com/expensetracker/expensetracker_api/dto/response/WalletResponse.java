package com.expensetracker.expensetracker_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletResponse {

    private Long id;
    private String name;
    private String type;
    private BigDecimal balance;
    private String description;
    private Long userId;
    private String userName;
}