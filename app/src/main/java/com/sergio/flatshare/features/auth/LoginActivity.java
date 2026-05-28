package com.sergio.flatshare.features.auth;

import com.sergio.flatshare.shared.ui.NoticeUtils;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.graphics.Paint;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Toast;
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
            NoticeUtils.show(this, "Debes verificar tu correo antes de entrar");
        });
    }

    private void login() {
        String email = emailEt.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = passwordEt.getText().toString().trim();
        boolean rememberMe = rememberMeSwitch.isChecked();

        if (TextUtils.isEmpty(email)) {
            NoticeUtils.show(this, "Falta el correo electrónico");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            NoticeUtils.show(this, "Falta la contraseña");
            return;
        }
        signIn(email, password, rememberMe);
    }

    private void signIn(String email, String password, boolean rememberMe) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        NoticeUtils.show(this, "No se pudo iniciar sesión");
                        return;
                    }

                    user.reload().addOnCompleteListener(task -> {
                        FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (refreshedUser == null) {
                            NoticeUtils.show(this, "Sesión no disponible");
                            return;
                        }

                        if (!refreshedUser.isEmailVerified()) {
                            SessionStore.setRememberMeEnabled(this, false);
                            NoticeUtils.show(this, "Tu correo aún no está verificado");
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
                .addOnFailureListener(e -> Toast.makeText(this, mapAuthErrorToSpanish(e), Toast.LENGTH_LONG).show());
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
            NoticeUtils.show(this, "Introduce tu correo electrónico");
            return;
        }

        if (!email.contains("@")) {
            NoticeUtils.show(this, "La recuperación solo está disponible con correo electrónico");
            return;
        }

        sendPasswordReset(email);
    }

    private void sendPasswordReset(String email) {
        AuthEmailLocale.apply(this);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim().toLowerCase(Locale.ROOT))
                .addOnSuccessListener(result -> Toast.makeText(this, "Te hemos enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, mapAuthErrorToSpanish(e), Toast.LENGTH_LONG).show());
    }

    private String mapAuthErrorToSpanish(Exception e) {
        if (e instanceof FirebaseAuthException authException) {
            String code = authException.getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) return "El correo electrónico no es válido.";
            if ("ERROR_USER_NOT_FOUND".equals(code)) return "No existe ninguna cuenta con ese correo.";
            if ("ERROR_WRONG_PASSWORD".equals(code) || "ERROR_INVALID_CREDENTIAL".equals(code)) {
                return "Correo o contraseña incorrectos.";
            }
            if ("ERROR_USER_DISABLED".equals(code)) return "Esta cuenta está deshabilitada.";
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) return "Demasiados intentos. Espera un momento y vuelve a intentarlo.";
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) return "Error de conexión. Revisa internet e inténtalo otra vez.";
        }
        return "No se pudo completar la operación. Revisa los datos e inténtalo de nuevo.";
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

