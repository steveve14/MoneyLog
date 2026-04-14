package com.moneylog.data.repository;

import android.content.Context;
import android.util.Log;

import com.moneylog.R;
import com.moneylog.data.db.entity.TransactionEntity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

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
     */
    public boolean isGeminiAvailable() {
        // TODO: AICore 의존성 활성화 후 실제 체크 구현
        Log.d(TAG, "Gemini Nano availability check (stub) → false");
        return false;
    }

    /**
     * 거래 데이터를 분석하여 요약 생성.
     * Gemini Nano 사용 가능 시 AI 분석, 미지원 시 규칙 기반 로컬 분석.
     */
    public void analyze(List<TransactionEntity> transactions, String yearMonth, AiCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (transactions == null || transactions.isEmpty()) {
                    callback.onSuccess(context.getString(R.string.ai_no_data));
                    return;
                }

                if (isGeminiAvailable()) {
                    // TODO: Gemini Nano 비동기 호출
                    callback.onSuccess(generateLocalSummary(transactions, yearMonth));
                } else {
                    callback.onSuccess(generateLocalSummary(transactions, yearMonth));
                }
            } catch (Exception e) {
                Log.e(TAG, "Analysis failed", e);
                callback.onFailure(context.getString(R.string.ai_analysis_failed));
            }
        });
    }

    /**
     * 규칙 기반 로컬 분석 — Gemini Nano 없이도 유용한 인사이트 제공.
     */
    private String generateLocalSummary(List<TransactionEntity> transactions, String yearMonth) {
        long totalIncome = 0, totalExpense = 0;
        int incomeCount = 0, expenseCount = 0;
        Map<Long, Long> categoryExpense = new HashMap<>();
        Map<Long, Integer> categoryCount = new HashMap<>();
        Map<String, Long> dailyExpense = new HashMap<>();

        for (TransactionEntity tx : transactions) {
            if ("INCOME".equals(tx.type)) {
                totalIncome += tx.amount;
                incomeCount++;
            } else {
                totalExpense += tx.amount;
                expenseCount++;
                categoryExpense.merge(tx.categoryId, tx.amount, Long::sum);
                categoryCount.merge(tx.categoryId, 1, Integer::sum);
                dailyExpense.merge(tx.date, tx.amount, Long::sum);
            }
        }

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.KOREA);
        StringBuilder sb = new StringBuilder();

        // 1. 월 요약
        String[] parts = yearMonth.split("-");
        sb.append("📊 ").append(parts[0]).append("년 ").append(Integer.parseInt(parts[1])).append("월 재정 분석\n\n");

        sb.append("총 수입: ").append(nf.format(totalIncome)).append("원 (").append(incomeCount).append("건)\n");
        sb.append("총 지출: ").append(nf.format(totalExpense)).append("원 (").append(expenseCount).append("건)\n");
        long balance = totalIncome - totalExpense;
        if (balance >= 0) {
            sb.append("잔액: +").append(nf.format(balance)).append("원 ✅\n");
        } else {
            sb.append("잔액: ").append(nf.format(balance)).append("원 ⚠️\n");
        }

        // 2. 저축률
        if (totalIncome > 0) {
            long savingsRate = ((totalIncome - totalExpense) * 100) / totalIncome;
            sb.append("\n💰 저축률: ").append(savingsRate).append("%");
            if (savingsRate >= 30) {
                sb.append(" — 훌륭합니다! 안정적인 재정 관리입니다.");
            } else if (savingsRate >= 10) {
                sb.append(" — 양호합니다. 조금 더 절약해보세요.");
            } else if (savingsRate >= 0) {
                sb.append(" — 지출을 줄일 필요가 있습니다.");
            } else {
                sb.append(" — 수입보다 지출이 많습니다. 지출 점검이 필요합니다.");
            }
            sb.append("\n");
        }

        // 3. 최다 지출 카테고리
        if (!categoryExpense.isEmpty()) {
            sb.append("\n📌 카테고리별 지출 분석\n");

            List<Map.Entry<Long, Long>> sorted = new ArrayList<>(categoryExpense.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            int rank = 1;
            for (Map.Entry<Long, Long> entry : sorted) {
                if (rank > 5) break;
                long pct = totalExpense > 0 ? (entry.getValue() * 100) / totalExpense : 0;
                int count = categoryCount.getOrDefault(entry.getKey(), 0);
                sb.append(rank).append(". 카테고리 #").append(entry.getKey())
                        .append(": ").append(nf.format(entry.getValue())).append("원")
                        .append(" (").append(pct).append("%, ").append(count).append("건)\n");
                rank++;
            }
        }

        // 4. 일별 소비 패턴
        if (!dailyExpense.isEmpty()) {
            long maxDayAmount = 0;
            String maxDay = "";
            long total = 0;
            for (Map.Entry<String, Long> entry : dailyExpense.entrySet()) {
                total += entry.getValue();
                if (entry.getValue() > maxDayAmount) {
                    maxDayAmount = entry.getValue();
                    maxDay = entry.getKey();
                }
            }
            long avgDaily = total / dailyExpense.size();
            sb.append("\n📅 소비 패턴\n");
            sb.append("일 평균 지출: ").append(nf.format(avgDaily)).append("원\n");
            sb.append("최대 지출일: ").append(maxDay).append(" (").append(nf.format(maxDayAmount)).append("원)\n");

            if (maxDayAmount > avgDaily * 3) {
                sb.append("⚠️ 최대 지출일이 평균의 ").append(maxDayAmount / avgDaily).append("배입니다. 큰 지출을 점검해보세요.\n");
            }
        }

        // 5. 조언
        sb.append("\n💡 절약 팁\n");
        if (totalExpense > totalIncome && totalIncome > 0) {
            sb.append("• 수입 대비 지출이 초과되었습니다. 고정 지출 항목을 점검해보세요.\n");
        }
        if (expenseCount > 0) {
            long avgTx = totalExpense / expenseCount;
            if (avgTx < 10000) {
                sb.append("• 소액 지출이 많습니다 (평균 ").append(nf.format(avgTx)).append("원). 소비 습관을 점검해보세요.\n");
            }
        }
        if (categoryExpense.size() >= 5) {
            sb.append("• 지출 카테고리가 다양합니다. 주요 카테고리에 예산을 설정해보세요.\n");
        }

        return sb.toString();
    }

    public interface AiCallback {
        void onSuccess(String summary);
        void onFailure(String message);
    }
}
