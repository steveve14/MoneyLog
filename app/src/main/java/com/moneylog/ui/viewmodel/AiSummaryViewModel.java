package com.moneylog.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.moneylog.data.repository.AiSummaryRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AiSummaryViewModel extends ViewModel {

    private final AiSummaryRepository aiRepo;

    private final MutableLiveData<String> summaryText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isAvailable;

    @Inject
    public AiSummaryViewModel(AiSummaryRepository aiRepo) {
        this.aiRepo = aiRepo;
        isAvailable = new MutableLiveData<>(aiRepo.isAvailable());
    }

    public LiveData<String> getSummaryText() {
        return summaryText;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsAvailable() {
        return isAvailable;
    }

    public void analyze(String prompt) {
        if (!Boolean.TRUE.equals(isAvailable.getValue())) {
            summaryText.setValue("이 기기는 Gemini Nano를 지원하지 않습니다.");
            return;
        }
        isLoading.setValue(true);
        aiRepo.generateMonthlySummary(prompt, new AiSummaryRepository.AiCallback() {
            @Override
            public void onSuccess(String summary) {
                isLoading.postValue(false);
                summaryText.postValue(summary);
            }

            @Override
            public void onFailure(String message) {
                isLoading.postValue(false);
                summaryText.postValue("분석 실패: " + message);
            }

            @Override
            public void onUnavailable() {
                isLoading.postValue(false);
                isAvailable.postValue(false);
                summaryText.postValue("이 기기는 Gemini Nano를 지원하지 않습니다.");
            }
        });
    }
}
