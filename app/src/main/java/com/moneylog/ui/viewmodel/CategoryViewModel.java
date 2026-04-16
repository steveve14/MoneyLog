package com.moneylog.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.repository.CategoryRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CategoryViewModel extends ViewModel {

    private final CategoryRepository repo;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public CategoryViewModel(CategoryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<CategoryEntity>> getAll() {
        return repo.getAll();
    }

    public LiveData<List<CategoryEntity>> getCategoriesByType(String type) {
        return repo.getByType(type);
    }

    public void save(CategoryEntity category) {
        repo.add(category);
    }

    public void update(CategoryEntity category) {
        repo.update(category);
    }

    public void delete(long id) {
        repo.delete(id);
    }

    public void updateSortOrders(List<CategoryEntity> categories) {
        repo.updateSortOrders(categories);
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
