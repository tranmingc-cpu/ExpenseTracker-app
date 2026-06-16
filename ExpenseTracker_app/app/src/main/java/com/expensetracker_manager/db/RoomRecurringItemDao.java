package com.expensetracker_manager.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RoomRecurringItemDao {
    @Query("SELECT * FROM recurring_items")
    List<RoomRecurringItem> getAllRecurringItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RoomRecurringItem> items);

    @Query("DELETE FROM recurring_items")
    void clear();
}
