package com.expensetracker_manager.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface RoomReportSummaryDao {
    @Query("SELECT * FROM report_summary WHERE id = 1 LIMIT 1")
    RoomReportSummary getSummary();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSummary(RoomReportSummary summary);

    @Query("DELETE FROM report_summary")
    void clear();
}
