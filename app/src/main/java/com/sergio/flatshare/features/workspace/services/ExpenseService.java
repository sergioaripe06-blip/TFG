package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public class ExpenseService {
    @NonNull
    public String normalizeCategory(@Nullable String category) {
        if (category == null) return "";
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return "";
        return normalized.replaceAll("\\s+", " ");
    }

    @NonNull
    public String normalizePriority(@Nullable String priority) {
        if (priority == null || priority.trim().isEmpty()) return "media";
        return priority.trim().toLowerCase(Locale.ROOT);
    }
}

