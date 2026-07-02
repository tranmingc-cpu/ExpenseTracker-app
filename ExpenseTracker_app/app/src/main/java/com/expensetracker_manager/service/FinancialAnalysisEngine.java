package com.expensetracker_manager.service;

import android.content.Context;
import com.expensetracker_manager.model.response.BudgetResponse;
import com.expensetracker_manager.model.response.ReportSummaryResponse;
import com.expensetracker_manager.model.response.SavingGoalResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.utils.OfflineCacheManager;

import java.text.SimpleDateFormat;
import java.util.*;

public class FinancialAnalysisEngine {

    public static class AnalysisResult {
        public double totalIncome;
        public double totalExpense;
        public double currentBalance;
        public double predictedEndOfMonthSpending;
        public double remainingBudget;
        public String financialHealth;
        public int financialHealthScore;
        public String overspendingRisk;
        
        public String primaryGoalName;
        public double goalTarget;
        public double goalCurrent;
        public double goalProgressPct;
        public int remainingDays;
        public String estimatedCompletionDate;

        public Map<String, Double> categorySpending = new HashMap<>();
        public Map<String, Double> categoryBudgets = new HashMap<>();
        public Map<String, Double> categoryRatios = new HashMap<>();
        public Map<String, Double> recommendedBudgets = new HashMap<>();
        public List<String> abnormalExpenses = new ArrayList<>();
        public List<TransactionResponse> recentTransactions = new ArrayList<>();
    }

    public static AnalysisResult analyze(Context context) {
        return analyze(context, null);
    }

    public static AnalysisResult analyze(Context context, Long selectedGoalId) {
        AnalysisResult result = new AnalysisResult();

        OfflineCacheManager cache = OfflineCacheManager.getInstance(context);
        List<TransactionResponse> transactions = cache.getCachedTransactions();
        List<BudgetResponse> budgets = cache.getCachedBudgets();
        List<SavingGoalResponse> goals = cache.getCachedSavingGoals();
        ReportSummaryResponse summary = cache.getCachedReportSummary();

        Calendar cal = Calendar.getInstance();
        int curYear = cal.get(Calendar.YEAR);
        int curMonth = cal.get(Calendar.MONTH) + 1;
        int curDay = cal.get(Calendar.DAY_OF_MONTH);
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String curMonthPrefix = String.format(Locale.US, "%04d-%02d", curYear, curMonth);

        // Calculate Income & Expense from transactions of current month
        double monthIncome = 0;
        double monthExpense = 0;
        double totalBudgetsLimit = 0;

        for (TransactionResponse tr : transactions) {
            if (tr.getTransactionDate() != null && tr.getTransactionDate().startsWith(curMonthPrefix)) {
                if ("INCOME".equalsIgnoreCase(tr.getType())) {
                    monthIncome += tr.getAmount();
                } else if ("EXPENSE".equalsIgnoreCase(tr.getType())) {
                    monthExpense += tr.getAmount();
                    String cat = tr.getCategoryName();
                    if (cat == null || cat.isEmpty()) cat = "Khác";
                    result.categorySpending.put(cat, result.categorySpending.getOrDefault(cat, 0.0) + tr.getAmount());
                }
                result.recentTransactions.add(tr);
            }
        }
        // If no transactions in current month, fallback to report summary totals if they look valid
        if (monthIncome == 0 && summary.getTotalIncome() > 0) {
            monthIncome = summary.getTotalIncome();
        }
        if (monthExpense == 0 && summary.getTotalExpense() > 0) {
            monthExpense = summary.getTotalExpense();
        }
        result.totalIncome = monthIncome;
        result.totalExpense = monthExpense;
        result.currentBalance = monthIncome - monthExpense;

        // Budgets
        for (BudgetResponse b : budgets) {
            String cat = b.getCategoryName();
            if (cat == null || cat.isEmpty()) cat = "Khác";
            result.categoryBudgets.put(cat, b.getAmount());
        }

        double adjustmentFactor = 0.9;
        
        totalBudgetsLimit = 0;
        for (Double limit : result.categoryBudgets.values()) {
            totalBudgetsLimit += limit;
        }

        double daysRatio = (double) curDay / maxDays;
        double rawPrediction = daysRatio > 0 ? (monthExpense / daysRatio) : 0;
        
        if (curDay < 7 && totalBudgetsLimit > 0) {
            double weight = (double) curDay / 7.0;
            result.predictedEndOfMonthSpending = (rawPrediction * weight) + (totalBudgetsLimit * (1 - weight));
        } else {
            result.predictedEndOfMonthSpending = rawPrediction;
        }
        result.predictedEndOfMonthSpending = Math.round(result.predictedEndOfMonthSpending / 1000.0) * 1000.0;

        if (result.predictedEndOfMonthSpending > (totalBudgetsLimit > 0 ? totalBudgetsLimit : monthIncome)) {
            result.overspendingRisk = "High";
        } else if (result.predictedEndOfMonthSpending > (totalBudgetsLimit > 0 ? totalBudgetsLimit : monthIncome) * 0.85) {
            result.overspendingRisk = "Medium";
        } else {
            result.overspendingRisk = "Low";
        }

        SavingGoalResponse activeGoal = null;
        if (selectedGoalId != null) {
            for (SavingGoalResponse g : goals) {
                if (g.getId() != null && g.getId().equals(selectedGoalId)) {
                    activeGoal = g;
                    break;
                }
            }
        }
        
        if (activeGoal == null) {
            for (SavingGoalResponse g : goals) {
                double target = g.getTargetAmount() != null ? g.getTargetAmount().doubleValue() : 0;
                double currentBal = summary != null ? summary.getCurrentBalance() : 0;
                if (currentBal < target) {
                    activeGoal = g;
                    break;
                }
            }
        }
        
        if (activeGoal == null && !goals.isEmpty()) {
            activeGoal = goals.get(0);
        }

        if (activeGoal != null) {
            result.primaryGoalName = activeGoal.getName();
            result.goalTarget = activeGoal.getTargetAmount() != null ? activeGoal.getTargetAmount().doubleValue() : 0;
            result.goalCurrent = summary != null ? summary.getCurrentBalance() : 0;
            result.goalProgressPct = result.goalTarget > 0 ? (result.goalCurrent / result.goalTarget) * 100 : 0;

            if (result.goalProgressPct >= 80) {
                adjustmentFactor = 1.05;
            } else if (result.goalProgressPct < 30) {
                adjustmentFactor = 0.8;
            }

            double remainingTarget = Math.max(0, result.goalTarget - result.goalCurrent);
            double dailySavings = curDay > 0 ? ((monthIncome - monthExpense) / curDay) : 0;

            if (dailySavings > 0) {
                result.remainingDays = (int) Math.ceil(remainingTarget / dailySavings);
                Calendar compCal = Calendar.getInstance();
                compCal.add(Calendar.DAY_OF_YEAR, result.remainingDays);
                result.estimatedCompletionDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(compCal.getTime());
            } else {
                result.remainingDays = -1;
                result.estimatedCompletionDate = "Chưa xác định";
            }
        } else {
            result.primaryGoalName = "Chưa thiết lập mục tiêu";
            result.goalTarget = 0;
            result.goalCurrent = 0;
            result.goalProgressPct = 0;
            result.remainingDays = -1;
            result.estimatedCompletionDate = "N/A";
        }

        for (Map.Entry<String, Double> entry : result.categorySpending.entrySet()) {
            double spent = entry.getValue();
            double ratio = monthExpense > 0 ? (spent / monthExpense) * 100 : 0;
            result.categoryRatios.put(entry.getKey(), ratio);
            
            double currentLimit = result.categoryBudgets.getOrDefault(entry.getKey(), 0.0);
            result.recommendedBudgets.put(entry.getKey(), currentLimit > 0 ? currentLimit * adjustmentFactor : spent * adjustmentFactor);
        }

        for (TransactionResponse tr : transactions) {
            if (tr.getTransactionDate() != null && tr.getTransactionDate().startsWith(curMonthPrefix) 
                    && "EXPENSE".equalsIgnoreCase(tr.getType()) && tr.getAmount() > 1000000) {
                result.abnormalExpenses.add(tr.getDescription() + " (" + String.format(Locale.US, "%,.0f", tr.getAmount()) + "đ)");
            }
        }

        result.remainingBudget = Math.max(0, (totalBudgetsLimit > 0 ? totalBudgetsLimit : monthIncome) - monthExpense);

        double savingsRate = monthIncome > 0 ? (monthIncome - monthExpense) / monthIncome : 0;
        int savingsScore = (int) Math.max(0, Math.min(100, (savingsRate / 0.20) * 100));
        int budgetScore = 100;
        if (!result.categoryBudgets.isEmpty()) {
            int overCount = 0;
            for (Map.Entry<String, Double> entry : result.categoryBudgets.entrySet()) {
                double spent = result.categorySpending.getOrDefault(entry.getKey(), 0.0);
                if (spent > entry.getValue()) {
                    overCount++;
                }
            }
            budgetScore = 100 - (overCount * 100 / result.categoryBudgets.size());
        }

        // Goal progress score
        int goalScore = (int) Math.min(100, result.goalProgressPct);

        // Weighted Health Score
        result.financialHealthScore = (int) (savingsScore * 0.4 + budgetScore * 0.4 + goalScore * 0.2);
        if (result.financialHealthScore >= 80) {
            result.financialHealth = "Excellent";
        } else if (result.financialHealthScore >= 60) {
            result.financialHealth = "Good";
        } else if (result.financialHealthScore >= 40) {
            result.financialHealth = "Fair";
        } else {
            result.financialHealth = "Poor";
        }

        return result;
    }

    public static AnalysisResult simulateDecision(Context context, String category, double newLimit) {
        AnalysisResult res = analyze(context);
        if (res.categoryBudgets.containsKey(category)) {
            res.categoryBudgets.put(category, newLimit);
        }
        return res;
    }
}
