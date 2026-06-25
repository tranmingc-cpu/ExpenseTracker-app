package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class FinancialOverviewResponse {

    @SerializedName("month")
    private String month;

    @SerializedName("totalIncome")
    private double totalIncome;

    @SerializedName("totalExpense")
    private double totalExpense;

    @SerializedName("balance")
    private double balance;

    public FinancialOverviewResponse() {}

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
