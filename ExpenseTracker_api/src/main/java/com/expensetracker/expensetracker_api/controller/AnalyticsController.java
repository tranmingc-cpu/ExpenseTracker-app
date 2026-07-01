package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.response.CategoryReportDTO;
import com.expensetracker.expensetracker_api.dto.response.FinancialOverviewDTO;
import com.expensetracker.expensetracker_api.entity.TransactionEntity;
import com.expensetracker.expensetracker_api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/overview")
    public List<FinancialOverviewDTO> getOverview(@RequestParam Long userId) {
        List<TransactionEntity> transactions = transactionRepository.findByUserId(userId);
        
        Map<String, List<TransactionEntity>> groupedByMonth = transactions.stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                ));

        List<FinancialOverviewDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<TransactionEntity>> entry : groupedByMonth.entrySet()) {
            String month = entry.getKey();
            BigDecimal income = entry.getValue().stream()
                    .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                    .map(TransactionEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = entry.getValue().stream()
                    .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                    .map(TransactionEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = income.subtract(expense);
            result.add(new FinancialOverviewDTO(month, income, expense, balance));
        }

        result.sort(Comparator.comparing(FinancialOverviewDTO::getMonth));
        return result;
    }

    @GetMapping("/categories")
    public List<CategoryReportDTO> getCategories(@RequestParam Long userId, @RequestParam String type, @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        List<TransactionEntity> transactions;
        if (month != null && year != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
        } else {
            transactions = transactionRepository.findByUserId(userId);
        }
        
        transactions = transactions.stream()
                .filter(t -> type.equalsIgnoreCase(t.getType()) && t.getCategory() != null)
                .collect(Collectors.toList());

        Map<String, BigDecimal> groupedByCategory = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, TransactionEntity::getAmount, BigDecimal::add)
                ));

        BigDecimal total = groupedByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryReportDTO> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : groupedByCategory.entrySet()) {
            String categoryName = entry.getKey();
            BigDecimal amount = entry.getValue();
            double percentage = 0.0;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(new BigDecimal("100"))
                        .divide(total, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            result.add(new CategoryReportDTO(categoryName, amount, percentage, type));
        }

        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return result;
    }
}
