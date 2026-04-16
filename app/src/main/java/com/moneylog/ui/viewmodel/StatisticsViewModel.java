package com.moneylog.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.moneylog.data.db.dao.CategorySummary;
import com.moneylog.data.db.dao.MonthlyTrend;
import com.moneylog.data.db.dao.MonthlySummary;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.repository.CategoryRepository;
import com.moneylog.data.repository.TransactionRepository;
import com.moneylog.util.DateUtils;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StatisticsViewModel extends ViewModel {

    private final MutableLiveData<String> selectedYearMonth;

    public final LiveData<MonthlySummary> monthlySummary;
    public final LiveData<List<CategorySummary>> categoryExpenses;
    public final LiveData<List<CategoryEntity>> categories;
    public final LiveData<List<MonthlyTrend>> monthlyTrend;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public StatisticsViewModel(TransactionRepository transactionRepo,
                               CategoryRepository categoryRepo) {
        selectedYearMonth = new MutableLiveData<>(DateUtils.currentYearMonth());

        monthlySummary = Transformations.switchMap(selectedYearMonth,
                ym -> transactionRepo.getMonthlySummary(ym));
        categoryExpenses = Transformations.switchMap(selectedYearMonth,
                ym -> transactionRepo.getMonthlyExpenseByCategory(ym));
        categories = categoryRepo.getAll();

        // 월별 추이: 선택된 월 기준 6개월 전부터
        monthlyTrend = Transformations.switchMap(selectedYearMonth, ym -> {
            String fromDate = DateUtils.monthsAgo(ym, 5);
            return transactionRepo.getMonthlyTrend(fromDate);
        });
    }

    public LiveData<String> getSelectedYearMonth() {
        return selectedYearMonth;
    }

    public void goToPreviousMonth() {
        String current = selectedYearMonth.getValue();
        if (current != null) {
            selectedYearMonth.setValue(DateUtils.previousMonth(current));
        }
    }

    public void goToNextMonth() {
        String current = selectedYearMonth.getValue();
        if (current != null) {
            selectedYearMonth.setValue(DateUtils.nextMonth(current));
        }
    }

    public void setYearMonth(String yearMonth) {
        selectedYearMonth.setValue(yearMonth);
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
