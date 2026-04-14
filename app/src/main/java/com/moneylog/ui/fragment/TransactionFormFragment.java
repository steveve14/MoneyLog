package com.moneylog.ui.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneylog.R;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.databinding.FragmentTransactionFormBinding;
import com.moneylog.ui.viewmodel.RecurringViewModel;
import com.moneylog.ui.viewmodel.TransactionViewModel;
import com.moneylog.util.DateUtils;
import com.moneylog.util.FormatUtils;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransactionFormFragment extends Fragment {

    private FragmentTransactionFormBinding binding;
    private TransactionViewModel viewModel;
    private RecurringViewModel recurringViewModel;

    private String selectedType = "EXPENSE";
    private long selectedCategoryId = 0L;
    private String selectedDate = DateUtils.today();
    private String selectedPaymentMethod = "CARD";
    private String selectedRecurringInterval = "MONTHLY";
    private boolean isEdit = false;
    private TransactionEntity editingTx = null;
    private boolean categoriesExpanded = false;
    private int collapsedHeight = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        recurringViewModel = new ViewModelProvider(this).get(RecurringViewModel.class);

        // 편집 모드 확인
        if (getArguments() != null) {
            long txId = getArguments().getLong("transactionId", 0L);
            if (txId > 0) {
                isEdit = true;
                viewModel.loadTransaction(txId);
            }
        }

        binding.tvTitle.setText(isEdit ? getString(R.string.transaction_edit) : getString(R.string.add_transaction));
        binding.tvDate.setText(DateUtils.toDisplayDate(selectedDate));

        // 닫기 버튼
        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        // 타입 탭
        binding.btnExpense.setOnClickListener(v -> setType("EXPENSE"));
        binding.btnIncome.setOnClickListener(v -> setType("INCOME"));
        setType("EXPENSE");

        // 날짜 선택
        binding.cardDate.setOnClickListener(v -> showDatePicker());

        // 결제 수단
        binding.chipCard.setOnClickListener(v -> selectedPaymentMethod = "CARD");
        binding.chipCash.setOnClickListener(v -> selectedPaymentMethod = "CASH");
        binding.chipTransfer.setOnClickListener(v -> selectedPaymentMethod = "TRANSFER");
        binding.chipOther.setOnClickListener(v -> selectedPaymentMethod = "OTHER");

        // 고정 거래 토글
        binding.switchRecurring.setOnCheckedChangeListener((btn, checked) ->
            binding.chipGroupRecurringInterval.setVisibility(checked ? View.VISIBLE : View.GONE));
        binding.chipRDaily.setOnClickListener(v -> selectedRecurringInterval = "DAILY");
        binding.chipRWeekly.setOnClickListener(v -> selectedRecurringInterval = "WEEKLY");
        binding.chipRMonthly.setOnClickListener(v -> selectedRecurringInterval = "MONTHLY");
        binding.chipRYearly.setOnClickListener(v -> selectedRecurringInterval = "YEARLY");

        // 금액 입력 자동 포맷
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

        // 저장 버튼
        binding.btnSave.setOnClickListener(v -> submit());

        // 삭제 버튼 (편집 모드에서만)
        if (isEdit) {
            binding.btnDelete.setVisibility(View.VISIBLE);
            binding.btnDelete.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.transaction_delete_title))
                    .setMessage(getString(R.string.transaction_delete_message))
                    .setPositiveButton(getString(R.string.delete), (d, w) -> {
                        if (editingTx != null) {
                            viewModel.deleteTransaction(editingTx.id);
                        }
                        Navigation.findNavController(requireView()).popBackStack();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show());
        }



        // 카테고리 옵저버
        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            if (cats != null) buildCategoryChips(cats);
        });

        // 편집 데이터 로드
        viewModel.getEditingTransaction().observe(getViewLifecycleOwner(), tx -> {
            if (tx != null && isEdit && editingTx == null) {
                editingTx = tx;
                setType(tx.type);
                binding.etAmount.setText(FormatUtils.formatAmountInput(tx.amount));
                selectedCategoryId = tx.categoryId;
                selectedDate = tx.date;
                binding.tvDate.setText(DateUtils.toDisplayDate(selectedDate));
                binding.etMemo.setText(tx.memo);
                selectedPaymentMethod = tx.paymentMethod;
                updatePaymentMethodChips(tx.paymentMethod);
            }
        });
    }

    private void setType(String type) {
        selectedType = type;
        boolean isExpense = "EXPENSE".equals(type);
        binding.btnExpense.setSelected(isExpense);
        binding.btnIncome.setSelected(!isExpense);

        // 배경 토글
        binding.btnExpense.setBackgroundResource(isExpense ? R.drawable.bg_segment_selected : 0);
        binding.btnIncome.setBackgroundResource(!isExpense ? R.drawable.bg_segment_selected : 0);

        // 텍스트 스타일 토글
        binding.btnExpense.setTextAppearance(isExpense
                ? com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
                : com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        binding.btnIncome.setTextAppearance(!isExpense
                ? com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
                : com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);

        int expenseColor = requireContext().getColor(R.color.expense_color);
        int incomeColor = requireContext().getColor(R.color.income_color);
        int defaultColor = requireContext().getColor(com.google.android.material.R.color.material_on_surface_emphasis_medium);
        binding.btnExpense.setTextColor(isExpense ? expenseColor : defaultColor);
        binding.btnIncome.setTextColor(!isExpense ? incomeColor : defaultColor);

        List<CategoryEntity> cats = viewModel.categories.getValue();
        if (cats != null) buildCategoryChips(cats);
    }

    private void buildCategoryChips(List<CategoryEntity> allCategories) {
        binding.chipGroupCategory.removeAllViews();
        binding.chipGroupCategory.setVisibility(View.VISIBLE);
        categoriesExpanded = false;
        for (CategoryEntity cat : allCategories) {
            if (!cat.type.equals(selectedType)) continue;
            Chip chip = new Chip(requireContext());
            chip.setText(cat.name);
            chip.setCheckable(true);
            chip.setChecked(cat.id == selectedCategoryId);
            chip.setOnClickListener(v -> selectedCategoryId = cat.id);
            binding.chipGroupCategory.addView(chip);
        }

        // 카테고리가 많으면 접기/펼치기
        binding.chipGroupCategory.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        binding.chipGroupCategory.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        int fullHeight = binding.chipGroupCategory.getHeight();
                        int oneRowHeight = (int) (48 * getResources().getDisplayMetrics().density);
                        if (fullHeight > oneRowHeight) {
                            collapsedHeight = oneRowHeight;
                            ViewGroup.LayoutParams lp = binding.chipGroupCategory.getLayoutParams();
                            lp.height = collapsedHeight;
                            binding.chipGroupCategory.setLayoutParams(lp);
                            binding.tvExpandCategories.setVisibility(View.VISIBLE);
                            binding.tvExpandCategories.setText(R.string.expand_categories);
                        } else {
                            binding.tvExpandCategories.setVisibility(View.GONE);
                        }
                    }
                });

        binding.tvExpandCategories.setOnClickListener(v -> {
            categoriesExpanded = !categoriesExpanded;
            ViewGroup.LayoutParams lp = binding.chipGroupCategory.getLayoutParams();
            if (categoriesExpanded) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                binding.tvExpandCategories.setText(R.string.collapse_categories);
            } else {
                lp.height = collapsedHeight;
                binding.tvExpandCategories.setText(R.string.expand_categories);
            }
            binding.chipGroupCategory.setLayoutParams(lp);
        });
    }

    private void showDatePicker() {
        LocalDate d = DateUtils.parseDate(selectedDate);
        new DatePickerDialog(requireContext(),
            (picker, year, month, day) -> {
                LocalDate picked = LocalDate.of(year, month + 1, day);
                selectedDate = DateUtils.formatDate(picked);
                binding.tvDate.setText(DateUtils.toDisplayDate(selectedDate));
            },
            d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()
        ).show();
    }

    private void updatePaymentMethodChips(String method) {
        binding.chipCard.setChecked("CARD".equals(method));
        binding.chipCash.setChecked("CASH".equals(method));
        binding.chipTransfer.setChecked("TRANSFER".equals(method));
        binding.chipOther.setChecked("OTHER".equals(method));
    }

    private void submit() {
        long amount = FormatUtils.parseAmountInput(
            binding.etAmount.getText().toString());
        if (amount <= 0 || selectedCategoryId == 0) return;

        long now = System.currentTimeMillis();
        TransactionEntity tx = (isEdit && editingTx != null) ? editingTx : new TransactionEntity();
        tx.type          = selectedType;
        tx.amount        = amount;
        tx.categoryId    = selectedCategoryId;
        tx.date          = selectedDate;
        tx.memo          = binding.etMemo.getText().toString().trim();
        tx.paymentMethod = selectedPaymentMethod;
        tx.updatedAt     = now;
        if (!isEdit) {
            tx.createdAt = now;
        }

        viewModel.saveTransaction(tx);

        // 고정 거래로 등록
        if (!isEdit && binding.switchRecurring.isChecked()) {
            tx.isAuto = true;
            viewModel.saveTransaction(tx);

            RecurringEntity recurring = new RecurringEntity();
            recurring.type = selectedType;
            recurring.amount = amount;
            recurring.categoryId = selectedCategoryId;
            recurring.intervalType = selectedRecurringInterval;
            recurring.memo = tx.memo;
            recurring.paymentMethod = selectedPaymentMethod;

            LocalDate date = DateUtils.parseDate(selectedDate);
            switch (selectedRecurringInterval) {
                case "WEEKLY":
                    recurring.dayOfMonth = date.getDayOfWeek().getValue(); // 1=월~7=일
                    break;
                case "YEARLY":
                    recurring.monthOfYear = date.getMonthValue();
                    recurring.dayOfMonth = date.getDayOfMonth();
                    break;
                case "MONTHLY":
                    recurring.dayOfMonth = Math.min(date.getDayOfMonth(), 28);
                    break;
                case "DAILY":
                default:
                    break;
            }
            recurringViewModel.save(recurring);
        }

        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
