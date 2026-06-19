package com.expensetracker_manager.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        RoomReportSummary.class,
        RoomTransaction.class,
        RoomBudget.class,
        RoomSavingGoal.class,
        RoomRecurringItem.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract RoomReportSummaryDao reportSummaryDao();
    public abstract RoomTransactionDao transactionDao();
    public abstract RoomBudgetDao budgetDao();
    public abstract RoomSavingGoalDao savingGoalDao();
    public abstract RoomRecurringItemDao recurringItemDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "expense_tracker_room"
            ).allowMainThreadQueries().build();
        }
        return instance;
    }
}
