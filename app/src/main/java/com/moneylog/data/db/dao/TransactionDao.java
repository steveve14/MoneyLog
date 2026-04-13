package com.moneylog.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.moneylog.data.db.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Query("UPDATE transactions SET is_deleted = 1, updated_at = :now WHERE id = :id")
    void softDelete(long id, long now);

    @Query("SELECT * FROM transactions WHERE id = :id AND is_deleted = 0")
    TransactionEntity getById(long id);

    /** 월별 거래 목록 (최신순) */
    @Query("SELECT * FROM transactions " +
           "WHERE date LIKE :yearMonth || '%' AND is_deleted = 0 " +
           "ORDER BY date DESC, id DESC")
    LiveData<List<TransactionEntity>> getByMonth(String yearMonth);

    /** 월별 수입·지출 합계 */
    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END), 0) AS totalIncome, " +
           "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS totalExpense " +
           "FROM transactions " +
           "WHERE date LIKE :yearMonth || '%' AND is_deleted = 0")
    LiveData<MonthlySummary> getMonthlySummary(String yearMonth);

    /** 월별 카테고리별 거래 합계 (지출+수입) */
    @Query("SELECT t.category_id AS categoryId, c.name AS categoryName, t.type AS type, SUM(t.amount) AS total " +
           "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
           "WHERE t.date LIKE :yearMonth || '%' AND t.is_deleted = 0 " +
           "GROUP BY t.category_id, t.type ORDER BY total DESC")
    LiveData<List<CategorySummary>> getMonthlyExpenseByCategory(String yearMonth);

    /** 최근 N건 거래 */
    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date DESC, id DESC LIMIT :limit")
    LiveData<List<TransactionEntity>> getRecent(int limit);

    /** 특정 카테고리 + 월 지출 합계 */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
           "WHERE category_id = :categoryId AND date LIKE :yearMonth || '%' " +
           "AND type = 'EXPENSE' AND is_deleted = 0")
    long getMonthlyExpenseForCategory(long categoryId, String yearMonth);
}
