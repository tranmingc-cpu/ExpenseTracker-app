package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.response.AiBudgetAnalysisDTO;
import com.expensetracker.expensetracker_api.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiService geminiService;

    @GetMapping("/budget-analysis")
    public AiBudgetAnalysisDTO getBudgetAnalysis(@RequestParam Long userId) {
        return geminiService.analyzeBudget(userId);
    }
}
