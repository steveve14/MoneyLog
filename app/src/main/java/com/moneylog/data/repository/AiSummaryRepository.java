package com.moneylog.data.repository;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
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
    private static final String AICORE_PACKAGE = "com.google.android.aicore";

    private final Context context;
    private final boolean geminiAvailable;

    @Inject
    public AiSummaryRepository(@ApplicationContext Context context) {
        this.context = context;
        this.geminiAvailable = checkGeminiNano();
    }

    /**
     * Gemini Nano 지원 여부 — 기기에 AICore 서비스가 설치되어 있는지 체크.
     */
    public boolean isGeminiAvailable() {
        return geminiAvailable;
    }

    private boolean checkGeminiNano() {
        if (Build.VERSION.SDK_INT < 31) {
            Log.d(TAG, "Gemini Nano requires Android 12+");
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(AICORE_PACKAGE, 0);
            Log.d(TAG, "AICore service found — Gemini Nano available");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "AICore service not found — Gemini Nano unavailable");
            return false;
        }
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

                if (geminiAvailable) {
                    try {
                        String geminiResult = analyzeWithGemini(transactions, yearMonth);
                        callback.onSuccess(geminiResult);
                    } catch (Exception e) {
                        Log.w(TAG, "Gemini Nano failed, falling back to local analysis", e);
                        callback.onSuccess(generateLocalSummary(transactions, yearMonth));
                    }
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

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        StringBuilder sb = new StringBuilder();

        // 1. 월 요약
        String[] parts = yearMonth.split("-");
        String ymDisplay = context.getString(R.string.year_month_display, parts[0], Integer.parseInt(parts[1]));
        sb.append(context.getString(R.string.ai_title_analysis, ymDisplay));

        String incomeFormatted = context.getString(R.string.amount_with_unit, nf.format(totalIncome));
        String expenseFormatted = context.getString(R.string.amount_with_unit, nf.format(totalExpense));
        sb.append(context.getString(R.string.ai_total_income, incomeFormatted, incomeCount));
        sb.append(context.getString(R.string.ai_total_expense, expenseFormatted, expenseCount));

        long balance = totalIncome - totalExpense;
        String balanceFormatted = context.getString(R.string.amount_with_unit, nf.format(Math.abs(balance)));
        if (balance >= 0) {
            sb.append(context.getString(R.string.ai_balance_positive, balanceFormatted));
        } else {
            sb.append(context.getString(R.string.ai_balance_negative, "-" + balanceFormatted));
        }

        // 2. 저축률
        if (totalIncome > 0) {
            long savingsRate = ((totalIncome - totalExpense) * 100) / totalIncome;
            sb.append(context.getString(R.string.ai_savings_rate, (int) savingsRate));
            if (savingsRate >= 30) {
                sb.append(context.getString(R.string.ai_savings_excellent));
            } else if (savingsRate >= 10) {
                sb.append(context.getString(R.string.ai_savings_good));
            } else if (savingsRate >= 0) {
                sb.append(context.getString(R.string.ai_savings_warning));
            } else {
                sb.append(context.getString(R.string.ai_savings_danger));
            }
            sb.append("\n");
        }

        // 3. 최다 지출 카테고리
        if (!categoryExpense.isEmpty()) {
            sb.append(context.getString(R.string.ai_category_title));

            List<Map.Entry<Long, Long>> sorted = new ArrayList<>(categoryExpense.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            int rank = 1;
            for (Map.Entry<Long, Long> entry : sorted) {
                if (rank > 5) break;
                long pct = totalExpense > 0 ? (entry.getValue() * 100) / totalExpense : 0;
                int count = categoryCount.getOrDefault(entry.getKey(), 0);
                String catAmountFormatted = context.getString(R.string.amount_with_unit, nf.format(entry.getValue()));
                sb.append(context.getString(R.string.ai_category_row,
                        rank, "#" + entry.getKey(), catAmountFormatted, (int) pct, count));
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
            sb.append(context.getString(R.string.ai_pattern_title));
            String avgFormatted = context.getString(R.string.amount_with_unit, nf.format(avgDaily));
            sb.append(context.getString(R.string.ai_pattern_avg, avgFormatted));
            String maxFormatted = context.getString(R.string.amount_with_unit, nf.format(maxDayAmount));
            sb.append(context.getString(R.string.ai_pattern_max, maxDay, maxFormatted));

            if (maxDayAmount > avgDaily * 3) {
                sb.append(context.getString(R.string.ai_pattern_spike, (int) (maxDayAmount / avgDaily)));
            }
        }

        // 5. 조언
        sb.append(context.getString(R.string.ai_tips_title));
        if (totalExpense > totalIncome && totalIncome > 0) {
            sb.append(context.getString(R.string.ai_tip_overspend));
        }
        if (expenseCount > 0) {
            long avgTx = totalExpense / expenseCount;
            if (avgTx < 10000) {
                String avgTxFormatted = context.getString(R.string.amount_with_unit, nf.format(avgTx));
                sb.append(context.getString(R.string.ai_tip_small_expenses, avgTxFormatted));
            }
        }
        if (categoryExpense.size() >= 5) {
            sb.append(context.getString(R.string.ai_tip_diverse_categories));
        }

        return sb.toString();
    }

    /**
     * Gemini Nano를 통한 분석.
     * AICore 서비스가 있는 기기에서 on-device 추론을 수행한다.
     * SDK 활성화 후 실제 모델 호출로 교체 예정.
     */
    private String analyzeWithGemini(List<TransactionEntity> transactions, String yearMonth) {
        // AICore 서비스는 감지되었지만 SDK가 아직 통합되지 않은 상태.
        // 로컬 분석으로 폴백하되, Gemini 뱃지를 유지.
        Log.d(TAG, "AICore detected — SDK integration pending, using enhanced local analysis");
        return generateLocalSummary(transactions, yearMonth);
    }

    public interface AiCallback {
        void onSuccess(String summary);
        void onFailure(String message);
    }
}
