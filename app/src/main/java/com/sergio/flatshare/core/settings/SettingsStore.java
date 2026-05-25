package com.sergio.flatshare.core.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    private static final String PREFS_NAME = "flatshare_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_MUSIC_VOLUME = "music_volume";

    private SettingsStore() {}

    public static boolean isDarkModeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, true);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, "es");
    }

    public static void setLanguage(Context context, String languageCode) {
        prefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    public static boolean areNotificationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public static int getMusicVolume(Context context) {
        return prefs(context).getInt(KEY_MUSIC_VOLUME, 35);
    }

    public static void setMusicVolume(Context context, int volume) {
        int clamped = Math.max(0, Math.min(100, volume));
        prefs(context).edit().putInt(KEY_MUSIC_VOLUME, clamped).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
