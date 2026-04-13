package com.moneylog.data.repository;

import android.content.Context;
import android.util.Log;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gemini Nano 온디바이스 AI 요약 (Phase 6 scaffold).
 *
 * <p>android-ai-edge (Gemini Nano AICore) 연동 예정.
 * 현재는 기기 지원 여부 확인 + fallback 메시지만 구현.
 * 실제 텍스트 생성은 libs.versions.toml ai-edge 주석 해제 후 구현.</p>
 */
@Singleton
public class AiSummaryRepository {

    private static final String TAG = "AiSummaryRepository";

    private final Context context;

    @Inject
    public AiSummaryRepository(@ApplicationContext Context context) {
        this.context = context;
    }

    /**
     * Gemini Nano 지원 여부 확인.
     * <p>실제 AICore API 호환 여부를 체크 — 미지원 기기에서는 false 반환.</p>
     */
    public boolean isAvailable() {
        // TODO: AICore 의존성 활성화 후 실제 체크 구현
        // try {
        //     GenerativeModel model = new GenerativeModel("gemini-nano");
        //     return model.isAvailable();
        // } catch (Exception e) { return false; }
        Log.d(TAG, "Gemini Nano availability check (stub) → false");
        return false;
    }

    /**
     * 월간 지출 요약 생성.
     *
     * @param prompt  소비 데이터 요약 텍스트
     * @param callback 결과 콜백
     */
    public void generateMonthlySummary(String prompt, AiCallback callback) {
        if (!isAvailable()) {
            callback.onUnavailable();
            return;
        }
        // TODO: AICore 의존성 활성화 후 비동기 생성 구현
        callback.onFailure("AI 요약 기능은 아직 구현되지 않았습니다.");
    }

    public interface AiCallback {
        void onSuccess(String summary);
        void onFailure(String message);
        void onUnavailable();
    }
}
