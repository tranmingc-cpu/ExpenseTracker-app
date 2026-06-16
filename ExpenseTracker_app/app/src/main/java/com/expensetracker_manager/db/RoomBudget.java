package com.expensetracker_manager.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class RoomBudget {
    @PrimaryKey
    private long id;
    
    private double amount;
    private int month;
    private int year;
    private String categoryName;
    private double spent;

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

    public double getSpent() { return spent; }
    public void setSpent(double spent) { this.spent = spent; }
}
