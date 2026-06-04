package com.sergio.flatshare.features.auth;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import com.sergio.flatshare.core.settings.ThemeUtils;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.shared.ui.CountryPhoneUtils;
import com.sergio.flatshare.shared.ui.DateInputUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyAppTheme(this);
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
                NoticeUtils.show(this, getString(R.string.auth_email_missing));
                return;
            }
            if (TextUtils.isEmpty(password)) {
                NoticeUtils.show(this, getString(R.string.auth_password_missing));
                return;
            }
            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(localPhone)
                    || TextUtils.isEmpty(birthDate) || TextUtils.isEmpty(password)) {
                NoticeUtils.show(this, getString(R.string.register_complete_all_fields));
                return;
            }

            if (!isAdult(birthDate)) {
                NoticeUtils.show(this, getString(R.string.register_adult_required));
                return;
            }

            if (!termsSwitch.isChecked()) {
                NoticeUtils.show(this, getString(R.string.register_terms_required));
                return;
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        if (authResult.getUser() == null) {
                            NoticeUtils.show(this, getString(R.string.register_create_failed));
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
                                    NoticeUtils.show(this, getString(R.string.register_verification_sent));
                                    openVerifyEmailScreen(email, fullName, phone, birthDate, true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(
                                            this,
                                            getString(R.string.register_verification_send_failed),
                                            Toast.LENGTH_LONG
                                    ).show();
                                    openVerifyEmailScreen(email, fullName, phone, birthDate, true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            NoticeUtils.show(this, getString(R.string.register_email_already_used));
                            return;
                        }
                        NoticeUtils.show(this, mapAuthError(e));
                    });
        });

        loginTv.setOnClickListener(v -> finish());
    }

    private void showTermsDialog() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(8), padding, dp(8));

        TextView introTv = buildTermsIntro();
        content.addView(introTv);

        String[] sections = getString(R.string.register_terms_content).split("\\n\\n");
        for (String rawSection : sections) {
            String section = rawSection == null ? "" : rawSection.trim();
            if (section.isEmpty()) continue;
            if (section.equalsIgnoreCase(getString(R.string.register_terms_title))
                    || section.startsWith(getString(R.string.register_terms_title))) {
                continue;
            }
            int newlineIndex = section.indexOf('\n');
            if (newlineIndex > 0) {
                String heading = section.substring(0, newlineIndex).trim();
                String body = section.substring(newlineIndex + 1).trim();
                content.addView(buildTermsHeading(heading));
                content.addView(buildTermsBody(body));
            } else {
                content.addView(buildTermsBody(section));
            }
        }

        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        new AlertDialog.Builder(this, R.style.ThemeOverlay_FlatShare_Dialog)
                .setTitle(getString(R.string.register_terms_title))
                .setView(scrollView)
                .setPositiveButton(getString(R.string.common_close), null)
                .show();
    }

    private TextView buildTermsIntro() {
        TextView textView = new TextView(this);
        textView.setText("Lee este resumen antes de completar el registro. El objetivo es que el uso de FlatShare sea claro, seguro y ordenado para todas las personas del piso.");
        textView.setTextColor(getColor(R.color.text_light));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setLineSpacing(0f, 1.2f);
        textView.setBackgroundResource(R.drawable.bg_input_dark_round);
        textView.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(14);
        textView.setLayoutParams(params);
        return textView;
    }

    private TextView buildTermsHeading(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.primary_green));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(6);
        textView.setLayoutParams(params);
        return textView;
    }

    private TextView buildTermsBody(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.text_light));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setLineSpacing(0f, 1.25f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(6);
        params.bottomMargin = dp(8);
        textView.setLayoutParams(params);
        return textView;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private boolean isAdult(String birthDateText) {
        Date birthDate = DateInputUtils.parseDayOrNull(birthDateText);
        if (birthDate == null) {
            NoticeUtils.show(this, getString(R.string.register_birth_date_invalid));
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
        birthDateEt.setText(DateInputUtils.formatDay(new Date()));
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
            Date parsed = DateInputUtils.parseDayOrNull(currentValue);
            if (parsed != null) {
                calendar.setTime(parsed);
            }
        } else {
            calendar.add(Calendar.YEAR, -18);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    targetField.setText(DateInputUtils.formatDay(selected.getTime()));
                },
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

    private String mapAuthError(Exception e) {
        if (e instanceof FirebaseAuthException authException) {
            String code = authException.getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) return getString(R.string.auth_error_invalid_email);
            if ("ERROR_WEAK_PASSWORD".equals(code)) return getString(R.string.register_error_weak_password);
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) return getString(R.string.register_email_already_used);
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) return getString(R.string.auth_error_network);
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) return getString(R.string.auth_error_too_many_requests);
        }
        return getString(R.string.register_error_generic);
    }
}
