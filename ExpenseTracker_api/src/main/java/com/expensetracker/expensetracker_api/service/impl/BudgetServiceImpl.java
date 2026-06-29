package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.BudgetRequest;
import com.expensetracker.expensetracker_api.dto.response.BudgetResponse;
import com.expensetracker.expensetracker_api.entity.BudgetEntity;
import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.BudgetRepository;
import com.expensetracker.expensetracker_api.repository.CategoryRepository;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.repository.TransactionRepository;
import com.expensetracker.expensetracker_api.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public BudgetResponse create(BudgetRequest request) {
        // Tìm user theo id
        UserEntity user = userRepository.findById(request.getUserId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        // Tìm category theo id
        CategoryEntity category =
                categoryRepository.findById(request.getCategoryId()).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"));
        BudgetEntity budget = new BudgetEntity();
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        // Gán quan hệ
        budget.setUser(user);
        budget.setCategory(category);

        // Lưu xuống database
        budget = budgetRepository.save(budget);

        return mapToResponse(budget);
    }

    @Override
    public BudgetResponse getById(Long id) {

        BudgetEntity budget = budgetRepository.findById(id).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Budget not found with id: " + id));
        return mapToResponse(budget);
    }

    @Override
    public List<BudgetResponse> getByUser(Long userId) {

        return budgetRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public BudgetResponse update(Long id, BudgetRequest request) {

        BudgetEntity budget = budgetRepository.findById(id).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Budget not found with id: " + id));
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());

        budget = budgetRepository.save(budget);

        return mapToResponse(budget);
    }

    @Override
    public void delete(Long id) {

        budgetRepository.deleteById(id);
    }

    private BudgetResponse mapToResponse(BudgetEntity budget) {

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setAmount(budget.getAmount());
        response.setMonth(budget.getMonth());
        response.setYear(budget.getYear());

        if (budget.getCategory() != null) {
            response.setCategoryName(
                    budget.getCategory().getName());

            // Tính toán chi tiêu hàng tháng theo danh mục
            try {
                java.time.LocalDate startLocalDate = java.time.LocalDate.of(budget.getYear(), budget.getMonth(), 1);
                java.time.LocalDate endLocalDate = startLocalDate.plusMonths(1).minusDays(1);
                java.time.LocalDateTime start = startLocalDate.atStartOfDay();
                java.time.LocalDateTime end = endLocalDate.atTime(23, 59, 59);

                List<com.expensetracker.expensetracker_api.entity.TransactionEntity> txs =
                        transactionRepository.findByUserIdAndTransactionDateBetween(budget.getUser().getId(), start, end);

                java.math.BigDecimal spent = java.math.BigDecimal.ZERO;
                for (com.expensetracker.expensetracker_api.entity.TransactionEntity tx : txs) {
                    if ("EXPENSE".equalsIgnoreCase(tx.getType()) && tx.getCategory() != null
                            && tx.getCategory().getId().equals(budget.getCategory().getId())) {
                        spent = spent.add(tx.getAmount());
                    }
                }
                response.setSpent(spent);
            } catch (Exception e) {
                response.setSpent(java.math.BigDecimal.ZERO);
            }
        } else {
            response.setSpent(java.math.BigDecimal.ZERO);
        }

        return response;
    }
}