package com.moneylog.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.moneylog.R;
import com.moneylog.data.repository.BackupRepository;
import com.moneylog.databinding.FragmentSettingsBinding;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.LocaleHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    @Inject
    BackupRepository backupRepository;

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateLanguageLabel();

        binding.switchAmountText.setChecked(LocaleHelper.isAmountTextMode(requireContext()));
        binding.switchAmountText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LocaleHelper.setAmountTextMode(requireContext(), isChecked);
            FormatUtils.setTextMode(isChecked);
        });

        binding.rowLanguage.setOnClickListener(v -> showLanguageDialog());

        binding.rowCategory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.categoryFragment));

        binding.rowChangePin.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.pinLockFragment));

        binding.rowBackupNow.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "백업 중...", Toast.LENGTH_SHORT).show();
            backupRepository.backupToGoogleDrive(new BackupRepository.BackupCallback() {
                @Override
                public void onSuccess() {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "백업 완료", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onFailure(String message) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                }
            });
        });

        binding.rowRestore.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("데이터 복원")
                    .setMessage("Google Drive에서 데이터를 복원합니다. 현재 데이터가 덮어쓰여집니다.")
                    .setPositiveButton("복원", (dialog, which) -> {
                        backupRepository.restoreFromGoogleDrive(new BackupRepository.BackupCallback() {
                            @Override
                            public void onSuccess() {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(), "복원 완료", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onFailure(String message) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                            }
                        });
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        binding.btnResetAllData.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("데이터 초기화")
                        .setMessage(getString(R.string.settings_reset_warning))
                        .setPositiveButton("초기화", (dialog, which) ->
                                Toast.makeText(requireContext(), "초기화 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show())
                        .setNegativeButton("취소", null)
                        .show());
    }

    private void showLanguageDialog() {
        String[] labels = {
                getString(R.string.language_system_default),
                getString(R.string.language_korean),
                getString(R.string.language_english),
                getString(R.string.language_japanese)
        };

        String currentTag = LocaleHelper.getCurrentLocaleTag();
        int checkedIndex = 0;
        for (int i = 0; i < LocaleHelper.LOCALE_TAGS.length; i++) {
            if (LocaleHelper.LOCALE_TAGS[i].equals(currentTag)) {
                checkedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    String tag = LocaleHelper.LOCALE_TAGS[which];
                    LocaleHelper.saveLanguageTag(requireContext(), tag);
                    LocaleHelper.setLocale(tag);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateLanguageLabel() {
        String tag = LocaleHelper.getCurrentLocaleTag();
        String label;
        switch (tag) {
            case "ko":
                label = getString(R.string.language_korean);
                break;
            case "en":
                label = getString(R.string.language_english);
                break;
            case "ja":
                label = getString(R.string.language_japanese);
                break;
            default:
                label = getString(R.string.language_system_default);
                break;
        }
        binding.tvCurrentLanguage.setText(label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
