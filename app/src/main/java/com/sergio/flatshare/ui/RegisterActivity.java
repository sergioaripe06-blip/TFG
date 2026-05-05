package com.sergio.flatshare.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.UserSync;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText fullNameEt = findViewById(R.id.fullNameEt);
        EditText usernameEt = findViewById(R.id.usernameEt);
        EditText emailEt = findViewById(R.id.emailEt);
        EditText phoneEt = findViewById(R.id.phoneEt);
        EditText cityEt = findViewById(R.id.cityEt);
        EditText passwordEt = findViewById(R.id.passwordEt);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView loginTv = findViewById(R.id.loginTv);

        registerBtn.setOnClickListener(v -> {
            String fullName = fullNameEt.getText().toString().trim();
            String username = usernameEt.getText().toString().trim().toLowerCase();
            String email = emailEt.getText().toString().trim();
            String phone = phoneEt.getText().toString().trim();
            String city = cityEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Completa nombre, usuario, email y contrase\u00f1a", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore.getInstance().collection("usernames")
                    .document(username)
                    .get()
                    .addOnSuccessListener(result -> {
                        if (result.exists()) {
                            Toast.makeText(this, "Ese usuario ya existe", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener(authResult -> {
                                    if (authResult.getUser() != null) {
                                        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(fullName)
                                                .build();
                                        authResult.getUser().updateProfile(profile);
                                    }
                                    UserSync.saveCurrentUserProfile(fullName, username, phone, city);
                                    Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        });

        loginTv.setOnClickListener(v -> finish());
    }
}
