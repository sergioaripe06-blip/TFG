package com.sergio.flatshare.features.auth;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.shared.ui.CountryPhoneUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText fullNameEt = findViewById(R.id.fullNameEt);
        EditText emailEt = findViewById(R.id.emailEt);
        Spinner phoneCountrySpinner = findViewById(R.id.phoneCountrySpinner);
        EditText phoneEt = findViewById(R.id.phoneEt);
        EditText birthDateEt = findViewById(R.id.birthDateEt);
        EditText passwordEt = findViewById(R.id.passwordEt);
        SwitchMaterial termsSwitch = findViewById(R.id.termsSwitch);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView loginTv = findViewById(R.id.loginTv);
        TextView viewTermsTv = findViewById(R.id.viewTermsTv);

        List<CountryPhoneUtils.CountryOption> countries = CountryPhoneUtils.loadCountries();
        phoneCountrySpinner.setAdapter(CountryPhoneUtils.buildAdapter(this, countries));
        CountryPhoneUtils.selectRegion(phoneCountrySpinner, countries, "ES");

        registerBtn.setPaintFlags(registerBtn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        loginTv.setPaintFlags(loginTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        setupBirthDateField(birthDateEt);
        viewTermsTv.setOnClickListener(v -> showTermsDialog());

        registerBtn.setOnClickListener(v -> {
            String fullName = fullNameEt.getText().toString().trim();
            String email = emailEt.getText().toString().trim().toLowerCase(Locale.ROOT);
            String localPhone = phoneEt.getText().toString().trim();
            String birthDate = birthDateEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            CountryPhoneUtils.CountryOption selectedCountry = CountryPhoneUtils.selected(phoneCountrySpinner, countries);
            String phone = CountryPhoneUtils.buildFullPhone(selectedCountry, localPhone);

            if (TextUtils.isEmpty(email)) {
                NoticeUtils.show(this, "Falta el correo electrónico");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                NoticeUtils.show(this, "Falta la contraseña");
                return;
            }
            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(localPhone)
                    || TextUtils.isEmpty(birthDate) || TextUtils.isEmpty(password)) {
                NoticeUtils.show(this, "Completa todos los campos del registro");
                return;
            }

            if (!isAdult(birthDate)) {
                NoticeUtils.show(this, "Debes tener al menos 18 años");
                return;
            }

            if (!termsSwitch.isChecked()) {
                NoticeUtils.show(this, "Debes aceptar los términos y condiciones de uso");
                return;
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        if (authResult.getUser() == null) {
                            NoticeUtils.show(this, "No se pudo crear la cuenta. Inténtalo de nuevo.");
                            return;
                        }
                        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();
                        authResult.getUser().updateProfile(profile);
                        SessionStore.savePendingRegistrationProfile(
                                this,
                                email,
                                fullName,
                                phone,
                                birthDate,
                                true
                        );
                        AuthEmailLocale.apply(this);
                        authResult.getUser().sendEmailVerification()
                                .addOnSuccessListener(unused -> {
                                    NoticeUtils.show(this, "Te hemos enviado un correo de verificación");
                                    openVerifyEmailScreen(email, fullName, phone, birthDate, true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(
                                            this,
                                            "Cuenta creada, pero no pudimos enviar el correo de verificación. Puedes reenviarlo desde la siguiente pantalla.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                    openVerifyEmailScreen(email, fullName, phone, birthDate, true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            NoticeUtils.show(this, "Ese correo ya tiene una cuenta registrada");
                            return;
                        }
                        NoticeUtils.show(this, mapAuthErrorToSpanish(e));
                    });
        });

        loginTv.setOnClickListener(v -> finish());
    }

    private void showTermsDialog() {
        String content = "Términos y condiciones de uso\n\n"
                + "1. Debes usar la app de forma legal y respetuosa.\n"
                + "2. Eres responsable de los datos que introduces.\n"
                + "3. No debes compartir el acceso de tu cuenta.\n"
                + "4. Los gastos, pagos y recordatorios son responsabilidad de los usuarios.\n"
                + "5. Puedes dejar de usar la app cuando quieras.\n"
                + "6. Al registrarte, aceptas estas condiciones.";

        new AlertDialog.Builder(this)
                .setTitle("Términos y condiciones")
                .setMessage(content)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private boolean isAdult(String birthDateIso) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        fmt.setLenient(false);
        Date birthDate;
        try {
            birthDate = fmt.parse(birthDateIso);
        } catch (ParseException e) {
            NoticeUtils.show(this, "Formato inválido. Usa YYYY-MM-DD");
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
        String todayIso = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        birthDateEt.setText(todayIso);
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

    private void openVerifyEmailScreen(String email, String fullName, String phone, String birthDate, boolean termsAccepted) {
        Intent intent = new Intent(this, VerifyEmailActivity.class);
        intent.putExtra(VerifyEmailActivity.EXTRA_EMAIL, email);
        intent.putExtra(VerifyEmailActivity.EXTRA_FULL_NAME, fullName);
        intent.putExtra(VerifyEmailActivity.EXTRA_PHONE, phone);
        intent.putExtra(VerifyEmailActivity.EXTRA_BIRTH_DATE, birthDate);
        intent.putExtra(VerifyEmailActivity.EXTRA_TERMS_ACCEPTED, termsAccepted);
        startActivity(intent);
        finish();
    }

    private String mapAuthErrorToSpanish(Exception e) {
        if (e instanceof FirebaseAuthException authException) {
            String code = authException.getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) return "El correo electrónico no es válido.";
            if ("ERROR_WEAK_PASSWORD".equals(code)) return "La contraseña es demasiado débil. Usa al menos 6 caracteres.";
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) return "Ese correo ya tiene una cuenta registrada.";
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) return "Error de conexión. Revisa internet e inténtalo otra vez.";
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) return "Demasiados intentos. Espera un momento y vuelve a intentarlo.";
        }
        return "No se pudo completar el registro. Revisa los datos e inténtalo de nuevo.";
    }
}
