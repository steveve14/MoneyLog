package com.moneylog.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.data.repository.CategoryRepository;
import com.moneylog.data.repository.RecurringRepository;
import com.moneylog.data.repository.TransactionRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RecurringViewModel extends ViewModel {

    private final RecurringRepository repo;

    public final LiveData<List<RecurringEntity>> expenses;
    public final LiveData<List<RecurringEntity>> incomes;
    public final LiveData<List<CategoryEntity>> categories;

    private final MutableLiveData<RecurringEntity> editingItem = new MutableLiveData<>();

    @Inject
    public RecurringViewModel(RecurringRepository repo, CategoryRepository categoryRepo) {
        this.repo = repo;
        expenses = repo.getByType("EXPENSE");
        incomes  = repo.getByType("INCOME");
        categories = categoryRepo.getAll();
    }

    public LiveData<RecurringEntity> getEditingItem() {
        return editingItem;
    }

    public void loadById(long id) {
        repo.loadById(id, result -> editingItem.postValue(result));
    }

    public void save(RecurringEntity item) {
        if (item.id == 0) {
            repo.save(item);
        } else {
            repo.update(item);
        }
    }

    public void delete(long id) {
        repo.delete(id);
    }

    public void setActive(long id, boolean active) {
        repo.setActive(id, active);
    }
}
