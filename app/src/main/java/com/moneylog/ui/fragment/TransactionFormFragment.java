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
import com.google.android.material.snackbar.Snackbar;
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
    private int selectedDayOfWeek = 1;   // 1=월 ~ 7=일
    private int selectedDayOfMonth = 1;
    private int selectedMonth = 1;
    private boolean isEdit = false;
    private boolean isRecurringEdit = false;
    private TransactionEntity editingTx = null;
    private RecurringEntity editingRecurring = null;
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
            long recId = getArguments().getLong("recurringId", 0L);
            if (txId > 0) {
                isEdit = true;
                viewModel.loadTransaction(txId);
            } else if (recId > 0) {
                isEdit = true;
                isRecurringEdit = true;
                recurringViewModel.loadById(recId);
            }
            // 고정거래 화면에서 진입 시 스위치 자동 선택
            if (getArguments().getBoolean("autoRecurring", false)) {
                binding.switchRecurring.setChecked(true);
                binding.chipGroupRecurringInterval.setVisibility(View.VISIBLE);
            }
        }

        binding.tvTitle.setText(isRecurringEdit
                ? getString(R.string.recurring_form_edit)
                : (isEdit ? getString(R.string.transaction_edit) : getString(R.string.add_transaction)));
        binding.tvDate.setText(DateUtils.toDisplayDate(selectedDate, requireContext()));

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
        binding.switchRecurring.setOnCheckedChangeListener((btn, checked) -> {
            binding.chipGroupRecurringInterval.setVisibility(checked ? View.VISIBLE : View.GONE);
            binding.tvRecurringHint.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked) {
                binding.cardDate.setEnabled(false);
                binding.cardDate.setClickable(false);
                binding.cardDate.setAlpha(0.4f);
                updateScheduleInputVisibility(selectedRecurringInterval);
            } else {
                binding.cardDate.setEnabled(true);
                binding.cardDate.setClickable(true);
                binding.cardDate.setAlpha(1.0f);
                hideAllScheduleInputs();
            }
        });
        binding.chipRDaily.setOnClickListener(v -> {
            selectedRecurringInterval = "DAILY";
            updateScheduleInputVisibility("DAILY");
        });
        binding.chipRWeekly.setOnClickListener(v -> {
            selectedRecurringInterval = "WEEKLY";
            updateScheduleInputVisibility("WEEKLY");
        });
        binding.chipRMonthly.setOnClickListener(v -> {
            selectedRecurringInterval = "MONTHLY";
            updateScheduleInputVisibility("MONTHLY");
        });
        binding.chipRYearly.setOnClickListener(v -> {
            selectedRecurringInterval = "YEARLY";
            updateScheduleInputVisibility("YEARLY");
        });

        // 요일 선택 칩
        binding.chipDowMon.setOnClickListener(v -> selectedDayOfWeek = 1);
        binding.chipDowTue.setOnClickListener(v -> selectedDayOfWeek = 2);
        binding.chipDowWed.setOnClickListener(v -> selectedDayOfWeek = 3);
        binding.chipDowThu.setOnClickListener(v -> selectedDayOfWeek = 4);
        binding.chipDowFri.setOnClickListener(v -> selectedDayOfWeek = 5);
        binding.chipDowSat.setOnClickListener(v -> selectedDayOfWeek = 6);
        binding.chipDowSun.setOnClickListener(v -> selectedDayOfWeek = 7);

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
            binding.btnDelete.setOnClickListener(v -> {
                String title = isRecurringEdit
                        ? getString(R.string.delete)
                        : getString(R.string.transaction_delete_title);
                String message = isRecurringEdit
                        ? getString(R.string.recurring_delete_confirm_msg)
                        : getString(R.string.transaction_delete_message);
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.delete), (d, w) -> {
                        if (isRecurringEdit && editingRecurring != null) {
                            recurringViewModel.delete(editingRecurring.id);
                        } else if (editingTx != null) {
                            viewModel.deleteTransaction(editingTx.id);
                        }
                        Snackbar.make(binding.getRoot(), R.string.msg_deleted, Snackbar.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            });
        }



        // 카테고리 옵저버
        viewModel.categories.observe(getViewLifecycleOwner(), cats -> {
            if (cats != null) buildCategoryChips(cats);
        });

        // 내역에서 거래수정으로 진입 시 고정 거래 스위치 비활성화
        if (isEdit && !isRecurringEdit) {
            binding.switchRecurring.setEnabled(false);
        }

        // 편집 데이터 로드
        viewModel.getEditingTransaction().observe(getViewLifecycleOwner(), tx -> {
            if (tx != null && isEdit && !isRecurringEdit && editingTx == null) {
                editingTx = tx;
                setType(tx.type);
                binding.etAmount.setText(FormatUtils.formatAmountInput(tx.amount));
                selectedCategoryId = tx.categoryId;
                selectedDate = tx.date;
                binding.tvDate.setText(DateUtils.toDisplayDate(selectedDate, requireContext()));
                binding.etMemo.setText(tx.memo);
                selectedPaymentMethod = tx.paymentMethod;
                updatePaymentMethodChips(tx.paymentMethod);
            }
        });

        // 고정거래 편집 데이터 로드
        recurringViewModel.getEditingItem().observe(getViewLifecycleOwner(), rec -> {
            if (rec != null && isRecurringEdit && editingRecurring == null) {
                editingRecurring = rec;
                setType(rec.type);
                binding.etAmount.setText(FormatUtils.formatAmountInput(rec.amount));
                selectedCategoryId = rec.categoryId;
                binding.etMemo.setText(rec.memo);
                if (rec.paymentMethod != null) {
                    selectedPaymentMethod = rec.paymentMethod;
                    updatePaymentMethodChips(rec.paymentMethod);
                }

                // 고정거래 스위치 & 반복 주기 표시
                binding.switchRecurring.setChecked(true);
                binding.switchRecurring.setEnabled(false);
                binding.chipGroupRecurringInterval.setVisibility(View.VISIBLE);
                binding.cardDate.setEnabled(false);
                binding.cardDate.setClickable(false);
                binding.cardDate.setAlpha(0.4f);
                selectedRecurringInterval = rec.intervalType;
                updateRecurringIntervalChips(rec.intervalType);
                updateScheduleInputVisibility(rec.intervalType);

                // 스케줄 값 복원
                switch (rec.intervalType) {
                    case "WEEKLY":
                        selectedDayOfWeek = rec.dayOfMonth;
                        updateDayOfWeekChips(rec.dayOfMonth);
                        break;
                    case "MONTHLY":
                        selectedDayOfMonth = rec.dayOfMonth;
                        binding.etRecurringDay.setText(String.valueOf(rec.dayOfMonth));
                        break;
                    case "YEARLY":
                        selectedMonth = rec.monthOfYear;
                        selectedDayOfMonth = rec.dayOfMonth;
                        binding.etRecurringMonth.setText(String.valueOf(rec.monthOfYear));
                        binding.etRecurringDay.setText(String.valueOf(rec.dayOfMonth));
                        break;
                }
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
                binding.tvDate.setText(DateUtils.toDisplayDate(selectedDate, requireContext()));
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

    private void updateRecurringIntervalChips(String interval) {
        binding.chipRDaily.setChecked("DAILY".equals(interval));
        binding.chipRWeekly.setChecked("WEEKLY".equals(interval));
        binding.chipRMonthly.setChecked("MONTHLY".equals(interval));
        binding.chipRYearly.setChecked("YEARLY".equals(interval));
    }

    private void hideAllScheduleInputs() {
        binding.chipGroupDayOfWeek.setVisibility(View.GONE);
        binding.llRecurringDayInput.setVisibility(View.GONE);
    }

    private void updateScheduleInputVisibility(String interval) {
        binding.chipGroupDayOfWeek.setVisibility(View.GONE);
        binding.llRecurringDayInput.setVisibility(View.GONE);
        binding.tilRecurringMonth.setVisibility(View.GONE);

        switch (interval) {
            case "DAILY":
                // 입력 없음
                break;
            case "WEEKLY":
                binding.chipGroupDayOfWeek.setVisibility(View.VISIBLE);
                break;
            case "MONTHLY":
                binding.llRecurringDayInput.setVisibility(View.VISIBLE);
                binding.tilRecurringDay.setVisibility(View.VISIBLE);
                break;
            case "YEARLY":
                binding.llRecurringDayInput.setVisibility(View.VISIBLE);
                binding.tilRecurringMonth.setVisibility(View.VISIBLE);
                binding.tilRecurringDay.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateDayOfWeekChips(int dayOfWeek) {
        binding.chipDowMon.setChecked(dayOfWeek == 1);
        binding.chipDowTue.setChecked(dayOfWeek == 2);
        binding.chipDowWed.setChecked(dayOfWeek == 3);
        binding.chipDowThu.setChecked(dayOfWeek == 4);
        binding.chipDowFri.setChecked(dayOfWeek == 5);
        binding.chipDowSat.setChecked(dayOfWeek == 6);
        binding.chipDowSun.setChecked(dayOfWeek == 7);
    }

    private int readDayInput() {
        String s = binding.etRecurringDay.getText() != null
                ? binding.etRecurringDay.getText().toString().trim() : "";
        if (s.isEmpty()) return 1;
        try {
            int day = Integer.parseInt(s);
            return Math.max(1, Math.min(28, day));
        } catch (NumberFormatException e) { return 1; }
    }

    private int readMonthInput() {
        String s = binding.etRecurringMonth.getText() != null
                ? binding.etRecurringMonth.getText().toString().trim() : "";
        if (s.isEmpty()) return 1;
        try {
            int month = Integer.parseInt(s);
            return Math.max(1, Math.min(12, month));
        } catch (NumberFormatException e) { return 1; }
    }

    private void submit() {
        long amount = FormatUtils.parseAmountInput(
            binding.etAmount.getText().toString());
        if (amount <= 0 || amount > 999_999_999_999L) {
            Snackbar.make(binding.getRoot(), R.string.transaction_amount_required, Snackbar.LENGTH_SHORT).show();
            binding.etAmount.requestFocus();
            return;
        }
        if (selectedCategoryId == 0) {
            Snackbar.make(binding.getRoot(), R.string.category_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        // 고정거래 편집 모드
        if (isRecurringEdit && editingRecurring != null) {
            editingRecurring.type = selectedType;
            editingRecurring.amount = amount;
            editingRecurring.categoryId = selectedCategoryId;
            editingRecurring.memo = binding.etMemo.getText().toString().trim();
            editingRecurring.paymentMethod = selectedPaymentMethod;
            editingRecurring.intervalType = selectedRecurringInterval;
            applyScheduleToRecurring(editingRecurring);
            editingRecurring.updatedAt = now;
            recurringViewModel.save(editingRecurring);
            Snackbar.make(binding.getRoot(), R.string.msg_updated, Snackbar.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

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

        // 고정 거래로 등록
        if (!isEdit && binding.switchRecurring.isChecked()) {
            tx.isAuto = true;
        }

        viewModel.saveTransaction(tx);

        if (!isEdit && binding.switchRecurring.isChecked()) {
            RecurringEntity recurring = new RecurringEntity();
            recurring.type = selectedType;
            recurring.amount = amount;
            recurring.categoryId = selectedCategoryId;
            recurring.intervalType = selectedRecurringInterval;
            recurring.memo = tx.memo;
            recurring.paymentMethod = selectedPaymentMethod;
            applyScheduleToRecurring(recurring);
            recurringViewModel.save(recurring);
        }

        Snackbar.make(binding.getRoot(), isEdit ? R.string.msg_updated : R.string.msg_added, Snackbar.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }

    private void applyScheduleToRecurring(RecurringEntity rec) {
        switch (rec.intervalType) {
            case "WEEKLY":
                rec.dayOfMonth = selectedDayOfWeek;
                rec.monthOfYear = 0;
                break;
            case "YEARLY":
                rec.monthOfYear = readMonthInput();
                rec.dayOfMonth = readDayInput();
                break;
            case "MONTHLY":
                rec.dayOfMonth = readDayInput();
                rec.monthOfYear = 0;
                break;
            case "DAILY":
            default:
                rec.dayOfMonth = 0;
                rec.monthOfYear = 0;
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
