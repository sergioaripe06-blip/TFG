package com.sergio.flatshare.features.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.core.sync.UserSync;
import com.sergio.flatshare.features.shell.MainActivity;

import java.util.Locale;

public class VerifyEmailActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_FULL_NAME = "extra_full_name";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_PHONE = "extra_phone";
    public static final String EXTRA_BIRTH_DATE = "extra_birth_date";
    public static final String EXTRA_TERMS_ACCEPTED = "extra_terms_accepted";

    private String email;
    private String fullName;
    private String username;
    private String phone;
    private String birthDate;
    private boolean termsAccepted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        TextView emailTv = findViewById(R.id.verifyEmailTv);
        Button resendBtn = findViewById(R.id.resendVerificationBtn);
        Button verifiedBtn = findViewById(R.id.verifiedContinueBtn);
        Button backToLoginBtn = findViewById(R.id.backToLoginBtn);

        readExtras();
        emailTv.setText(email);

        resendBtn.setOnClickListener(v -> resendVerificationEmail());
        verifiedBtn.setOnClickListener(v -> checkVerificationAndContinue());
        backToLoginBtn.setOnClickListener(v -> returnToLogin());
    }

    private void readExtras() {
        Intent intent = getIntent();
        fullName = safe(intent.getStringExtra(EXTRA_FULL_NAME));
        username = safe(intent.getStringExtra(EXTRA_USERNAME));
        phone = safe(intent.getStringExtra(EXTRA_PHONE));
        birthDate = safe(intent.getStringExtra(EXTRA_BIRTH_DATE));
        termsAccepted = intent.getBooleanExtra(EXTRA_TERMS_ACCEPTED, false);

        String emailFromIntent = safe(intent.getStringExtra(EXTRA_EMAIL));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (!TextUtils.isEmpty(emailFromIntent)) {
            email = emailFromIntent;
        } else if (user != null && user.getEmail() != null) {
            email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        } else {
            email = "(correo no disponible)";
        }

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username)) {
            SessionStore.PendingRegistrationProfile pending = SessionStore.getPendingRegistrationProfile(this);
            String normalizedPendingEmail = safe(pending.email).toLowerCase(Locale.ROOT);
            String normalizedCurrentEmail = safe(email).toLowerCase(Locale.ROOT);
            if (!TextUtils.isEmpty(normalizedPendingEmail) && normalizedPendingEmail.equals(normalizedCurrentEmail)) {
                fullName = safe(pending.fullName);
                username = safe(pending.username);
                phone = safe(pending.phone);
                birthDate = safe(pending.birthDate);
                termsAccepted = termsAccepted || pending.termsAccepted;
            }
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Tu sesión ha caducado. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            returnToLogin();
            return;
        }

        user.sendEmailVerification()
                .addOnSuccessListener(unused -> Toast.makeText(this, "Correo de verificación reenviado", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void checkVerificationAndContinue() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Tu sesión ha caducado. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            returnToLogin();
            return;
        }

        user.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (refreshedUser == null) {
                        Toast.makeText(this, "Tu sesión ha caducado. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
                        returnToLogin();
                        return;
                    }

                    if (!refreshedUser.isEmailVerified()) {
                        Toast.makeText(this, "Aún no está verificado. Revisa tu correo y pulsa " +
                                "\"Ya he verificado\".", Toast.LENGTH_LONG).show();
                        return;
                    }

                    refreshedUser.getIdToken(true)
                            .addOnSuccessListener(result -> {
                                finalizeProfileIfNeeded();
                                UserSync.ensureCurrentUserDocument();
                                SessionStore.clearPendingRegistrationProfile(this);
                                Toast.makeText(this, "Correo verificado. Bienvenido/a", Toast.LENGTH_SHORT).show();
                                openMain();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void finalizeProfileIfNeeded() {
        if (!TextUtils.isEmpty(fullName) && !TextUtils.isEmpty(username)) {
            UserSync.saveCurrentUserProfile(fullName, username, phone, birthDate);
        }
        if (termsAccepted) {
            UserSync.saveTermsAcceptance();
        }
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void returnToLogin() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
