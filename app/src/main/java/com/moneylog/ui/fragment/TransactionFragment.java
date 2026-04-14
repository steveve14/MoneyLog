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

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 월 네비게이션
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());
        binding.tvYearMonth.setOnClickListener(v -> {
            String current = viewModel.getSelectedYearMonth().getValue();
            if (current != null) {
                YearMonthPickerDialog.show(requireContext(), current, viewModel::setYearMonth);
            }
        });

        // 달력 토글
        binding.btnToggleCalendar.setOnClickListener(v -> {
            calendarExpanded = !calendarExpanded;
            binding.layoutCalendar.setVisibility(calendarExpanded ? View.VISIBLE : View.GONE);
            if (calendarExpanded) {
                renderCalendar(viewModel.dailySummary.getValue());
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

        // -- Observers ---
        viewModel.getSelectedYearMonth().observe(getViewLifecycleOwner(), ym ->
                binding.tvYearMonth.setText(DateUtils.toDisplayYearMonth(ym, requireContext())));

        viewModel.monthlySummary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                binding.tvIncome.setText(getString(R.string.amount_zero));
                binding.tvExpense.setText(getString(R.string.amount_zero));
                binding.tvBalance.setText(getString(R.string.amount_zero));
                return;
            }
            binding.tvIncome.setText(FormatUtils.formatAmountWithUnit(summary.totalIncome, requireContext()));
            binding.tvExpense.setText(FormatUtils.formatAmountWithUnit(summary.totalExpense, requireContext()));
            long balance = summary.totalIncome - summary.totalExpense;
            binding.tvBalance.setText(FormatUtils.formatAmountWithUnit(balance, requireContext()));
        });

        viewModel.dailySummary.observe(getViewLifecycleOwner(), dailies -> {
            if (calendarExpanded) renderCalendar(dailies);
        });

        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            categoryMap.clear();
            if (cats != null) for (CategoryEntity c : cats) categoryMap.put(c.id, c);
            refreshList();
        });

        viewModel.transactions.observe(getViewLifecycleOwner(), txList -> refreshList());

        viewModel.getTypeFilter().observe(getViewLifecycleOwner(), filter -> {
            updateChipSelection(filter);
            refreshList();
        });
    }

    private void updateChipSelection(String filter) {
        if (filter == null) filter = "ALL";
        android.util.TypedValue tv = new android.util.TypedValue();
        android.content.res.Resources.Theme theme = requireContext().getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, tv, true);
        int onPrimary = tv.data;
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, tv, true);
        int onSurfaceVariant = tv.data;

        setChipState(binding.chipAll,      "ALL".equals(filter),      onPrimary, onSurfaceVariant);
        setChipState(binding.chipExpense,  "EXPENSE".equals(filter),  onPrimary, onSurfaceVariant);
        setChipState(binding.chipIncome,   "INCOME".equals(filter),   onPrimary, onSurfaceVariant);
        setChipState(binding.chipRecurring,"RECURRING".equals(filter), onPrimary, onSurfaceVariant);
    }

    private void setChipState(TextView chip, boolean selected, int onPrimary, int onSurfaceVariant) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
        chip.setTextColor(selected ? onPrimary : onSurfaceVariant);
    }

    private void renderCalendar(List<DailySummary> dailies) {
        binding.llCalendarGrid.removeAllViews();
        String ym = viewModel.getSelectedYearMonth().getValue();
        if (ym == null) return;

        YearMonth yearMonth = YearMonth.parse(ym);
        int daysInMonth = yearMonth.lengthOfMonth();
        DayOfWeek firstDay = yearMonth.atDay(1).getDayOfWeek();
        int startOffset = firstDay.getValue() % 7; // 일=0, 월=1...

        Map<Integer, DailySummary> dayMap = new HashMap<>();
        if (dailies != null) {
            for (DailySummary ds : dailies) {
                try {
                    int day = Integer.parseInt(ds.date.substring(ds.date.lastIndexOf('-') + 1));
                    dayMap.put(day, ds);
                } catch (Exception ignored) {}
            }
        }

        LinearLayout weekRow = null;
        int cellIndex = 0;

        for (int i = 0; i < startOffset + daysInMonth; i++) {
            if (cellIndex % 7 == 0) {
                weekRow = new LinearLayout(requireContext());
                weekRow.setOrientation(LinearLayout.HORIZONTAL);
                weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                binding.llCalendarGrid.addView(weekRow);
            }

            LinearLayout cell = new LinearLayout(requireContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cellParams.topMargin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            cellParams.bottomMargin = cellParams.topMargin;
            cell.setLayoutParams(cellParams);

            if (i >= startOffset) {
                int day = i - startOffset + 1;

                TextView tvDay = new TextView(requireContext());
                tvDay.setText(String.valueOf(day));
                tvDay.setGravity(Gravity.CENTER);
                tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                cell.addView(tvDay);

                DailySummary ds = dayMap.get(day);
                if (ds != null && ds.totalExpense > 0) {
                    TextView tvExp = new TextView(requireContext());
                    tvExp.setText("-" + FormatUtils.formatCompact(ds.totalExpense, requireContext()));
                    tvExp.setGravity(Gravity.CENTER);
                    tvExp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
                    tvExp.setTextColor(requireContext().getColor(R.color.expense_color));
                    cell.addView(tvExp);
                }
                if (ds != null && ds.totalIncome > 0) {
                    TextView tvInc = new TextView(requireContext());
                    tvInc.setText("+" + FormatUtils.formatCompact(ds.totalIncome, requireContext()));
                    tvInc.setGravity(Gravity.CENTER);
                    tvInc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
                    tvInc.setTextColor(requireContext().getColor(R.color.income_color));
                    cell.addView(tvInc);
                }
            }

            if (weekRow != null) weekRow.addView(cell);
            cellIndex++;
        }

        // 마지막 주 빈 셀 채우기
        if (weekRow != null) {
            int remaining = 7 - (cellIndex % 7);
            if (remaining < 7) {
                for (int i = 0; i < remaining; i++) {
                    LinearLayout empty = new LinearLayout(requireContext());
                    empty.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    weekRow.addView(empty);
                }
            }
        }
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
        adapter.submitTransactions(filtered, categoryMap, requireContext());

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
            .setTitle(getString(R.string.transaction_delete_title))
            .setMessage(getString(R.string.transaction_delete_message))
            .setPositiveButton(getString(R.string.delete), (d, w) -> viewModel.deleteTransaction(tx.id))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
