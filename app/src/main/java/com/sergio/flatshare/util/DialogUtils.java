package com.sergio.flatshare.util;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sergio.flatshare.R;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static Shell buildShell(@NonNull Context context, @NonNull String title, @Nullable String subtitle, @Nullable View content, @Nullable String cancelLabel, @Nullable String confirmLabel) {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_shell, null, false);
        TextView titleTv = root.findViewById(R.id.dialogTitleTv);
        TextView subtitleTv = root.findViewById(R.id.dialogSubtitleTv);
        FrameLayout contentContainer = root.findViewById(R.id.dialogContentContainer);
        Button cancelBtn = root.findViewById(R.id.dialogCancelBtn);
        Button confirmBtn = root.findViewById(R.id.dialogConfirmBtn);
        TextView closeXBtn = root.findViewById(R.id.dialogCloseXBtn);

        titleTv.setText(title);
        if (subtitle == null || subtitle.trim().isEmpty()) {
            subtitleTv.setVisibility(View.GONE);
        } else {
            subtitleTv.setText(subtitle);
            subtitleTv.setVisibility(View.VISIBLE);
        }

        if (content != null) {
            contentContainer.removeAllViews();
            contentContainer.addView(content);
            contentContainer.setVisibility(View.VISIBLE);
        } else {
            contentContainer.setVisibility(View.GONE);
        }

        if (cancelLabel == null || cancelLabel.trim().isEmpty()) {
            cancelBtn.setVisibility(View.GONE);
        } else {
            cancelBtn.setText(cancelLabel);
            cancelBtn.setVisibility(View.VISIBLE);
        }

        if (confirmLabel == null || confirmLabel.trim().isEmpty()) {
            confirmBtn.setVisibility(View.GONE);
        } else {
            confirmBtn.setText(confirmLabel);
            confirmBtn.setVisibility(View.VISIBLE);
        }

        boolean cancelIsClose = isCloseLabel(cancelLabel);
        boolean confirmIsClose = isCloseLabel(confirmLabel);

        if (cancelIsClose || confirmIsClose) {
            closeXBtn.setVisibility(View.VISIBLE);
            if (cancelIsClose) cancelBtn.setVisibility(View.GONE);
            if (confirmIsClose) confirmBtn.setVisibility(View.GONE);
        } else {
            closeXBtn.setVisibility(View.GONE);
        }

        Button closeProxyBtn = cancelIsClose ? cancelBtn : (confirmIsClose ? confirmBtn : null);
        return new Shell(root, contentContainer, cancelBtn, confirmBtn, closeXBtn, closeProxyBtn);
    }

    public static AlertDialog show(@NonNull Context context, @NonNull View root) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.ThemeOverlay_FlatShare_Dialog)
                .setView(root)
                .create();
        dialog.show();
        TextView closeXBtn = root.findViewById(R.id.dialogCloseXBtn);
        if (closeXBtn != null && closeXBtn.getVisibility() == View.VISIBLE) {
            Object closeProxy = closeXBtn.getTag();
            if (closeProxy instanceof Button) {
                Button closeProxyBtn = (Button) closeProxy;
                closeXBtn.setOnClickListener(v -> closeProxyBtn.performClick());
            } else {
                closeXBtn.setOnClickListener(v -> dialog.dismiss());
            }
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private static boolean isCloseLabel(@Nullable String label) {
        return label != null && "cerrar".equalsIgnoreCase(label.trim());
    }

    public static TextView createMessageView(@NonNull Context context, @NonNull String message) {
        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextColor(context.getColor(R.color.text_light));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textView.setLineSpacing(0f, 1.15f);
        return textView;
    }

    public static LinearLayout createVerticalActions(@NonNull Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static Button createActionButton(@NonNull Context context, @NonNull String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setTextColor(context.getColor(primary ? R.color.on_primary_green : R.color.text_light));
        button.setBackgroundResource(primary ? R.drawable.bg_button_pill_primary : R.drawable.bg_tab_default);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 46)
        );
        params.topMargin = dp(context, 10);
        button.setLayoutParams(params);
        return button;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    public static final class Shell {
        public final View root;
        public final FrameLayout contentContainer;
        public final Button cancelBtn;
        public final Button confirmBtn;
        public final TextView closeXBtn;
        public final Button closeProxyBtn;

        private Shell(View root, FrameLayout contentContainer, Button cancelBtn, Button confirmBtn, TextView closeXBtn, Button closeProxyBtn) {
            this.root = root;
            this.contentContainer = contentContainer;
            this.cancelBtn = cancelBtn;
            this.confirmBtn = confirmBtn;
            this.closeXBtn = closeXBtn;
            this.closeProxyBtn = closeProxyBtn;
            if (this.closeXBtn != null) {
                this.closeXBtn.setTag(closeProxyBtn);
            }
        }
    }
}
