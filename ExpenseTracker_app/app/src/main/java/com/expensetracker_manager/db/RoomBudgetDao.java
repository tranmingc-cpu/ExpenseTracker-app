package com.expensetracker_manager.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RoomBudgetDao {
    @Query("SELECT * FROM budgets")
    List<RoomBudget> getAllBudgets();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RoomBudget> budgets);

    @Query("DELETE FROM budgets")
    void clear();
}
