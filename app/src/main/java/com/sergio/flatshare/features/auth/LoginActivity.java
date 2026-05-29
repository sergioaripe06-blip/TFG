package com.sergio.flatshare.features.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.core.sync.UserSync;
import com.sergio.flatshare.features.shell.MainActivity;
import com.sergio.flatshare.shared.ui.NoticeUtils;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEt;
    private EditText passwordEt;
    private SwitchMaterial rememberMeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        rememberMeSwitch = findViewById(R.id.rememberMeSwitch);
        Button loginBtn = findViewById(R.id.loginBtn);
        TextView registerTv = findViewById(R.id.registerTv);
        TextView forgotPasswordTv = findViewById(R.id.forgotPasswordTv);
        loginBtn.setPaintFlags(loginBtn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        registerTv.setPaintFlags(registerTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        forgotPasswordTv.setPaintFlags(forgotPasswordTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        boolean rememberEnabled = SessionStore.isRememberMeEnabled(this);
        rememberMeSwitch.setChecked(rememberEnabled);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (rememberEnabled && currentUser != null) {
            handleRememberedSession(currentUser);
            return;
        }

        loginBtn.setOnClickListener(v -> login());
        registerTv.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordTv.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void handleRememberedSession(FirebaseUser currentUser) {
        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean verified = refreshedUser != null && refreshedUser.isEmailVerified();
            if (verified) {
                refreshedUser.getIdToken(true)
                        .addOnSuccessListener(result -> {
                            UserSync.ensureCurrentUserDocument();
                            openMain();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                return;
            }
            SessionStore.setRememberMeEnabled(this, false);
            FirebaseAuth.getInstance().signOut();
            NoticeUtils.show(this, getString(R.string.auth_verify_required));
        });
    }

    private void login() {
        String email = emailEt.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = passwordEt.getText().toString().trim();
        boolean rememberMe = rememberMeSwitch.isChecked();

        if (TextUtils.isEmpty(email)) {
            NoticeUtils.show(this, getString(R.string.auth_email_missing));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            NoticeUtils.show(this, getString(R.string.auth_password_missing));
            return;
        }
        signIn(email, password, rememberMe);
    }

    private void signIn(String email, String password, boolean rememberMe) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        NoticeUtils.show(this, getString(R.string.auth_login_unavailable));
                        return;
                    }

                    user.reload().addOnCompleteListener(task -> {
                        FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (refreshedUser == null) {
                            NoticeUtils.show(this, getString(R.string.auth_session_unavailable));
                            return;
                        }

                        if (!refreshedUser.isEmailVerified()) {
                            SessionStore.setRememberMeEnabled(this, false);
                            NoticeUtils.show(this, getString(R.string.auth_verify_pending));
                            openVerifyEmailScreen(refreshedUser.getEmail());
                            return;
                        }

                        refreshedUser.getIdToken(true)
                                .addOnSuccessListener(tokenResult -> {
                                    UserSync.ensureCurrentUserDocument();
                                    SessionStore.setRememberMeEnabled(this, rememberMe);
                                    openMain();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, mapAuthError(e), Toast.LENGTH_LONG).show());
    }

    private void openVerifyEmailScreen(String email) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            AuthEmailLocale.apply(this);
            currentUser.sendEmailVerification();
        }
        Intent intent = new Intent(this, VerifyEmailActivity.class);
        intent.putExtra(VerifyEmailActivity.EXTRA_EMAIL, email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        showSingleInputDialog(
                getString(R.string.auth_forgot_title),
                getString(R.string.auth_forgot_subtitle),
                getString(R.string.common_email_label),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                getString(R.string.common_send),
                emailEt.getText().toString().trim(),
                value -> {
                    requestPasswordReset(value.trim());
                    return true;
                }
        );
    }

    private void requestPasswordReset(String email) {
        if (TextUtils.isEmpty(email)) {
            NoticeUtils.show(this, getString(R.string.auth_enter_email));
            return;
        }

        if (!email.contains("@")) {
            NoticeUtils.show(this, getString(R.string.auth_forgot_email_only));
            return;
        }

        sendPasswordReset(email);
    }

    private void sendPasswordReset(String email) {
        AuthEmailLocale.apply(this);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim().toLowerCase(Locale.ROOT))
                .addOnSuccessListener(result -> Toast.makeText(this, getString(R.string.auth_forgot_email_sent), Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, mapAuthError(e), Toast.LENGTH_LONG).show());
    }

    private String mapAuthError(Exception e) {
        if (e instanceof FirebaseAuthException authException) {
            String code = authException.getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) return getString(R.string.auth_error_invalid_email);
            if ("ERROR_USER_NOT_FOUND".equals(code)) return getString(R.string.auth_error_user_not_found);
            if ("ERROR_WRONG_PASSWORD".equals(code) || "ERROR_INVALID_CREDENTIAL".equals(code)) {
                return getString(R.string.auth_error_bad_credentials);
            }
            if ("ERROR_USER_DISABLED".equals(code)) return getString(R.string.auth_error_user_disabled);
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) return getString(R.string.auth_error_too_many_requests);
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) return getString(R.string.auth_error_network);
        }
        return getString(R.string.auth_error_generic);
    }

    private void showSingleInputDialog(
            String title,
            String subtitle,
            String hint,
            int inputType,
            String actionLabel,
            String initialValue,
            SingleInputAction action
    ) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null, false);
        TextView titleTv = form.findViewById(R.id.dialogTitleTv);
        TextView subtitleTv = form.findViewById(R.id.dialogSubtitleTv);
        TextView inputLabelTv = form.findViewById(R.id.dialogInputLabelTv);
        TextView closeXBtn = form.findViewById(R.id.dialogCloseXBtn);
        EditText inputEt = form.findViewById(R.id.dialogInputEt);
        Button cancelBtn = form.findViewById(R.id.dialogCancelBtn);
        Button confirmBtn = form.findViewById(R.id.dialogConfirmBtn);

        titleTv.setText(title);
        subtitleTv.setText(subtitle);
        inputLabelTv.setText(hint);
        inputEt.setHint("");
        inputEt.setInputType(inputType);
        inputEt.setText(initialValue == null ? "" : initialValue);
        confirmBtn.setText(actionLabel);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_FlatShare_Dialog)
                .setView(form)
                .create();

        closeXBtn.setOnClickListener(v -> dialog.dismiss());
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        confirmBtn.setOnClickListener(v -> {
            boolean close = action.onConfirm(inputEt.getText().toString());
            if (close) dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private interface SingleInputAction {
        boolean onConfirm(String value);
    }
}
