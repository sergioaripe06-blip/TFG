package com.sergio.flatshare.shared.ui;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public final class NoticeUtils {
    private NoticeUtils() {
    }

    public static void show(@NonNull Context context, @StringRes int messageRes) {
        show(context, context.getString(messageRes));
    }

    public static void show(@NonNull Context context, @Nullable String message) {
        String resolvedMessage = message == null || message.trim().isEmpty()
                ? "Ha ocurrido un error inesperado."
                : message.trim();
        DialogUtils.Shell shell = DialogUtils.buildShell(
                context,
                "Aviso",
                null,
                DialogUtils.createMessageView(context, resolvedMessage),
                null,
                "Aceptar"
        );
        AlertDialog dialog = DialogUtils.show(context, shell.root);
        shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
    }
}
