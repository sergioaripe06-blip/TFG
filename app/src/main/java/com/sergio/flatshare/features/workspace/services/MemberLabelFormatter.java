package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;

public final class MemberLabelFormatter {
    private MemberLabelFormatter() {
    }

    public static String normalizeEmail(@Nullable String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String fallbackNameFromEmail(@Nullable String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return "Sin datos";
        int at = normalized.indexOf('@');
        if (at <= 0) return normalized;
        String local = normalized.substring(0, at)
                .replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();
        if (local.isEmpty()) return normalized;
        String[] parts = local.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(" ");
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.length() == 0 ? normalized : out.toString();
    }

    public static String formatTwoLines(@Nullable String email, Map<String, String> namesByEmail) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return "Sin datos\nsin correo";
        String displayName = namesByEmail.get(normalized);
        if (displayName == null || displayName.trim().isEmpty() || displayName.equalsIgnoreCase(normalized)) {
            displayName = fallbackNameFromEmail(normalized);
        }
        return displayName + "\n" + normalized;
    }

    public static String formatInline(@Nullable String email, Map<String, String> namesByEmail) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return "Sin datos - sin correo";
        String displayName = namesByEmail.get(normalized);
        if (displayName == null || displayName.trim().isEmpty() || displayName.equalsIgnoreCase(normalized)) {
            displayName = fallbackNameFromEmail(normalized);
        }
        return displayName + " - " + normalized;
    }
}
