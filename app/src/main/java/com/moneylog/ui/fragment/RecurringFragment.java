package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.moneylog.R;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.ui.adapter.RecurringAdapter;
import com.moneylog.ui.viewmodel.RecurringViewModel;
import com.moneylog.util.FormatUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RecurringFragment extends Fragment implements RecurringAdapter.OnRecurringActionListener {

    private RecurringViewModel viewModel;
    private RecurringAdapter adapter;
    private RecyclerView rvRecurring;
    private TextView tvEmpty;
    private TextView btnExpense;
    private TextView btnIncome;
    private String selectedType = "EXPENSE";

    private final Map<Long, String> categoryNameMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recurring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RecurringViewModel.class);

        rvRecurring = view.findViewById(R.id.rvRecurring);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnExpense = view.findViewById(R.id.btnExpense);
        btnIncome = view.findViewById(R.id.btnIncome);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        // RecyclerView
        adapter = new RecurringAdapter(this);
        rvRecurring.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecurring.setAdapter(adapter);

        // Segment toggle
        updateSegmentToggle();
        btnExpense.setOnClickListener(v -> {
            selectedType = "EXPENSE";
            updateSegmentToggle();
            observeByType();
        });
        btnIncome.setOnClickListener(v -> {
            selectedType = "INCOME";
            updateSegmentToggle();
            observeByType();
        });

        // Categories
        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            categoryNameMap.clear();
            if (cats != null) {
                for (CategoryEntity c : cats) categoryNameMap.put(c.id, c.name);
            }
            adapter.setCategoryNameMap(categoryNameMap, getString(R.string.category_fallback));
            adapter.notifyDataSetChanged();
        });

        observeByType();

        // FAB
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabAddRecurring);
        fab.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("autoRecurring", true);
            Navigation.findNavController(v)
                    .navigate(R.id.action_recurring_to_form, args);
        });

        // Scroll shrink/expand
        rvRecurring.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) fab.shrink();
                else if (dy < 0) fab.extend();
            }
        });
    }

    private void observeByType() {
        adapter.setCategoryNameMap(categoryNameMap, getString(R.string.category_fallback));
        if ("EXPENSE".equals(selectedType)) {
            viewModel.expenses.observe(getViewLifecycleOwner(), this::submitList);
        } else {
            viewModel.incomes.observe(getViewLifecycleOwner(), this::submitList);
        }
    }

    private void submitList(List<RecurringEntity> items) {
        if (items == null || items.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRecurring.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvRecurring.setVisibility(View.VISIBLE);
        }
        adapter.submitList(items);
    }

    private void updateSegmentToggle() {
        boolean isExpense = "EXPENSE".equals(selectedType);
        btnExpense.setBackgroundResource(isExpense ? R.drawable.bg_segment_selected : 0);
        btnIncome.setBackgroundResource(!isExpense ? R.drawable.bg_segment_selected : 0);
        btnExpense.setTextColor(requireContext().getColor(
                isExpense ? R.color.md_theme_primary : R.color.md_theme_on_surface_variant));
        btnIncome.setTextColor(requireContext().getColor(
                !isExpense ? R.color.md_theme_primary : R.color.md_theme_on_surface_variant));
    }

    @Override
    public void onToggleActive(long id, boolean active) {
        viewModel.setActive(id, active);
    }

    @Override
    public void onLongPress(RecurringEntity item) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                .setTitle(R.string.delete)
                .setMessage(R.string.recurring_delete_confirm_msg)
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete(item.id))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public String formatSchedule(RecurringEntity rec) {
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
}
