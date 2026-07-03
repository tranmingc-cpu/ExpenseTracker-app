package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.RecurringTransactionRequest;
import com.expensetracker.expensetracker_api.dto.request.TransactionRequest;
import com.expensetracker.expensetracker_api.dto.response.TransactionResponse;
import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import com.expensetracker.expensetracker_api.entity.RecurringTransactionEntity;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.BadRequestException;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.CategoryRepository;
import com.expensetracker.expensetracker_api.repository.RecurringTransactionRepository;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.service.RecurringTransactionService;
import com.expensetracker.expensetracker_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recurring transaction not found with id: " + id
                ));
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
    @Transactional
    public TransactionResponse pay(Long id, Long userId, Long categoryId) {
        if (userId == null || categoryId == null) {
            throw new BadRequestException("Thiếu thông tin người dùng hoặc danh mục thanh toán.");
        }

        RecurringTransactionEntity recurring = recurringTransactionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recurring transaction not found with id: " + id
                ));

        if (recurring.getUser() == null || !userId.equals(recurring.getUser().getId())) {
            throw new BadRequestException("Bạn không có quyền thanh toán khoản định kỳ này.");
        }

        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);

        if (isPaidInMonth(recurring, currentMonth)) {
            throw new BadRequestException(
                    String.format(
                            "Khoản định kỳ này đã được thanh toán trong tháng %02d/%d.",
                            currentMonth.getMonthValue(),
                            currentMonth.getYear()
                    )
            );
        }

        TransactionRequest request = new TransactionRequest();
        request.setAmount(recurring.getAmount());
        request.setDescription("Thanh toán định kỳ: " + recurring.getDescription());
        request.setTransactionDate(now);
        request.setType(recurring.getType());
        request.setUserId(userId);
        request.setCategoryId(categoryId);

        TransactionResponse response = transactionService.create(request);

        recurring.setLastPaidYear(currentMonth.getYear());
        recurring.setLastPaidMonth(currentMonth.getMonthValue());
        recurring.setLastPaidAt(now);
        recurring.setNextExecutionDate(calculateNextExecutionDate(
                recurring.getNextExecutionDate(),
                currentMonth,
                now
        ));
        recurringTransactionRepository.save(recurring);

        return response;
    }

    private boolean isPaidInMonth(RecurringTransactionEntity recurring, YearMonth month) {
        return recurring.getLastPaidYear() != null
                && recurring.getLastPaidMonth() != null
                && recurring.getLastPaidYear() == month.getYear()
                && recurring.getLastPaidMonth() == month.getMonthValue();
    }

    private LocalDateTime calculateNextExecutionDate(
            LocalDateTime currentExecutionDate,
            YearMonth paidMonth,
            LocalDateTime fallbackDate
    ) {
        LocalDateTime next = currentExecutionDate == null ? fallbackDate : currentExecutionDate;

        while (!YearMonth.from(next).isAfter(paidMonth)) {
            next = next.plusMonths(1);
        }

        return next;
    }

    @Override
    public void delete(Long id) {
        RecurringTransactionEntity entity = getById(id);
        recurringTransactionRepository.delete(entity);
    }
}
