package com.expensetracker_manager.model.request;

import com.google.gson.annotations.SerializedName;

public class TransactionRequest {

    @SerializedName("amount")
    private double amount;

    @SerializedName("description")
    private String description;

    @SerializedName("transactionDate")
    private String transactionDate;

    @SerializedName("type")
    private String type;

    @SerializedName("userId")
    private long userId;

    @SerializedName("categoryId")
    private long categoryId;

    public TransactionRequest() {}

    public TransactionRequest(double amount, String description, String transactionDate,
                              String type, long userId, long categoryId) {
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.type = type;
        this.userId = userId;
        this.categoryId = categoryId;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
}
