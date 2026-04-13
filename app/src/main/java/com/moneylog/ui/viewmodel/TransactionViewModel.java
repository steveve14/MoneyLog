package com.moneylog.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.moneylog.data.db.dao.CategorySummary;
import com.moneylog.data.db.dao.DailySummary;
import com.moneylog.data.db.dao.MonthlySummary;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.data.repository.CategoryRepository;
import com.moneylog.data.repository.TransactionRepository;
import com.moneylog.util.DateUtils;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionViewModel extends ViewModel {

    private final TransactionRepository transactionRepo;
    private final CategoryRepository categoryRepo;

    // ── 선택된 연월 ────────────────────────────────────────────
    private final MutableLiveData<String> selectedYearMonth;

    // ── 타입 필터 ("ALL" | "EXPENSE" | "INCOME") ──────────────
    private final MutableLiveData<String> typeFilter;

    // ── 거래 목록 — 연월 변경 시 자동 갱신 ────────────────────
    public final LiveData<List<TransactionEntity>> transactions;

    // ── 월별 요약 ─────────────────────────────────────────────
    public final LiveData<MonthlySummary> monthlySummary;

    // ── 카테고리별 지출 ───────────────────────────────────────
    public final LiveData<List<CategorySummary>> categoryExpenses;

    // ── 일별 수입·지출 합계 ──────────────────────────────────
    public final LiveData<List<DailySummary>> dailySummary;

    // ── 전체 카테고리 ─────────────────────────────────────────
    public final LiveData<List<CategoryEntity>> categories;

    // ── 편집 중인 거래 ────────────────────────────────────────
    private final MutableLiveData<TransactionEntity> editingTransaction;

    @Inject
    public TransactionViewModel(TransactionRepository transactionRepo,
                                CategoryRepository categoryRepo) {
        this.transactionRepo = transactionRepo;
        this.categoryRepo = categoryRepo;

        selectedYearMonth = new MutableLiveData<>(DateUtils.currentYearMonth());
        typeFilter = new MutableLiveData<>("ALL");
        editingTransaction = new MutableLiveData<>();

        transactions = Transformations.switchMap(selectedYearMonth,
            ym -> transactionRepo.getByMonth(ym));
        monthlySummary = Transformations.switchMap(selectedYearMonth,
            ym -> transactionRepo.getMonthlySummary(ym));
        categoryExpenses = Transformations.switchMap(selectedYearMonth,
            ym -> transactionRepo.getMonthlyExpenseByCategory(ym));
        dailySummary = Transformations.switchMap(selectedYearMonth,
            ym -> transactionRepo.getDailySummary(ym));
        categories = categoryRepo.getAll();
    }

    // ── 접근자 ────────────────────────────────────────────────

    public LiveData<String> getSelectedYearMonth() {
        return selectedYearMonth;
    }

    public LiveData<String> getTypeFilter() {
        return typeFilter;
    }

    public LiveData<TransactionEntity> getEditingTransaction() {
        return editingTransaction;
    }

    // ── 이벤트 ────────────────────────────────────────────────

    public void previousMonth() {
        String cur = selectedYearMonth.getValue();
        if (cur != null) selectedYearMonth.setValue(DateUtils.previousMonth(cur));
    }

    public void nextMonth() {
        String cur = selectedYearMonth.getValue();
        if (cur != null) selectedYearMonth.setValue(DateUtils.nextMonth(cur));
    }

    public void setYearMonth(String yearMonth) {
        selectedYearMonth.setValue(yearMonth);
    }

    public void setTypeFilter(String filter) {
        typeFilter.setValue(filter);
    }

    public void loadTransaction(long id) {
        transactionRepo.loadById(id, result -> editingTransaction.postValue(result));
    }

    public void saveTransaction(TransactionEntity tx) {
        if (tx.id == 0) {
            transactionRepo.add(tx);
        } else {
            transactionRepo.update(tx);
        }
    }

    public void deleteTransaction(long id) {
        transactionRepo.delete(id);
    }
}
