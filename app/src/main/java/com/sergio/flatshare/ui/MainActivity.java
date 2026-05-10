package com.sergio.flatshare.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.SettingsStore;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean darkModeEnabled = SettingsStore.isDarkModeEnabled(this);
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(SettingsStore.getLanguage(this))
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.menu_groups) f = new GroupsFragment();
            else if (id == R.id.menu_personal_balance) f = new PersonalBalanceFragment();
            else if (id == R.id.menu_calendar) f = new CalendarFragment();
            else f = new ProfileFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.menu_groups);
        }
        handleOpenWorkspaceIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenWorkspaceIntent(intent);
    }

    private void handleOpenWorkspaceIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra(OwnerRoomsActivity.EXTRA_OPEN_WORKSPACE, false)) {
            openCurrentGroupWorkspace();
            intent.removeExtra(OwnerRoomsActivity.EXTRA_OPEN_WORKSPACE);
        }
    }

    public void openCurrentGroupWorkspace() {
        if (nav != null) nav.setSelectedItemId(R.id.menu_groups);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new ExpensesFragment())
                .addToBackStack("workspace")
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(getString(R.string.logout));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
        return true;
    }
}


