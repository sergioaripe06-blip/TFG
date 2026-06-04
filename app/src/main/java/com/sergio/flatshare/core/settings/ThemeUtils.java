package com.sergio.flatshare.core.settings;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class ThemeUtils {
    private ThemeUtils() {
    }

    public static void applyAppTheme(Context context) {
        boolean darkModeEnabled = SettingsStore.isDarkModeEnabled(context);
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(SettingsStore.getLanguage(context))
        );
    }
}
