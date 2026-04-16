package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.moneylog.R;
import com.moneylog.data.db.dao.CategorySummary;
import com.moneylog.data.db.dao.MonthlyTrend;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.databinding.FragmentStatisticsBinding;
import com.moneylog.ui.viewmodel.StatisticsViewModel;
import com.moneylog.util.DateUtils;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.YearMonthPickerDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StatisticsFragment extends Fragment {

    private static final int[] CHART_COLORS = {
        0xFF506356, 0xFF7B9E87, 0xFF3D4C42, 0xFFA8C4B0,
        0xFF2E3A32, 0xFFBFD4C7, 0xFF6B8F7A, 0xFF8CB49E,
    };

    private FragmentStatisticsBinding binding;
    private StatisticsViewModel viewModel;
    private Map<Long, String> categoryNameMap = new HashMap<>();
    private String chartType = "EXPENSE";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
        binding.tvYearMonth.setOnClickListener(v -> {
            String current = viewModel.getSelectedYearMonth().getValue();
            if (current != null) {
                YearMonthPickerDialog.show(requireContext(), current, viewModel::setYearMonth);
            }
        });

        // 지출/수입 토글
        updateToggle();
        binding.btnStatExpense.setOnClickListener(v -> {
            chartType = "EXPENSE";
            updateToggle();
            refreshAll();
        });
        binding.btnStatIncome.setOnClickListener(v -> {
            chartType = "INCOME";
            updateToggle();
            refreshAll();
        });

        viewModel.getSelectedYearMonth().observe(getViewLifecycleOwner(), ym ->
                binding.tvYearMonth.setText(DateUtils.toDisplayYearMonth(ym, requireContext())));

        viewModel.monthlySummary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                binding.tvTotalIncome.setText(getString(R.string.amount_zero));
                binding.tvTotalExpense.setText(getString(R.string.amount_zero));
                return;
            }
            binding.tvTotalIncome.setText(FormatUtils.formatAmountWithUnit(summary.totalIncome, requireContext()));
            binding.tvTotalExpense.setText(FormatUtils.formatAmountWithUnit(summary.totalExpense, requireContext()));
        });

        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            categoryNameMap.clear();
            if (cats != null) {
                for (CategoryEntity c : cats) categoryNameMap.put(c.id, c.name);
            }
            refreshAll();
        });

        viewModel.categoryExpenses.observe(getViewLifecycleOwner(), summaries -> refreshAll());

        viewModel.monthlyTrend.observe(getViewLifecycleOwner(), this::renderTrendChart);

        // 스크롤 리스너: 헤더 숨김
        binding.scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (binding == null) return;
            int scrollY = binding.scrollView.getScrollY();
            int headerH = binding.llHeader.getHeight();

            // 헤더: 아래 스크롤 시 숨기고 위 스크롤 시 표시
            if (scrollY > headerH) {
                binding.llHeader.setVisibility(View.GONE);
            } else {
                binding.llHeader.setVisibility(View.VISIBLE);
            }
        });
    }

    private void refreshAll() {
        List<CategorySummary> summaries = viewModel.categoryExpenses.getValue();
        if (summaries == null) return;
        // chartType으로 필터링
        List<CategorySummary> filtered = new ArrayList<>();
        for (CategorySummary cs : summaries) {
            if (chartType.equals(cs.type)) filtered.add(cs);
        }
        renderBreakdown(filtered);
    }

    private void renderBreakdown(List<CategorySummary> summaries) {
        binding.llCategoryBreakdown.removeAllViews();
        if (summaries == null || summaries.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoData.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoData.setVisibility(View.GONE);
        binding.pieChart.setVisibility(View.VISIBLE);

        // 전체 합계 계산
        long grandTotal = 0;
        for (CategorySummary cs : summaries) grandTotal += cs.total;
        final long total = grandTotal;

        // 10% 이하 항목을 기타로 병합
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<CategorySummary> mainItems = new ArrayList<>();
        List<CategorySummary> othersItems = new ArrayList<>();
        long othersTotal = 0;
        int colorIdx = 0;

        for (CategorySummary cs : summaries) {
            double ratio = total > 0 ? (double) cs.total / total : 0.0;
            if (ratio <= 0.10) {
                othersTotal += cs.total;
                othersItems.add(cs);
            } else {
                String name = categoryNameMap.containsKey(cs.categoryId)
                        ? categoryNameMap.get(cs.categoryId)
                        : getString(R.string.category_unknown, cs.categoryId);
                entries.add(new PieEntry((float) cs.total, name));
                colors.add(CHART_COLORS[colorIdx % CHART_COLORS.length]);
                mainItems.add(cs);
                colorIdx++;
            }
        }

        if (othersTotal > 0) {
            entries.add(new PieEntry((float) othersTotal, getString(R.string.category_others)));
            colors.add(CHART_COLORS[colorIdx % CHART_COLORS.length]);
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

        // 차트 클릭: 도넛 중앙에 카테고리명 + 금액 표시
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
        binding.pieChart.setContentDescription(getString(R.string.chart_content_description));
        binding.llPieLegend.removeAllViews();
        for (int i = 0; i < mainItems.size(); i++) {
            CategorySummary cs = mainItems.get(i);
            View legendRow = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_bar, binding.llPieLegend, false);
            TextView tvLegendName = legendRow.findViewById(R.id.tvCategoryName);
            TextView tvLegendAmount = legendRow.findViewById(R.id.tvCategoryAmount);
            View colorDot = legendRow.findViewById(R.id.viewColorDot);
            String catName = categoryNameMap.containsKey(cs.categoryId)
                    ? categoryNameMap.get(cs.categoryId)
                    : getString(R.string.category_unknown, cs.categoryId);
            tvLegendName.setText(catName);
            tvLegendAmount.setVisibility(View.GONE);
            if (colorDot != null) {
                colorDot.setVisibility(View.VISIBLE);
                colorDot.getBackground().setTint(CHART_COLORS[i % CHART_COLORS.length]);
            }
            binding.llPieLegend.addView(legendRow);
        }
        if (othersTotal > 0) {
            View legendRow = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_bar, binding.llPieLegend, false);
            TextView tvLegendName = legendRow.findViewById(R.id.tvCategoryName);
            TextView tvLegendAmount = legendRow.findViewById(R.id.tvCategoryAmount);
            View colorDot = legendRow.findViewById(R.id.viewColorDot);
            tvLegendName.setText(getString(R.string.category_others));
            tvLegendAmount.setVisibility(View.GONE);
            if (colorDot != null) {
                colorDot.setVisibility(View.VISIBLE);
                colorDot.getBackground().setTint(CHART_COLORS[colorIdx % CHART_COLORS.length]);
            }
            binding.llPieLegend.addView(legendRow);
        }

        // --- 카테고리 상세 목록 (10% 초과 항목) ---
        for (CategorySummary cs : mainItems) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_statistics_category, binding.llCategoryBreakdown, false);

            TextView tvName = row.findViewById(R.id.tvCategoryName);
            TextView tvAmount = row.findViewById(R.id.tvCategoryAmount);
            TextView tvPercent = row.findViewById(R.id.tvCategoryPercent);
            ProgressBar progress = row.findViewById(R.id.progressCategory);

            String categoryName = categoryNameMap.containsKey(cs.categoryId)
                    ? categoryNameMap.get(cs.categoryId)
                    : getString(R.string.category_unknown, cs.categoryId);
            tvName.setText(categoryName);
            tvAmount.setText(FormatUtils.formatAmountWithUnit(cs.total, requireContext()));
            double ratio = total > 0 ? (double) cs.total / total : 0.0;
            tvPercent.setText(FormatUtils.formatPercent(ratio));
            progress.setProgress((int) (ratio * 100));

            binding.llCategoryBreakdown.addView(row);
        }

        // --- 기타 항목 (펼치기/접기) ---
        if (othersTotal > 0) {
            final long finalOthersTotal = othersTotal;

            View headerRow = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_statistics_category, binding.llCategoryBreakdown, false);

            TextView tvName = headerRow.findViewById(R.id.tvCategoryName);
            TextView tvAmount = headerRow.findViewById(R.id.tvCategoryAmount);
            TextView tvPercent = headerRow.findViewById(R.id.tvCategoryPercent);
            ProgressBar progress = headerRow.findViewById(R.id.progressCategory);

            double othersRatio = total > 0 ? (double) finalOthersTotal / total : 0.0;
            tvName.setText(getString(R.string.category_others) + "  ▼");
            tvAmount.setText(FormatUtils.formatAmountWithUnit(finalOthersTotal, requireContext()));
            tvPercent.setText(FormatUtils.formatPercent(othersRatio));
            progress.setProgress((int) (othersRatio * 100));

            // 하위 항목 컨테이너 (초기 구파)
            LinearLayout subContainer = new LinearLayout(requireContext());
            subContainer.setOrientation(LinearLayout.VERTICAL);
            subContainer.setVisibility(View.GONE);
            int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
            subContainer.setPaddingRelative(paddingPx, 0, 0, 0);

            for (CategorySummary cs : othersItems) {
                View subRow = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_statistics_category, subContainer, false);

                TextView subName = subRow.findViewById(R.id.tvCategoryName);
                TextView subAmount = subRow.findViewById(R.id.tvCategoryAmount);
                TextView subPercent = subRow.findViewById(R.id.tvCategoryPercent);
                ProgressBar subProgress = subRow.findViewById(R.id.progressCategory);

                String catName = categoryNameMap.containsKey(cs.categoryId)
                        ? categoryNameMap.get(cs.categoryId)
                        : getString(R.string.category_unknown, cs.categoryId);
                subName.setText(catName);
                subAmount.setText(FormatUtils.formatAmountWithUnit(cs.total, requireContext()));
                double subRatio = total > 0 ? (double) cs.total / total : 0.0;
                subPercent.setText(FormatUtils.formatPercent(subRatio));
                subProgress.setProgress((int) (subRatio * 100));

                subContainer.addView(subRow);
            }

            TypedValue ripple = new TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            headerRow.setBackgroundResource(ripple.resourceId);
            headerRow.setClickable(true);
            headerRow.setFocusable(true);
            headerRow.setOnClickListener(v -> {
                boolean expanded = subContainer.getVisibility() == View.VISIBLE;
                subContainer.setVisibility(expanded ? View.GONE : View.VISIBLE);
                tvName.setText(getString(R.string.category_others) + (expanded ? "  ▼" : "  ▲"));
            });

            binding.llCategoryBreakdown.addView(headerRow);
            binding.llCategoryBreakdown.addView(subContainer);
        }
    }

    private void renderTrendChart(List<MonthlyTrend> trends) {
        LineChart chart = binding.lineChart;
        if (trends == null || trends.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        List<Entry> expenseEntries = new ArrayList<>();
        List<Entry> incomeEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < trends.size(); i++) {
            MonthlyTrend t = trends.get(i);
            expenseEntries.add(new Entry(i, t.totalExpense));
            incomeEntries.add(new Entry(i, t.totalIncome));
            // "MM" 형식 라벨
            labels.add(t.yearMonth.length() >= 7 ? t.yearMonth.substring(5) : t.yearMonth);
        }

        LineDataSet expenseDs = new LineDataSet(expenseEntries, getString(R.string.transaction_expense));
        expenseDs.setColor(requireContext().getColor(R.color.expense_color));
        expenseDs.setCircleColor(requireContext().getColor(R.color.expense_color));
        expenseDs.setLineWidth(2f);
        expenseDs.setCircleRadius(3f);
        expenseDs.setDrawValues(false);
        expenseDs.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet incomeDs = new LineDataSet(incomeEntries, getString(R.string.transaction_income));
        incomeDs.setColor(requireContext().getColor(R.color.income_color));
        incomeDs.setCircleColor(requireContext().getColor(R.color.income_color));
        incomeDs.setLineWidth(2f);
        incomeDs.setCircleRadius(3f);
        incomeDs.setDrawValues(false);
        incomeDs.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(expenseDs, incomeDs);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(requireContext().getColor(R.color.md_theme_outline_variant));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.animateX(500);
        chart.invalidate();
    }

    private void updateToggle() {
        boolean isExpense = "EXPENSE".equals(chartType);
        binding.btnStatExpense.setBackgroundResource(isExpense ? R.drawable.bg_segment_selected : 0);
        binding.btnStatIncome.setBackgroundResource(!isExpense ? R.drawable.bg_segment_selected : 0);
        binding.btnStatExpense.setTextColor(requireContext().getColor(
                isExpense ? R.color.md_theme_primary : R.color.md_theme_on_surface_variant));
        binding.btnStatIncome.setTextColor(requireContext().getColor(
                !isExpense ? R.color.md_theme_primary : R.color.md_theme_on_surface_variant));

        // 차트 타이틀 & 비교 카드 라벨 동기화
        binding.tvChartTitle.setText(isExpense
                ? R.string.statistics_spending_trend : R.string.statistics_income_trend);
        binding.tvComparisonLabel.setText(isExpense
                ? R.string.statistics_spending : R.string.statistics_income_label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
