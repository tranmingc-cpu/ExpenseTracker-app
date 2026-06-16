package com.expensetracker_manager.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saving_goals")
public class RoomSavingGoal {
    @PrimaryKey
    private long id;
    
    private String name;
    private double targetAmount;
    private double currentAmount;
    private String targetDate;
    private boolean completed;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
