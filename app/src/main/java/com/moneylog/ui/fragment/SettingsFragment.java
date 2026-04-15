package com.moneylog.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneylog.R;
import com.moneylog.data.repository.BackupRepository;
import com.moneylog.databinding.FragmentSettingsBinding;
import com.moneylog.util.DataManagementHelper;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.LocaleHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    @Inject
    BackupRepository backupRepository;

    @Inject
    DataManagementHelper dataManagementHelper;

    private FragmentSettingsBinding binding;

    private final ActivityResultLauncher<String[]> csvPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) showImportModeDialog(uri);
            });

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .addOnSuccessListener(account -> {
                                updateGdriveStatus();
                                showGdriveDialog();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            R.string.gdrive_sign_in_failed, Toast.LENGTH_SHORT).show());
                }
            });

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

        // 닫기 버튼
        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        binding.rowLanguage.setOnClickListener(v -> showLanguageDialog());

        // 데이터 관리
        binding.rowDataManagement.setOnClickListener(v -> showDataManagementDialog());

        binding.rowCategory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.categoryFragment));

        // 고정거래 관리
        binding.rowRecurring.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.recurringFragment));

        // 카테고리 모두 삭제
        binding.rowCategoryDeleteAll.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                        .setTitle(R.string.category_delete_all_title)
                        .setMessage(R.string.category_delete_all_message)
                        .setPositiveButton(R.string.confirm, (dialog, which) ->
                                dataManagementHelper.deleteAllCategories(new DataManagementHelper.ResultCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        R.string.category_delete_all_complete,
                                                        Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                                    }
                                }))
                        .setNegativeButton(R.string.cancel, null)
                        .show());

        // 카테고리 기본으로
        binding.rowCategoryResetDefault.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                        .setTitle(R.string.category_reset_default_title)
                        .setMessage(R.string.category_reset_default_message)
                        .setPositiveButton(R.string.confirm, (dialog, which) ->
                                dataManagementHelper.resetCategoriesToDefault(new DataManagementHelper.ResultCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        R.string.category_reset_default_complete,
                                                        Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                                    }
                                }))
                        .setNegativeButton(R.string.cancel, null)
                        .show());

        // 금액 표시 모드
        binding.switchAmountText.setChecked(LocaleHelper.isAmountTextMode(requireContext()));
        binding.switchAmountText.setOnCheckedChangeListener((btn, checked) -> {
                LocaleHelper.setAmountTextMode(requireContext(), checked);
                FormatUtils.setTextMode(checked);
        });

        // Google Drive 백업 & 복원 (통합)
        updateGdriveStatus();
        binding.rowGdriveBackupRestore.setOnClickListener(v -> showGdriveDialog());

        binding.btnResetAllData.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                        .setTitle(getString(R.string.data_reset_title))
                        .setMessage(getString(R.string.settings_reset_warning))
                        .setPositiveButton(getString(R.string.settings_reset_all_data), (dialog, which) ->
                                dataManagementHelper.resetAllData(new DataManagementHelper.ResultCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        R.string.data_reset_complete,
                                                        Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        getString(R.string.data_reset_failed, message),
                                                        Toast.LENGTH_LONG).show());
                                    }
                                }))
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show());
    }

    private void showDataManagementDialog() {
        String[] items = {
                getString(R.string.data_export_csv),
                getString(R.string.data_import_csv),
                getString(R.string.data_cleanup)
        };

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                .setTitle(getString(R.string.data_management_title))
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            dataManagementHelper.exportCsv(new DataManagementHelper.ResultCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(),
                                                    getString(R.string.msg_export_complete) + ": " + message,
                                                    Toast.LENGTH_LONG).show());
                                }

                                @Override
                                public void onFailure(String message) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(),
                                                    getString(R.string.msg_export_failed) + ": " + message,
                                                    Toast.LENGTH_LONG).show());
                                }
                            });
                            break;
                        case 1:
                            csvPickerLauncher.launch(new String[]{"text/csv", "text/comma-separated-values", "*/*"});
                            break;
                        case 2:
                            new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                                    .setTitle(getString(R.string.data_cleanup_title))
                                    .setMessage(getString(R.string.data_cleanup_message))
                                    .setPositiveButton(getString(R.string.confirm), (d, w) ->
                                            dataManagementHelper.cleanupOldData(6, new DataManagementHelper.ResultCallback() {
                                                @Override
                                                public void onSuccess(String message) {
                                                    requireActivity().runOnUiThread(() ->
                                                            Toast.makeText(requireContext(),
                                                                    getString(R.string.msg_cleanup_complete) + " (" + message + ")",
                                                                    Toast.LENGTH_SHORT).show());
                                                }

                                                @Override
                                                public void onFailure(String message) {
                                                    requireActivity().runOnUiThread(() ->
                                                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                                                }
                                            }))
                                    .setNegativeButton(getString(R.string.cancel), null)
                                    .show();
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showImportModeDialog(Uri uri) {
        String[] items = {
                getString(R.string.import_mode_merge),
                getString(R.string.import_mode_clear_data),
                getString(R.string.import_mode_clear_all)
        };

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                .setTitle(getString(R.string.import_mode_title))
                .setItems(items, (dialog, which) -> {
                    DataManagementHelper.ImportMode mode;
                    switch (which) {
                        case 1:
                            mode = DataManagementHelper.ImportMode.CLEAR_DATA;
                            break;
                        case 2:
                            mode = DataManagementHelper.ImportMode.CLEAR_ALL;
                            break;
                        default:
                            mode = DataManagementHelper.ImportMode.MERGE;
                            break;
                    }

                    if (mode == DataManagementHelper.ImportMode.CLEAR_ALL) {
                        // 카테고리까지 삭제하는 경우 한 번 더 확인
                        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                                .setTitle(getString(R.string.import_mode_clear_all_confirm_title))
                                .setMessage(getString(R.string.import_mode_clear_all_confirm_message))
                                .setPositiveButton(getString(R.string.confirm), (d, w) -> importCsv(uri, mode))
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show();
                    } else {
                        importCsv(uri, mode);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void importCsv(Uri uri, DataManagementHelper.ImportMode mode) {
        dataManagementHelper.importCsv(uri, mode, new DataManagementHelper.ResultCallback() {
            @Override
            public void onSuccess(String count) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                getString(R.string.msg_import_complete, Integer.parseInt(count)),
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onFailure(String message) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                getString(R.string.msg_import_failed) + ": " + message,
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    private void updateGdriveStatus() {
        if (backupRepository.isSignedIn()) {
            String email = backupRepository.getSignedInEmail();
            binding.tvGdriveStatus.setText(email != null ? email : getString(R.string.settings_gdrive_connected));
        } else {
            binding.tvGdriveStatus.setText(R.string.gdrive_connect);
        }
    }

    private void showGdriveDialog() {
        if (!backupRepository.isSignedIn()) {
            signInLauncher.launch(backupRepository.getSignInIntent());
            return;
        }

        String[] items = {
                getString(R.string.gdrive_backup),
                getString(R.string.gdrive_restore),
                getString(R.string.gdrive_disconnect)
        };

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                .setTitle(getString(R.string.settings_gdrive_backup_restore))
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(requireContext(), R.string.msg_backup_in_progress, Toast.LENGTH_SHORT).show();
                            backupRepository.backupToGoogleDrive(new BackupRepository.BackupCallback() {
                                @Override
                                public void onSuccess() {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), R.string.msg_backup_complete, Toast.LENGTH_SHORT).show());
                                }

                                @Override
                                public void onFailure(String message) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                                }
                            });
                            break;
                        case 1:
                            new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                                    .setTitle(getString(R.string.data_restore_title))
                                    .setMessage(getString(R.string.data_restore_message))
                                    .setPositiveButton(getString(R.string.settings_restore), (d, w) -> {
                                        backupRepository.restoreFromGoogleDrive(new BackupRepository.BackupCallback() {
                                            @Override
                                            public void onSuccess() {
                                                requireActivity().runOnUiThread(() ->
                                                        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
                                                                .setTitle(getString(R.string.msg_restore_complete))
                                                                .setMessage(getString(R.string.gdrive_restart_message))
                                                                .setCancelable(false)
                                                                .setPositiveButton(getString(R.string.confirm), (rd, rw) -> {
                                                                    Intent intent = requireActivity().getPackageManager()
                                                                            .getLaunchIntentForPackage(requireActivity().getPackageName());
                                                                    if (intent != null) {
                                                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                        startActivity(intent);
                                                                    }
                                                                    requireActivity().finishAffinity();
                                                                })
                                                                .show());
                                            }

                                            @Override
                                            public void onFailure(String message) {
                                                requireActivity().runOnUiThread(() ->
                                                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
                                            }
                                        });
                                    })
                                    .setNegativeButton(getString(R.string.cancel), null)
                                    .show();
                            break;
                        case 2:
                            backupRepository.signOut(() ->
                                    requireActivity().runOnUiThread(this::updateGdriveStatus));
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
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

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyLog_Dialog)
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
