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
import com.moneylog.databinding.FragmentAiSummaryBinding;
import com.moneylog.ui.viewmodel.AiSummaryViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AiSummaryFragment extends Fragment {

    private FragmentAiSummaryBinding binding;
    private AiSummaryViewModel viewModel;

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

        viewModel.getIsAvailable().observe(getViewLifecycleOwner(), available -> {
            if (Boolean.TRUE.equals(available)) {
                binding.tvGeminiStatus.setText(R.string.gemini_status_supported);
                binding.tvGeminiStatus.setTextColor(
                        requireContext().getColor(R.color.income_color));
            } else {
                binding.tvGeminiStatus.setText(R.string.gemini_status_unsupported);
            }
            binding.btnAnalyze.setEnabled(Boolean.TRUE.equals(available));
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressAi.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            binding.tvAiSummary.setVisibility(Boolean.TRUE.equals(loading) ? View.GONE : View.VISIBLE);
        });

        viewModel.getSummaryText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) binding.tvAiSummary.setText(text);
        });

        binding.btnAnalyze.setOnClickListener(v ->
                viewModel.analyze("이번 달 지출 패턴 분석 요청"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
