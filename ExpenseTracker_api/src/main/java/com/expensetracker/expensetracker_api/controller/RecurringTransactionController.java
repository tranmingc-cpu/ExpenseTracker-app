package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.RecurringTransactionRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;
import com.expensetracker.expensetracker_api.entity.RecurringTransactionEntity;
import com.expensetracker.expensetracker_api.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    public RecurringTransactionEntity create(@Valid @RequestBody RecurringTransactionRequest request) {
        return recurringTransactionService.create(request);
    }

    @GetMapping("/{id}")
    public RecurringTransactionEntity getById(@PathVariable Long id) {
        return recurringTransactionService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<RecurringTransactionEntity> getByUser(@PathVariable Long userId) {
        return recurringTransactionService.getByUser(userId);
    }

    @PutMapping("/{id}")
    public RecurringTransactionEntity update(
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request
    ) {
        return recurringTransactionService.update(id, request);
    }

    @PostMapping("/{id}/pay")
    public TransactionResponse pay(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam Long categoryId
    ) {
        return recurringTransactionService.pay(id, userId, categoryId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        recurringTransactionService.delete(id);
    }
}
