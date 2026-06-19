package com.expensetracker.expensetracker_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryReportResponse {

    private String categoryName;

    private BigDecimal amount;
}