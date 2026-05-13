package com.sergio.flatshare.features.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sergio.flatshare.R;
import com.sergio.flatshare.features.settings.SettingsFragment;
import com.sergio.flatshare.shared.ui.DialogUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
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
    private String username = "";
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
                    username = safe(doc.getString("username"), "");
                    phone = safe(doc.getString("phone"), "");
                    birthDate = safe(doc.getString("birthDate"), "");
                    photoUri = safe(doc.getString("photoUri"), "");
                    renderProfileInfo();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Error cargando perfil", Toast.LENGTH_SHORT).show();
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
        EditText phoneEt = form.findViewById(R.id.editPhoneEt);
        EditText birthDateEt = form.findViewById(R.id.editBirthDateEt);
        fullNameEt.setText(fullName);
        phoneEt.setText(phone);
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
            String newPhone = phoneEt.getText().toString().trim();
            String newBirthDate = birthDateEt.getText().toString().trim();
            if (!newBirthDate.isEmpty() && !isAdult(newBirthDate)) {
                Toast.makeText(requireContext(), "Debes tener al menos 18 años", Toast.LENGTH_SHORT).show();
                return;
            }

            var user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            Map<String, Object> updates = new HashMap<>();
            updates.put("fullName", newName);
            updates.put("displayName", newName.isEmpty() ? email : newName);
            updates.put("phone", newPhone);
            updates.put("birthDate", newBirthDate);
            updates.put("email", email.toLowerCase(Locale.ROOT));
            updates.put("username", username);
            updates.put("photoUri", photoUri);
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(task -> {
                        if (!newName.isEmpty()) {
                            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(newName).build());
                        }
                        fullName = newName;
                        phone = newPhone;
                        birthDate = newBirthDate;
                        renderProfileInfo();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar", Toast.LENGTH_SHORT).show());
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
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                fmt.setLenient(false);
                calendar.setTime(fmt.parse(currentValue));
            } catch (Exception ignored) {
            }
        }
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> targetField.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private boolean isAdult(String birthDateIso) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        fmt.setLenient(false);
        try {
            Calendar birth = Calendar.getInstance();
            birth.setTime(fmt.parse(birthDateIso));
            Calendar today = Calendar.getInstance();
            int ageYears = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                ageYears--;
            }
            return ageYears >= 18;
        } catch (ParseException e) {
            Toast.makeText(requireContext(), "Formato inválido. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
