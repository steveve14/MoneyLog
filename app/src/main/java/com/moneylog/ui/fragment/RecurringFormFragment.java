package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.chip.Chip;
import com.moneylog.R;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.databinding.FragmentRecurringFormBinding;
import com.moneylog.ui.viewmodel.CategoryViewModel;
import com.moneylog.ui.viewmodel.RecurringViewModel;
import com.moneylog.util.FormatUtils;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RecurringFormFragment extends Fragment {

    private FragmentRecurringFormBinding binding;
    private RecurringViewModel viewModel;
    private CategoryViewModel categoryViewModel;

    private String selectedType = "EXPENSE";
    private String selectedPaymentMethod = "CARD";
    private String selectedInterval = "MONTHLY";
    private int selectedDayOfWeek = 1; // 1=월~7=일
    private long selectedCategoryId = 0;
    private boolean isEdit = false;
    private RecurringEntity editingItem = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecurringFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecurringViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // 수정 모드 확인
        if (getArguments() != null) {
            long recurringId = getArguments().getLong("recurringId", 0L);
            if (recurringId > 0) {
                isEdit = true;
                binding.tvTitle.setText("고정 거래 수정");
                binding.btnDelete.setVisibility(View.VISIBLE);
                viewModel.loadById(recurringId);
            }
        }

        // 유형 버튼
        binding.btnExpense.setOnClickListener(v -> setType("EXPENSE"));
        binding.btnIncome.setOnClickListener(v -> setType("INCOME"));
        setType("EXPENSE");

        // 결제 수단 칩
        binding.chipCard.setOnClickListener(v -> selectedPaymentMethod = "CARD");
        binding.chipCash.setOnClickListener(v -> selectedPaymentMethod = "CASH");
        binding.chipTransfer.setOnClickListener(v -> selectedPaymentMethod = "TRANSFER");
        binding.chipOther.setOnClickListener(v -> selectedPaymentMethod = "OTHER");
        binding.chipCard.setChecked(true);

        // 반복 주기 칩
        binding.chipDaily.setOnClickListener(v -> setInterval("DAILY"));
        binding.chipWeekly.setOnClickListener(v -> setInterval("WEEKLY"));
        binding.chipMonthly.setOnClickListener(v -> setInterval("MONTHLY"));
        binding.chipYearly.setOnClickListener(v -> setInterval("YEARLY"));

        // 요일 칩
        binding.chipMon.setOnClickListener(v -> selectedDayOfWeek = 1);
        binding.chipTue.setOnClickListener(v -> selectedDayOfWeek = 2);
        binding.chipWed.setOnClickListener(v -> selectedDayOfWeek = 3);
        binding.chipThu.setOnClickListener(v -> selectedDayOfWeek = 4);
        binding.chipFri.setOnClickListener(v -> selectedDayOfWeek = 5);
        binding.chipSat.setOnClickListener(v -> selectedDayOfWeek = 6);
        binding.chipSun.setOnClickListener(v -> selectedDayOfWeek = 7);
        binding.chipMon.setChecked(true);

        // 금액 자동 포맷
        binding.etAmount.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                long parsed = FormatUtils.parseAmountInput(s.toString());
                String formatted = FormatUtils.formatAmountInput(parsed);
                binding.etAmount.setText(formatted);
                binding.etAmount.setSelection(formatted.length());
                isFormatting = false;
            }
        });

        // 카테고리 칩 구성
        categoryViewModel.getCategoriesByType(selectedType)
            .observe(getViewLifecycleOwner(), this::buildCategoryChips);

        // 닫기
        binding.btnClose.setOnClickListener(v ->
            Navigation.findNavController(v).popBackStack());

        // 삭제
        binding.btnDelete.setOnClickListener(v -> {
            if (editingItem != null) {
                viewModel.delete(editingItem.id);
                Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(v).popBackStack();
            }
        });

        // 저장
        binding.btnSave.setOnClickListener(v -> submit());

        // 편집 데이터 로드
        viewModel.getEditingItem().observe(getViewLifecycleOwner(), item -> {
            if (item != null && isEdit && editingItem == null) {
                editingItem = item;
                fillForm(item);
            }
        });
    }

    private void setType(String type) {
        selectedType = type;
        boolean isExpense = "EXPENSE".equals(type);
        binding.btnExpense.setStrokeWidth(isExpense ? 5 : 0);
        binding.btnIncome.setStrokeWidth(isExpense ? 0 : 5);
        categoryViewModel.getCategoriesByType(type)
            .observe(getViewLifecycleOwner(), this::buildCategoryChips);
    }

    private void setInterval(String interval) {
        selectedInterval = interval;
        switch (interval) {
            case "DAILY":
                binding.chipGroupDayOfWeek.setVisibility(View.GONE);
                binding.tilDay.setVisibility(View.GONE);
                binding.tilMonth.setVisibility(View.GONE);
                break;
            case "WEEKLY":
                binding.chipGroupDayOfWeek.setVisibility(View.VISIBLE);
                binding.tilDay.setVisibility(View.GONE);
                binding.tilMonth.setVisibility(View.GONE);
                break;
            case "MONTHLY":
                binding.chipGroupDayOfWeek.setVisibility(View.GONE);
                binding.tilDay.setVisibility(View.VISIBLE);
                binding.tilMonth.setVisibility(View.GONE);
                break;
            case "YEARLY":
                binding.chipGroupDayOfWeek.setVisibility(View.GONE);
                binding.tilDay.setVisibility(View.VISIBLE);
                binding.tilMonth.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void buildCategoryChips(List<CategoryEntity> categories) {
        binding.chipGroupCategory.removeAllViews();
        if (categories == null) return;
        for (CategoryEntity cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat.name);
            chip.setCheckable(true);
            chip.setTag(cat.id);
            if (cat.id == selectedCategoryId) chip.setChecked(true);
            chip.setOnCheckedChangeListener((v, checked) -> {
                if (checked) selectedCategoryId = cat.id;
            });
            binding.chipGroupCategory.addView(chip);
        }
    }

    private void fillForm(RecurringEntity item) {
        binding.etAmount.setText(FormatUtils.formatAmountInput(item.amount));
        binding.etMemo.setText(item.memo);
        setType(item.type);
        selectedCategoryId = item.categoryId;
        selectedPaymentMethod = item.paymentMethod;

        // 주기 설정
        String interval = item.intervalType != null ? item.intervalType : "MONTHLY";
        selectedInterval = interval;
        binding.chipDaily.setChecked("DAILY".equals(interval));
        binding.chipWeekly.setChecked("WEEKLY".equals(interval));
        binding.chipMonthly.setChecked("MONTHLY".equals(interval));
        binding.chipYearly.setChecked("YEARLY".equals(interval));
        setInterval(interval);

        if ("WEEKLY".equals(interval)) {
            selectedDayOfWeek = item.dayOfMonth > 0 ? item.dayOfMonth : 1;
            binding.chipMon.setChecked(selectedDayOfWeek == 1);
            binding.chipTue.setChecked(selectedDayOfWeek == 2);
            binding.chipWed.setChecked(selectedDayOfWeek == 3);
            binding.chipThu.setChecked(selectedDayOfWeek == 4);
            binding.chipFri.setChecked(selectedDayOfWeek == 5);
            binding.chipSat.setChecked(selectedDayOfWeek == 6);
            binding.chipSun.setChecked(selectedDayOfWeek == 7);
        } else {
            binding.etDay.setText(String.valueOf(item.dayOfMonth));
        }

        if ("YEARLY".equals(interval)) {
            binding.etMonth.setText(String.valueOf(item.monthOfYear));
        }

        // 결제 수단 칩 반영
        binding.chipCard.setChecked("CARD".equals(item.paymentMethod));
        binding.chipCash.setChecked("CASH".equals(item.paymentMethod));
        binding.chipTransfer.setChecked("TRANSFER".equals(item.paymentMethod));
        binding.chipOther.setChecked("OTHER".equals(item.paymentMethod));
    }

    private void submit() {
        String amountStr = binding.etAmount.getText() != null ?
            binding.etAmount.getText().toString() : "";
        long amount = FormatUtils.parseAmountInput(amountStr);

        if (amount <= 0) {
            binding.tilAmount.setError("금액을 입력하세요");
            return;
        }
        binding.tilAmount.setError(null);

        int dayOfMonth = 0;
        int monthOfYear = 0;

        switch (selectedInterval) {
            case "DAILY":
                // 추가 입력 불필요
                break;
            case "WEEKLY":
                dayOfMonth = selectedDayOfWeek;
                break;
            case "MONTHLY": {
                String dayStr = binding.etDay.getText() != null ?
                    binding.etDay.getText().toString().trim() : "";
                try {
                    dayOfMonth = Integer.parseInt(dayStr);
                    if (dayOfMonth < 1 || dayOfMonth > 28) {
                        binding.tilDay.setError("1~28 사이의 날짜를 입력하세요");
                        return;
                    }
                } catch (NumberFormatException e) {
                    binding.tilDay.setError("실행일을 입력하세요");
                    return;
                }
                binding.tilDay.setError(null);
                break;
            }
            case "YEARLY": {
                String monthStr = binding.etMonth.getText() != null ?
                    binding.etMonth.getText().toString().trim() : "";
                String dayStr = binding.etDay.getText() != null ?
                    binding.etDay.getText().toString().trim() : "";
                try {
                    monthOfYear = Integer.parseInt(monthStr);
                    if (monthOfYear < 1 || monthOfYear > 12) {
                        binding.tilMonth.setError("1~12 사이의 월을 입력하세요");
                        return;
                    }
                } catch (NumberFormatException e) {
                    binding.tilMonth.setError("실행 월을 입력하세요");
                    return;
                }
                binding.tilMonth.setError(null);
                try {
                    dayOfMonth = Integer.parseInt(dayStr);
                    if (dayOfMonth < 1 || dayOfMonth > 28) {
                        binding.tilDay.setError("1~28 사이의 날짜를 입력하세요");
                        return;
                    }
                } catch (NumberFormatException e) {
                    binding.tilDay.setError("실행일을 입력하세요");
                    return;
                }
                binding.tilDay.setError(null);
                break;
            }
        }

        RecurringEntity item = isEdit && editingItem != null ? editingItem : new RecurringEntity();
        item.type = selectedType;
        item.amount = amount;
        item.categoryId = selectedCategoryId;
        item.intervalType = selectedInterval;
        item.dayOfMonth = dayOfMonth;
        item.monthOfYear = monthOfYear;
        item.memo = binding.etMemo.getText() != null ?
            binding.etMemo.getText().toString().trim() : "";
        item.paymentMethod = selectedPaymentMethod;
        item.updatedAt = System.currentTimeMillis();

        viewModel.save(item);
        Toast.makeText(requireContext(),
            isEdit ? "수정되었습니다" : "저장되었습니다", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
