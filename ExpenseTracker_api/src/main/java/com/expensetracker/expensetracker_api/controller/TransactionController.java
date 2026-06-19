package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.TransactionRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;
import com.expensetracker.expensetracker_api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public TransactionResponse create(
         @Valid @RequestBody TransactionRequest request) {

        return transactionService.create(request);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(
            @PathVariable Long id) {

        return transactionService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> getByUser(
            @PathVariable Long userId) {

        return transactionService.getByUser(userId);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable Long id,
          @Valid  @RequestBody TransactionRequest request) {

        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id) {

        transactionService.delete(id);
    }
}