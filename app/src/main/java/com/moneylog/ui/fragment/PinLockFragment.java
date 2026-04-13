package com.moneylog.ui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.moneylog.databinding.FragmentPinLockBinding;
import com.moneylog.util.CryptoUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PinLockFragment extends Fragment {

    private static final String PREFS_NAME = "moneylog_pin_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_FAIL_COUNT = "pin_fail_count";
    private static final String KEY_LOCKOUT_UNTIL = "pin_lockout_until";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L; // 5분

    private FragmentPinLockBinding binding;
    private final StringBuilder pinInput = new StringBuilder();

    /** true = 설정 모드, false = 확인 모드 */
    private boolean isSetupMode;
    private String firstPin = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPinLockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isSetupMode = !isPinSet();
        updateTitle();

        int[] numBtnIds = {
            com.moneylog.R.id.btn0, com.moneylog.R.id.btn1, com.moneylog.R.id.btn2,
            com.moneylog.R.id.btn3, com.moneylog.R.id.btn4, com.moneylog.R.id.btn5,
            com.moneylog.R.id.btn6, com.moneylog.R.id.btn7, com.moneylog.R.id.btn8,
            com.moneylog.R.id.btn9
        };
        for (int id : numBtnIds) {
            view.findViewById(id).setOnClickListener(v -> {
                String digit = ((com.google.android.material.button.MaterialButton) v)
                        .getText().toString();
                appendDigit(digit);
            });
        }

        binding.btnBackspace.setOnClickListener(v -> deleteDigit());
    }

    private void appendDigit(String digit) {
        if (pinInput.length() >= 4) return;
        pinInput.append(digit);
        updateDots();
        if (pinInput.length() == 4) {
            onPinComplete();
        }
    }

    private void deleteDigit() {
        if (pinInput.length() > 0) {
            pinInput.deleteCharAt(pinInput.length() - 1);
            updateDots();
        }
        hideError();
    }

    private void onPinComplete() {
        String pin = pinInput.toString();
        if (isSetupMode) {
            if (firstPin == null) {
                firstPin = pin;
                pinInput.setLength(0);
                updateDots();
                binding.tvPinTitle.setText(com.moneylog.R.string.pin_confirm);
                binding.tvPinSubtitle.setText("같은 PIN을 한 번 더 입력하세요");
            } else {
                if (firstPin.equals(pin)) {
                    savePin(pin);
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    showError("PIN이 일치하지 않습니다");
                    firstPin = null;
                    pinInput.setLength(0);
                    updateDots();
                    binding.tvPinTitle.setText(com.moneylog.R.string.pin_setup);
                    binding.tvPinSubtitle.setText("새 PIN을 설정하세요");
                }
            }
        } else {
            // 확인 모드 — 잠금 확인
            SharedPreferences prefs = getSecurePrefs();
            if (prefs != null) {
                long lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0);
                if (System.currentTimeMillis() < lockoutUntil) {
                    long remainSec = (lockoutUntil - System.currentTimeMillis()) / 1000;
                    showError("보안 잠금 중입니다 (" + remainSec + "초 후 재시도)");
                    pinInput.setLength(0);
                    updateDots();
                    return;
                }

                String storedHash = prefs.getString(KEY_PIN_HASH, null);
                if (storedHash != null && CryptoUtils.verifyPin(pin, storedHash)) {
                    // 성공: 실패 횟수 초기화
                    prefs.edit()
                            .putInt(KEY_FAIL_COUNT, 0)
                            .remove(KEY_LOCKOUT_UNTIL)
                            .apply();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    int fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1;
                    if (fails >= MAX_ATTEMPTS) {
                        prefs.edit()
                                .putInt(KEY_FAIL_COUNT, 0)
                                .putLong(KEY_LOCKOUT_UNTIL,
                                        System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                                .apply();
                        showError(MAX_ATTEMPTS + "회 실패 — 5분 후 다시 시도하세요");
                    } else {
                        prefs.edit().putInt(KEY_FAIL_COUNT, fails).apply();
                        showError("PIN이 올바르지 않습니다 (" + (MAX_ATTEMPTS - fails) + "회 남음)");
                    }
                    pinInput.setLength(0);
                    updateDots();
                }
            }
        }
    }

    private void updateDots() {
        View[] dots = {binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4};
        for (int i = 0; i < 4; i++) {
            dots[i].setBackgroundResource(i < pinInput.length()
                    ? com.moneylog.R.drawable.pin_dot_filled
                    : com.moneylog.R.drawable.pin_dot_empty);
        }
    }

    private void showError(String message) {
        binding.tvPinError.setText(message);
        binding.tvPinError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvPinError.setVisibility(View.INVISIBLE);
    }

    private void updateTitle() {
        if (isSetupMode) {
            binding.tvPinTitle.setText(com.moneylog.R.string.pin_setup);
            binding.tvPinSubtitle.setText("새 PIN을 설정하세요");
        } else {
            binding.tvPinTitle.setText(com.moneylog.R.string.pin_enter);
            binding.tvPinSubtitle.setText(com.moneylog.R.string.pin_enter_subtitle);
        }
    }

    private boolean isPinSet() {
        SharedPreferences prefs = getSecurePrefs();
        return prefs != null && prefs.contains(KEY_PIN_HASH);
    }

    private void savePin(String pin) {
        SharedPreferences prefs = getSecurePrefs();
        if (prefs != null) {
            prefs.edit().putString(KEY_PIN_HASH, CryptoUtils.hashPin(pin)).apply();
        }
    }

    @Nullable
    private SharedPreferences getSecurePrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(requireContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    requireContext(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
