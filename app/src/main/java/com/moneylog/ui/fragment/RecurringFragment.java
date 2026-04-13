package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.moneylog.R;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.databinding.FragmentRecurringBinding;
import com.moneylog.databinding.ItemRecurringBinding;
import com.moneylog.ui.viewmodel.RecurringViewModel;
import com.moneylog.util.FormatUtils;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RecurringFragment extends Fragment {

    private FragmentRecurringBinding binding;
    private RecurringViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecurringBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecurringViewModel.class);

        // 지출 목록 관찰
        viewModel.expenses.observe(getViewLifecycleOwner(), expenses -> {
            buildRecurringCards(binding.containerExpenses, expenses, "EXPENSE");
            int count = expenses != null ? expenses.size() : 0;
            binding.tvExpenseCount.setText(count + " ITEMS");
            updateSummary();
        });

        // 수입 목록 관찰
        viewModel.incomes.observe(getViewLifecycleOwner(), incomes -> {
            buildRecurringCards(binding.containerIncomes, incomes, "INCOME");
            int count = incomes != null ? incomes.size() : 0;
            binding.tvIncomeCount.setText(count + " ITEMS");
            updateSummary();
        });

        // FAB
        binding.fabAddRecurring.setOnClickListener(v ->
            Navigation.findNavController(v)
                .navigate(R.id.action_recurring_to_form));
    }

    private void buildRecurringCards(ViewGroup container,
                                     @Nullable List<RecurringEntity> items,
                                     String type) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (RecurringEntity item : items) {
            ItemRecurringBinding card = ItemRecurringBinding.inflate(inflater, container, false);

            card.tvName.setText(item.memo.isEmpty() ? "고정 " + (type.equals("EXPENSE") ? "지출" : "수입") : item.memo);
            card.tvSchedule.setText(getScheduleText(item));
            card.tvTypeBadge.setText(type.equals("EXPENSE") ? "DEBIT" : "CREDIT");
            card.tvAmount.setText(FormatUtils.formatAmount(item.amount));
            card.tvAmount.setTextColor(requireContext().getColor(
                type.equals("EXPENSE") ? R.color.expense_color : R.color.income_color));
            card.switchActive.setChecked(item.isActive);

            card.switchActive.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setActive(item.id, checked));

            // 카드 클릭 → 수정 폼
            card.getRoot().setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("recurringId", item.id);
                Navigation.findNavController(v)
                    .navigate(R.id.action_recurring_to_form, args);
            });

            container.addView(card.getRoot());
        }
    }

    private String getScheduleText(RecurringEntity item) {
        String interval = item.intervalType != null ? item.intervalType : "MONTHLY";
        switch (interval) {
            case "DAILY":
                return getString(R.string.schedule_daily);
            case "WEEKLY": {
                String[] days = getResources().getStringArray(R.array.day_of_week_names);
                int idx = (item.dayOfMonth >= 1 && item.dayOfMonth <= 7) ? item.dayOfMonth - 1 : 0;
                return getString(R.string.schedule_weekly, days[idx]);
            }
            case "YEARLY":
                return getString(R.string.schedule_yearly, item.monthOfYear, item.dayOfMonth);
            case "MONTHLY":
            default:
                return getString(R.string.recurring_schedule_format, item.dayOfMonth);
        }
    }

    private void updateSummary() {
        // 총 고정 지출 합계 계산
        List<RecurringEntity> exp = viewModel.expenses.getValue();
        long total = 0;
        int count = 0;
        if (exp != null) {
            for (RecurringEntity e : exp) {
                if (e.isActive) {
                    total += e.amount;
                    count++;
                }
            }
        }
        List<RecurringEntity> inc = viewModel.incomes.getValue();
        if (inc != null) {
            for (RecurringEntity e : inc) {
                if (e.isActive) count++;
            }
        }

        binding.tvTotalExpense.setText(FormatUtils.formatAmount(total));
        binding.tvItemCount.setText(count + " items scheduled this month");

        // AI Insight
        if (total > 0) {
            binding.tvAiInsight.setText(
                "이번 달 고정 지출은 " + FormatUtils.formatAmount(total) +
                "입니다. 예산을 설정하면 AI가 여유 금액을 분석해 드립니다.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
