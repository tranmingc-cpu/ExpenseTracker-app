package com.expensetracker.expensetracker_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryReportDTO {
    private String categoryName;
    private BigDecimal amount;
    private double percentage;
    private String type; // INCOME or EXPENSE
}
