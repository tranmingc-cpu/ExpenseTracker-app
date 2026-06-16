package com.expensetracker_manager.model.request;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class RecurringTransactionRequest {
    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    @SerializedName("frequency")
    private String frequency;

    @SerializedName("nextExecutionDate")
    private String nextExecutionDate;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("categoryId")
    private Long categoryId;

    // Getters and Setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getNextExecutionDate() { return nextExecutionDate; }
    public void setNextExecutionDate(String nextExecutionDate) { this.nextExecutionDate = nextExecutionDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
