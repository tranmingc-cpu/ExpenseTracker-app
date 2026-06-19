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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
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
    public List<TransactionResponse> getByUser(
            Long userId) {

        return transactionRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Override
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
}