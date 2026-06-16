package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.SavingGoalRequest;
import com.expensetracker.expensetracker_api.entity.SavingGoalEntity;
import java.util.List;

public interface SavingGoalService {
    SavingGoalEntity createSavingGoal(SavingGoalRequest request);
    SavingGoalEntity getSavingGoal(Long id);
    List<SavingGoalEntity> getSavingGoalsByUser(Long userId);
    SavingGoalEntity updateSavingGoal(Long id, SavingGoalRequest request);
    void deleteSavingGoal(Long id);
}
