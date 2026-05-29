package com.sergio.flatshare.features.shell;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sergio.flatshare.core.settings.SettingsStore;
import com.sergio.flatshare.features.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(SettingsStore.getLanguage(this))
        );
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        requestNotificationPermission();
        routeBySession();
    }

    private void routeBySession() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            openLogin();
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean verified = refreshedUser != null && refreshedUser.isEmailVerified();
            if (verified) {
                refreshedUser.getIdToken(true).addOnCompleteListener(tokenTask -> {
                    if (tokenTask.isSuccessful()) {
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        openLogin();
                    }
                    finish();
                });
            } else {
                FirebaseAuth.getInstance().signOut();
                openLogin();
                finish();
            }
        });
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("flatshare_reminders", "Recordatorios FlatShare", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }
}
