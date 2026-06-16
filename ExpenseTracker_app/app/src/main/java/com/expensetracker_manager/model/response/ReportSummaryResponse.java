package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class ReportSummaryResponse {

    @SerializedName("totalIncome")
    private double totalIncome;

    @SerializedName("totalExpense")
    private double totalExpense;

    @SerializedName("balance")
    private double balance;

    @SerializedName("currentBalance")
    private double currentBalance;

    public ReportSummaryResponse() {}

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }
}
