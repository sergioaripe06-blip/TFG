package com.sergio.flatshare.features.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.sergio.flatshare.core.settings.SettingsStore;

import java.util.Locale;

final class AuthEmailLocale {
    private AuthEmailLocale() {
    }

    static void apply(@NonNull Context context) {
        String language = SettingsStore.getLanguage(context);
        String normalized = "en".equalsIgnoreCase(language) ? "en" : "es";
        FirebaseAuth.getInstance().setLanguageCode(normalized.toLowerCase(Locale.ROOT));
    }
}

