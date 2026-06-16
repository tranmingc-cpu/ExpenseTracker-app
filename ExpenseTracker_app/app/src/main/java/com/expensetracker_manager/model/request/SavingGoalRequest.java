package com.expensetracker_manager.model.request;

import java.math.BigDecimal;

public class SavingGoalRequest {
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private Long userId;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
