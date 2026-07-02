package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.RecurringTransactionRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;
import com.expensetracker.expensetracker_api.entity.RecurringTransactionEntity;

import java.util.List;

public interface RecurringTransactionService {

    RecurringTransactionEntity create(RecurringTransactionRequest request);

    RecurringTransactionEntity getById(Long id);

    List<RecurringTransactionEntity> getByUser(Long userId);

    RecurringTransactionEntity update(Long id, RecurringTransactionRequest request);

    TransactionResponse pay(Long id, Long userId, Long categoryId);

    void delete(Long id);
}
