package com.moneylog.data.repository;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.dao.CategoryDao;
import com.moneylog.data.db.dao.DailySummary;
import com.moneylog.data.db.dao.LiveDataTestUtil;
import com.moneylog.data.db.dao.MonthlyTrend;
import com.moneylog.data.db.dao.MonthlySummary;
import com.moneylog.data.db.dao.TransactionDao;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.TransactionEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TransactionRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private TransactionDao dao;
    private TransactionRepository repository;
    private long expenseCategoryId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.transactionDao();
        repository = new TransactionRepository(dao);

        CategoryEntity cat = new CategoryEntity();
        cat.name = "식비";
        cat.type = "EXPENSE";
        cat.iconName = "restaurant";
        expenseCategoryId = db.categoryDao().insert(cat);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ── 헬퍼 ──

    private TransactionEntity createTx(long amount, String date, String memo) {
        TransactionEntity tx = new TransactionEntity();
        tx.type = "EXPENSE";
        tx.amount = amount;
        tx.categoryId = expenseCategoryId;
        tx.date = date;
        tx.memo = memo;
        tx.createdAt = tx.updatedAt = System.currentTimeMillis();
        return tx;
    }

    /** executor 내 작업이 완료될 때까지 잠시 대기 */
    private void waitForExecutor() throws InterruptedException {
        // Repository 내부 SingleThreadExecutor에 빈 작업 넣고 완료 대기
        CountDownLatch latch = new CountDownLatch(1);
        repository.add(createTx(0, "1970-01-01", "__sync__"));
        // add도 executor에서 실행되므로 그 다음 loadById를 통해 동기화
        Thread.sleep(200);
    }

    // ── add ──

    @Test
    public void add_insertsThroughExecutor() throws Exception {
        TransactionEntity tx = createTx(15000, "2026-04-01", "점심");
        repository.add(tx);
        Thread.sleep(200);

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                repository.getByMonth("2026-04"));

        // __sync__ 더미 + 실제 데이터가 아닌, 직접 add한 건만
        boolean found = false;
        for (TransactionEntity t : result) {
            if ("점심".equals(t.memo)) {
                assertEquals(15000, t.amount);
                found = true;
            }
        }
        assertTrue("추가된 거래를 찾지 못함", found);
    }

    // ── loadById (비동기 콜백) ──

    @Test
    public void loadById_returnsViaCallback() throws Exception {
        long id = dao.insert(createTx(30000, "2026-04-10", "콜백 테스트"));

        CountDownLatch latch = new CountDownLatch(1);
        final TransactionEntity[] holder = new TransactionEntity[1];
        repository.loadById(id, result -> {
            holder[0] = result;
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(holder[0]);
        assertEquals("콜백 테스트", holder[0].memo);
    }

    // ── delete (softDelete 래핑) ──

    @Test
    public void delete_softDeletesTransaction() throws Exception {
        long id = dao.insert(createTx(10000, "2026-04-05", "삭제 대상"));
        repository.delete(id);
        Thread.sleep(200);

        assertNull(dao.getById(id));
    }

    // ── getByMonth ──

    @Test
    public void getByMonth_delegatesToDao() throws Exception {
        dao.insert(createTx(10000, "2026-04-01", "4월"));
        dao.insert(createTx(20000, "2026-03-15", "3월"));

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                repository.getByMonth("2026-04"));

        assertEquals(1, result.size());
        assertEquals("4월", result.get(0).memo);
    }

    // ── getMonthlySummary ──

    @Test
    public void getMonthlySummary_delegatesToDao() throws Exception {
        dao.insert(createTx(50000, "2026-04-01", "지출"));

        CategoryEntity income = new CategoryEntity();
        income.name = "급여";
        income.type = "INCOME";
        income.iconName = "payments";
        long incId = db.categoryDao().insert(income);

        TransactionEntity incTx = new TransactionEntity();
        incTx.type = "INCOME";
        incTx.amount = 3000000;
        incTx.categoryId = incId;
        incTx.date = "2026-04-01";
        incTx.createdAt = incTx.updatedAt = System.currentTimeMillis();
        dao.insert(incTx);

        MonthlySummary summary = LiveDataTestUtil.getValue(
                repository.getMonthlySummary("2026-04"));

        assertEquals(3000000, summary.totalIncome);
        assertEquals(50000, summary.totalExpense);
    }

    // ── getRecent ──

    @Test
    public void getRecent_limitsResults() throws Exception {
        for (int i = 1; i <= 5; i++) {
            dao.insert(createTx(i * 1000, "2026-04-0" + i, "거래" + i));
        }

        List<TransactionEntity> result = LiveDataTestUtil.getValue(
                repository.getRecent(3));

        assertEquals(3, result.size());
    }

    // ── getDailySummary ──

    @Test
    public void getDailySummary_delegatesToDao() throws Exception {
        dao.insert(createTx(10000, "2026-04-01", ""));
        dao.insert(createTx(5000, "2026-04-01", ""));
        dao.insert(createTx(20000, "2026-04-02", ""));

        List<DailySummary> result = LiveDataTestUtil.getValue(
                repository.getDailySummary("2026-04"));

        assertEquals(2, result.size());
    }

    // ── getMonthlyTrend ──

    @Test
    public void getMonthlyTrend_delegatesToDao() throws Exception {
        dao.insert(createTx(100000, "2026-01-15", ""));
        dao.insert(createTx(200000, "2026-02-15", ""));

        List<MonthlyTrend> result = LiveDataTestUtil.getValue(
                repository.getMonthlyTrend("2026-01-01"));

        assertEquals(2, result.size());
    }
}
