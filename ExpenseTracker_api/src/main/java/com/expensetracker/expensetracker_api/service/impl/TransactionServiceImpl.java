package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.TransactionRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;
import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import com.expensetracker.expensetracker_api.entity.TransactionEntity;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.CategoryRepository;
import com.expensetracker.expensetracker_api.repository.TransactionRepository;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @CacheEvict(value = "budgetAnalysis", allEntries = true)
    public TransactionResponse create(TransactionRequest request) {

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        TransactionEntity transaction = new TransactionEntity();

        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setType(request.getType());
        transaction.setUser(user);
        transaction.setCategory(category);

        transaction = transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    @Override
    public TransactionResponse getById(Long id) {

        TransactionEntity transaction = transactionRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Transaction not found with id: " + id));
        return mapToResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getByUser(Long userId, Integer month, Integer year) {
        if (month != null && year != null) {
            java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
            java.time.LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            java.time.LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            return transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end)
                    .stream().map(this::mapToResponse).toList();
        }
        return transactionRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Override
    @CacheEvict(value = "budgetAnalysis", allEntries = true)
    public TransactionResponse update(Long id, TransactionRequest request) {

        TransactionEntity transaction = transactionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setType(request.getType());

        transaction = transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }

    @Override
    @CacheEvict(value = "budgetAnalysis", allEntries = true)
    public void delete(Long id) {

        TransactionEntity transaction = transactionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        transactionRepository.delete(transaction);    }
    private TransactionResponse mapToResponse(TransactionEntity transaction) {

        TransactionResponse response = new TransactionResponse();

        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setType(transaction.getType());
        if (transaction.getCategory() != null) {
            response.setCategoryName(transaction.getCategory().getName());
        }

        return response;
    }

    @Override
    public List<TransactionResponse> syncMomo(com.expensetracker.expensetracker_api.dto.request.SyncRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        List<CategoryEntity> categories = categoryRepository.findAll();
        CategoryEntity incomeCat = categories.stream().filter(c -> "Thu nhập".equalsIgnoreCase(c.getName())).findFirst().orElse(categories.get(0));
        CategoryEntity expenseCat = categories.stream().filter(c -> "Khác".equalsIgnoreCase(c.getName())).findFirst().orElse(categories.get(0));

        TransactionEntity t1 = new TransactionEntity();
        t1.setAmount(new java.math.BigDecimal("500000"));
        t1.setDescription("Hoàn tiền từ MoMo");
        t1.setTransactionDate(java.time.LocalDateTime.now());
        t1.setType("INCOME");
        t1.setUser(user);
        t1.setCategory(incomeCat);

        TransactionEntity t2 = new TransactionEntity();
        t2.setAmount(new java.math.BigDecimal("120000"));
        t2.setDescription("Thanh toán hóa đơn qua MoMo");
        t2.setTransactionDate(java.time.LocalDateTime.now());
        t2.setType("EXPENSE");
        t2.setUser(user);
        t2.setCategory(expenseCat);

        transactionRepository.save(t1);
        transactionRepository.save(t2);

        return java.util.Arrays.asList(mapToResponse(t1), mapToResponse(t2));
    }
}