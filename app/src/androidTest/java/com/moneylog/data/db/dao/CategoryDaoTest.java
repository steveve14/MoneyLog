package com.moneylog.data.db.dao;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.entity.CategoryEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CategoryDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private CategoryDao categoryDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        categoryDao = db.categoryDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ── 헬퍼 ──

    private CategoryEntity createCategory(String name, String type, String icon, int order) {
        CategoryEntity cat = new CategoryEntity();
        cat.name = name;
        cat.type = type;
        cat.iconName = icon;
        cat.sortOrder = order;
        return cat;
    }

    // ── INSERT ──

    @Test
    public void insert_returnsPositiveId() {
        CategoryEntity cat = createCategory("식비", "EXPENSE", "restaurant", 0);
        long id = categoryDao.insert(cat);
        assertTrue(id > 0);
    }

    @Test
    public void insert_ignoresDuplicate() {
        CategoryEntity cat = createCategory("식비", "EXPENSE", "restaurant", 0);
        long id1 = categoryDao.insert(cat);
        cat.id = id1;
        long id2 = categoryDao.insert(cat);
        // OnConflictStrategy.IGNORE → 동일 PK는 -1 반환
        assertEquals(-1, id2);
    }

    // ── getById ──

    @Test
    public void getById_returnsInsertedCategory() {
        CategoryEntity cat = createCategory("교통", "EXPENSE", "directions_bus", 1);
        long id = categoryDao.insert(cat);

        CategoryEntity loaded = categoryDao.getById(id);
        assertNotNull(loaded);
        assertEquals("교통", loaded.name);
        assertEquals("EXPENSE", loaded.type);
        assertEquals("directions_bus", loaded.iconName);
    }

    @Test
    public void getById_deletedCategory_returnsNull() {
        CategoryEntity cat = createCategory("삭제될", "EXPENSE", "icon", 0);
        long id = categoryDao.insert(cat);
        categoryDao.softDelete(id);

        assertNull(categoryDao.getById(id));
    }

    // ── UPDATE ──

    @Test
    public void update_changesFields() {
        CategoryEntity cat = createCategory("원래이름", "EXPENSE", "icon", 0);
        long id = categoryDao.insert(cat);

        CategoryEntity loaded = categoryDao.getById(id);
        loaded.name = "변경이름";
        loaded.iconName = "new_icon";
        categoryDao.update(loaded);

        CategoryEntity updated = categoryDao.getById(id);
        assertEquals("변경이름", updated.name);
        assertEquals("new_icon", updated.iconName);
    }

    // ── SOFT DELETE ──

    @Test
    public void softDelete_excludesFromGetAll() throws Exception {
        long id1 = categoryDao.insert(createCategory("유지", "EXPENSE", "a", 0));
        long id2 = categoryDao.insert(createCategory("삭제", "EXPENSE", "b", 1));
        categoryDao.softDelete(id2);

        List<CategoryEntity> result = LiveDataTestUtil.getValue(categoryDao.getAll());

        assertEquals(1, result.size());
        assertEquals("유지", result.get(0).name);
    }

    // ── getAll (LiveData) ──

    @Test
    public void getAll_orderedBySortOrder() throws Exception {
        categoryDao.insert(createCategory("세번째", "EXPENSE", "c", 3));
        categoryDao.insert(createCategory("첫번째", "EXPENSE", "a", 1));
        categoryDao.insert(createCategory("두번째", "INCOME", "b", 2));

        List<CategoryEntity> result = LiveDataTestUtil.getValue(categoryDao.getAll());

        assertEquals(3, result.size());
        assertEquals("첫번째", result.get(0).name);
        assertEquals("두번째", result.get(1).name);
        assertEquals("세번째", result.get(2).name);
    }

    // ── getByType ──

    @Test
    public void getByType_filtersCorrectly() throws Exception {
        categoryDao.insert(createCategory("식비", "EXPENSE", "a", 0));
        categoryDao.insert(createCategory("교통", "EXPENSE", "b", 1));
        categoryDao.insert(createCategory("급여", "INCOME", "c", 0));

        List<CategoryEntity> expenses = LiveDataTestUtil.getValue(
                categoryDao.getByType("EXPENSE"));
        List<CategoryEntity> incomes = LiveDataTestUtil.getValue(
                categoryDao.getByType("INCOME"));

        assertEquals(2, expenses.size());
        assertEquals(1, incomes.size());
        assertEquals("급여", incomes.get(0).name);
    }

    // ── updateSortOrder ──

    @Test
    public void updateSortOrder_changesOrder() throws Exception {
        long id = categoryDao.insert(createCategory("식비", "EXPENSE", "a", 5));

        categoryDao.updateSortOrder(id, 99);

        CategoryEntity loaded = categoryDao.getById(id);
        assertEquals(99, loaded.sortOrder);
    }

    // ── getByNameAndType ──

    @Test
    public void getByNameAndType_findsMatch() {
        categoryDao.insert(createCategory("식비", "EXPENSE", "restaurant", 0));
        categoryDao.insert(createCategory("식비", "INCOME", "other", 0));

        CategoryEntity found = categoryDao.getByNameAndType("식비", "EXPENSE");
        assertNotNull(found);
        assertEquals("restaurant", found.iconName);
    }

    @Test
    public void getByNameAndType_noMatch_returnsNull() {
        assertNull(categoryDao.getByNameAndType("존재안함", "EXPENSE"));
    }

    // ── getDefaultCategoriesSync ──

    @Test
    public void getDefaultCategoriesSync_returnsOnlyDefaults() {
        CategoryEntity def = createCategory("기본", "EXPENSE", "a", 0);
        def.isDefault = true;
        categoryDao.insert(def);

        CategoryEntity custom = createCategory("커스텀", "EXPENSE", "b", 1);
        custom.isDefault = false;
        categoryDao.insert(custom);

        List<CategoryEntity> defaults = categoryDao.getDefaultCategoriesSync();
        assertEquals(1, defaults.size());
        assertEquals("기본", defaults.get(0).name);
    }

    // ── deleteAll ──

    @Test
    public void deleteAll_removesEverything() throws Exception {
        categoryDao.insert(createCategory("A", "EXPENSE", "a", 0));
        categoryDao.insert(createCategory("B", "INCOME", "b", 1));

        categoryDao.deleteAll();

        List<CategoryEntity> result = LiveDataTestUtil.getValue(categoryDao.getAll());
        assertTrue(result.isEmpty());
    }

    // ── updateDefaultCategoryName ──

    @Test
    public void updateDefaultCategoryName_updatesOnlyDefault() {
        CategoryEntity def = createCategory("원래", "EXPENSE", "a", 0);
        def.isDefault = true;
        long defId = categoryDao.insert(def);

        CategoryEntity custom = createCategory("원래", "EXPENSE", "b", 1);
        custom.isDefault = false;
        long customId = categoryDao.insert(custom);

        categoryDao.updateDefaultCategoryName(defId, "변경됨");

        assertEquals("변경됨", categoryDao.getById(defId).name);
        assertEquals("원래", categoryDao.getById(customId).name);
    }
}
