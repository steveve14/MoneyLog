package com.moneylog.ui.fragment;

import android.os.Bundle;
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

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.moneylog.R;
import com.moneylog.data.db.dao.CategorySummary;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.databinding.FragmentDashboardBinding;
import com.moneylog.ui.viewmodel.DashboardViewModel;
import com.moneylog.util.DateUtils;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.YearMonthPickerDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private static final int[] CHART_COLORS = {
        0xFF506356, 0xFF7B9E87, 0xFF3D4C42, 0xFFA8C4B0,
        0xFF2E3A32, 0xFFBFD4C7, 0xFF6B8F7A, 0xFF8CB49E,
    };

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private Map<Long, String> categoryNameMap = new HashMap<>();
    private String chartType = "EXPENSE";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
        binding.tvYearMonth.setOnClickListener(v -> {
            String current = viewModel.getSelectedYearMonth().getValue();
            if (current != null) {
                YearMonthPickerDialog.show(requireContext(), current, viewModel::setYearMonth);
            }
        });
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.settingsFragment));
        binding.btnSeeAll.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.transactionFragment));

        viewModel.getSelectedYearMonth().observe(getViewLifecycleOwner(), ym ->
                binding.tvYearMonth.setText(DateUtils.toDisplayYearMonth(ym, requireContext())));

        viewModel.monthlySummary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                binding.tvBalance.setText(getString(R.string.amount_zero));
                binding.tvIncome.setText(getString(R.string.amount_zero));
                binding.tvExpense.setText(getString(R.string.amount_zero));
                return;
            }
            long balance = summary.totalIncome - summary.totalExpense;
            binding.tvBalance.setText(FormatUtils.formatAmountWithUnit(balance, requireContext()));
            binding.tvIncome.setText(FormatUtils.formatAmountWithUnit(summary.totalIncome, requireContext()));
            binding.tvExpense.setText(FormatUtils.formatAmountWithUnit(summary.totalExpense, requireContext()));
        });

        viewModel.categoryExpenses.observe(getViewLifecycleOwner(), this::renderCategoryBars);
        viewModel.recentTransactions.observe(getViewLifecycleOwner(), this::renderRecentTransactions);

        // Chart type toggle
        updateChartToggle();
        binding.btnChartExpense.setOnClickListener(v -> {
            chartType = "EXPENSE";
            updateChartToggle();
            renderCategoryBars(viewModel.categoryExpenses.getValue());
        });
        binding.btnChartIncome.setOnClickListener(v -> {
            chartType = "INCOME";
            updateChartToggle();
            renderCategoryBars(viewModel.categoryExpenses.getValue());
        });
        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            categoryNameMap.clear();
            if (cats != null) {
                for (CategoryEntity c : cats) categoryNameMap.put(c.id, c.name);
            }
            renderRecentTransactions(viewModel.recentTransactions.getValue());
            renderRecurringItems(viewModel.allRecurring.getValue());
        });

        viewModel.allRecurring.observe(getViewLifecycleOwner(), this::renderRecurringItems);

        binding.btnRecurringSeeMore.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.recurringFragment));
    }

    private void renderCategoryBars(List<CategorySummary> summaries) {
        binding.llCategoryBars.removeAllViews();
        if (summaries == null || summaries.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoCategoryData.setVisibility(View.VISIBLE);
            return;
        }

        // Filter by selected chart type
        List<CategorySummary> filtered = new ArrayList<>();
        for (CategorySummary cs : summaries) {
            if (chartType.equals(cs.type)) filtered.add(cs);
        }
        if (filtered.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoCategoryData.setVisibility(View.VISIBLE);
            return;
        }

        binding.pieChart.setVisibility(View.VISIBLE);
        binding.tvNoCategoryData.setVisibility(View.GONE);

        // 전체 합계 계산 (최대 8개)
        int shown = Math.min(filtered.size(), 8);
        long grandTotal = 0;
        for (int i = 0; i < shown; i++) grandTotal += filtered.get(i).total;

        // 10% 이하 항목을 기타로 병합
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<CategorySummary> legendItems = new ArrayList<>();
        long othersTotal = 0;
        int colorIdx = 0;

        for (int i = 0; i < shown; i++) {
            CategorySummary cs = filtered.get(i);
            double ratio = grandTotal > 0 ? (double) cs.total / grandTotal : 0.0;
            if (ratio <= 0.10) {
                othersTotal += cs.total;
            } else {
                String name = cs.categoryName != null ? cs.categoryName : getString(R.string.category_unknown, cs.categoryId);
                entries.add(new PieEntry((float) cs.total, name));
                colors.add(CHART_COLORS[colorIdx % CHART_COLORS.length]);
                legendItems.add(cs);
                colorIdx++;
            }
        }

        int othersColorIdx = -1;
        if (othersTotal > 0) {
            entries.add(new PieEntry((float) othersTotal, getString(R.string.category_others)));
            othersColorIdx = colorIdx % CHART_COLORS.length;
            colors.add(CHART_COLORS[othersColorIdx]);
        }

        // --- MPAndroidChart PieChart 설정 ---
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2.5f);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        binding.pieChart.setData(data);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleRadius(60f);
        binding.pieChart.setTransparentCircleRadius(64f);
        binding.pieChart.setHoleColor(0x00000000);
        binding.pieChart.setTransparentCircleColor(0x00000000);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.getLegend().setEnabled(false);
        binding.pieChart.setRotationEnabled(false);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.animateY(700);

        // Chart click: show amount in center hole
        final long total = grandTotal;
        binding.pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;
                    String prefix = "INCOME".equals(chartType) ? "+" : "-";
                    binding.pieChart.setCenterText(pe.getLabel() + "\n"
                            + prefix + FormatUtils.formatAmountWithUnit((long) pe.getValue(), requireContext()));
                    binding.pieChart.setCenterTextSize(12f);
                    binding.pieChart.setCenterTextColor(requireContext().getColor(
                            "INCOME".equals(chartType) ? R.color.income_color : R.color.expense_color));
                }
            }
            @Override
            public void onNothingSelected() {
                binding.pieChart.setCenterText("");
            }
        });

        binding.pieChart.invalidate();

        // --- 카테고리 범례 (10% 초과 항목) ---
        for (int i = 0; i < legendItems.size(); i++) {
            CategorySummary cs = legendItems.get(i);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_bar, binding.llCategoryBars, false);

            TextView tvName = row.findViewById(R.id.tvCategoryName);
            TextView tvAmount = row.findViewById(R.id.tvCategoryAmount);
            View colorDot = row.findViewById(R.id.viewColorDot);

            String name = cs.categoryName != null ? cs.categoryName : getString(R.string.category_unknown, cs.categoryId);
            tvName.setText(name);
            tvAmount.setVisibility(View.GONE);
            if (colorDot != null) {
                colorDot.setVisibility(View.VISIBLE);
                colorDot.getBackground().setTint(CHART_COLORS[i % CHART_COLORS.length]);
            }
            binding.llCategoryBars.addView(row);
        }

        // --- 기타 범례 ---
        if (othersTotal > 0) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_bar, binding.llCategoryBars, false);
            TextView tvName = row.findViewById(R.id.tvCategoryName);
            TextView tvAmount = row.findViewById(R.id.tvCategoryAmount);
            View colorDot = row.findViewById(R.id.viewColorDot);
            tvName.setText(getString(R.string.category_others));
            tvAmount.setVisibility(View.GONE);
            if (colorDot != null) {
                colorDot.setVisibility(View.VISIBLE);
                colorDot.getBackground().setTint(CHART_COLORS[othersColorIdx]);
            }
            binding.llCategoryBars.addView(row);
        }
    }

    private void renderRecentTransactions(List<TransactionEntity> transactions) {
        binding.llRecentTransactions.removeAllViews();
        if (transactions == null || transactions.isEmpty()) {
            binding.tvNoRecentData.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoRecentData.setVisibility(View.GONE);

        for (TransactionEntity tx : transactions) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_dashboard_transaction, binding.llRecentTransactions, false);

            TextView tvDate = row.findViewById(R.id.tvTxDate);
            TextView tvMemo = row.findViewById(R.id.tvTxMemo);
            TextView tvAmount = row.findViewById(R.id.tvTxAmount);
            TextView tvCategory = row.findViewById(R.id.tvTxCategory);

            tvDate.setText(DateUtils.toDisplayDate(tx.date, requireContext()));

            String catName = categoryNameMap.get(tx.categoryId);
            if (catName != null) {
                tvCategory.setText(catName);
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            tvMemo.setText(tx.memo != null && !tx.memo.isEmpty() ? tx.memo : "");
            tvMemo.setVisibility(tx.memo != null && !tx.memo.isEmpty() ? View.VISIBLE : View.GONE);

            boolean isIncome = "INCOME".equals(tx.type);
            String prefix = isIncome ? "+" : "-";
            tvAmount.setText(prefix + FormatUtils.formatAmountWithUnit(tx.amount, requireContext()));
            tvAmount.setTextColor(requireContext().getColor(
                    isIncome ? R.color.income_color : R.color.expense_color));

            binding.llRecentTransactions.addView(row);
        }
    }

    private static final int MAX_RECURRING_ITEMS = 5;

    private void renderRecurringItems(List<RecurringEntity> items) {
        binding.llRecurringItems.removeAllViews();
        if (items == null || items.isEmpty()) {
            binding.tvNoRecurringData.setVisibility(View.VISIBLE);
            binding.btnRecurringSeeMore.setVisibility(View.GONE);
            return;
        }
        binding.tvNoRecurringData.setVisibility(View.GONE);
        binding.btnRecurringSeeMore.setVisibility(View.VISIBLE);

        int count = Math.min(items.size(), MAX_RECURRING_ITEMS);
        for (int i = 0; i < count; i++) {
            RecurringEntity rec = items.get(i);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_recurring_dashboard, binding.llRecurringItems, false);

            TextView tvCategory = row.findViewById(R.id.tvRecCategory);
            TextView tvMemo = row.findViewById(R.id.tvRecMemo);
            TextView tvSchedule = row.findViewById(R.id.tvRecSchedule);
            TextView tvAmount = row.findViewById(R.id.tvRecAmount);
            MaterialSwitch switchActive = row.findViewById(R.id.switchRecActive);

            String catName = categoryNameMap.get(rec.categoryId);
            tvCategory.setText(catName != null ? catName : getString(R.string.category_fallback));

            if (rec.memo != null && !rec.memo.isEmpty()) {
                tvMemo.setText(rec.memo);
                tvMemo.setVisibility(View.VISIBLE);
            } else {
                tvMemo.setVisibility(View.GONE);
            }

            tvSchedule.setText(formatSchedule(rec));

            boolean isIncome = "INCOME".equals(rec.type);
            String prefix = isIncome ? "+" : "-";
            tvAmount.setText(prefix + FormatUtils.formatAmountWithUnit(rec.amount, requireContext()));
            tvAmount.setTextColor(requireContext().getColor(
                    isIncome ? R.color.income_color : R.color.expense_color));

            if (!rec.isActive) {
                tvCategory.setAlpha(0.4f);
                tvMemo.setAlpha(0.4f);
                tvSchedule.setAlpha(0.4f);
                tvAmount.setAlpha(0.4f);
            }

            switchActive.setChecked(rec.isActive);
            switchActive.setOnCheckedChangeListener((btn, checked) ->
                    viewModel.setRecurringActive(rec.id, checked));

            binding.llRecurringItems.addView(row);
        }
    }

    private String formatSchedule(RecurringEntity rec) {
        switch (rec.intervalType) {
            case "DAILY":
                return getString(R.string.schedule_daily);
            case "WEEKLY":
                int dow = rec.dayOfMonth;
                String[] days = getResources().getStringArray(R.array.day_of_week_names);
                String dayName = (dow >= 1 && dow <= 7) ? days[dow - 1] : "?";
                return getString(R.string.schedule_weekly, dayName);
            case "YEARLY":
                return getString(R.string.schedule_yearly, rec.monthOfYear, rec.dayOfMonth);
            case "MONTHLY":
            default:
                return getString(R.string.recurring_schedule_format, rec.dayOfMonth);
        }
    }

    private void updateChartToggle() {
        boolean isExpense = "EXPENSE".equals(chartType);
        binding.btnChartExpense.setBackgroundResource(isExpense ? R.drawable.bg_segment_selected : 0);
        binding.btnChartIncome.setBackgroundResource(!isExpense ? R.drawable.bg_segment_selected : 0);
        binding.btnChartExpense.setTextColor(requireContext().getColor(
                isExpense ? R.color.md_theme_primary : R.color.md_theme_on_surface_variant));
        binding.btnChartIncome.setTextColor(requireContext().getColor(
                !isExpense ? R.color.md_theme_primary : R.color.md_theme_on_surface_variant));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
