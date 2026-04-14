package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.moneylog.R;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.databinding.FragmentAiSummaryBinding;
import com.moneylog.ui.viewmodel.AiSummaryViewModel;
import com.moneylog.util.YearMonthPickerDialog;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AiSummaryFragment extends Fragment {

    private FragmentAiSummaryBinding binding;
    private AiSummaryViewModel viewModel;
    private List<TransactionEntity> currentTransactions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiSummaryViewModel.class);

        // 월 선택 UI
        viewModel.getSelectedYearMonth().observe(getViewLifecycleOwner(), ym -> {
            if (ym != null) {
                String[] parts = ym.split("-");
                binding.tvYearMonth.setText(parts[0] + "년 " + Integer.parseInt(parts[1]) + "월");
            }
        });

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.prevMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());
        binding.tvYearMonth.setOnClickListener(v -> {
            String current = viewModel.getSelectedYearMonth().getValue();
            if (current != null) {
                YearMonthPickerDialog.show(requireContext(), current, viewModel::setYearMonth);
            }
        });

        // 거래 데이터 관찰
        viewModel.monthlyTransactions.observe(getViewLifecycleOwner(), transactions ->
                currentTransactions = transactions);

        viewModel.getIsGeminiAvailable().observe(getViewLifecycleOwner(), available -> {
            if (Boolean.TRUE.equals(available)) {
                binding.tvGeminiStatus.setText(R.string.gemini_status_supported);
                binding.tvGeminiStatus.setTextColor(
                        requireContext().getColor(R.color.income_color));
            } else {
                binding.tvGeminiStatus.setText(R.string.ai_local_analysis);
            }
        });
        binding.btnAnalyze.setEnabled(true);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressAi.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            binding.tvAiSummary.setVisibility(Boolean.TRUE.equals(loading) ? View.GONE : View.VISIBLE);
        });

        viewModel.getSummaryText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) binding.tvAiSummary.setText(text);
        });

        binding.btnAnalyze.setOnClickListener(v ->
                viewModel.analyze(currentTransactions));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
