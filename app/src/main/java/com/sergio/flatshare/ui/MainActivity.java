package com.sergio.flatshare.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.sergio.flatshare.R;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.menu_groups) f = new GroupsFragment();
            else if (id == R.id.menu_expenses) f = new ExpensesFragment();
            else f = new ProfileFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.menu_groups);
        }
    }

    public void openCurrentGroupWorkspace() {
        if (nav != null) {
            nav.setSelectedItemId(R.id.menu_expenses);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add("Cerrar sesión");
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

