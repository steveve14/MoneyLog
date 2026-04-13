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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneylog.R;
import com.moneylog.data.db.dao.MonthlySummary;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.databinding.FragmentTransactionBinding;
import com.moneylog.ui.adapter.TransactionAdapter;
import com.moneylog.ui.viewmodel.TransactionViewModel;
import com.moneylog.util.DateUtils;
import com.moneylog.util.FormatUtils;

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
}
