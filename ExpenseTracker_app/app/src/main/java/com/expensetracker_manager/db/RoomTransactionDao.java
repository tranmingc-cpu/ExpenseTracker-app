package com.expensetracker_manager.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RoomTransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, id DESC")
    List<RoomTransaction> getAllTransactions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RoomTransaction> transactions);

    @Query("DELETE FROM transactions")
    void clear();
}
