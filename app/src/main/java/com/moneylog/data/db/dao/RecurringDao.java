package com.moneylog.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.moneylog.data.db.entity.RecurringEntity;

import java.util.List;

@Dao
public interface RecurringDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecurringEntity recurring);

    @Update
    void update(RecurringEntity recurring);

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    void delete(long id);

    @Query("SELECT * FROM recurring_transactions ORDER BY sort_order ASC, created_at DESC")
    LiveData<List<RecurringEntity>> getAll();

    @Query("SELECT * FROM recurring_transactions WHERE type = :type ORDER BY sort_order ASC, created_at DESC")
    LiveData<List<RecurringEntity>> getByType(String type);

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    RecurringEntity getById(long id);

    @Query("UPDATE recurring_transactions SET is_active = :active, updated_at = :now WHERE id = :id")
    void setActive(long id, boolean active, long now);

    @Query("UPDATE recurring_transactions SET sort_order = :order, updated_at = :now WHERE id = :id")
    void updateSortOrder(long id, int order, long now);

    @Query("UPDATE recurring_transactions SET last_executed_date = :date, updated_at = :now WHERE id = :id")
    void updateLastExecutedDate(long id, String date, long now);

    /** WorkManager용: 활성화된 반복 거래 전체 조회 (동기) */
    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1")
    List<RecurringEntity> getAllActiveSync();

    /** 전체 반복 거래 동기 조회 (CSV 내보내기용) */
    @Query("SELECT * FROM recurring_transactions ORDER BY sort_order ASC, created_at DESC")
    List<RecurringEntity> getAllSync();

    /** 전체 반복 거래 삭제 (데이터 초기화용) */
    @Query("DELETE FROM recurring_transactions")
    void deleteAll();
}
