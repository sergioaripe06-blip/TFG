package com.sergio.flatshare.features.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.BuildConfig;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.sync.UserSync;
import com.sergio.flatshare.features.shell.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        loginBtn.setOnClickListener(v -> login());
        if (BuildConfig.DEBUG) {
            debugSeedLoginBtn.setVisibility(View.VISIBLE);
            debugSeedLoginBtn.setOnClickListener(v -> showDebugSeedPicker());
        }
        registerTv.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
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
                    signIn(email, password);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void login() {
        String userOrEmail = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if (TextUtils.isEmpty(userOrEmail) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa usuario y contrase\u00f1a", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userOrEmail.contains("@")) {
            signIn(userOrEmail, password);
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
                    signIn(email, password);
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void signIn(String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    UserSync.ensureCurrentUserDocument();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
