package com.expensetracker.expensetracker_api.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportSummaryRes {

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal balance;

    private BigDecimal currentBalance;
}