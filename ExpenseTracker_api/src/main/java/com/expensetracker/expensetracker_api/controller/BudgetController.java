package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.BudgetRequest;
import com.expensetracker.expensetracker_api.dto.response.BudgetResponse;
import com.expensetracker.expensetracker_api.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public BudgetResponse create(
            @RequestBody BudgetRequest request) {

        return budgetService.create(request);
    }

    @GetMapping("/{id}")
    public BudgetResponse getById(
            @PathVariable Long id) {

        return budgetService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<BudgetResponse> getByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        return budgetService.getByUser(userId, month, year);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(
            @PathVariable Long id,
            @RequestBody BudgetRequest request) {

        return budgetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id) {

        budgetService.delete(id);
    }
}