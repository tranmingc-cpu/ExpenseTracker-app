package com.expensetracker_manager.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "report_summary")
public class RoomReportSummary {
    @PrimaryKey
    private int id = 1;
    
    private double totalIncome;
    private double totalExpense;
    private double balance;
    private double currentBalance;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }
}
