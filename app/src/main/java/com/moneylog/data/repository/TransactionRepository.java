package com.moneylog.data.repository;

import androidx.lifecycle.LiveData;

import com.moneylog.data.db.dao.CategorySummary;
import com.moneylog.data.db.dao.MonthlySummary;
import com.moneylog.data.db.dao.TransactionDao;
import com.moneylog.data.db.entity.TransactionEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionRepository {

    private final TransactionDao dao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Inject
    public TransactionRepository(TransactionDao dao) {
        this.dao = dao;
    }

    public void add(TransactionEntity tx) {
        executor.execute(() -> dao.insert(tx));
    }

    public void update(TransactionEntity tx) {
        executor.execute(() -> dao.update(tx));
    }

    public void delete(long id) {
        executor.execute(() -> dao.softDelete(id, System.currentTimeMillis()));
    }

    public void loadById(long id, Callback<TransactionEntity> cb) {
        executor.execute(() -> cb.onResult(dao.getById(id)));
    }

    public LiveData<List<TransactionEntity>> getByMonth(String yearMonth) {
        return dao.getByMonth(yearMonth);
    }

    public LiveData<MonthlySummary> getMonthlySummary(String yearMonth) {
        return dao.getMonthlySummary(yearMonth);
    }

    public LiveData<List<CategorySummary>> getMonthlyExpenseByCategory(String yearMonth) {
        return dao.getMonthlyExpenseByCategory(yearMonth);
    }

    public LiveData<List<TransactionEntity>> getRecent(int limit) {
        return dao.getRecent(limit);
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
