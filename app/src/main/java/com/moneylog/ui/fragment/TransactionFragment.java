package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneylog.R;
import com.moneylog.data.db.dao.DailySummary;
import com.moneylog.data.db.dao.MonthlySummary;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.databinding.FragmentTransactionBinding;
import com.moneylog.ui.adapter.TransactionAdapter;
import com.moneylog.ui.viewmodel.TransactionViewModel;
import com.moneylog.util.DateUtils;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.YearMonthPickerDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.DayOfWeek;
import java.time.YearMonth;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransactionFragment extends Fragment
        implements TransactionAdapter.OnTransactionClickListener {

    private FragmentTransactionBinding binding;
    private TransactionViewModel viewModel;
    private TransactionAdapter adapter;
    private Map<Long, CategoryEntity> categoryMap = new HashMap<>();
    private boolean calendarExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // RecyclerView 설정
        adapter = new TransactionAdapter(this);
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTransactions.setAdapter(adapter);

        // 월 선택 헤더
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());
        binding.tvYearMonth.setOnClickListener(v -> {
            String current = viewModel.getSelectedYearMonth().getValue();
            if (current != null) {
                YearMonthPickerDialog.show(requireContext(), current, viewModel::setYearMonth);
            }
        });

        // 타입 필터 칩
        binding.chipAll.setOnClickListener(v -> viewModel.setTypeFilter("ALL"));
        binding.chipExpense.setOnClickListener(v -> viewModel.setTypeFilter("EXPENSE"));
        binding.chipIncome.setOnClickListener(v -> viewModel.setTypeFilter("INCOME"));
        binding.chipRecurring.setOnClickListener(v -> viewModel.setTypeFilter("RECURRING"));

        // FAB → TransactionFormFragment
        binding.fabAdd.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_transaction_to_form));

        // 달력 토글
        binding.btnToggleCalendar.setOnClickListener(v -> {
            calendarExpanded = !calendarExpanded;
            binding.layoutCalendar.setVisibility(calendarExpanded ? View.VISIBLE : View.GONE);
        });

        // -- Observers ---
        viewModel.getSelectedYearMonth().observe(getViewLifecycleOwner(), ym ->
            binding.tvYearMonth.setText(DateUtils.toDisplayYearMonth(ym)));

        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            categoryMap.clear();
            if (cats != null) for (CategoryEntity c : cats) categoryMap.put(c.id, c);
            refreshList();
        });

        viewModel.transactions.observe(getViewLifecycleOwner(), txList -> refreshList());

        viewModel.getTypeFilter().observe(getViewLifecycleOwner(), filter -> refreshList());

        viewModel.monthlySummary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) return;
            binding.tvIncome.setText(FormatUtils.formatAmountWithUnit(summary.totalIncome));
            binding.tvExpense.setText(FormatUtils.formatAmountWithUnit(summary.totalExpense));
            binding.tvBalance.setText(
                FormatUtils.formatAmountWithUnit(summary.totalIncome - summary.totalExpense));
        });

        viewModel.dailySummary.observe(getViewLifecycleOwner(), dailyList -> {
            String ym = viewModel.getSelectedYearMonth().getValue();
            if (ym != null) renderCalendar(ym, dailyList);
        });
    }

    private void refreshList() {
        List<TransactionEntity> all = viewModel.transactions.getValue();
        if (all == null) all = Collections.emptyList();

        String filter = viewModel.getTypeFilter().getValue();
        if (filter == null) filter = "ALL";
        List<TransactionEntity> filtered = new ArrayList<>();
        for (TransactionEntity tx : all) {
            if ("ALL".equals(filter)) {
                filtered.add(tx);
            } else if ("RECURRING".equals(filter)) {
                if (tx.isAuto) filtered.add(tx);
            } else if (tx.type.equals(filter)) {
                filtered.add(tx);
            }
        }
        adapter.submitTransactions(filtered, categoryMap);

        boolean empty = filtered.isEmpty();
        binding.recyclerTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEdit(TransactionEntity tx) {
        Bundle args = new Bundle();
        args.putLong("transactionId", tx.id);
        Navigation.findNavController(requireView())
            .navigate(R.id.action_transaction_to_edit, args);
    }

    @Override
    public void onDelete(TransactionEntity tx) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("거래 삭제")
            .setMessage("이 거래를 삭제하시겠습니까?")
            .setPositiveButton("삭제", (d, w) -> viewModel.deleteTransaction(tx.id))
            .setNegativeButton("취소", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void renderCalendar(String yearMonthStr, List<DailySummary> dailyList) {
        binding.llCalendarGrid.removeAllViews();
        YearMonth ym = YearMonth.parse(yearMonthStr);
        int daysInMonth = ym.lengthOfMonth();
        // 1일의 요일: SUNDAY=7 → 0, MONDAY=1→1, ...
        DayOfWeek firstDow = ym.atDay(1).getDayOfWeek();
        int startOffset = firstDow.getValue() % 7; // SUN=0

        // 일별 합계 맵: day(1~31) → DailySummary
        Map<Integer, DailySummary> dayMap = new HashMap<>();
        if (dailyList != null) {
            for (DailySummary ds : dailyList) {
                if (ds.date != null && ds.date.length() >= 10) {
                    int day = Integer.parseInt(ds.date.substring(8, 10));
                    dayMap.put(day, ds);
                }
            }
        }

        int day = 1;
        int totalCells = startOffset + daysInMonth;
        int weeks = (totalCells + 6) / 7;

        for (int w = 0; w < weeks; w++) {
            LinearLayout weekRow = new LinearLayout(requireContext());
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int d = 0; d < 7; d++) {
                int cellIndex = w * 7 + d;
                LinearLayout cell = new LinearLayout(requireContext());
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                cellLp.setMargins(1, 2, 1, 2);
                cell.setLayoutParams(cellLp);

                if (cellIndex >= startOffset && day <= daysInMonth) {
                    // 날짜 텍스트
                    TextView tvDay = new TextView(requireContext());
                    tvDay.setText(String.valueOf(day));
                    tvDay.setGravity(Gravity.CENTER);
                    tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    if (d == 0) tvDay.setTextColor(requireContext().getColor(R.color.expense_color));
                    else if (d == 6) tvDay.setTextColor(requireContext().getColor(R.color.income_color));
                    cell.addView(tvDay);

                    // 일별 합계
                    DailySummary ds = dayMap.get(day);
                    if (ds != null) {
                        long net = ds.totalIncome - ds.totalExpense;
                        TextView tvSum = new TextView(requireContext());
                        tvSum.setGravity(Gravity.CENTER);
                        tvSum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
                        tvSum.setMaxLines(1);
                        if (net >= 0) {
                            tvSum.setText("+" + FormatUtils.formatAmount(net));
                            tvSum.setTextColor(requireContext().getColor(R.color.income_color));
                        } else {
                            tvSum.setText(FormatUtils.formatAmount(net));
                            tvSum.setTextColor(requireContext().getColor(R.color.expense_color));
                        }
                        cell.addView(tvSum);
                    }
                    day++;
                }
                weekRow.addView(cell);
            }
            binding.llCalendarGrid.addView(weekRow);
        }
    }
}
