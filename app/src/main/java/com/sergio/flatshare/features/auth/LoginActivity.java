package com.sergio.flatshare.features.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.BuildConfig;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.sync.UserSync;
import com.sergio.flatshare.features.shell.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final int DEBUG_SEED_USER_COUNT = 20;
    private static final String DEBUG_SEED_PREFIX = "seeduser";
    private static final String DEBUG_SEED_DOMAIN = "seed.flatshare.local";
    private static final String DEBUG_SEED_PASSWORD_PREFIX = "FlatShareSeed!";

    private EditText emailEt;
    private EditText passwordEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button debugSeedLoginBtn = findViewById(R.id.debugSeedLoginBtn);
        TextView registerTv = findViewById(R.id.registerTv);
        TextView forgotPasswordTv = findViewById(R.id.forgotPasswordTv);

        loginBtn.setOnClickListener(v -> login());
        if (BuildConfig.DEBUG) {
            debugSeedLoginBtn.setVisibility(View.VISIBLE);
            debugSeedLoginBtn.setOnClickListener(v -> showDebugSeedPicker());
        }
        registerTv.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordTv.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showDebugSeedPicker() {
        List<String> labels = new ArrayList<>();
        for (int i = 1; i <= DEBUG_SEED_USER_COUNT; i++) {
            String suffix = String.format(Locale.ROOT, "%03d", i);
            labels.add(DEBUG_SEED_PREFIX + suffix + "@" + DEBUG_SEED_DOMAIN);
        }
        new AlertDialog.Builder(this)
                .setTitle("Entrar con cuenta seed")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    int index = which + 1;
                    String suffix = String.format(Locale.ROOT, "%03d", index);
                    String email = DEBUG_SEED_PREFIX + suffix + "@" + DEBUG_SEED_DOMAIN;
                    String password = DEBUG_SEED_PASSWORD_PREFIX + suffix;
                    emailEt.setText(email);
                    passwordEt.setText(password);
                    signIn(email, password, true);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void login() {
        String userOrEmail = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if (TextUtils.isEmpty(userOrEmail) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userOrEmail.contains("@")) {
            signIn(userOrEmail, password, false);
            return;
        }

        FirebaseFirestore.getInstance().collection("usernames")
                .document(userOrEmail.toLowerCase())
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.exists()) {
                        Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String email = result.getString("email");
                    if (email == null || email.trim().isEmpty()) {
                        Toast.makeText(this, "Ese usuario no tiene email asociado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    signIn(email, password, false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void signIn(String email, String password) {
        signIn(email, password, false);
    }

    private void signIn(String email, String password, boolean allowSeedAutocreate) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    UserSync.ensureCurrentUserDocument();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (allowSeedAutocreate
                            && isDebugSeedEmail(email)
                            && e instanceof FirebaseAuthInvalidUserException) {
                        createSeedAccountAndSignIn(email, password);
                        return;
                    }
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean isDebugSeedEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(DEBUG_SEED_PREFIX) && normalized.endsWith("@" + DEBUG_SEED_DOMAIN);
    }

    private void createSeedAccountAndSignIn(String email, String password) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    UserSync.ensureCurrentUserDocument();
                    ensureSeedUsernameAlias(email);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "No se pudo crear la cuenta seed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void ensureSeedUsernameAlias(String email) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        int atPos = email.indexOf('@');
        if (atPos <= 0) return;

        String username = email.substring(0, atPos).toLowerCase(Locale.ROOT);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", normalizedEmail);
        data.put("displayName", username);

        FirebaseFirestore.getInstance()
                .collection("usernames")
                .document(username)
                .set(data);
    }

    private void showForgotPasswordDialog() {
        showSingleInputDialog(
                "¿Olvidaste la contraseña?",
                "Introduce tu correo electrónico y te enviaremos un enlace para restablecerla.",
                "Correo electrónico",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                "Enviar",
                emailEt.getText().toString().trim(),
                value -> {
                    requestPasswordReset(value.trim());
                    return true;
                }
        );
    }

    private void requestPasswordReset(String email) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Introduce tu correo electrónico", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            Toast.makeText(this, "La recuperación solo está disponible con correo electrónico", Toast.LENGTH_LONG).show();
            return;
        }

        sendPasswordReset(email);
    }

    private void sendPasswordReset(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim().toLowerCase(Locale.ROOT))
                .addOnSuccessListener(result -> Toast.makeText(this, "Te hemos enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
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
