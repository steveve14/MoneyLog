package com.moneylog.data.db.dao;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.entity.RecurringEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RecurringDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private RecurringDao recurringDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        recurringDao = db.recurringDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ── 헬퍼 ──

    private RecurringEntity createRecurring(String type, long amount, String memo,
                                            String intervalType, int dayOfMonth) {
        RecurringEntity r = new RecurringEntity();
        r.type = type;
        r.amount = amount;
        r.categoryId = 1;
        r.memo = memo;
        r.paymentMethod = "CARD";
        r.intervalType = intervalType;
        r.dayOfMonth = dayOfMonth;
        r.isActive = true;
        r.createdAt = r.updatedAt = System.currentTimeMillis();
        return r;
    }

    // ── INSERT ──

    @Test
    public void insert_returnsPositiveId() {
        RecurringEntity r = createRecurring("EXPENSE", 50000, "월세", "MONTHLY", 1);
        long id = recurringDao.insert(r);
        assertTrue(id > 0);
    }

    @Test
    public void insert_replace_updatesExisting() {
        RecurringEntity r = createRecurring("EXPENSE", 50000, "월세", "MONTHLY", 1);
        long id = recurringDao.insert(r);

        r.id = id;
        r.amount = 60000;
        recurringDao.insert(r); // REPLACE

        RecurringEntity loaded = recurringDao.getById(id);
        assertEquals(60000, loaded.amount);
    }

    // ── getById ──

    @Test
    public void getById_returnsInserted() {
        RecurringEntity r = createRecurring("INCOME", 3500000, "급여", "MONTHLY", 25);
        long id = recurringDao.insert(r);

        RecurringEntity loaded = recurringDao.getById(id);
        assertNotNull(loaded);
        assertEquals("INCOME", loaded.type);
        assertEquals(3500000, loaded.amount);
        assertEquals("급여", loaded.memo);
        assertEquals("MONTHLY", loaded.intervalType);
        assertEquals(25, loaded.dayOfMonth);
    }

    @Test
    public void getById_nonExistent_returnsNull() {
        assertNull(recurringDao.getById(999));
    }

    // ── UPDATE ──

    @Test
    public void update_changesFields() {
        RecurringEntity r = createRecurring("EXPENSE", 50000, "원래", "MONTHLY", 1);
        long id = recurringDao.insert(r);

        RecurringEntity loaded = recurringDao.getById(id);
        loaded.amount = 70000;
        loaded.memo = "수정됨";
        loaded.intervalType = "WEEKLY";
        loaded.dayOfMonth = 3;
        recurringDao.update(loaded);

        RecurringEntity updated = recurringDao.getById(id);
        assertEquals(70000, updated.amount);
        assertEquals("수정됨", updated.memo);
        assertEquals("WEEKLY", updated.intervalType);
        assertEquals(3, updated.dayOfMonth);
    }

    // ── DELETE (hard delete) ──

    @Test
    public void delete_removesRecord() {
        RecurringEntity r = createRecurring("EXPENSE", 10000, "삭제 테스트", "DAILY", 0);
        long id = recurringDao.insert(r);

        recurringDao.delete(id);

        assertNull(recurringDao.getById(id));
    }

    // ── getAll (LiveData) ──

    @Test
    public void getAll_orderedBySortOrderThenCreatedAt() throws Exception {
        RecurringEntity r1 = createRecurring("EXPENSE", 10000, "두번째", "MONTHLY", 1);
        r1.sortOrder = 2;
        r1.createdAt = 1000;
        recurringDao.insert(r1);

        RecurringEntity r2 = createRecurring("INCOME", 3000000, "첫번째", "MONTHLY", 25);
        r2.sortOrder = 1;
        r2.createdAt = 2000;
        recurringDao.insert(r2);

        RecurringEntity r3 = createRecurring("EXPENSE", 5000, "세번째(최신)", "DAILY", 0);
        r3.sortOrder = 2;
        r3.createdAt = 3000;
        recurringDao.insert(r3);

        List<RecurringEntity> result = LiveDataTestUtil.getValue(recurringDao.getAll());

        assertEquals(3, result.size());
        assertEquals("첫번째", result.get(0).memo);      // sortOrder=1
        assertEquals("세번째(최신)", result.get(1).memo);  // sortOrder=2, createdAt=3000 (DESC)
        assertEquals("두번째", result.get(2).memo);        // sortOrder=2, createdAt=1000
    }

    // ── getByType (LiveData) ──

    @Test
    public void getByType_filtersCorrectly() throws Exception {
        recurringDao.insert(createRecurring("EXPENSE", 50000, "월세", "MONTHLY", 1));
        recurringDao.insert(createRecurring("EXPENSE", 10000, "교통", "DAILY", 0));
        recurringDao.insert(createRecurring("INCOME", 3500000, "급여", "MONTHLY", 25));

        List<RecurringEntity> expenses = LiveDataTestUtil.getValue(
                recurringDao.getByType("EXPENSE"));
        List<RecurringEntity> incomes = LiveDataTestUtil.getValue(
                recurringDao.getByType("INCOME"));

        assertEquals(2, expenses.size());
        assertEquals(1, incomes.size());
        assertEquals("급여", incomes.get(0).memo);
    }

    // ── setActive ──

    @Test
    public void setActive_togglesFlag() {
        RecurringEntity r = createRecurring("EXPENSE", 10000, "활성화 테스트", "MONTHLY", 1);
        long id = recurringDao.insert(r);

        long now = System.currentTimeMillis();
        recurringDao.setActive(id, false, now);

        RecurringEntity loaded = recurringDao.getById(id);
        assertFalse(loaded.isActive);

        recurringDao.setActive(id, true, now + 1);
        loaded = recurringDao.getById(id);
        assertTrue(loaded.isActive);
    }

    // ── updateSortOrder ──

    @Test
    public void updateSortOrder_changesOrder() {
        RecurringEntity r = createRecurring("EXPENSE", 10000, "정렬", "MONTHLY", 1);
        long id = recurringDao.insert(r);

        recurringDao.updateSortOrder(id, 99, System.currentTimeMillis());

        RecurringEntity loaded = recurringDao.getById(id);
        assertEquals(99, loaded.sortOrder);
    }

    // ── updateLastExecutedDate ──

    @Test
    public void updateLastExecutedDate_setsDate() {
        RecurringEntity r = createRecurring("EXPENSE", 50000, "실행일", "MONTHLY", 1);
        long id = recurringDao.insert(r);

        recurringDao.updateLastExecutedDate(id, "2026-04-15", System.currentTimeMillis());

        RecurringEntity loaded = recurringDao.getById(id);
        assertEquals("2026-04-15", loaded.lastExecutedDate);
    }

    // ── getAllActiveSync ──

    @Test
    public void getAllActiveSync_returnsOnlyActive() {
        RecurringEntity active1 = createRecurring("EXPENSE", 50000, "활성1", "MONTHLY", 1);
        RecurringEntity active2 = createRecurring("INCOME", 3500000, "활성2", "MONTHLY", 25);
        RecurringEntity inactive = createRecurring("EXPENSE", 10000, "비활성", "DAILY", 0);
        inactive.isActive = false;

        recurringDao.insert(active1);
        recurringDao.insert(active2);
        recurringDao.insert(inactive);

        List<RecurringEntity> result = recurringDao.getAllActiveSync();
        assertEquals(2, result.size());
    }

    // ── getAllSync ──

    @Test
    public void getAllSync_returnsAllIncludingInactive() {
        RecurringEntity active = createRecurring("EXPENSE", 50000, "활성", "MONTHLY", 1);
        RecurringEntity inactive = createRecurring("EXPENSE", 10000, "비활성", "DAILY", 0);
        inactive.isActive = false;

        recurringDao.insert(active);
        recurringDao.insert(inactive);

        List<RecurringEntity> result = recurringDao.getAllSync();
        assertEquals(2, result.size());
    }

    // ── deleteAll ──

    @Test
    public void deleteAll_removesEverything() {
        recurringDao.insert(createRecurring("EXPENSE", 10000, "A", "MONTHLY", 1));
        recurringDao.insert(createRecurring("INCOME", 20000, "B", "WEEKLY", 3));

        recurringDao.deleteAll();

        List<RecurringEntity> result = recurringDao.getAllSync();
        assertTrue(result.isEmpty());
    }

    // ── intervalType / monthOfYear 저장 확인 ──

    @Test
    public void yearlyRecurring_savesMonthOfYear() {
        RecurringEntity r = createRecurring("EXPENSE", 120000, "연회비", "YEARLY", 15);
        r.monthOfYear = 3; // 3월 15일
        long id = recurringDao.insert(r);

        RecurringEntity loaded = recurringDao.getById(id);
        assertEquals("YEARLY", loaded.intervalType);
        assertEquals(3, loaded.monthOfYear);
        assertEquals(15, loaded.dayOfMonth);
    }
}
