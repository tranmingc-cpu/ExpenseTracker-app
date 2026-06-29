package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.response.AiBudgetAnalysisDTO;

public interface GeminiService {
    AiBudgetAnalysisDTO analyzeBudget(Long userId);
}
