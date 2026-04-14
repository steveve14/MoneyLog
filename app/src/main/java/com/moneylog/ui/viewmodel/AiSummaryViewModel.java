package com.moneylog.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.data.repository.AiSummaryRepository;
import com.moneylog.data.repository.TransactionRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AiSummaryViewModel extends ViewModel {

    private final AiSummaryRepository aiRepo;
    private final TransactionRepository transactionRepo;

    private final MutableLiveData<String> summaryText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isGeminiAvailable;

    private final MutableLiveData<String> selectedYearMonth = new MutableLiveData<>();
    public final LiveData<List<TransactionEntity>> monthlyTransactions;

    @Inject
    public AiSummaryViewModel(AiSummaryRepository aiRepo, TransactionRepository transactionRepo) {
        this.aiRepo = aiRepo;
        this.transactionRepo = transactionRepo;
        isGeminiAvailable = new MutableLiveData<>(aiRepo.isGeminiAvailable());

        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        selectedYearMonth.setValue(now);

        monthlyTransactions = Transformations.switchMap(selectedYearMonth,
                ym -> transactionRepo.getByMonth(ym));
    }

    public LiveData<String> getSelectedYearMonth() {
        return selectedYearMonth;
    }

    public void setYearMonth(String yearMonth) {
        selectedYearMonth.setValue(yearMonth);
    }

    public void prevMonth() {
        String current = selectedYearMonth.getValue();
        if (current == null) return;
        YearMonth ym = YearMonth.parse(current);
        selectedYearMonth.setValue(ym.minusMonths(1).toString());
    }

    public void nextMonth() {
        String current = selectedYearMonth.getValue();
        if (current == null) return;
        YearMonth ym = YearMonth.parse(current);
        selectedYearMonth.setValue(ym.plusMonths(1).toString());
    }

    public LiveData<String> getSummaryText() {
        return summaryText;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsGeminiAvailable() {
        return isGeminiAvailable;
    }

    public void analyze(List<TransactionEntity> transactions) {
        String ym = selectedYearMonth.getValue();
        if (ym == null) return;

        isLoading.setValue(true);
        aiRepo.analyze(transactions, ym, new AiSummaryRepository.AiCallback() {
            @Override
            public void onSuccess(String summary) {
                isLoading.postValue(false);
                summaryText.postValue(summary);
            }

            @Override
            public void onFailure(String message) {
                isLoading.postValue(false);
                summaryText.postValue(message);
            }
        });
    }
}
