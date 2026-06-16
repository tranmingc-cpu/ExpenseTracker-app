package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class BudgetResponse {

    @SerializedName("id")
    private long id;

    @SerializedName("amount")
    private double amount;

    @SerializedName("month")
    private int month;

    @SerializedName("year")
    private int year;

    @SerializedName("categoryName")
    private String categoryName;

    public BudgetResponse() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    @SerializedName("spent")
    private double spent;
    public double getSpent() { return spent; }
    public void setSpent(double spent) { this.spent = spent; }
}
