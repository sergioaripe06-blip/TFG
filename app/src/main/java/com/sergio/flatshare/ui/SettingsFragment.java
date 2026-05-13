package com.sergio.flatshare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.SettingsStore;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        SwitchMaterial themeSwitch = view.findViewById(R.id.themeSwitch);
        SwitchMaterial notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        Spinner languageSpinner = view.findViewById(R.id.languageSpinner);
        Button settingsLogoutBtn = view.findViewById(R.id.settingsLogoutBtn);

        boolean darkModeEnabled = SettingsStore.isDarkModeEnabled(requireContext());
        themeSwitch.setChecked(darkModeEnabled);
        themeSwitch.setText(darkModeEnabled ? R.string.theme_dark : R.string.theme_light);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsStore.setDarkModeEnabled(requireContext(), isChecked);
            themeSwitch.setText(isChecked ? R.string.theme_dark : R.string.theme_light);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        notificationsSwitch.setChecked(SettingsStore.areNotificationsEnabled(requireContext()));
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                SettingsStore.setNotificationsEnabled(requireContext(), isChecked)
        );

        String[] languageOptions = new String[]{getString(R.string.language_es), getString(R.string.language_en)};
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                languageOptions
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(requireContext().getColor(R.color.text_light));
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setTextColor(requireContext().getColor(R.color.text_light));
                return v;
            }
        };
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        boolean spanishSelected = "es".equals(SettingsStore.getLanguage(requireContext()));
        languageSpinner.setSelection(spanishSelected ? 0 : 1);
        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String languageCode = position == 0 ? "es" : "en";
                if (!languageCode.equals(SettingsStore.getLanguage(requireContext()))) {
                    SettingsStore.setLanguage(requireContext(), languageCode);
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        settingsLogoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }
}
