package com.sergio.flatshare.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.DialogUtils;
import com.sergio.flatshare.util.SessionStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupsFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<String> display = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private View detailsCard;
    private TextView detailNameTv;
    private TextView detailDescTv;
    private TextView detailMembersTv;
    private String selectedGroupId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        ListView listView = view.findViewById(R.id.groupsLv);
        FloatingActionButton fab = view.findViewById(R.id.addGroupFab);
        detailsCard = view.findViewById(R.id.groupDetailsCard);
        detailNameTv = view.findViewById(R.id.detailGroupNameTv);
        detailDescTv = view.findViewById(R.id.detailGroupDescTv);
        detailMembersTv = view.findViewById(R.id.detailGroupMembersTv);
        Button editBtn = view.findViewById(R.id.editGroupBtn);
        Button inviteBtn = view.findViewById(R.id.inviteGroupBtn);
        Button closeBtn = view.findViewById(R.id.closeDetailsBtn);

        adapter = new ArrayAdapter<>(requireContext(), R.layout.item_group_row, R.id.groupNameTv, display);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            selectedGroupId = ids.get(position);
            SessionStore.setCurrentGroup(requireContext(), selectedGroupId);
            loadGroupDetails(selectedGroupId);
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
            }
        });

        editBtn.setOnClickListener(v -> editSelectedGroup());
        inviteBtn.setOnClickListener(v -> showShareCodeDialog(selectedGroupId));
        closeBtn.setOnClickListener(v -> {
            selectedGroupId = null;
            detailsCard.setVisibility(View.GONE);
        });

        fab.setOnClickListener(v -> showActionDialog());
        loadGroups();
        checkInvitations();
        return view;
    }

    private void showActionDialog() {
        View content = DialogUtils.createVerticalActions(requireContext());
        Button createBtn = DialogUtils.createActionButton(requireContext(), "Crear piso", true);
        Button joinBtn = DialogUtils.createActionButton(requireContext(), "Unirse piso", false);
        ((ViewGroup) content).addView(createBtn);
        ((ViewGroup) content).addView(joinBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Acciones",
                "Elige como quieres entrar en tu proximo piso.",
                content,
                "Cerrar",
                null
        );

        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        createBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createGroupDialog();
        });
        joinBtn.setOnClickListener(v -> {
            dialog.dismiss();
            joinGroupDialog();
        });
    }

    private void createGroupDialog() {
        showSingleInputDialog(
                "Nuevo piso",
                "Crea un espacio compartido con el mismo estilo de la app.",
                "Nombre del piso",
                InputType.TYPE_CLASS_TEXT,
                "Crear",
                value -> {
                    String name = value.trim();
                    if (name.isEmpty()) return false;

                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
                    Map<String, Object> group = new HashMap<>();
                    group.put("name", name);
                    group.put("description", "Piso compartido");
                    group.put("ownerId", uid);
                    group.put("members", java.util.Collections.singletonList(uid));
                    group.put("memberEmails", java.util.Collections.singletonList(email));
                    group.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("groups").add(group).addOnSuccessListener(doc -> {
                        String shareCode = doc.getId().toUpperCase(Locale.ROOT);
                        Map<String, Object> codeData = new HashMap<>();
                        codeData.put("groupId", doc.getId());
                        codeData.put("ownerId", uid);
                        codeData.put("name", name);
                        doc.update("shareCode", shareCode);
                        db.collection("group_codes").document(shareCode).set(codeData);
                        SessionStore.setCurrentGroup(requireContext(), doc.getId());
                        loadGroups();
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
                        }
                    });
                    return true;
                }
        );
    }

    private void joinGroupDialog() {
        showSingleInputDialog(
                "Unirse a un piso",
                "Escribe el codigo para entrar en un piso compartido.",
                "Codigo del piso",
                InputType.TYPE_CLASS_TEXT,
                "Unirse",
                value -> {
                    String code = value.trim().toUpperCase(Locale.ROOT);
                    if (code.isEmpty()) return false;
                    joinGroupByCode(code);
                    return true;
                }
        );
    }

    private void inviteDialog(String groupId) {
        if (groupId == null) {
            Toast.makeText(requireContext(), "Selecciona un grupo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        showSingleInputDialog(
                "Invitar al grupo",
                "Comparte el piso por email con otra persona.",
                "persona@email.com",
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                "Enviar",
                value -> {
                    String invitedEmail = value.trim().toLowerCase(Locale.ROOT);
                    if (invitedEmail.isEmpty()) return false;

                    Map<String, Object> inv = new HashMap<>();
                    inv.put("groupId", groupId);
                    inv.put("invitedEmail", invitedEmail);
                    inv.put("inviterUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    inv.put("status", "pending");
                    inv.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("invitations").add(inv)
                            .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Invitacion enviada", Toast.LENGTH_SHORT).show());
                    return true;
                }
        );
    }

    private void showSingleInputDialog(String title, String subtitle, String hint, int inputType, String actionLabel, SingleInputAction action) {
        View form = buildSingleInputDialog(title, subtitle, hint, inputType, actionLabel);
        EditText inputEt = form.findViewById(R.id.dialogInputEt);
        Button cancelBtn = form.findViewById(R.id.dialogCancelBtn);
        Button confirmBtn = form.findViewById(R.id.dialogConfirmBtn);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_FlatShare_Dialog)
                .setView(form)
                .create();

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        confirmBtn.setOnClickListener(v -> {
            boolean shouldClose = action.onConfirm(inputEt.getText().toString());
            if (shouldClose) {
                dialog.dismiss();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private View buildSingleInputDialog(String title, String subtitle, String hint, int inputType, String actionLabel) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_single_input, null, false);
        TextView titleTv = form.findViewById(R.id.dialogTitleTv);
        TextView subtitleTv = form.findViewById(R.id.dialogSubtitleTv);
        EditText inputEt = form.findViewById(R.id.dialogInputEt);
        Button confirmBtn = form.findViewById(R.id.dialogConfirmBtn);

        titleTv.setText(title);
        subtitleTv.setText(subtitle);
        inputEt.setHint(hint);
        inputEt.setInputType(inputType);
        confirmBtn.setText(actionLabel);
        return form;
    }

    private void joinGroupByCode(String code) {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("group_codes")
                .document(code)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.exists()) {
                        Toast.makeText(requireContext(), "Codigo no valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String groupId = result.getString("groupId");
                    if (groupId == null || groupId.trim().isEmpty()) {
                        Toast.makeText(requireContext(), "Codigo no valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addUserToGroup(groupId, uid, email);
                });
    }

    private void addUserToGroup(String groupId, String uid, String email) {
        db.collection("groups").document(groupId).update(
                "members", FieldValue.arrayUnion(uid),
                "memberEmails", FieldValue.arrayUnion(email)
        ).addOnSuccessListener(v -> {
            selectedGroupId = groupId;
            SessionStore.setCurrentGroup(requireContext(), groupId);
            loadGroups();
            loadGroupDetails(groupId);
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
            }
        });
    }

    private void showShareCodeDialog(String groupId) {
        if (groupId == null) {
            Toast.makeText(requireContext(), "Selecciona un grupo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
            String code = doc.getString("shareCode");
            if (code == null || code.trim().isEmpty()) {
                code = doc.getId().toUpperCase(Locale.ROOT);
            }
            View content = DialogUtils.createMessageView(requireContext(), code);
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Codigo del piso",
                    "Comparte este codigo para invitar a otra persona.",
                    content,
                    null,
                    "Cerrar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
        });
    }

    private void checkInvitations() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || FirebaseAuth.getInstance().getCurrentUser().getEmail() == null) {
            return;
        }
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        db.collection("invitations")
                .whereEqualTo("invitedEmail", myEmail)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(res -> {
                    if (res.isEmpty()) return;
                    DocumentSnapshot doc = res.getDocuments().get(0);
                    String groupId = doc.getString("groupId");
                    showInvitationDecision(doc.getId(), groupId);
                });
    }

    private void showInvitationDecision(String invitationId, String groupId) {
        View content = DialogUtils.createMessageView(requireContext(), "Tienes una invitacion a un piso compartido.");
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Invitacion pendiente",
                "Decide si quieres entrar ahora o rechazarla.",
                content,
                "Rechazar",
                "Aceptar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> {
            db.collection("invitations").document(invitationId).update("status", "rejected");
            dialog.dismiss();
        });
        shell.confirmBtn.setOnClickListener(v -> {
            acceptInvitation(invitationId, groupId);
            dialog.dismiss();
        });
    }

    private void acceptInvitation(String invitationId, String groupId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        db.collection("groups").document(groupId).update(
                "members", FieldValue.arrayUnion(uid),
                "memberEmails", FieldValue.arrayUnion(email)
        ).addOnSuccessListener(v -> {
            db.collection("invitations").document(invitationId).update("status", "accepted");
            SessionStore.setCurrentGroup(requireContext(), groupId);
            loadGroups();
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
            }
        });
    }

    private void loadGroups() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("groups").whereArrayContains("members", uid).get().addOnSuccessListener(res -> {
            display.clear();
            ids.clear();
            res.forEach(doc -> {
                String name = doc.getString("name");
                Object membersField = doc.get("members");
                List<?> members = membersField instanceof List ? (List<?>) membersField : null;
                int count = members == null ? 0 : members.size();
                display.add(name + " (" + count + " miembros)");
                ids.add(doc.getId());
            });
            adapter.notifyDataSetChanged();
        });
    }

    private void loadGroupDetails(String groupId) {
        if (groupId == null) return;
        db.collection("groups").document(groupId).get().addOnSuccessListener(this::renderGroupDetails);
    }

    private void renderGroupDetails(DocumentSnapshot doc) {
        if (!isAdded() || doc == null || !doc.exists()) return;
        String name = doc.getString("name");
        String desc = doc.getString("description");
        if (desc == null || desc.trim().isEmpty()) desc = "Sin descripcion";

        Object emailsField = doc.get("memberEmails");
        List<String> emails = emailsField instanceof List ? (List<String>) emailsField : new ArrayList<>();

        detailNameTv.setText(name == null ? "Grupo" : name);
        String shareCode = doc.getString("shareCode");
        detailDescTv.setText(shareCode == null || shareCode.trim().isEmpty() ? desc : desc + "\nCodigo: " + shareCode);
        if (emails.isEmpty()) {
            detailMembersTv.setText("Miembros: sin datos");
        } else {
            StringBuilder membersText = new StringBuilder("Miembros:");
            for (String email : emails) {
                membersText.append("\n- ").append(email);
            }
            detailMembersTv.setText(membersText.toString());
        }
        detailsCard.setVisibility(View.VISIBLE);
    }

    private void editSelectedGroup() {
        if (selectedGroupId == null) {
            Toast.makeText(requireContext(), "Selecciona un grupo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_group, null, false);
        EditText nameEt = form.findViewById(R.id.editGroupNameEt);
        EditText descEt = form.findViewById(R.id.editGroupDescEt);
        nameEt.setText(detailNameTv.getText());
        descEt.setText(detailDescTv.getText());

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Editar grupo",
                "Actualiza el nombre y la descripcion del piso.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String newName = nameEt.getText().toString().trim();
            String newDesc = descEt.getText().toString().trim();
            if (newName.isEmpty()) return;
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("description", newDesc.isEmpty() ? "Sin descripcion" : newDesc);
            db.collection("groups").document(selectedGroupId).update(updates).addOnSuccessListener(task -> {
                loadGroups();
                loadGroupDetails(selectedGroupId);
                dialog.dismiss();
            });
        });
    }

    private interface SingleInputAction {
        boolean onConfirm(String value);
    }
}
