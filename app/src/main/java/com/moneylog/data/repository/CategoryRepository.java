package com.moneylog.data.repository;

import androidx.lifecycle.LiveData;

import com.moneylog.data.db.dao.CategoryDao;
import com.moneylog.data.db.entity.CategoryEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CategoryRepository {

    private final CategoryDao dao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Inject
    public CategoryRepository(CategoryDao dao) {
        this.dao = dao;
    }

    public LiveData<List<CategoryEntity>> getAll() {
        return dao.getAll();
    }

    public LiveData<List<CategoryEntity>> getByType(String type) {
        return dao.getByType(type);
    }

    public void loadById(long id, TransactionRepository.Callback<CategoryEntity> cb) {
        executor.execute(() -> cb.onResult(dao.getById(id)));
    }

    public void add(CategoryEntity category) {
        executor.execute(() -> dao.insert(category));
    }

    public void update(CategoryEntity category) {
        executor.execute(() -> dao.update(category));
    }

    public void delete(long id) {
        executor.execute(() -> dao.softDelete(id));
    }

    public void updateSortOrders(List<CategoryEntity> categories) {
        executor.execute(() -> {
            for (int i = 0; i < categories.size(); i++) {
                dao.updateSortOrder(categories.get(i).id, i);
            }
        });
    }
}
