package com.moneylog.data.db.dao;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.TransactionEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TransactionDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private TransactionDao transactionDao;
    private CategoryDao categoryDao;
    private long expenseCategoryId;
    private long incomeCategoryId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        transactionDao = db.transactionDao();
        categoryDao = db.categoryDao();

        // 테스트용 카테고리 생성
        CategoryEntity expense = new CategoryEntity();
        expense.name = "식비";
        expense.type = "EXPENSE";
        expense.iconName = "restaurant";
        expenseCategoryId = categoryDao.insert(expense);

        CategoryEntity income = new CategoryEntity();
        income.name = "급여";
        income.type = "INCOME";
        income.iconName = "payments";
        incomeCategoryId = categoryDao.insert(income);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ── 헬퍼 ──

    private TransactionEntity createExpense(long amount, String date, String memo) {
        TransactionEntity tx = new TransactionEntity();
        tx.type = "EXPENSE";
        tx.amount = amount;
        tx.categoryId = expenseCategoryId;
        tx.date = date;
        tx.memo = memo;
        tx.createdAt = tx.updatedAt = System.currentTimeMillis();
        return tx;
    }

    private TransactionEntity createIncome(long amount, String date, String memo) {
        TransactionEntity tx = new TransactionEntity();
        tx.type = "INCOME";
        tx.amount = amount;
        tx.categoryId = incomeCategoryId;
        tx.date = date;
        tx.memo = memo;
        tx.createdAt = tx.updatedAt = System.currentTimeMillis();
        return tx;
    }

    // ── INSERT / getById ──

    @Test
    public void insert_returnsPositiveId() {
        TransactionEntity tx = createExpense(10000, "2026-04-01", "점심");
        long id = transactionDao.insert(tx);
        assertTrue(id > 0);
    }

    @Test
    public void getById_returnsInsertedTransaction() {
        TransactionEntity tx = createExpense(15000, "2026-04-05", "저녁");
        long id = transactionDao.insert(tx);

        TransactionEntity loaded = transactionDao.getById(id);
        assertNotNull(loaded);
        assertEquals("EXPENSE", loaded.type);
        assertEquals(15000, loaded.amount);
        assertEquals("2026-04-05", loaded.date);
        assertEquals("저녁", loaded.memo);
    }

    @Test
    public void getById_deletedTransaction_returnsNull() {
        TransactionEntity tx = createExpense(5000, "2026-04-01", "삭제될 거래");
        long id = transactionDao.insert(tx);
        transactionDao.softDelete(id, System.currentTimeMillis());

        TransactionEntity loaded = transactionDao.getById(id);
        assertNull(loaded);
    }

    // ── UPDATE ──

    @Test
    public void update_changesFields() {
        TransactionEntity tx = createExpense(10000, "2026-04-01", "원래 메모");
        long id = transactionDao.insert(tx);

        TransactionEntity loaded = transactionDao.getById(id);
        loaded.amount = 20000;
        loaded.memo = "수정된 메모";
        loaded.updatedAt = System.currentTimeMillis();
        transactionDao.update(loaded);

        TransactionEntity updated = transactionDao.getById(id);
        assertEquals(20000, updated.amount);
        assertEquals("수정된 메모", updated.memo);
    }

    // ── SOFT DELETE ──

    @Test
    public void softDelete_setsDeletedFlag() {
        TransactionEntity tx = createExpense(8000, "2026-04-10", "삭제 테스트");
        long id = transactionDao.insert(tx);

        transactionDao.softDelete(id, System.currentTimeMillis());

        // getById는 is_deleted=0 조건이므로 null 반환
        assertNull(transactionDao.getById(id));
    }

    // ── getByMonth (LiveData) ──

    @Test
    public void getByMonth_returnsOnlyMatchingMonth() throws Exception {
        transactionDao.insert(createExpense(10000, "2026-04-01", "4월"));
        transactionDao.insert(createExpense(20000, "2026-04-15", "4월"));
        transactionDao.insert(createExpense(30000, "2026-03-20", "3월"));

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                transactionDao.getByMonth("2026-04"));

        assertEquals(2, result.size());
    }

    @Test
    public void getByMonth_excludesDeletedTransactions() throws Exception {
        long id = transactionDao.insert(createExpense(10000, "2026-04-01", "삭제될"));
        transactionDao.insert(createExpense(20000, "2026-04-02", "유지될"));
        transactionDao.softDelete(id, System.currentTimeMillis());

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                transactionDao.getByMonth("2026-04"));

        assertEquals(1, result.size());
        assertEquals("유지될", result.get(0).memo);
    }

    @Test
    public void getByMonth_orderedByDateDescending() throws Exception {
        transactionDao.insert(createExpense(10000, "2026-04-01", "1일"));
        transactionDao.insert(createExpense(20000, "2026-04-15", "15일"));
        transactionDao.insert(createExpense(30000, "2026-04-10", "10일"));

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                transactionDao.getByMonth("2026-04"));

        assertEquals("2026-04-15", result.get(0).date);
        assertEquals("2026-04-10", result.get(1).date);
        assertEquals("2026-04-01", result.get(2).date);
    }

    // ── getMonthlySummary ──

    @Test
    public void getMonthlySummary_calculatesCorrectly() throws Exception {
        transactionDao.insert(createIncome(3500000, "2026-04-01", "급여"));
        transactionDao.insert(createExpense(500000, "2026-04-05", "월세"));
        transactionDao.insert(createExpense(120000, "2026-04-10", "식비"));

        MonthlySummary summary = LiveDataTestUtil.getValue(
                transactionDao.getMonthlySummary("2026-04"));

        assertNotNull(summary);
        assertEquals(3500000, summary.totalIncome);
        assertEquals(620000, summary.totalExpense);
    }

    @Test
    public void getMonthlySummary_emptyMonth_returnsZeros() throws Exception {
        MonthlySummary summary = LiveDataTestUtil.getValue(
                transactionDao.getMonthlySummary("2026-04"));

        assertNotNull(summary);
        assertEquals(0, summary.totalIncome);
        assertEquals(0, summary.totalExpense);
    }

    // ── getMonthlyExpenseByCategory ──

    @Test
    public void getMonthlyExpenseByCategory_groupsCorrectly() throws Exception {
        transactionDao.insert(createExpense(10000, "2026-04-01", "식비1"));
        transactionDao.insert(createExpense(20000, "2026-04-05", "식비2"));

        List<CategorySummary> result = LiveDataTestUtil.getValue(
                transactionDao.getMonthlyExpenseByCategory("2026-04"));

        assertFalse(result.isEmpty());
        // 같은 카테고리이므로 1개 그룹
        long total = 0;
        for (CategorySummary cs : result) {
            if (cs.categoryId == expenseCategoryId) {
                total = cs.total;
            }
        }
        assertEquals(30000, total);
    }

    // ── getDailySummary ──

    @Test
    public void getDailySummary_groupsByDate() throws Exception {
        transactionDao.insert(createExpense(10000, "2026-04-01", ""));
        transactionDao.insert(createExpense(5000, "2026-04-01", ""));
        transactionDao.insert(createIncome(100000, "2026-04-02", ""));

        List<DailySummary> result = LiveDataTestUtil.getValue(
                transactionDao.getDailySummary("2026-04"));

        assertEquals(2, result.size());  // 2날짜
        // 4/1: expense 15000
        assertEquals("2026-04-01", result.get(0).date);
        assertEquals(15000, result.get(0).totalExpense);
        assertEquals(0, result.get(0).totalIncome);
        // 4/2: income 100000
        assertEquals("2026-04-02", result.get(1).date);
        assertEquals(100000, result.get(1).totalIncome);
    }

    // ── getRecent ──

    @Test
    public void getRecent_limitsResults() throws Exception {
        for (int i = 1; i <= 5; i++) {
            transactionDao.insert(createExpense(i * 1000, "2026-04-0" + i, "거래" + i));
        }

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                transactionDao.getRecent(3));

        assertEquals(3, result.size());
    }

    // ── getMonthlyTrend ──

    @Test
    public void getMonthlyTrend_groupsByYearMonth() throws Exception {
        transactionDao.insert(createExpense(100000, "2026-01-15", "1월"));
        transactionDao.insert(createExpense(200000, "2026-02-15", "2월"));
        transactionDao.insert(createIncome(3000000, "2026-02-01", "2월급여"));

        List<MonthlyTrend> result = LiveDataTestUtil.getValue(
                transactionDao.getMonthlyTrend("2026-01-01"));

        assertEquals(2, result.size());
        assertEquals("2026-01", result.get(0).yearMonth);
        assertEquals(100000, result.get(0).totalExpense);
        assertEquals("2026-02", result.get(1).yearMonth);
        assertEquals(200000, result.get(1).totalExpense);
        assertEquals(3000000, result.get(1).totalIncome);
    }

    // ── softDeleteBefore ──

    @Test
    public void softDeleteBefore_deletesOlderTransactions() {
        transactionDao.insert(createExpense(10000, "2026-01-15", "오래된"));
        transactionDao.insert(createExpense(20000, "2026-04-15", "최근"));

        int deleted = transactionDao.softDeleteBefore("2026-03-01", System.currentTimeMillis());

        assertEquals(1, deleted);
    }

    // ── deleteAll ──

    @Test
    public void deleteAll_removesEverything() {
        transactionDao.insert(createExpense(10000, "2026-04-01", ""));
        transactionDao.insert(createExpense(20000, "2026-04-02", ""));

        transactionDao.deleteAll();

        assertEquals(0, transactionDao.countActive());
    }

    // ── getAllForExport ──

    @Test
    public void getAllForExport_excludesDeleted() {
        long id = transactionDao.insert(createExpense(10000, "2026-04-01", "삭제될"));
        transactionDao.insert(createExpense(20000, "2026-04-02", "유지될"));
        transactionDao.softDelete(id, System.currentTimeMillis());

        List<TransactionEntity> result = transactionDao.getAllForExport();

        assertEquals(1, result.size());
    }
}
