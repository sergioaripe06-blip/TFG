package com.sergio.flatshare.shared.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateInputUtils {
    private static final String DISPLAY_PATTERN = "dd/MM/yyyy";
    private static final String LEGACY_PATTERN = "yyyy-MM-dd";
    private static final String MONTH_KEY_PATTERN = "yyyy-MM";

    private DateInputUtils() {
    }

    @Nullable
    public static Date parseDayOrNull(@Nullable String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;

        Date parsed = parseWithPattern(value, DISPLAY_PATTERN);
        if (parsed != null) return parsed;
        return parseWithPattern(value, LEGACY_PATTERN);
    }

    @NonNull
    public static String formatDay(@NonNull Date date) {
        return formatter(DISPLAY_PATTERN).format(date);
    }

    @NonNull
    public static String normalizeToDisplay(@Nullable String raw) {
        if (raw == null) return "";
        Date parsed = parseDayOrNull(raw);
        return parsed == null ? raw.trim() : formatDay(parsed);
    }

    @NonNull
    public static String normalizeMonthKeyToDisplay(@Nullable String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.isEmpty()) return "";

        Date parsedMonth = parseWithPattern(value, MONTH_KEY_PATTERN);
        if (parsedMonth == null) {
            return normalizeToDisplay(value);
        }

        return formatDay(parsedMonth);
    }

    @Nullable
    private static Date parseWithPattern(@NonNull String value, @NonNull String pattern) {
        SimpleDateFormat format = formatter(pattern);
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    @NonNull
    private static SimpleDateFormat formatter(@NonNull String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setLenient(false);
        return format;
    }
}
