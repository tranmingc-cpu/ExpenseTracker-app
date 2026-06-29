package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.response.CategoryReportDTO;
import com.expensetracker.expensetracker_api.dto.response.FinancialOverviewDTO;
import com.expensetracker.expensetracker_api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/overview")
    public List<FinancialOverviewDTO> getOverview(@RequestParam Long userId) {
        List<Object[]> rows = transactionRepository.getMonthlyOverview(userId);
        List<FinancialOverviewDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            String month = (String) row[0];
            BigDecimal income = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            BigDecimal expense = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            BigDecimal balance = income.subtract(expense);
            result.add(new FinancialOverviewDTO(month, income, expense, balance));
        }
        return result;
    }

    @GetMapping("/categories")
    public List<CategoryReportDTO> getCategories(@RequestParam Long userId, @RequestParam String type) {
        List<Object[]> rows = transactionRepository.getCategoryDistribution(userId, type);
        List<CategoryReportDTO> result = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        // First pass: calculate total amount for percentage calculation
        for (Object[] row : rows) {
            BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            total = total.add(amount);
        }

        // Second pass: build DTOs with percentages
        for (Object[] row : rows) {
            String categoryName = (String) row[0];
            BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            String tType = (String) row[2];
            double percentage = 0.0;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(new BigDecimal("100"))
                        .divide(total, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            result.add(new CategoryReportDTO(categoryName, amount, percentage, tType));
        }
        return result;
    }
}
