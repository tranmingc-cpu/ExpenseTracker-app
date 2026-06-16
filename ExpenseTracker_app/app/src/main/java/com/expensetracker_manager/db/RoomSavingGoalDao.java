package com.expensetracker_manager.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RoomSavingGoalDao {
    @Query("SELECT * FROM saving_goals")
    List<RoomSavingGoal> getAllGoals();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RoomSavingGoal> goals);

    @Query("DELETE FROM saving_goals")
    void clear();
}
