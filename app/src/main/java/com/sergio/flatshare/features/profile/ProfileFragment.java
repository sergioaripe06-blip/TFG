package com.sergio.flatshare.features.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.sergio.flatshare.R;
import com.sergio.flatshare.features.settings.SettingsFragment;
import com.sergio.flatshare.shared.ui.CountryPhoneUtils;
import com.sergio.flatshare.shared.ui.DateInputUtils;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private ShapeableImageView profilePhotoIv;
    private TextView profileInitialsTv;
    private TextView profileFullNameTv;
    private TextView profilePhoneTv;
    private TextView profileEmailTv;
    private TextView profileBirthDateTv;

    private String fullName = "";
    private String email = "";
    private String phone = "";
    private String birthDate = "";
    private String photoUri = "";

    private final ActivityResultLauncher<String[]> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPhotoPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        profilePhotoIv = view.findViewById(R.id.profilePhotoIv);
        profileInitialsTv = view.findViewById(R.id.profileInitialsTv);
        profileFullNameTv = view.findViewById(R.id.profileFullNameTv);
        profilePhoneTv = view.findViewById(R.id.profilePhoneTv);
        profileEmailTv = view.findViewById(R.id.profileEmailTv);
        profileBirthDateTv = view.findViewById(R.id.profileBirthDateTv);
        ImageButton changePhotoBtn = view.findViewById(R.id.changePhotoBtn);
        ImageButton openSettingsBtn = view.findViewById(R.id.openSettingsBtn);
        Button editProfileBtn = view.findViewById(R.id.editProfileBtn);

        changePhotoBtn.setOnClickListener(v -> pickPhotoLauncher.launch(new String[]{"image/*"}));
        openSettingsBtn.setOnClickListener(v -> {
            FragmentActivity activity = requireActivity();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .addToBackStack("settings_from_profile")
                    .commit();
        });
        editProfileBtn.setOnClickListener(v -> showEditProfileDialog());
        loadProfileData();
        return view;
    }

    private void onPhotoPicked(@Nullable Uri uri) {
        if (uri == null || !isAdded()) return;
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }
        photoUri = uri.toString();
        profilePhotoIv.setImageURI(uri);
        profileInitialsTv.setVisibility(View.GONE);
        saveProfilePhotoUri();
    }

    private void saveProfilePhotoUri() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("photoUri", photoUri);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(updates, SetOptions.merge());
        user.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(photoUri)).build());
    }

    private void loadProfileData() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    email = safe(doc.getString("email"), user.getEmail());
                    fullName = safe(doc.getString("fullName"), "");
                    phone = safe(doc.getString("phone"), "");
                    birthDate = safe(doc.getString("birthDate"), "");
                    photoUri = safe(doc.getString("photoUri"), "");
                    renderProfileInfo();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    NoticeUtils.show(requireContext(), "Error cargando perfil");
                });
    }

    private String safe(@Nullable String value, @Nullable String fallback) {
        return value == null ? (fallback == null ? "" : fallback) : value;
    }

    private void renderProfileInfo() {
        profileFullNameTv.setText(fullName.isEmpty() ? "-" : fullName);
        profilePhoneTv.setText(phone.isEmpty() ? "-" : phone);
        profileEmailTv.setText(email.isEmpty() ? "-" : email);
        profileBirthDateTv.setText(birthDate.isEmpty() ? "-" : birthDate);
        if (!photoUri.isEmpty()) {
            try {
                profilePhotoIv.setImageURI(Uri.parse(photoUri));
                profileInitialsTv.setVisibility(View.GONE);
            } catch (Exception ignored) {
                showInitialsPlaceholder();
            }
        } else {
            showInitialsPlaceholder();
        }
    }

    private void showInitialsPlaceholder() {
        profilePhotoIv.setImageResource(android.R.color.transparent);
        profileInitialsTv.setText(buildInitials(fullName, email));
        profileInitialsTv.setVisibility(View.VISIBLE);
    }

    private String buildInitials(String name, String mail) {
        String source = name == null || name.trim().isEmpty() ? mail : name;
        if (source == null || source.trim().isEmpty()) return "U";
        String[] parts = source.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        String first = parts[0].substring(0, 1);
        String second = parts[1].substring(0, 1);
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private void showEditProfileDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);
        EditText fullNameEt = form.findViewById(R.id.editFullNameEt);
        Spinner phoneCountrySpinner = form.findViewById(R.id.editPhoneCountrySpinner);
        EditText phoneEt = form.findViewById(R.id.editPhoneEt);
        EditText birthDateEt = form.findViewById(R.id.editBirthDateEt);

        List<CountryPhoneUtils.CountryOption> countries = CountryPhoneUtils.loadCountries();
        phoneCountrySpinner.setAdapter(CountryPhoneUtils.buildAdapter(requireContext(), countries));

        CountryPhoneUtils.ParsedPhone parsedPhone = CountryPhoneUtils.splitStoredPhone(phone, countries);
        if (parsedPhone.selectedIndex >= 0 && parsedPhone.selectedIndex < countries.size()) {
            phoneCountrySpinner.setSelection(parsedPhone.selectedIndex);
        } else {
            CountryPhoneUtils.selectRegion(phoneCountrySpinner, countries, "ES");
        }

        fullNameEt.setText(fullName);
        phoneEt.setText(parsedPhone.localNumber);
        birthDateEt.setText(birthDate);
        setupBirthDateField(birthDateEt);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Editar perfil",
                "Actualiza tus datos",
                form,
                "Cancelar",
                "Guardar"
        );
        android.app.AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String newName = fullNameEt.getText().toString().trim();
            String localPhone = phoneEt.getText().toString().trim();
            String newBirthDate = birthDateEt.getText().toString().trim();
            if (newName.isEmpty() || localPhone.isEmpty() || newBirthDate.isEmpty()) {
                NoticeUtils.show(requireContext(), "Completa todos los campos del perfil");
                return;
            }
            if (!isAdult(newBirthDate)) {
                NoticeUtils.show(requireContext(), "Debes tener al menos 18 a\u00f1os");
                return;
            }

            CountryPhoneUtils.CountryOption selectedCountry = CountryPhoneUtils.selected(phoneCountrySpinner, countries);
            String newPhone = CountryPhoneUtils.buildFullPhone(selectedCountry, localPhone);

            var user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            Map<String, Object> updates = new HashMap<>();
            updates.put("fullName", newName);
            updates.put("displayName", newName.isEmpty() ? email : newName);
            updates.put("phone", newPhone);
            updates.put("birthDate", newBirthDate);
            updates.put("email", email.toLowerCase(Locale.ROOT));
            updates.put("photoUri", photoUri);
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(task -> {
                        if (!newName.isEmpty()) {
                            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(newName).build());
                        }
                        propagateProfileNameAcrossCollections(user.getUid(), email.toLowerCase(Locale.ROOT), newName);
                        fullName = newName;
                        phone = newPhone;
                        birthDate = newBirthDate;
                        renderProfileInfo();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar", Toast.LENGTH_SHORT).show());
        });
    }

    private void propagateProfileNameAcrossCollections(@NonNull String uid, @NonNull String normalizedEmail, @NonNull String newName) {
        String safeName = newName.trim();
        if (safeName.isEmpty()) return;

        Map<String, List<String>> byUid = new LinkedHashMap<>();
        byUid.put("groups", Collections.singletonList("ownerId"));
        byUid.put("rooms_groups", Arrays.asList("createdByUid", "updatedByUid"));
        byUid.put("expenses", Collections.singletonList("payerId"));
        byUid.put("reminders", Collections.singletonList("ownerUid"));
        byUid.put("activity_logs", Collections.singletonList("actorUid"));
        byUid.put("rental_contracts", Arrays.asList("createdByUid", "updatedByUid"));
        byUid.put("rent_collections", Arrays.asList("createdByUid", "updatedByUid"));
        byUid.put("maintenance_tickets", Arrays.asList("createdByUid", "updatedByUid"));
        byUid.put("group_documents", Arrays.asList("createdByUid", "updatedByUid"));
        byUid.put("audit_events", Collections.singletonList("actorUid"));
        byUid.put("rent_automations", Arrays.asList("createdByUid", "updatedByUid"));
        byUid.put("event_reminder_rules", Arrays.asList("createdByUid", "updatedByUid"));

        Map<String, List<String>> byEmail = new LinkedHashMap<>();
        byEmail.put("payments", Arrays.asList("fromEmail", "toEmail"));
        byEmail.put("payment_deadlines", Arrays.asList("debtorEmail", "creditorEmail"));
        byEmail.put("reminders", Collections.singletonList("ownerEmail"));
        byEmail.put("rent_collections", Collections.singletonList("tenantEmail"));
        byEmail.put("maintenance_tickets", Collections.singletonList("responsibleEmail"));
        byEmail.put("rent_automations", Collections.singletonList("tenantEmail"));
        byEmail.put("event_reminder_jobs", Collections.singletonList("targetEmail"));
        byEmail.put("activity_logs", Collections.singletonList("actorEmail"));
        byEmail.put("audit_events", Collections.singletonList("actorEmail"));

        String[] nameFields = new String[]{
                "name", "displayName", "fullName",
                "ownerName", "creatorName", "updatedByName", "createdByName",
                "actorName", "payerName", "fromName", "toName",
                "debtorName", "creditorName", "tenantName", "responsibleName", "targetName"
        };

        List<Task<QuerySnapshot>> readTasks = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byUid.entrySet()) {
            for (String field : entry.getValue()) {
                readTasks.add(FirebaseFirestore.getInstance().collection(entry.getKey()).whereEqualTo(field, uid).get());
            }
        }
        for (Map.Entry<String, List<String>> entry : byEmail.entrySet()) {
            for (String field : entry.getValue()) {
                readTasks.add(FirebaseFirestore.getInstance().collection(entry.getKey()).whereEqualTo(field, normalizedEmail).get());
            }
        }

        Tasks.whenAllSuccess(readTasks).addOnSuccessListener(results -> {
            List<Task<Void>> writeTasks = new ArrayList<>();
            for (Object result : results) {
                if (!(result instanceof QuerySnapshot querySnapshot)) continue;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Map<String, Object> updates = new HashMap<>();
                    for (String candidate : nameFields) {
                        if (doc.contains(candidate)) {
                            updates.put(candidate, safeName);
                        }
                    }
                    if (!updates.isEmpty()) {
                        writeTasks.add(doc.getReference().update(updates));
                    }
                }
            }
            Tasks.whenAllComplete(writeTasks);
        });
    }

    private void setupBirthDateField(EditText dateField) {
        dateField.setFocusable(false);
        dateField.setClickable(true);
        dateField.setOnClickListener(v -> openBirthDatePicker(dateField));
    }

    private void openBirthDatePicker(EditText targetField) {
        Calendar calendar = Calendar.getInstance();
        String currentValue = targetField.getText() == null ? "" : targetField.getText().toString().trim();
        if (!currentValue.isEmpty()) {
            Date parsed = DateInputUtils.parseDayOrNull(currentValue);
            if (parsed != null) {
                calendar.setTime(parsed);
            }
        }
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
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
        dialog.show();
    }

    private boolean isAdult(String birthDateText) {
        Date birthDateValue = DateInputUtils.parseDayOrNull(birthDateText);
        if (birthDateValue == null) {
            NoticeUtils.show(requireContext(), "Formato inv\u00e1lido. Usa DD/MM/AAAA");
            return false;
        }
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthDateValue);
        Calendar today = Calendar.getInstance();
        int ageYears = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            ageYears--;
        }
        return ageYears >= 18;
    }
}

