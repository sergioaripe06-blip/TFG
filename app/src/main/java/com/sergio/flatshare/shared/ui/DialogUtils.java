package com.sergio.flatshare.shared.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sergio.flatshare.R;

import java.util.List;
import java.util.Map;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static Shell buildShell(@NonNull Context context, @NonNull String title, @Nullable String subtitle, @Nullable View content, @Nullable String cancelLabel, @Nullable String confirmLabel) {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_shell, null, false);
        TextView titleTv = root.findViewById(R.id.dialogTitleTv);
        TextView subtitleTv = root.findViewById(R.id.dialogSubtitleTv);
        FrameLayout contentContainer = root.findViewById(R.id.dialogContentContainer);
        LinearLayout buttonsRow = root.findViewById(R.id.dialogButtonsRow);
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
        closeXBtn.setVisibility(View.VISIBLE);
        if (cancelIsClose) cancelBtn.setVisibility(View.GONE);
        if (confirmIsClose) confirmBtn.setVisibility(View.GONE);

        if (buttonsRow != null) {
            boolean hasVisibleAction = cancelBtn.getVisibility() == View.VISIBLE || confirmBtn.getVisibility() == View.VISIBLE;
            buttonsRow.setVisibility(hasVisibleAction ? View.VISIBLE : View.GONE);
        }

        return new Shell(root, contentContainer, cancelBtn, confirmBtn, closeXBtn);
    }

    public static AlertDialog show(@NonNull Context context, @NonNull View root) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.ThemeOverlay_FlatShare_Dialog)
                .setView(root)
                .create();
        dialog.show();
        TextView closeXBtn = root.findViewById(R.id.dialogCloseXBtn);
        if (closeXBtn != null && closeXBtn.getVisibility() == View.VISIBLE) {
            closeXBtn.setOnClickListener(v -> dialog.dismiss());
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            );
        }
        return dialog;
    }

    private static boolean isCloseLabel(@Nullable String label) {
        if (label == null) return false;
        String normalized = label.trim().toLowerCase();
        return "cerrar".equals(normalized) || "close".equals(normalized);
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

    public static LinearLayout createInfoRowsView(@NonNull Context context, @NonNull Map<String, String> rows) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        for (Map.Entry<String, String> entry : rows.entrySet()) {
            String label = entry.getKey() == null ? "" : entry.getKey().trim();
            String value = entry.getValue() == null || entry.getValue().trim().isEmpty() ? "Sin datos" : entry.getValue().trim();

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_input_dark_round);
            card.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.topMargin = dp(context, 8);
            card.setLayoutParams(cardParams);

            TextView labelTv = new TextView(context);
            labelTv.setText(label);
            labelTv.setTextColor(context.getColor(R.color.text_muted));
            labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            card.addView(labelTv);

            TextView valueTv = new TextView(context);
            valueTv.setText(value);
            valueTv.setTextColor(context.getColor(R.color.text_light));
            valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            valueTv.setLineSpacing(0f, 1.12f);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            valueParams.topMargin = dp(context, 3);
            valueTv.setLayoutParams(valueParams);
            card.addView(valueTv);

            root.addView(card);
        }

        return root;
    }

    public static LinearLayout createReminderDetailView(
            @NonNull Context context,
            @NonNull String groupName,
            @NonNull String targetTitle,
            @NonNull List<String> targetItems,
            @NonNull String frequency,
            @NonNull String fromDate,
            @NonNull String toDate
    ) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(createInfoCard(context, "Nombre del piso", groupName));

        StringBuilder targetsText = new StringBuilder();
        if (targetItems.isEmpty()) {
            targetsText.append("• Sin datos");
        } else {
            for (int i = 0; i < targetItems.size(); i++) {
                if (i > 0) targetsText.append("\n");
                targetsText.append("• ").append(targetItems.get(i));
            }
        }
        root.addView(createInfoCard(context, targetTitle, targetsText.toString()));
        root.addView(createInfoCard(context, "Frecuencia", frequency));

        LinearLayout datesRow = new LinearLayout(context);
        datesRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(context, 8);
        datesRow.setLayoutParams(rowParams);

        View fromCard = createInfoCard(context, "Desde", fromDate);
        View toCard = createInfoCard(context, "Hasta", toDate);

        LinearLayout.LayoutParams fromParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        fromCard.setLayoutParams(fromParams);

        LinearLayout.LayoutParams toParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        toParams.leftMargin = dp(context, 8);
        toCard.setLayoutParams(toParams);

        datesRow.addView(fromCard);
        datesRow.addView(toCard);
        root.addView(datesRow);
        return root;
    }

    private static LinearLayout createInfoCard(@NonNull Context context, @NonNull String label, @NonNull String value) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_input_dark_round);
        card.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(context, 8);
        card.setLayoutParams(cardParams);

        TextView labelTv = new TextView(context);
        labelTv.setText(label);
        labelTv.setTextColor(context.getColor(R.color.text_muted));
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        card.addView(labelTv);

        TextView valueTv = new TextView(context);
        valueTv.setText(value == null || value.trim().isEmpty() ? "Sin datos" : value.trim());
        valueTv.setTextColor(context.getColor(R.color.text_light));
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        valueTv.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueParams.topMargin = dp(context, 3);
        valueTv.setLayoutParams(valueParams);
        card.addView(valueTv);
        return card;
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

        private Shell(View root, FrameLayout contentContainer, Button cancelBtn, Button confirmBtn, TextView closeXBtn) {
            this.root = root;
            this.contentContainer = contentContainer;
            this.cancelBtn = cancelBtn;
            this.confirmBtn = confirmBtn;
            this.closeXBtn = closeXBtn;
        }
    }
}
