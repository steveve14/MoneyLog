package com.moneylog.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.moneylog.R;
import com.moneylog.databinding.FragmentOnboardingBinding;
import com.moneylog.util.DataManagementHelper;
import com.moneylog.util.LocaleHelper;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OnboardingFragment extends Fragment {

    @Inject
    DataManagementHelper dataManagementHelper;

    private FragmentOnboardingBinding binding;
    private String selectedTag = "en";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String systemLang = Locale.getDefault().getLanguage();
        if ("ko".equals(systemLang) || "en".equals(systemLang) || "ja".equals(systemLang)) {
            selectedTag = systemLang;
        } else {
            selectedTag = "en";
            if (LocaleHelper.getCurrentLocaleTag().isEmpty()) {
                LocaleHelper.setLocale("en");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateSelection();

        binding.cardKorean.setOnClickListener(v -> {
            selectedTag = "ko";
            updateSelection();
        });

        binding.cardEnglish.setOnClickListener(v -> {
            selectedTag = "en";
            updateSelection();
        });

        binding.cardJapanese.setOnClickListener(v -> {
            selectedTag = "ja";
            updateSelection();
        });

        binding.btnContinue.setOnClickListener(v -> {
            LocaleHelper.saveLanguageTag(requireContext(), selectedTag);
            LocaleHelper.setLocale(selectedTag);
            LocaleHelper.setOnboardingDone(requireContext());
            dataManagementHelper.resetCategoriesToDefault(new DataManagementHelper.ResultCallback() {
                @Override public void onSuccess(String message) { }
                @Override public void onFailure(String message) { }
            });
            Navigation.findNavController(v).navigate(R.id.action_onboarding_to_dashboard);
        });
    }

    private void updateSelection() {
        binding.checkKorean.setVisibility("ko".equals(selectedTag) ? View.VISIBLE : View.GONE);
        binding.checkEnglish.setVisibility("en".equals(selectedTag) ? View.VISIBLE : View.GONE);
        binding.checkJapanese.setVisibility("ja".equals(selectedTag) ? View.VISIBLE : View.GONE);

        setCardSelected(binding.cardKorean, "ko".equals(selectedTag));
        setCardSelected(binding.cardEnglish, "en".equals(selectedTag));
        setCardSelected(binding.cardJapanese, "ja".equals(selectedTag));
    }

    private void setCardSelected(com.google.android.material.card.MaterialCardView card,
                                  boolean selected) {
        if (selected) {
            card.setStrokeColor(requireContext().getColor(R.color.md_theme_primary));
            card.setStrokeWidth(dpToPx(2));
        } else {
            card.setStrokeColor(requireContext().getColor(R.color.md_theme_outline_variant));
            card.setStrokeWidth(dpToPx(1));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
