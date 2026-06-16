package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.RecurringTransactionRequest;
import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import com.expensetracker.expensetracker_api.entity.RecurringTransactionEntity;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.CategoryRepository;
import com.expensetracker.expensetracker_api.repository.RecurringTransactionRepository;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public RecurringTransactionEntity create(RecurringTransactionRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CategoryEntity category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        RecurringTransactionEntity entity = new RecurringTransactionEntity();
        entity.setAmount(request.getAmount());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setFrequency(request.getFrequency());
        entity.setNextExecutionDate(request.getNextExecutionDate());
        entity.setUser(user);
        entity.setCategory(category);

        return recurringTransactionRepository.save(entity);
    }

    @Override
    public RecurringTransactionEntity getById(Long id) {
        return recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found with id: " + id));
    }

    @Override
    public List<RecurringTransactionEntity> getByUser(Long userId) {
        return recurringTransactionRepository.findByUserId(userId);
    }

    @Override
    public RecurringTransactionEntity update(Long id, RecurringTransactionRequest request) {
        RecurringTransactionEntity entity = getById(id);
        entity.setAmount(request.getAmount());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setFrequency(request.getFrequency());
        entity.setNextExecutionDate(request.getNextExecutionDate());

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            entity.setCategory(category);
        } else {
            entity.setCategory(null);
        }

        return recurringTransactionRepository.save(entity);
    }

    @Override
    public void delete(Long id) {
        recurringTransactionRepository.deleteById(id);
    }
}
