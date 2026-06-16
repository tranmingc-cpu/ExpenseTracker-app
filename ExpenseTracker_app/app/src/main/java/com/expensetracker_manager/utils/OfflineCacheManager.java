package com.expensetracker_manager.utils;

import android.content.Context;
import com.expensetracker_manager.db.*;
import com.expensetracker_manager.model.response.ReportSummaryResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.model.response.BudgetResponse;
import com.expensetracker_manager.model.response.SavingGoalResponse;
import com.example.expensetracker_app.RecurringActivity.RecurringItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OfflineCacheManager {
    private static OfflineCacheManager instance;
    private final AppDatabase db;

    private OfflineCacheManager(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public static synchronized OfflineCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineCacheManager(context);
        }
        return instance;
    }

    public void cacheReportSummary(ReportSummaryResponse summary) {
        if (summary == null) return;
        RoomReportSummary room = new RoomReportSummary();
        room.setTotalIncome(summary.getTotalIncome());
        room.setTotalExpense(summary.getTotalExpense());
        room.setBalance(summary.getBalance());
        room.setCurrentBalance(summary.getCurrentBalance());
        db.reportSummaryDao().insertSummary(room);
    }

    public ReportSummaryResponse getCachedReportSummary() {
        RoomReportSummary room = db.reportSummaryDao().getSummary();
        ReportSummaryResponse res = new ReportSummaryResponse();
        if (room == null) {
            res.setTotalIncome(10000000);
            res.setTotalExpense(6500000);
            res.setBalance(3500000);
            res.setCurrentBalance(3500000);
            return res;
        }
        res.setTotalIncome(room.getTotalIncome());
        res.setTotalExpense(room.getTotalExpense());
        res.setBalance(room.getBalance());
        res.setCurrentBalance(room.getCurrentBalance());
        return res;
    }

    public void cacheTransactions(List<TransactionResponse> transactions) {
        if (transactions == null) return;
        db.transactionDao().clear();
        List<RoomTransaction> list = new ArrayList<>();
        for (TransactionResponse tx : transactions) {
            RoomTransaction room = new RoomTransaction();
            room.setId(tx.getId());
            room.setAmount(tx.getAmount());
            room.setDescription(tx.getDescription());
            room.setTransactionDate(tx.getTransactionDate());
            room.setType(tx.getType());
            room.setCategoryName(tx.getCategoryName());
            list.add(room);
        }
        db.transactionDao().insertAll(list);
    }

    public List<TransactionResponse> getCachedTransactions() {
        List<RoomTransaction> roomList = db.transactionDao().getAllTransactions();
        List<TransactionResponse> resList = new ArrayList<>();
        for (RoomTransaction room : roomList) {
            TransactionResponse tx = new TransactionResponse();
            tx.setId(room.getId());
            tx.setAmount(room.getAmount());
            tx.setDescription(room.getDescription());
            tx.setTransactionDate(room.getTransactionDate());
            tx.setType(room.getType());
            tx.setCategoryName(room.getCategoryName());
            resList.add(tx);
        }
        return resList;
    }

    public void cacheBudgets(List<BudgetResponse> budgets) {
        if (budgets == null) return;
        db.budgetDao().clear();
        List<RoomBudget> list = new ArrayList<>();
        for (BudgetResponse b : budgets) {
            RoomBudget room = new RoomBudget();
            room.setId(b.getId());
            room.setAmount(b.getAmount());
            room.setMonth(b.getMonth());
            room.setYear(b.getYear());
            room.setCategoryName(b.getCategoryName());
            room.setSpent(b.getSpent());
            list.add(room);
        }
        db.budgetDao().insertAll(list);
    }

    public List<BudgetResponse> getCachedBudgets() {
        List<RoomBudget> roomList = db.budgetDao().getAllBudgets();
        List<BudgetResponse> resList = new ArrayList<>();
        for (RoomBudget room : roomList) {
            BudgetResponse b = new BudgetResponse();
            b.setId(room.getId());
            b.setAmount(room.getAmount());
            b.setMonth(room.getMonth());
            b.setYear(room.getYear());
            b.setCategoryName(room.getCategoryName());
            b.setSpent(room.getSpent());
            resList.add(b);
        }
        return resList;
    }

    public void cacheSavingGoals(List<SavingGoalResponse> goals) {
        if (goals == null) return;
        db.savingGoalDao().clear();
        List<RoomSavingGoal> list = new ArrayList<>();
        for (SavingGoalResponse g : goals) {
            RoomSavingGoal room = new RoomSavingGoal();
            room.setId(g.getId());
            room.setName(g.getName());
            room.setTargetAmount(g.getTargetAmount() != null ? g.getTargetAmount().doubleValue() : 0);
            room.setCurrentAmount(g.getCurrentAmount() != null ? g.getCurrentAmount().doubleValue() : 0);
            room.setTargetDate(g.getTargetDate());
            room.setCompleted(g.isCompleted());
            list.add(room);
        }
        db.savingGoalDao().insertAll(list);
    }

    public List<SavingGoalResponse> getCachedSavingGoals() {
        List<RoomSavingGoal> roomList = db.savingGoalDao().getAllGoals();
        List<SavingGoalResponse> resList = new ArrayList<>();
        for (RoomSavingGoal room : roomList) {
            SavingGoalResponse g = new SavingGoalResponse();
            g.setId(room.getId());
            g.setName(room.getName());
            g.setTargetAmount(BigDecimal.valueOf(room.getTargetAmount()));
            g.setCurrentAmount(BigDecimal.valueOf(room.getCurrentAmount()));
            g.setTargetDate(room.getTargetDate());
            g.setCompleted(room.isCompleted());
            resList.add(g);
        }
        return resList;
    }

    public void cacheRecurringItems(List<RecurringItem> items) {
        if (items == null) return;
        db.recurringItemDao().clear();
        List<RoomRecurringItem> list = new ArrayList<>();
        for (RecurringItem item : items) {
            RoomRecurringItem room = new RoomRecurringItem();
            room.setId(item.id);
            room.setName(item.name);
            room.setAmount(item.amount);
            room.setDay(item.day);
            list.add(room);
        }
        db.recurringItemDao().insertAll(list);
    }

    public List<RecurringItem> getCachedRecurringItems() {
        List<RoomRecurringItem> roomList = db.recurringItemDao().getAllRecurringItems();
        List<RecurringItem> resList = new ArrayList<>();
        for (RoomRecurringItem room : roomList) {
            resList.add(new RecurringItem(room.getId(), room.getName(), room.getAmount(), room.getDay()));
        }
        return resList;
    }
}
