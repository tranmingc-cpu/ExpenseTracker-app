package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class CategoryReportResponse {

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("amount")
    private double amount;

    @SerializedName("percentage")
    private double percentage;

    @SerializedName("type")
    private String type;

    public CategoryReportResponse() {}

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
