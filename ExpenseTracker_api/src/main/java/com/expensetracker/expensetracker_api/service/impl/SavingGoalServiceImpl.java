package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.SavingGoalRequest;
import com.expensetracker.expensetracker_api.entity.SavingGoalEntity;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.SavingGoalRepository;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.service.SavingGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingGoalServiceImpl implements SavingGoalService {

    private final SavingGoalRepository savingGoalRepository;
    private final UserRepository userRepository;

    @Override
    public SavingGoalEntity createSavingGoal(SavingGoalRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SavingGoalEntity goal = new SavingGoalEntity();
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount() != null ? request.getCurrentAmount() : java.math.BigDecimal.ZERO);
        goal.setTargetDate(request.getTargetDate());
        goal.setUser(user);
        goal.setCompleted(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0);

        return savingGoalRepository.save(goal);
    }

    @Override
    public SavingGoalEntity getSavingGoal(Long id) {
        return savingGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saving goal not found with id: " + id));
    }

    @Override
    public List<SavingGoalEntity> getSavingGoalsByUser(Long userId) {
        return savingGoalRepository.findByUserId(userId);
    }

    @Override
    public SavingGoalEntity updateSavingGoal(Long id, SavingGoalRequest request) {
        SavingGoalEntity goal = getSavingGoal(id);
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }
        goal.setTargetDate(request.getTargetDate());
        goal.setCompleted(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0);
        return savingGoalRepository.save(goal);
    }

    @Override
    public void deleteSavingGoal(Long id) {
        savingGoalRepository.deleteById(id);
    }
}
