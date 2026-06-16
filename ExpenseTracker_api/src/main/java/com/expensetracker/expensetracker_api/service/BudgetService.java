package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.BudgetRequest;
import com.expensetracker.expensetracker_api.dto.response.BudgetResponse;

import java.util.List;

public interface BudgetService {

    BudgetResponse create(BudgetRequest request);

    BudgetResponse getById(Long id);

    List<BudgetResponse> getByUser(Long userId);

    BudgetResponse update(Long id,
                          BudgetRequest request);

    void delete(Long id);
}