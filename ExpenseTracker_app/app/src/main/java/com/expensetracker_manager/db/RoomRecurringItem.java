package com.expensetracker_manager.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_items")
public class RoomRecurringItem {
    @PrimaryKey
    private long id;
    
    private String name;
    private double amount;
    private int day;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
}
