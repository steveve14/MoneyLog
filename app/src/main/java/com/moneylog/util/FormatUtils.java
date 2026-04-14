package com.moneylog.util;

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

    /** Long 금액 → "1,234,567원" (텍스트 모드: "약 1억원") */
    public static String formatAmountWithUnit(long amount) {
        if (textMode) {
            return formatAmountText(amount);
        }
        return AMOUNT_FORMAT.format(amount) + "원";
    }

    /** 금액을 한국어 텍스트로 변환 (약 1억원, 약 500만원 등) */
    public static String formatAmountText(long amount) {
        if (amount >= 1_0000_0000L) {
            long eok = amount / 1_0000_0000L;
            long remainder = amount % 1_0000_0000L;
            long cheonMan = remainder / 1000_0000L;
            if (remainder == 0) {
                return "약 " + eok + "억원";
            } else if (cheonMan > 0) {
                return "약 " + eok + "억 " + cheonMan + "천만원";
            }
            return "약 " + eok + "억원";
        } else if (amount >= 1_0000L) {
            long man = amount / 1_0000L;
            long remainder = amount % 1_0000L;
            long cheon = remainder / 1000L;
            if (remainder == 0) {
                return "약 " + man + "만원";
            } else if (cheon > 0) {
                return "약 " + man + "만 " + cheon + "천원";
            }
            return "약 " + man + "만원";
        }
        return AMOUNT_FORMAT.format(amount) + "원";
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

    /** 달력 셀용 간략 금액 표시 (만 단위 이상이면 '1.2만' 형태) */
    public static String formatCompact(long amount) {
        if (amount >= 100_000_000L) {
            return String.format("%.1f억", amount / 100_000_000.0);
        } else if (amount >= 10_000L) {
            return String.format("%.0f만", amount / 10_000.0);
        } else {
            return AMOUNT_FORMAT.format(amount);
        }
    }
}
