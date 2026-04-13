package com.moneylog.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DATE_FMT        = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YEAR_MONTH_FMT  = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DISPLAY_FMT     = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter DISPLAY_YM_FMT  = DateTimeFormatter.ofPattern("yyyy년 M월");

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

    /** "yyyy-MM-dd" → "M월 d일" */
    public static String toDisplayDate(String dateStr) {
        return parseDate(dateStr).format(DISPLAY_FMT);
    }

    /** "yyyy-MM" → "yyyy년 M월" */
    public static String toDisplayYearMonth(String yearMonth) {
        return YearMonth.parse(yearMonth, YEAR_MONTH_FMT).format(DISPLAY_YM_FMT);
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
