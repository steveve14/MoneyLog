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
    private final MutableLiveData<Boolean> isAvailable;

    private final MutableLiveData<String> selectedYearMonth = new MutableLiveData<>();
    public final LiveData<List<TransactionEntity>> monthlyTransactions;

    @Inject
    public AiSummaryViewModel(AiSummaryRepository aiRepo, TransactionRepository transactionRepo) {
        this.aiRepo = aiRepo;
        this.transactionRepo = transactionRepo;
        isAvailable = new MutableLiveData<>(aiRepo.isAvailable());

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

    public LiveData<Boolean> getIsAvailable() {
        return isAvailable;
    }

    public void analyze(List<TransactionEntity> transactions) {
        if (!Boolean.TRUE.equals(isAvailable.getValue())) {
            summaryText.setValue("이 기기는 Gemini Nano를 지원하지 않습니다.");
            return;
        }

        String ym = selectedYearMonth.getValue();
        StringBuilder prompt = new StringBuilder();
        prompt.append(ym).append(" 지출/수입 패턴 분석 요청\n\n");

        if (transactions == null || transactions.isEmpty()) {
            prompt.append("해당 월에 거래 데이터가 없습니다.");
        } else {
            long totalIncome = 0, totalExpense = 0;
            for (TransactionEntity tx : transactions) {
                if ("INCOME".equals(tx.type)) totalIncome += tx.amount;
                else totalExpense += tx.amount;
            }
            prompt.append("총 수입: ").append(totalIncome).append("원\n");
            prompt.append("총 지출: ").append(totalExpense).append("원\n");
            prompt.append("거래 건수: ").append(transactions.size()).append("건\n\n");
            prompt.append("거래 내역:\n");
            for (TransactionEntity tx : transactions) {
                prompt.append("- ").append(tx.date).append(" | ")
                        .append(tx.type).append(" | ")
                        .append(tx.amount).append("원");
                if (tx.memo != null && !tx.memo.isEmpty()) {
                    prompt.append(" (").append(tx.memo).append(")");
                }
                prompt.append("\n");
            }
        }

        isLoading.setValue(true);
        aiRepo.generateMonthlySummary(prompt.toString(), new AiSummaryRepository.AiCallback() {
            @Override
            public void onSuccess(String summary) {
                isLoading.postValue(false);
                summaryText.postValue(summary);
            }

            @Override
            public void onFailure(String message) {
                isLoading.postValue(false);
                summaryText.postValue("분석 실패: " + message);
            }

            @Override
            public void onUnavailable() {
                isLoading.postValue(false);
                isAvailable.postValue(false);
                summaryText.postValue("이 기기는 Gemini Nano를 지원하지 않습니다.");
            }
        });
    }
}
