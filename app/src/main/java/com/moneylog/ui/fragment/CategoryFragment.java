package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.databinding.BottomSheetCategoryBinding;
import com.moneylog.databinding.FragmentCategoryBinding;
import com.moneylog.ui.adapter.CategoryAdapter;
import com.moneylog.ui.adapter.IconPickerAdapter;
import com.moneylog.ui.viewmodel.CategoryViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CategoryFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private FragmentCategoryBinding binding;
    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private String selectedType = "EXPENSE";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // RecyclerView
        adapter = new CategoryAdapter(this);
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);

        // 탭 설정
        binding.tabType.addTab(binding.tabType.newTab().setText("지출"));
        binding.tabType.addTab(binding.tabType.newTab().setText("수입"));
        binding.tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedType = tab.getPosition() == 0 ? "EXPENSE" : "INCOME";
                observeCategories();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        observeCategories();

        // FAB
        binding.fabAddCategory.setOnClickListener(v -> showCategorySheet(null));
    }

    private void observeCategories() {
        viewModel.getCategoriesByType(selectedType).observe(getViewLifecycleOwner(),
            list -> adapter.submitList(list));
    }

    @Override
    public void onEdit(CategoryEntity category) {
        showCategorySheet(category);
    }

    private void showCategorySheet(@Nullable CategoryEntity existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        BottomSheetCategoryBinding sheet = BottomSheetCategoryBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        boolean isEdit = existing != null;

        // 아이콘 피커 설정
        final String[] selectedIcon = {isEdit ? existing.iconName : "category"};
        IconPickerAdapter iconAdapter = new IconPickerAdapter(iconName -> selectedIcon[0] = iconName);
        sheet.rvIconPicker.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        sheet.rvIconPicker.setAdapter(iconAdapter);
        iconAdapter.setSelectedIcon(selectedIcon[0]);

        if (isEdit) {
            sheet.tvSheetTitle.setText("카테고리 수정");
            sheet.etName.setText(existing.name);
            // 유형 버튼 초기 선택
            if ("EXPENSE".equals(existing.type)) {
                sheet.btnExpense.setStrokeWidth(3);
            } else {
                sheet.btnIncome.setStrokeWidth(3);
            }
            sheet.btnDelete.setVisibility(View.VISIBLE);
        }

        // 유형 선택 상태
        final String[] sheetType = {isEdit ? (existing.type) : selectedType};
        updateTypeButtons(sheet, sheetType[0]);
        sheet.btnExpense.setOnClickListener(v -> {
            sheetType[0] = "EXPENSE";
            updateTypeButtons(sheet, sheetType[0]);
        });
        sheet.btnIncome.setOnClickListener(v -> {
            sheetType[0] = "INCOME";
            updateTypeButtons(sheet, sheetType[0]);
        });

        // 저장
        sheet.btnSave.setOnClickListener(v -> {
            String name = sheet.etName.getText() != null ?
                sheet.etName.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                sheet.tilName.setError("이름을 입력하세요");
                return;
            }
            sheet.tilName.setError(null);

            CategoryEntity cat = isEdit ? existing : new CategoryEntity();
            cat.name = name;
            cat.iconName = selectedIcon[0];
            cat.type = sheetType[0];

            if (isEdit) {
                viewModel.update(cat);
                Toast.makeText(requireContext(), "수정되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.save(cat);
                Toast.makeText(requireContext(), "추가되었습니다", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        // 삭제
        sheet.btnDelete.setOnClickListener(v -> {
            if (existing != null) {
                viewModel.delete(existing.id);
                Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateTypeButtons(BottomSheetCategoryBinding sheet, String type) {
        int selected = 3;
        int unselected = 0;
        if ("EXPENSE".equals(type)) {
            sheet.btnExpense.setStrokeWidth(selected);
            sheet.btnIncome.setStrokeWidth(unselected);
        } else {
            sheet.btnExpense.setStrokeWidth(unselected);
            sheet.btnIncome.setStrokeWidth(selected);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
