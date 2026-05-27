package com.sergio.flatshare.features.workspace.services;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sergio.flatshare.shared.ui.DialogUtils;

public class ExpenseDialogs {
    private ExpenseDialogs() {
    }

    @NonNull
    public static View wrapFormForDialogScroll(@NonNull Context context, @NonNull View form) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setNestedScrollingEnabled(true);
        scrollView.setFocusable(true);
        scrollView.setFocusableInTouchMode(true);
        scrollView.addView(form, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    public static void tuneLongFormShell(@NonNull DialogUtils.Shell shell) {
        if (shell.contentContainer.getLayoutParams() instanceof LinearLayout.LayoutParams params) {
            params.height = 0;
            params.weight = 1f;
            shell.contentContainer.setLayoutParams(params);
        }
    }

    public static void adjustLongFormDialogWindow(@NonNull Context context, @Nullable AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.96f);
        int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.90f);
        dialog.getWindow().setLayout(width, height);
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );
    }
}

