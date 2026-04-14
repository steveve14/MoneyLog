package com.moneylog.util;

import android.content.Context;

import com.moneylog.R;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DATE_FMT        = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YEAR_MONTH_FMT  = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateUtils() {}

    /** 오늘 날짜 → "yyyy-MM-dd" */
    public static String today() {
        return LocalDate.now().format(DATE_FMT);
    }

    /** 현재 연월 → "yyyy-MM" */
    public static String currentYearMonth() {
        return YearMonth.now().format(YEAR_MONTH_FMT);
    }

    /** String "yyyy-MM-dd" → LocalDate */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FMT);
    }

    /** LocalDate → "yyyy-MM-dd" */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FMT);
    }

    /** "yyyy-MM-dd" → locale-aware display (e.g., "4월 14일" / "Apr 14") */
    public static String toDisplayDate(String dateStr, Context context) {
        String pattern = context.getString(R.string.date_format_md);
        return parseDate(dateStr).format(DateTimeFormatter.ofPattern(pattern));
    }

    /** "yyyy-MM-dd" → locale-aware display (fallback without context) */
    public static String toDisplayDate(String dateStr) {
        return parseDate(dateStr).format(DateTimeFormatter.ofPattern("M/d"));
    }

    /** "yyyy-MM" → locale-aware display (e.g., "2026년 4월" / "April 2026") */
    public static String toDisplayYearMonth(String yearMonth, Context context) {
        String pattern = context.getString(R.string.date_format_ym);
        return YearMonth.parse(yearMonth, YEAR_MONTH_FMT).format(DateTimeFormatter.ofPattern(pattern));
    }

    /** "yyyy-MM" → locale-aware display (fallback without context) */
    public static String toDisplayYearMonth(String yearMonth) {
        return YearMonth.parse(yearMonth, YEAR_MONTH_FMT).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /** 이전 달 → "yyyy-MM" */
    public static String previousMonth(String yearMonth) {
        return YearMonth.parse(yearMonth, YEAR_MONTH_FMT).minusMonths(1).format(YEAR_MONTH_FMT);
    }

    /** 다음 달 → "yyyy-MM" */
    public static String nextMonth(String yearMonth) {
        return YearMonth.parse(yearMonth, YEAR_MONTH_FMT).plusMonths(1).format(YEAR_MONTH_FMT);
    }
}
