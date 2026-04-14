package com.moneylog.util;

import android.content.Context;

import com.moneylog.R;

import java.text.DecimalFormat;

public final class FormatUtils {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,###");
    private static boolean textMode = false;

    private FormatUtils() {}

    public static void setTextMode(boolean enabled) {
        textMode = enabled;
    }

    public static boolean isTextMode() {
        return textMode;
    }

    /** Long 금액 → "1,234,567" */
    public static String formatAmount(long amount) {
        return AMOUNT_FORMAT.format(amount);
    }

    /** Long 금액 → locale-aware unit (Context 버전) */
    public static String formatAmountWithUnit(long amount, Context context) {
        if (textMode) {
            return formatAmountText(amount, context);
        }
        return context.getString(R.string.amount_with_unit, AMOUNT_FORMAT.format(amount));
    }

    /** Fallback (Context 없음) */
    public static String formatAmountWithUnit(long amount) {
        if (textMode) {
            return formatAmountText(amount);
        }
        return AMOUNT_FORMAT.format(amount);
    }

    /** 금액을 텍스트로 변환 — locale-aware (Context 버전) */
    public static String formatAmountText(long amount, Context context) {
        if (amount >= 1_0000_0000L) {
            long eok = amount / 1_0000_0000L;
            long remainder = amount % 1_0000_0000L;
            long cheonMan = remainder / 1000_0000L;
            if (remainder == 0 || cheonMan == 0) {
                return context.getString(R.string.amount_approx_eok, (int) eok);
            } else {
                return context.getString(R.string.amount_approx_eok_cheonman, (int) eok, (int) cheonMan);
            }
        } else if (amount >= 1_0000L) {
            long man = amount / 1_0000L;
            long remainder = amount % 1_0000L;
            long cheon = remainder / 1000L;
            if (remainder == 0 || cheon == 0) {
                return context.getString(R.string.amount_approx_man, (int) man);
            } else {
                return context.getString(R.string.amount_approx_man_cheon, (int) man, (int) cheon);
            }
        }
        return context.getString(R.string.amount_with_unit, AMOUNT_FORMAT.format(amount));
    }

    /** Fallback (Context 없음) */
    public static String formatAmountText(long amount) {
        return AMOUNT_FORMAT.format(amount);
    }

    /** Double 비율 → "75.3%" */
    public static String formatPercent(double ratio) {
        return String.format("%.1f%%", ratio * 100.0);
    }

    /** 텍스트 입력 → Long (비숫자 제거, 실패 시 0) */
    public static long parseAmountInput(String input) {
        if (input == null || input.isEmpty()) return 0L;
        String clean = input.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return 0L;
        try { return Long.parseLong(clean); }
        catch (NumberFormatException e) { return 0L; }
    }

    /** Long 금액 → 입력 필드 표시용 콤마 문자열 */
    public static String formatAmountInput(long amount) {
        return amount == 0L ? "" : AMOUNT_FORMAT.format(amount);
    }

    /** 달력 셀용 간략 금액 표시 — locale-aware (Context 버전) */
    public static String formatCompact(long amount, Context context) {
        boolean western = context.getResources().getBoolean(R.bool.compact_western);
        if (western) {
            if (amount >= 1_000_000L)
                return context.getString(R.string.compact_large, amount / 1_000_000.0);
            if (amount >= 1_000L)
                return context.getString(R.string.compact_medium, amount / 1_000.0);
        } else {
            if (amount >= 100_000_000L)
                return context.getString(R.string.compact_large, amount / 100_000_000.0);
            if (amount >= 10_000L)
                return context.getString(R.string.compact_medium, amount / 10_000.0);
        }
        return AMOUNT_FORMAT.format(amount);
    }

    /** Fallback (Context 없음) */
    public static String formatCompact(long amount) {
        if (amount >= 100_000_000L) {
            return String.format("%.1f억", amount / 100_000_000.0);
        } else if (amount >= 10_000L) {
            return String.format("%.0f만", amount / 10_000.0);
        }
        return AMOUNT_FORMAT.format(amount);
    }
}
