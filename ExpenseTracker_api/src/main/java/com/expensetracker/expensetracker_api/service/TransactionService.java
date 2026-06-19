package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.TransactionRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse create(TransactionRequest request);

    TransactionResponse getById(Long id);

    List<TransactionResponse> getByUser(Long userId);

    TransactionResponse update(Long id, TransactionRequest request);

    void delete(Long id);
}