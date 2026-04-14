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

import com.moneylog.R;
import com.moneylog.data.db.dao.CategorySummary;
import com.moneylog.data.db.entity.CategoryEntity;
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

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private Map<Long, String> categoryNameMap = new HashMap<>();

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
                binding.tvYearMonth.setText(DateUtils.toDisplayYearMonth(ym)));

        viewModel.monthlySummary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                binding.tvBalance.setText(getString(R.string.amount_zero));
                binding.tvIncome.setText(getString(R.string.amount_zero));
                binding.tvExpense.setText(getString(R.string.amount_zero));
                return;
            }
            long balance = summary.totalIncome - summary.totalExpense;
            binding.tvBalance.setText(FormatUtils.formatAmountWithUnit(balance));
            binding.tvIncome.setText(FormatUtils.formatAmountWithUnit(summary.totalIncome));
            binding.tvExpense.setText(FormatUtils.formatAmountWithUnit(summary.totalExpense));
        });

        viewModel.categoryExpenses.observe(getViewLifecycleOwner(), this::renderCategoryBars);
        viewModel.recentTransactions.observe(getViewLifecycleOwner(), this::renderRecentTransactions);
        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            categoryNameMap.clear();
            if (cats != null) {
                for (CategoryEntity c : cats) categoryNameMap.put(c.id, c.name);
            }
            renderRecentTransactions(viewModel.recentTransactions.getValue());
        });
    }

    private void renderCategoryBars(List<CategorySummary> summaries) {
        binding.llCategoryBars.removeAllViews();
        if (summaries == null || summaries.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoCategoryData.setVisibility(View.VISIBLE);
            return;
        }
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.tvNoCategoryData.setVisibility(View.GONE);

        int shown = Math.min(summaries.size(), 8);
        List<Float> values = new ArrayList<>();
        for (int i = 0; i < shown; i++) {
            values.add((float) summaries.get(i).total);
        }
        binding.pieChart.setData(values);

        for (int i = 0; i < shown; i++) {
            CategorySummary cs = summaries.get(i);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_bar, binding.llCategoryBars, false);

            TextView tvName = row.findViewById(R.id.tvCategoryName);
            TextView tvAmount = row.findViewById(R.id.tvCategoryAmount);
            View colorDot = row.findViewById(R.id.viewColorDot);

            String name = cs.categoryName != null ? cs.categoryName : (getString(R.string.category_unknown, cs.categoryId));
            boolean isIncome = "INCOME".equals(cs.type);
            String prefix = isIncome ? "+" : "-";
            tvName.setText(name);
            tvAmount.setText(prefix + FormatUtils.formatAmountWithUnit(cs.total));
            if (isIncome) {
                tvAmount.setTextColor(requireContext().getColor(R.color.income_color));
            }
            if (colorDot != null) {
                colorDot.setVisibility(View.VISIBLE);
                colorDot.getBackground().setTint(binding.pieChart.getColor(i));
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

            tvDate.setText(DateUtils.toDisplayDate(tx.date));

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
            tvAmount.setText(prefix + FormatUtils.formatAmountWithUnit(tx.amount));
            tvAmount.setTextColor(requireContext().getColor(
                    isIncome ? R.color.income_color : R.color.expense_color));

            binding.llRecentTransactions.addView(row);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
