package com.sergio.flatshare.ui;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.DialogUtils;
import com.sergio.flatshare.util.SessionStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Hashtable;

public class GroupsFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<GroupItem> groups = new ArrayList<>();
    private GroupsAdapter adapter;
    private View detailsCard;
    private TextView emptyGroupsTv;
    private TextView detailNameTv;
    private TextView detailDescTv;
    private TextView detailMembersTv;
    private TextView totalGroupsTv;
    private TextView totalMembersTv;
    private final SparseBooleanArray animatedPositions = new SparseBooleanArray();
    private String selectedGroupId;
    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (!isAdded()) return;
                if (result == null || result.getContents() == null) return;
                String raw = result.getContents().trim();
                String code = extractGroupCode(raw);
                if (code.isEmpty()) {
                    Toast.makeText(requireContext(), "QR no valido para unirse a piso", Toast.LENGTH_SHORT).show();
                    return;
                }
                joinGroupByCode(code);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        ListView listView = view.findViewById(R.id.groupsLv);
        Button addGroupBtn = view.findViewById(R.id.addGroupBtn);
        Button joinGroupBtn = view.findViewById(R.id.joinGroupBtn);
        detailsCard = view.findViewById(R.id.groupDetailsCard);
        emptyGroupsTv = view.findViewById(R.id.emptyGroupsTv);
        totalGroupsTv = view.findViewById(R.id.totalGroupsTv);
        totalMembersTv = view.findViewById(R.id.totalMembersTv);
        detailNameTv = view.findViewById(R.id.detailGroupNameTv);
        detailDescTv = view.findViewById(R.id.detailGroupDescTv);
        detailMembersTv = view.findViewById(R.id.detailGroupMembersTv);
        Button editBtn = view.findViewById(R.id.editGroupBtn);
        Button inviteBtn = view.findViewById(R.id.inviteGroupBtn);
        Button closeBtn = view.findViewById(R.id.closeDetailsBtn);

        adapter = new GroupsAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            selectedGroupId = groups.get(position).id;
            SessionStore.setCurrentGroup(requireContext(), selectedGroupId);
            loadGroupDetails(selectedGroupId);
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
            }
        });

        editBtn.setOnClickListener(v -> editSelectedGroup());
        inviteBtn.setOnClickListener(v -> showInviteOptionsDialog(selectedGroupId));
        closeBtn.setOnClickListener(v -> {
            selectedGroupId = null;
            detailsCard.setVisibility(View.GONE);
        });

        addGroupBtn.setOnClickListener(v -> createGroupDialog());
        joinGroupBtn.setOnClickListener(v -> joinGroupDialog());
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
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group, null, false);
        EditText groupNameEt = form.findViewById(R.id.groupNameEt);
        EditText streetEt = form.findViewById(R.id.streetEt);
        EditText portalEt = form.findViewById(R.id.portalEt);
        EditText postalCodeEt = form.findViewById(R.id.postalCodeEt);
        EditText cityEt = form.findViewById(R.id.cityEt);
        EditText provinceEt = form.findViewById(R.id.provinceEt);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nuevo piso",
                "Completa la ubicación para crear el piso.",
                form,
                "Cancelar",
                "Crear"
        );

        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String name = groupNameEt.getText().toString().trim();
            String street = streetEt.getText().toString().trim();
            String portal = portalEt.getText().toString().trim();
            String postalCode = postalCodeEt.getText().toString().trim();
            String city = cityEt.getText().toString().trim();
            String province = provinceEt.getText().toString().trim();

            if (name.isEmpty() || street.isEmpty() || portal.isEmpty()
                    || postalCode.isEmpty() || city.isEmpty() || province.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los datos de ubicación", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
            Map<String, Object> location = new HashMap<>();
            location.put("street", street);
            location.put("portal", portal);
            location.put("postalCode", postalCode);
            location.put("city", city);
            location.put("province", province);

            Map<String, Object> group = new HashMap<>();
            group.put("name", name);
            group.put("description", street + ", " + portal + " - " + postalCode + " " + city + " (" + province + ")");
            group.put("location", location);
            group.put("ownerId", uid);
            Map<String, Object> roles = new HashMap<>();
            roles.put(uid, "admin");
            group.put("roles", roles);
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
                loadGroups();
                dialog.dismiss();
                Toast.makeText(requireContext(), "Piso creado. Tocalo en la lista para entrar.", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e ->
                    Toast.makeText(requireContext(), "Error creando piso: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });
    }

    private void joinGroupDialog() {
        View content = DialogUtils.createVerticalActions(requireContext());
        Button codeBtn = DialogUtils.createActionButton(requireContext(), "Escribir codigo", true);
        Button qrBtn = DialogUtils.createActionButton(requireContext(), "Escanear QR", false);
        ((LinearLayout) content).addView(codeBtn);
        ((LinearLayout) content).addView(qrBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Unirse a un piso",
                "Puedes escribir el codigo o escanear un QR.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        codeBtn.setOnClickListener(v -> {
            dialog.dismiss();
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
        });
        qrBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openQrScanner();
        });
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
                "memberEmails", FieldValue.arrayUnion(email),
                "roles." + uid, "member"
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

    private void showInviteOptionsDialog(String groupId) {
        if (groupId == null) {
            Toast.makeText(requireContext(), "Selecciona un grupo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        View content = DialogUtils.createVerticalActions(requireContext());
        Button codeBtn = DialogUtils.createActionButton(requireContext(), "Invitar por código", true);
        Button qrBtn = DialogUtils.createActionButton(requireContext(), "Invitar por QR", false);
        ((LinearLayout) content).addView(codeBtn);
        ((LinearLayout) content).addView(qrBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Invitar al piso",
                "Elige cómo quieres compartir el acceso.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        codeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showShareCodeOnlyDialog(groupId);
        });
        qrBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showShareQrOnlyDialog(groupId);
        });
    }

    private void showShareCodeOnlyDialog(String groupId) {
        if (groupId == null) return;
        db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
            String code = doc.getString("shareCode");
            if (code == null || code.trim().isEmpty()) {
                code = doc.getId().toUpperCase(Locale.ROOT);
            }
            View content = DialogUtils.createMessageView(requireContext(), code);
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Código del piso",
                    "Comparte este código para unirse al piso.",
                    content,
                    null,
                    "Cerrar"
            );
            AlertDialog codeDialog = DialogUtils.show(requireContext(), shell.root);
            shell.confirmBtn.setOnClickListener(v -> codeDialog.dismiss());
        });
    }

    private void showShareQrOnlyDialog(String groupId) {
        if (groupId == null) return;
        db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
            String code = doc.getString("shareCode");
            if (code == null || code.trim().isEmpty()) {
                code = doc.getId().toUpperCase(Locale.ROOT);
            }
            String payload = "flatshare://join?code=" + code;
            View content = buildQrContent(payload);
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "QR del piso",
                    "Comparte este QR para invitar a otra persona.",
                    content,
                    null,
                    "Cerrar"
            );
            AlertDialog qrDialog = DialogUtils.show(requireContext(), shell.root);
            shell.confirmBtn.setOnClickListener(v -> qrDialog.dismiss());
        });
    }

    private void openQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Escanea el QR del piso");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        qrScannerLauncher.launch(options);
    }

    private String extractGroupCode(String raw) {
        if (raw == null) return "";
        String normalized = raw.trim();
        String key = "code=";
        int idx = normalized.toLowerCase(Locale.ROOT).indexOf(key);
        if (idx >= 0) {
            String value = normalized.substring(idx + key.length()).trim();
            int amp = value.indexOf('&');
            if (amp >= 0) value = value.substring(0, amp);
            return value.toUpperCase(Locale.ROOT);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private View buildQrContent(String payload) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 8, 0, 0);

        ImageView qrImage = new ImageView(requireContext());
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220)
        );
        qrImage.setLayoutParams(imgParams);
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrImage.setImageBitmap(generateQrBitmap(payload, 900));
        layout.addView(qrImage);
        return layout;
    }

    private Bitmap generateQrBitmap(String text, int size) {
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            Bitmap bitmap = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.RGB_565);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Bitmap fallback = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            fallback.eraseColor(Color.WHITE);
            return fallback;
        }
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density);
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
                "memberEmails", FieldValue.arrayUnion(email),
                "roles." + uid, "member"
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
            groups.clear();
            animatedPositions.clear();
            int membersTotal = 0;
            for (DocumentSnapshot doc : res.getDocuments()) {
                String name = doc.getString("name");
                String description = doc.getString("description");
                Object membersField = doc.get("members");
                List<?> members = membersField instanceof List ? (List<?>) membersField : null;
                int count = members == null ? 0 : members.size();
                membersTotal += count;
                groups.add(new GroupItem(
                        doc.getId(),
                        name == null ? "Piso" : name,
                        count,
                        description == null || description.trim().isEmpty() ? "Sin direccion cargada" : description
                ));
            }
            adapter.notifyDataSetChanged();
            totalGroupsTv.setText(String.valueOf(groups.size()));
            totalMembersTv.setText(String.valueOf(membersTotal));
            if (emptyGroupsTv != null) {
                emptyGroupsTv.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), "Error cargando pisos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (emptyGroupsTv != null) emptyGroupsTv.setVisibility(View.VISIBLE);
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
        detailDescTv.setText(desc);
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

    private static class GroupItem {
        final String id;
        final String name;
        final int members;
        final String description;

        GroupItem(String id, String name, int members, String description) {
            this.id = id;
            this.name = name;
            this.members = members;
            this.description = description;
        }
    }

    private class GroupsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public Object getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_row, parent, false);
            }
            GroupItem item = groups.get(position);
            TextView groupNameTv = view.findViewById(R.id.groupNameTv);
            TextView groupMetaTv = view.findViewById(R.id.groupMetaTv);
            TextView groupMembersBadgeTv = view.findViewById(R.id.groupMembersBadgeTv);

            groupNameTv.setText(item.name);
            groupMetaTv.setText(item.description);
            groupMembersBadgeTv.setText(item.members + " miembros");

            if (!animatedPositions.get(position, false)) {
                view.setAlpha(0f);
                view.setTranslationY(24f);
                view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(260)
                        .setStartDelay(Math.min(position * 30L, 180L))
                        .start();
                animatedPositions.put(position, true);
            } else {
                view.setAlpha(1f);
                view.setTranslationY(0f);
            }
            return view;
        }
    }
}
