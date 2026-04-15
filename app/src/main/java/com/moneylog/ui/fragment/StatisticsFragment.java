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

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.moneylog.R;
import com.moneylog.data.db.dao.CategorySummary;
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
            // 카테고리 로드 후 breakdown 갱신
            List<CategorySummary> summaries = viewModel.categoryExpenses.getValue();
            if (summaries != null) renderBreakdown(summaries);
        });

        viewModel.categoryExpenses.observe(getViewLifecycleOwner(), this::renderBreakdown);
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
        binding.pieChart.invalidate();

        // --- 파이차트 하단 범례 (색상 + 카테고리명) ---
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
