package com.expensetracker_manager.model.response;

import java.math.BigDecimal;

public class SavingGoalResponse {
    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private String targetDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }

    private boolean completed;
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
