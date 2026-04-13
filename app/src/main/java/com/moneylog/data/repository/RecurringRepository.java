package com.moneylog.data.repository;

import androidx.lifecycle.LiveData;

import com.moneylog.data.db.dao.RecurringDao;
import com.moneylog.data.db.entity.RecurringEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecurringRepository {

    private final RecurringDao dao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Inject
    public RecurringRepository(RecurringDao dao) {
        this.dao = dao;
    }

    public LiveData<List<RecurringEntity>> getAll() {
        return dao.getAll();
    }

    public LiveData<List<RecurringEntity>> getByType(String type) {
        return dao.getByType(type);
    }

    public void loadById(long id, TransactionRepository.Callback<RecurringEntity> cb) {
        executor.execute(() -> cb.onResult(dao.getById(id)));
    }

    public void save(RecurringEntity recurring) {
        executor.execute(() -> dao.insert(recurring));
    }

    public void update(RecurringEntity recurring) {
        executor.execute(() -> dao.update(recurring));
    }

    public void delete(long id) {
        executor.execute(() -> dao.delete(id));
    }

    public void setActive(long id, boolean active) {
        executor.execute(() -> dao.setActive(id, active, System.currentTimeMillis()));
    }

    public void getAllActiveInBackground(TransactionRepository.Callback<List<RecurringEntity>> cb) {
        executor.execute(() -> cb.onResult(dao.getAllActiveSync()));
    }
}
