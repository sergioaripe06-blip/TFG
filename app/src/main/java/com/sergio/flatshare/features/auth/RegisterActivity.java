package com.sergio.flatshare.features.auth;

import android.app.DatePickerDialog;
import android.os.Build;
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
import com.sergio.flatshare.core.sync.UserSync;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText fullNameEt = findViewById(R.id.fullNameEt);
        EditText usernameEt = findViewById(R.id.usernameEt);
        EditText emailEt = findViewById(R.id.emailEt);
        EditText phoneEt = findViewById(R.id.phoneEt);
        EditText birthDateEt = findViewById(R.id.birthDateEt);
        EditText passwordEt = findViewById(R.id.passwordEt);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView loginTv = findViewById(R.id.loginTv);
        setupBirthDateField(birthDateEt);

        registerBtn.setOnClickListener(v -> {
            String fullName = fullNameEt.getText().toString().trim();
            String username = usernameEt.getText().toString().trim().toLowerCase(Locale.ROOT);
            String email = emailEt.getText().toString().trim();
            String phone = phoneEt.getText().toString().trim();
            String birthDate = birthDateEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username) || TextUtils.isEmpty(email)
                    || TextUtils.isEmpty(phone) || TextUtils.isEmpty(birthDate)
                    || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Completa todos los campos del registro", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isAdult(birthDate)) {
                Toast.makeText(this, "Debes tener al menos 18 años", Toast.LENGTH_SHORT).show();
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
                                    UserSync.saveCurrentUserProfile(fullName, username, phone, birthDate);
                                    Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        });

        loginTv.setOnClickListener(v -> finish());
    }

    private boolean isAdult(String birthDateIso) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        fmt.setLenient(false);
        Date birthDate;
        try {
            birthDate = fmt.parse(birthDateIso);
        } catch (ParseException e) {
            Toast.makeText(this, "Formato inválido. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthDate);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age >= 18;
    }

    private void setupBirthDateField(EditText birthDateEt) {
        birthDateEt.setKeyListener(null);
        birthDateEt.setFocusable(false);
        birthDateEt.setFocusableInTouchMode(false);
        birthDateEt.setClickable(true);
        birthDateEt.setCursorVisible(false);
        birthDateEt.setLongClickable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            birthDateEt.setShowSoftInputOnFocus(false);
        }
        birthDateEt.setOnLongClickListener(v -> true);
        birthDateEt.setOnClickListener(v -> openBirthDatePicker(birthDateEt));
        birthDateEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) openBirthDatePicker(birthDateEt);
        });
    }

    private void openBirthDatePicker(EditText targetField) {
        Calendar calendar = Calendar.getInstance();
        String currentValue = targetField.getText() == null ? "" : targetField.getText().toString().trim();
        if (!currentValue.isEmpty()) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                fmt.setLenient(false);
                calendar.setTime(fmt.parse(currentValue));
            } catch (Exception ignored) {
            }
        } else {
            calendar.add(Calendar.YEAR, -18);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> targetField.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }
}
