package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.response.CategoryReportResponse;
import com.expensetracker.expensetracker_api.dto.response.ReportSummaryRes;
import com.expensetracker.expensetracker_api.entity.TransactionEntity;
import com.expensetracker.expensetracker_api.repository.TransactionRepository;
import com.expensetracker.expensetracker_api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;

    @Override
    public ReportSummaryRes getSummary(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(23,59,59);

        List<TransactionEntity> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (TransactionEntity transaction : transactions) {

            if ("INCOME".equalsIgnoreCase(
                    transaction.getType())) {
                income = income.add(transaction.getAmount());

            } else {
                expense = expense.add(transaction.getAmount());
            }
        }
        ReportSummaryRes response = new ReportSummaryRes();
        response.setTotalIncome(income);
        response.setTotalExpense(expense);
        response.setBalance(
                income.subtract(expense));

        List<TransactionEntity> allTransactions = transactionRepository.findByUserId(userId);
        BigDecimal allIncome = BigDecimal.ZERO;
        BigDecimal allExpense = BigDecimal.ZERO;
        for (TransactionEntity tx : allTransactions) {
            if ("INCOME".equalsIgnoreCase(tx.getType())) {
                allIncome = allIncome.add(tx.getAmount());
            } else {
                allExpense = allExpense.add(tx.getAmount());
            }
        }
        response.setCurrentBalance(allIncome.subtract(allExpense));

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = expense.doubleValue() / income.doubleValue() * 100;
            if (ratio > 80) {
                response.setBudgetWarningMessage(String.format(Locale.US, "Cảnh báo: Bạn đã chi tiêu %.1f%% thu nhập tháng này.", ratio));
            } else {
                response.setBudgetWarningMessage(String.format(Locale.US, "Tốt: Chi tiêu của bạn đang ở mức an toàn (%.1f%% thu nhập).", ratio));
            }
        } else {
            response.setBudgetWarningMessage("Chưa đủ dữ liệu để đánh giá ngân sách.");
        }

        return response;
    }

    @Override
    public List<CategoryReportResponse>
    getExpenseByCategory(Long userId, LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();

        LocalDateTime end = endDate.atTime(23,59,59);

        List<TransactionEntity> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);

        Map<String, BigDecimal> map = new HashMap<>();

        for (TransactionEntity transaction :
                transactions) {

            if ("EXPENSE".equalsIgnoreCase(
                    transaction.getType())) {

                String category = transaction.getCategory().getName();

                map.put(category, map.getOrDefault(category,BigDecimal.ZERO).add(transaction.getAmount())
                );
            }
        }
        List<CategoryReportResponse> result = new ArrayList<>();
        map.forEach((category, amount) -> result.add(new CategoryReportResponse(category, amount)));
        return result;
    }
}