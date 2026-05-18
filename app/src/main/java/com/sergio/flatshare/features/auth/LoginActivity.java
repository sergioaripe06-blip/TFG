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

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.core.sync.UserSync;
import com.sergio.flatshare.features.shell.MainActivity;

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

        boolean rememberEnabled = SessionStore.isRememberMeEnabled(this);
        rememberMeSwitch.setChecked(rememberEnabled);
        if (rememberEnabled && FirebaseAuth.getInstance().getCurrentUser() != null) {
            UserSync.ensureCurrentUserDocument();
            openMain();
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

    private void login() {
        String userOrEmail = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();
        boolean rememberMe = rememberMeSwitch.isChecked();

        if (TextUtils.isEmpty(userOrEmail) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userOrEmail.contains("@")) {
            signIn(userOrEmail, password, rememberMe);
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
                    signIn(email, password, rememberMe);
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void signIn(String email, String password, boolean rememberMe) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    UserSync.ensureCurrentUserDocument();
                    SessionStore.setRememberMeEnabled(this, rememberMe);
                    openMain();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
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
