package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class CategoryReportResponse {

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("amount")
    private double amount;

    public CategoryReportResponse() {}

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
