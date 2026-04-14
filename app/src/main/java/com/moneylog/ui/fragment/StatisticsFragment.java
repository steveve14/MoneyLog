package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

        // PieChart 데이터 설정
        List<Float> pieValues = new ArrayList<>();
        for (CategorySummary cs : summaries) {
            pieValues.add((float) cs.total);
        }
        binding.pieChart.setData(pieValues);

        long totalExpense = 0;
        for (CategorySummary cs : summaries) totalExpense += cs.total;
        final long total = totalExpense;

        for (CategorySummary cs : summaries) {
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
