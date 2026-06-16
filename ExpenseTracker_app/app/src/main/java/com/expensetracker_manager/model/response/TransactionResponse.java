package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class TransactionResponse {

    @SerializedName("id")
    private long id;

    @SerializedName("amount")
    private double amount;

    @SerializedName("description")
    private String description;

    @SerializedName("transactionDate")
    private String transactionDate;

    @SerializedName("type")
    private String type;

    @SerializedName("categoryName")
    private String categoryName;

    public TransactionResponse() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
