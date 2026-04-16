package com.moneylog.data.repository;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.dao.LiveDataTestUtil;
import com.moneylog.data.db.dao.RecurringDao;
import com.moneylog.data.db.entity.RecurringEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RecurringRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private RecurringDao dao;
    private RecurringRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.recurringDao();
        repository = new RecurringRepository(dao);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ── 헬퍼 ──

    private RecurringEntity createRecurring(String type, long amount, String memo) {
        RecurringEntity r = new RecurringEntity();
        r.type = type;
        r.amount = amount;
        r.categoryId = 1;
        r.memo = memo;
        r.paymentMethod = "CARD";
        r.intervalType = "MONTHLY";
        r.dayOfMonth = 1;
        r.isActive = true;
        r.createdAt = r.updatedAt = System.currentTimeMillis();
        return r;
    }

    // ── getAll ──

    @Test
    public void getAll_delegatesToDao() throws Exception {
        dao.insert(createRecurring("EXPENSE", 50000, "월세"));
        dao.insert(createRecurring("INCOME", 3500000, "급여"));

        List<RecurringEntity> result = LiveDataTestUtil.getValue(repository.getAll());
        assertEquals(2, result.size());
    }

    // ── getByType ──

    @Test
    public void getByType_filtersCorrectly() throws Exception {
        dao.insert(createRecurring("EXPENSE", 50000, "월세"));
        dao.insert(createRecurring("INCOME", 3500000, "급여"));

        List<RecurringEntity> expenses = LiveDataTestUtil.getValue(
                repository.getByType("EXPENSE"));
        assertEquals(1, expenses.size());
        assertEquals("월세", expenses.get(0).memo);
    }

    // ── save ──

    @Test
    public void save_insertsThroughExecutor() throws Exception {
        repository.save(createRecurring("EXPENSE", 10000, "교통비"));
        Thread.sleep(200);

        List<RecurringEntity> result = LiveDataTestUtil.getValue(repository.getAll());
        assertEquals(1, result.size());
        assertEquals("교통비", result.get(0).memo);
    }

    // ── update ──

    @Test
    public void update_modifiesThroughExecutor() throws Exception {
        long id = dao.insert(createRecurring("EXPENSE", 50000, "원래"));

        RecurringEntity loaded = dao.getById(id);
        loaded.amount = 60000;
        loaded.memo = "수정됨";
        repository.update(loaded);
        Thread.sleep(200);

        RecurringEntity updated = dao.getById(id);
        assertEquals(60000, updated.amount);
        assertEquals("수정됨", updated.memo);
    }

    // ── delete ──

    @Test
    public void delete_removesThroughExecutor() throws Exception {
        long id = dao.insert(createRecurring("EXPENSE", 10000, "삭제 대상"));
        repository.delete(id);
        Thread.sleep(200);

        assertNull(dao.getById(id));
    }

    // ── setActive ──

    @Test
    public void setActive_togglesThroughExecutor() throws Exception {
        long id = dao.insert(createRecurring("EXPENSE", 10000, "활성화"));
        repository.setActive(id, false);
        Thread.sleep(200);

        assertFalse(dao.getById(id).isActive);
    }

    // ── loadById ──

    @Test
    public void loadById_returnsViaCallback() throws Exception {
        long id = dao.insert(createRecurring("INCOME", 3500000, "콜백 테스트"));

        CountDownLatch latch = new CountDownLatch(1);
        final RecurringEntity[] holder = new RecurringEntity[1];
        repository.loadById(id, result -> {
            holder[0] = result;
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(holder[0]);
        assertEquals("콜백 테스트", holder[0].memo);
    }

    // ── updateSortOrders ──

    @Test
    public void updateSortOrders_updatesAllItems() throws Exception {
        long id1 = dao.insert(createRecurring("EXPENSE", 10000, "첫번째"));
        long id2 = dao.insert(createRecurring("EXPENSE", 20000, "두번째"));
        long id3 = dao.insert(createRecurring("EXPENSE", 30000, "세번째"));

        RecurringEntity r1 = dao.getById(id1);
        RecurringEntity r2 = dao.getById(id2);
        RecurringEntity r3 = dao.getById(id3);

        // 역순으로 정렬 변경: 세번째→0, 두번째→1, 첫번째→2
        repository.updateSortOrders(Arrays.asList(r3, r2, r1));
        Thread.sleep(300);

        assertEquals(2, dao.getById(id1).sortOrder);
        assertEquals(1, dao.getById(id2).sortOrder);
        assertEquals(0, dao.getById(id3).sortOrder);
    }

    // ── getAllActiveInBackground ──

    @Test
    public void getAllActiveInBackground_returnsOnlyActive() throws Exception {
        dao.insert(createRecurring("EXPENSE", 50000, "활성"));
        RecurringEntity inactive = createRecurring("EXPENSE", 10000, "비활성");
        inactive.isActive = false;
        dao.insert(inactive);

        CountDownLatch latch = new CountDownLatch(1);
        final List<RecurringEntity>[] holder = new List[1];
        repository.getAllActiveInBackground(result -> {
            holder[0] = result;
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, holder[0].size());
        assertEquals("활성", holder[0].get(0).memo);
    }
}
