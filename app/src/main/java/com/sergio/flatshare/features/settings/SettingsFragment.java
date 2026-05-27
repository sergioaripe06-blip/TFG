package com.sergio.flatshare.features.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.core.settings.SettingsStore;
import com.sergio.flatshare.features.auth.LoginActivity;
import com.sergio.flatshare.shared.ui.DialogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private Button deleteAccountBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        SwitchMaterial themeSwitch = view.findViewById(R.id.themeSwitch);
        SwitchMaterial notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        Spinner languageSpinner = view.findViewById(R.id.languageSpinner);
        deleteAccountBtn = view.findViewById(R.id.deleteAccountBtn);
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
                R.layout.item_spinner_selected,
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
        languageAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
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

        deleteAccountBtn.setOnClickListener(v -> showPasswordConfirmationDialog());

        settingsLogoutBtn.setOnClickListener(v -> {
            SessionStore.setRememberMeEnabled(requireContext(), false);
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void showPasswordConfirmationDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), R.string.settings_delete_account_need_login, Toast.LENGTH_LONG).show();
            return;
        }

        int pad = (int) (14 * requireContext().getResources().getDisplayMetrics().density);

        LinearLayout formLayout = new LinearLayout(requireContext());
        formLayout.setOrientation(LinearLayout.VERTICAL);
        formLayout.setPadding(0, 0, 0, 0);

        final android.widget.EditText emailEt = new android.widget.EditText(requireContext());
        emailEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEt.setHint(R.string.settings_delete_account_email_hint);
        emailEt.setBackgroundResource(R.drawable.bg_input_dark_round);
        emailEt.setTextColor(requireContext().getColor(R.color.text_light));
        emailEt.setHintTextColor(requireContext().getColor(R.color.text_muted));
        emailEt.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams emailLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        formLayout.addView(emailEt, emailLp);

        final android.widget.EditText passwordEt = new android.widget.EditText(requireContext());
        passwordEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEt.setHint(R.string.settings_delete_account_password_hint);
        passwordEt.setBackgroundResource(R.drawable.bg_input_dark_round);
        passwordEt.setTextColor(requireContext().getColor(R.color.text_light));
        passwordEt.setHintTextColor(requireContext().getColor(R.color.text_muted));
        passwordEt.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams passwordLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        passwordLp.topMargin = (int) (10 * requireContext().getResources().getDisplayMetrics().density);
        formLayout.addView(passwordEt, passwordLp);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                getString(R.string.settings_delete_account_password_title),
                getString(R.string.settings_delete_account_confirm_body),
                formLayout,
                "Cancelar",
                getString(R.string.settings_delete_account_password_cta)
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String enteredEmail = emailEt.getText() == null ? "" : emailEt.getText().toString().trim().toLowerCase(Locale.ROOT);
            String password = passwordEt.getText() == null ? "" : passwordEt.getText().toString().trim();
            String currentEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase(Locale.ROOT);

            if (enteredEmail.isEmpty()) {
                Toast.makeText(requireContext(), R.string.settings_delete_account_email_empty, Toast.LENGTH_LONG).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), R.string.settings_delete_account_password_empty, Toast.LENGTH_LONG).show();
                return;
            }
            if (!enteredEmail.equals(currentEmail)) {
                Toast.makeText(requireContext(), R.string.settings_delete_account_credentials_invalid, Toast.LENGTH_LONG).show();
                return;
            }
            dialog.dismiss();
            reauthenticateThenConfirm(user, enteredEmail, password);
        });
    }

    private void reauthenticateThenConfirm(FirebaseUser user, String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.settings_delete_account_need_login, Toast.LENGTH_LONG).show();
            return;
        }
        user.reauthenticate(EmailAuthProvider.getCredential(email.trim(), password))
                .addOnSuccessListener(unused -> showDeleteAccountConfirmation())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), R.string.settings_delete_account_credentials_invalid, Toast.LENGTH_LONG).show());
    }

    private void showDeleteAccountConfirmation() {
        TextView message = DialogUtils.createMessageView(requireContext(), getString(R.string.settings_delete_account_confirm_body));
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                getString(R.string.settings_delete_account_confirm_title),
                null,
                message,
                "Cancelar",
                getString(R.string.settings_delete_account_confirm_action)
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            deleteCurrentAccount();
        });
    }

    private void deleteCurrentAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), R.string.settings_delete_account_need_login, Toast.LENGTH_LONG).show();
            return;
        }

        deleteAccountBtn.setEnabled(false);
        Toast.makeText(requireContext(), R.string.settings_delete_account_progress, Toast.LENGTH_SHORT).show();

        String uid = user.getUid();
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("groups")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(groupsResult -> processGroupCleanupAndDelete(db, user, uid, email, groupsResult))
                .addOnFailureListener(e -> {
                    deleteAccountBtn.setEnabled(true);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void processGroupCleanupAndDelete(
            FirebaseFirestore db,
            FirebaseUser user,
            String uid,
            String email,
            QuerySnapshot groupsResult
    ) {
        List<Task<?>> tasks = new ArrayList<>();

        for (DocumentSnapshot groupDoc : groupsResult.getDocuments()) {
            String groupId = groupDoc.getId();
            String ownerId = safe(groupDoc.getString("ownerId"));
            List<String> members = castStrings(groupDoc.get("members"));
            List<String> memberEmails = castStrings(groupDoc.get("memberEmails"));

            boolean isOwner = uid.equals(ownerId);
            List<String> remainingMembers = new ArrayList<>();
            for (String member : members) {
                if (!uid.equals(member)) remainingMembers.add(member);
            }

            if (isOwner && remainingMembers.isEmpty()) {
                deleteAccountBtn.setEnabled(true);
                Toast.makeText(requireContext(), R.string.settings_delete_account_owner_blocked, Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("members", FieldValue.arrayRemove(uid));
            updates.put("memberEmails", FieldValue.arrayRemove(email));
            updates.put("roles." + uid, FieldValue.delete());

            if (isOwner && !remainingMembers.isEmpty()) {
                String newOwnerUid = remainingMembers.get(0);
                updates.put("ownerId", newOwnerUid);
                updates.put("roles." + newOwnerUid, "admin");
            }

            tasks.add(db.collection("groups").document(groupId).update(updates));

            if (isOwner) {
                tasks.add(
                        db.collection("rooms_groups")
                                .whereEqualTo("groupId", groupId)
                                .get()
                                .continueWithTask(task -> {
                                    if (!task.isSuccessful() || task.getResult() == null) {
                                        Exception ex = task.getException();
                                        if (ex != null) throw ex;
                                        return Tasks.forResult(null);
                                    }
                                    List<Task<?>> roomTasks = new ArrayList<>();
                                    for (DocumentSnapshot roomDoc : task.getResult().getDocuments()) {
                                        Map<String, Object> roomUpdates = new HashMap<>();
                                        roomUpdates.put("memberEmails", FieldValue.arrayRemove(email));
                                        roomUpdates.put("updatedByUid", uid);
                                        roomTasks.add(roomDoc.getReference().update(roomUpdates));
                                    }
                                    return Tasks.whenAllComplete(roomTasks);
                                })
                );
            }
        }

        tasks.add(deleteInvitations(db, uid, email));
        tasks.add(db.collection("users").document(uid).delete());

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(results -> {
                    for (Task<?> result : results) {
                        if (!result.isSuccessful()) {
                            Exception ex = result.getException();
                            deleteAccountBtn.setEnabled(true);
                            Toast.makeText(requireContext(), ex == null ? "No se pudo completar el borrado" : ex.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    deleteAuthUserAndLogout(user);
                })
                .addOnFailureListener(e -> {
                    deleteAccountBtn.setEnabled(true);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Task<?> deleteInvitations(FirebaseFirestore db, String uid, String email) {
        Task<QuerySnapshot> invitedTask = db.collection("invitations")
                .whereEqualTo("invitedEmail", email)
                .get();

        Task<QuerySnapshot> inviterTask = db.collection("invitations")
                .whereEqualTo("inviterUid", uid)
                .get();

        return Tasks.whenAllSuccess(invitedTask, inviterTask)
                .continueWithTask(task -> {
                    List<Task<?>> deleteTasks = new ArrayList<>();
                    if (invitedTask.isSuccessful() && invitedTask.getResult() != null) {
                        for (DocumentSnapshot doc : invitedTask.getResult().getDocuments()) {
                            deleteTasks.add(doc.getReference().delete());
                        }
                    }
                    if (inviterTask.isSuccessful() && inviterTask.getResult() != null) {
                        for (DocumentSnapshot doc : inviterTask.getResult().getDocuments()) {
                            deleteTasks.add(doc.getReference().delete());
                        }
                    }
                    return Tasks.whenAllComplete(deleteTasks);
                });
    }

    private void deleteAuthUserAndLogout(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(unused -> {
                    SessionStore.setRememberMeEnabled(requireContext(), false);
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    deleteAccountBtn.setEnabled(true);
                    String message = e.getMessage();
                    if (!TextUtils.isEmpty(message) && message.toLowerCase(Locale.ROOT).contains("recent")) {
                        Toast.makeText(requireContext(), R.string.settings_delete_account_need_login, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private List<String> castStrings(Object raw) {
        List<String> out = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return out;
        for (Object v : list) {
            if (v != null) {
                String text = String.valueOf(v).trim();
                if (!text.isEmpty()) out.add(text);
            }
        }
        return out;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

}

