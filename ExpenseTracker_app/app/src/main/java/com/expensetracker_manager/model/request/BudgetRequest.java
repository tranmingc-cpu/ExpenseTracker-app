package com.expensetracker_manager.model.request;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class BudgetRequest {

    @SerializedName("amount")
    private double amount;

    @SerializedName("month")
    private int month;

    @SerializedName("year")
    private int year;

    @SerializedName("userId")
    private long userId;

    @SerializedName("categoryId")
    private long categoryId;

    public BudgetRequest() {}

    public BudgetRequest(double amount, int month, int year, long userId, long categoryId) {
        this.amount = amount;
        this.month = month;
        this.year = year;
        this.userId = userId;
        this.categoryId = categoryId;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }


}
