package com.moneylog.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Per-App Language helper using AppCompatDelegate (Android 13+ system integration + backward compat).
 */
public final class LocaleHelper {

    private static final String PREFS_NAME = "moneylog_prefs";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_AMOUNT_TEXT_MODE = "amount_text_mode";

    /** Supported locales in display order. */
    public static final String[] LOCALE_TAGS = {"", "ko", "en", "ja"};

    private LocaleHelper() { }

    /**
     * Apply the saved locale via AppCompatDelegate.
     * Call once in Application.onCreate() or Activity.onCreate().
     */
    public static void applySavedLocale(Context context) {
        String tag = getSavedLanguageTag(context);
        setLocale(tag);
    }

    /**
     * Change locale and persist the choice.
     * @param tag BCP-47 tag ("ko", "en", "ja") or empty string for system default.
     */
    public static void setLocale(String tag) {
        if (tag == null || tag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag));
        }
    }

    public static void saveLanguageTag(Context context, String tag) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, tag).apply();
    }

    public static String getSavedLanguageTag(Context context) {
        return getPrefs(context).getString(KEY_LANGUAGE, "");
    }

    public static boolean isOnboardingDone(Context context) {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(Context context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public static boolean isAmountTextMode(Context context) {
        return getPrefs(context).getBoolean(KEY_AMOUNT_TEXT_MODE, false);
    }

    public static void setAmountTextMode(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_AMOUNT_TEXT_MODE, enabled).apply();
    }

    /**
     * Returns the currently active locale tag from AppCompatDelegate.
     * Empty string means system default.
     */
    public static String getCurrentLocaleTag() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return "";
        }
        return locales.get(0) != null ? locales.get(0).toLanguageTag() : "";
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
