package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class PaymentAccessPolicy {
    private PaymentAccessPolicy() {
    }

    public static boolean canAcceptPendingPayment(
            @NonNull String normalizedStatus,
            boolean isOwner,
            @NonNull String currentUserEmail,
            @Nullable String fromEmail
    ) {
        if (!"pending".equals(normalizedStatus)) return false;
        if (isOwner) return true;
        return currentUserEmail.equals(normalizeEmail(fromEmail));
    }

    public static boolean canDeletePayment(
            boolean isOwner,
            @NonNull String currentUserEmail,
            @Nullable String fromEmail
    ) {
        if (isOwner) return true;
        return currentUserEmail.equals(normalizeEmail(fromEmail));
    }

    @NonNull
    public static String normalizeEmail(@Nullable String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
