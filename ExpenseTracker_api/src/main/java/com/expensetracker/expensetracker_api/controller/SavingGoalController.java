package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.SavingGoalRequest;
import com.expensetracker.expensetracker_api.entity.SavingGoalEntity;
import com.expensetracker.expensetracker_api.service.SavingGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/saving-goals")
@RequiredArgsConstructor
public class SavingGoalController {

    private final SavingGoalService savingGoalService;

    @PostMapping
    public SavingGoalEntity createSavingGoal(@Valid @RequestBody SavingGoalRequest request) {
        return savingGoalService.createSavingGoal(request);
    }

    @GetMapping("/{id}")
    public SavingGoalEntity getSavingGoal(@PathVariable Long id) {
        return savingGoalService.getSavingGoal(id);
    }

    @GetMapping("/user/{userId}")
    public List<SavingGoalEntity> getSavingGoalsByUser(@PathVariable Long userId) {
        return savingGoalService.getSavingGoalsByUser(userId);
    }

    @PutMapping("/{id}")
    public SavingGoalEntity updateSavingGoal(@PathVariable Long id, @Valid @RequestBody SavingGoalRequest request) {
        return savingGoalService.updateSavingGoal(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteSavingGoal(@PathVariable Long id) {
        savingGoalService.deleteSavingGoal(id);
    }
}
