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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.DialogUtils;
import com.sergio.flatshare.util.SessionStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupsFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<GroupItem> groups = new ArrayList<>();
    private final SparseBooleanArray animatedPositions = new SparseBooleanArray();
    private static final LinkedHashMap<String, List<String>> PROVINCE_CITIES = buildProvinceCityMap();

    private GroupsAdapter adapter;
    private View detailsCard;
    private TextView emptyGroupsTv;
    private TextView detailNameTv;
    private TextView detailDescTv;
    private TextView detailMembersTv;
    private TextView totalGroupsTv;
    private TextView totalMembersTv;
    private Button manageRoomsBtn;
    private Button deleteGroupBtn;

    private String selectedGroupId;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (!isAdded()) return;
                if (result == null || result.getContents() == null) return;
                String raw = result.getContents().trim();
                String code = extractGroupCode(raw);
                if (code.isEmpty()) {
                    Toast.makeText(requireContext(), "QR no válido para unirse al piso", Toast.LENGTH_SHORT).show();
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
        manageRoomsBtn = view.findViewById(R.id.manageRoomsBtn);
        deleteGroupBtn = view.findViewById(R.id.deleteGroupBtn);
        Button editBtn = view.findViewById(R.id.editGroupBtn);
        Button inviteBtn = view.findViewById(R.id.inviteGroupBtn);
        Button closeBtn = view.findViewById(R.id.closeDetailsBtn);

        adapter = new GroupsAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            GroupItem selected = groups.get(position);
            selectedGroupId = selected.id;
            SessionStore.setCurrentGroup(requireContext(), selectedGroupId);
            openCurrentGroupWorkspace();
        });

        listView.setOnItemLongClickListener((parent, v, position, id) -> {
            selectedGroupId = groups.get(position).id;
            loadGroupDetails(selectedGroupId);
            return true;
        });

        editBtn.setOnClickListener(v -> editSelectedGroup());
        inviteBtn.setOnClickListener(v -> showInviteOptionsDialog(selectedGroupId));
        manageRoomsBtn.setOnClickListener(v -> openCurrentGroupWorkspace());
        deleteGroupBtn.setOnClickListener(v -> requestDeleteSelectedGroup());
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

    private void createGroupDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group, null, false);
        EditText groupNameEt = form.findViewById(R.id.groupNameEt);
        EditText roomCountEt = form.findViewById(R.id.roomCountEt);
        EditText streetEt = form.findViewById(R.id.streetEt);
        EditText portalEt = form.findViewById(R.id.portalEt);
        EditText numberEt = form.findViewById(R.id.numberEt);
        EditText postalCodeEt = form.findViewById(R.id.postalCodeEt);
        Spinner provinceSpinner = form.findViewById(R.id.provinceSpinner);
        Spinner citySpinner = form.findViewById(R.id.citySpinner);
        setupProvinceCitySpinners(provinceSpinner, citySpinner);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nuevo piso",
                "Completa los datos del piso y define cuántas habitaciones tendrá.",
                form,
                "Cancelar",
                "Crear"
        );

        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String name = groupNameEt.getText().toString().trim();
            String roomCountText = roomCountEt.getText().toString().trim();
            String street = streetEt.getText().toString().trim();
            String portal = portalEt.getText().toString().trim();
            String number = numberEt.getText().toString().trim();
            String postalCode = postalCodeEt.getText().toString().trim();
            String province = getSelectedSpinnerValue(provinceSpinner);
            String city = getSelectedSpinnerValue(citySpinner);

            if (name.isEmpty() || roomCountText.isEmpty() || street.isEmpty() || portal.isEmpty() || number.isEmpty()
                    || postalCode.isEmpty() || city.isEmpty() || province.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los datos del piso", Toast.LENGTH_SHORT).show();
                return;
            }

            int roomCount;
            try {
                roomCount = Integer.parseInt(roomCountText);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "El número de habitaciones no es válido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (roomCount <= 0) {
                Toast.makeText(requireContext(), "Debe haber al menos una habitación", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
            String portalAndNumber = portal + " " + number;
            Map<String, Object> location = new HashMap<>();
            location.put("street", street);
            location.put("portal", portalAndNumber);
            location.put("postalCode", postalCode);
            location.put("city", city);
            location.put("province", province);

            Map<String, Object> group = new HashMap<>();
            group.put("name", name);
            group.put("description", street + ", Portal " + portal + " Nº " + number + " - " + postalCode + " " + city + " (" + province + ")");
            group.put("location", location);
            group.put("ownerId", uid);
            Map<String, Object> roles = new HashMap<>();
            roles.put(uid, "admin");
            group.put("roles", roles);
            group.put("members", java.util.Collections.singletonList(uid));
            group.put("memberEmails", java.util.Collections.singletonList(email));
            group.put("roomCount", roomCount);
            group.put("createdAt", FieldValue.serverTimestamp());

            db.collection("groups").add(group).addOnSuccessListener(doc -> {
                String shareCode = doc.getId().toUpperCase(Locale.ROOT);
                Map<String, Object> codeData = new HashMap<>();
                codeData.put("groupId", doc.getId());
                codeData.put("ownerId", uid);
                codeData.put("name", name);
                doc.update("shareCode", shareCode);
                db.collection("group_codes").document(shareCode).set(codeData);

                selectedGroupId = doc.getId();
                SessionStore.setCurrentGroup(requireContext(), selectedGroupId);
                loadGroups();
                dialog.dismiss();
                Toast.makeText(requireContext(), "Piso creado. Define las habitaciones.", Toast.LENGTH_SHORT).show();
                startInitialRoomsSetup(selectedGroupId, roomCount);
            }).addOnFailureListener(e ->
                    Toast.makeText(requireContext(), "Error creando piso: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });
    }

    private void startInitialRoomsSetup(String groupId, int roomCount) {
        collectRoomDrafts(groupId, roomCount, 1, new ArrayList<>());
    }

    private void collectRoomDrafts(String groupId, int totalRooms, int currentNumber, List<RoomDraft> drafts) {
        if (currentNumber > totalRooms) {
            saveInitialRooms(groupId, drafts);
            return;
        }
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_room_setup, null, false);
        EditText roomNameEt = form.findViewById(R.id.roomNameEt);
        EditText roomCapacityEt = form.findViewById(R.id.roomCapacityEt);
        EditText roomCostEt = form.findViewById(R.id.roomCostEt);
        roomNameEt.setHint("Habitación " + currentNumber);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Habitación " + currentNumber + " de " + totalRooms,
                "Indica nombre, capacidad y coste mensual.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
            saveInitialRooms(groupId, drafts);
        });
        shell.confirmBtn.setOnClickListener(v -> {
            String name = roomNameEt.getText().toString().trim();
            String capacityText = roomCapacityEt.getText().toString().trim();
            String costText = roomCostEt.getText().toString().trim();
            if (name.isEmpty() || capacityText.isEmpty() || costText.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos de la habitación", Toast.LENGTH_SHORT).show();
                return;
            }
            int capacity;
            double monthlyCost;
            try {
                capacity = Integer.parseInt(capacityText);
                monthlyCost = Double.parseDouble(costText);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Capacidad o coste no válidos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (capacity <= 0) {
                Toast.makeText(requireContext(), "La capacidad debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                return;
            }
            if (monthlyCost < 0) {
                Toast.makeText(requireContext(), "El coste no puede ser negativo", Toast.LENGTH_SHORT).show();
                return;
            }
            drafts.add(new RoomDraft(currentNumber, name, capacity, monthlyCost));
            dialog.dismiss();
            collectRoomDrafts(groupId, totalRooms, currentNumber + 1, drafts);
        });
    }

    private void saveInitialRooms(String groupId, List<RoomDraft> drafts) {
        if (drafts.isEmpty()) {
            openCurrentGroupWorkspace();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        WriteBatch batch = db.batch();
        for (RoomDraft draft : drafts) {
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("groupId", groupId);
            roomData.put("roomNumber", draft.roomNumber);
            roomData.put("name", draft.name);
            roomData.put("capacity", draft.capacity);
            roomData.put("monthlyCost", draft.monthlyCost);
            roomData.put("memberEmails", new ArrayList<String>());
            roomData.put("memberCount", 0);
            roomData.put("createdByUid", uid);
            roomData.put("updatedByUid", uid);
            roomData.put("createdAt", FieldValue.serverTimestamp());
            roomData.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(db.collection("rooms_groups").document(), roomData);
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Habitaciones guardadas", Toast.LENGTH_SHORT).show();
                    openCurrentGroupWorkspace();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "No se pudieron guardar todas las habitaciones", Toast.LENGTH_LONG).show();
                    openCurrentGroupWorkspace();
                });
    }

    private void openCurrentGroupWorkspace() {
        SessionStore.clearCurrentRoom(requireContext());
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
        }
    }

    private void joinGroupDialog() {
        View content = DialogUtils.createVerticalActions(requireContext());
        Button codeBtn = DialogUtils.createActionButton(requireContext(), "Escribir código", true);
        Button qrBtn = DialogUtils.createActionButton(requireContext(), "Escanear QR", false);
        ((LinearLayout) content).addView(codeBtn);
        ((LinearLayout) content).addView(qrBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Unirse a un piso",
                "Puedes escribir el código o escanear un QR.",
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
                    "Escribe el código para entrar en un piso compartido.",
                    "Código del piso",
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
                        Toast.makeText(requireContext(), "Código no válido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String groupId = result.getString("groupId");
                    if (groupId == null || groupId.trim().isEmpty()) {
                        Toast.makeText(requireContext(), "Código no válido", Toast.LENGTH_SHORT).show();
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
            SessionStore.clearCurrentRoom(requireContext());
            loadGroups();
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
        Button emailBtn = DialogUtils.createActionButton(requireContext(), "Invitar por email", false);
        ((LinearLayout) content).addView(codeBtn);
        ((LinearLayout) content).addView(qrBtn);
        ((LinearLayout) content).addView(emailBtn);

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
        emailBtn.setOnClickListener(v -> {
            dialog.dismiss();
            inviteByEmail(groupId);
        });
    }

    private void inviteByEmail(String groupId) {
        showSingleInputDialog(
                "Invitar por email",
                "Comparte el piso por correo con otra persona.",
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
                            .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Invitación enviada", Toast.LENGTH_SHORT).show());
                    return true;
                }
        );
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
        View content = DialogUtils.createMessageView(requireContext(), "Tienes una invitación a un piso compartido.");
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Invitación pendiente",
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
            SessionStore.clearCurrentRoom(requireContext());
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
                String ownerId = doc.getString("ownerId");
                Object membersField = doc.get("members");
                List<?> members = membersField instanceof List ? (List<?>) membersField : null;
                int count = members == null ? 0 : members.size();
                membersTotal += count;
                groups.add(new GroupItem(
                        doc.getId(),
                        name == null ? "Piso" : name,
                        count,
                        description == null || description.trim().isEmpty() ? "Sin direccion cargada" : description,
                        uid.equals(ownerId)
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

    @SuppressWarnings("unchecked")
    private void renderGroupDetails(DocumentSnapshot doc) {
        if (!isAdded() || doc == null || !doc.exists()) return;
        String name = doc.getString("name");
        String desc = doc.getString("description");
        if (desc == null || desc.trim().isEmpty()) desc = "Sin descripción";

        Object emailsField = doc.get("memberEmails");
        List<String> emails = emailsField instanceof List ? (List<String>) emailsField : new ArrayList<>();
        List<String> memberIds = castStrings(doc.get("members"));

        String ownerId = doc.getString("ownerId");
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = ownerId != null && ownerId.equals(myUid);

        detailNameTv.setText(name == null ? "Grupo" : name);
        detailDescTv.setText(desc);
        resolveMemberDisplayNames(memberIds, emails, labels -> {
            if (!isAdded()) return;
            if (labels.isEmpty()) {
                detailMembersTv.setText("Miembros: sin datos");
            } else {
                StringBuilder membersText = new StringBuilder("Miembros:");
                for (String label : labels) {
                    membersText.append("\n- ").append(label);
                }
                detailMembersTv.setText(membersText.toString());
            }
        });

        manageRoomsBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        deleteGroupBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        detailsCard.setVisibility(View.VISIBLE);
    }

    private void resolveMemberDisplayNames(List<String> memberIds, List<String> memberEmails, MemberLabelsCallback callback) {
        if (memberEmails.isEmpty()) {
            callback.onResolved(new ArrayList<>());
            return;
        }
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : memberIds) {
            if (uid != null && !uid.trim().isEmpty()) {
                tasks.add(db.collection("users").document(uid).get());
            }
        }
        if (tasks.isEmpty()) {
            callback.onResolved(new ArrayList<>(memberEmails));
            return;
        }
        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(done -> {
                    Map<String, String> namesByEmail = new HashMap<>();
                    for (Task<DocumentSnapshot> task : tasks) {
                        if (!task.isSuccessful() || task.getResult() == null) continue;
                        DocumentSnapshot userDoc = task.getResult();
                        String email = userDoc.getString("email");
                        if (email == null || email.trim().isEmpty()) continue;
                        String displayName = userDoc.getString("displayName");
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = userDoc.getString("username");
                        }
                        namesByEmail.put(email.toLowerCase(Locale.ROOT), (displayName == null || displayName.trim().isEmpty())
                                ? email.toLowerCase(Locale.ROOT)
                                : displayName.trim());
                    }
                    List<String> labels = new ArrayList<>();
                    for (String email : memberEmails) {
                        String normalized = email.toLowerCase(Locale.ROOT);
                        labels.add(namesByEmail.getOrDefault(normalized, normalized));
                    }
                    callback.onResolved(labels);
                })
                .addOnFailureListener(e -> callback.onResolved(new ArrayList<>(memberEmails)));
    }

    private List<String> castStrings(Object raw) {
        List<String> values = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
        }
        return values;
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
                "Actualiza el nombre y la descripción del piso.",
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
            updates.put("description", newDesc.isEmpty() ? "Sin descripción" : newDesc);
            db.collection("groups").document(selectedGroupId).update(updates).addOnSuccessListener(task -> {
                loadGroups();
                loadGroupDetails(selectedGroupId);
                dialog.dismiss();
            });
        });
    }

    private void requestDeleteSelectedGroup() {
        if (selectedGroupId == null) {
            Toast.makeText(requireContext(), "Selecciona un piso primero", Toast.LENGTH_SHORT).show();
            return;
        }
        View content = DialogUtils.createMessageView(requireContext(),
                "Se eliminará el piso, sus habitaciones, gastos, pagos, recordatorios e invitaciones.");
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Eliminar piso",
                "Primer paso de confirmación",
                content,
                "Cancelar",
                "Continuar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            deleteGroupCascade(selectedGroupId);
        });
    }

    private void deleteGroupCascade(String groupId) {
        List<Task<QuerySnapshot>> queries = new ArrayList<>();
        queries.add(db.collection("rooms_groups").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("expenses").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("payments").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("payment_deadlines").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("reminders").whereEqualTo("groupId", groupId).get());

        Tasks.whenAllSuccess(queries)
                .addOnSuccessListener(results -> {
                    WriteBatch batch = db.batch();
                    for (Object result : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                    }
                    batch.delete(db.collection("groups").document(groupId));
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                if (groupId.equals(SessionStore.getCurrentGroup(requireContext()))) {
                                    SessionStore.clearCurrentGroup(requireContext());
                                    SessionStore.clearCurrentRoom(requireContext());
                                }
                                selectedGroupId = null;
                                detailsCard.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Piso eliminado", Toast.LENGTH_SHORT).show();
                                loadGroups();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    requireContext(),
                                    "No se pudo eliminar el piso: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "No se pudieron preparar los datos: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void setupProvinceCitySpinners(Spinner provinceSpinner, Spinner citySpinner) {
        List<String> provinces = new ArrayList<>();
        provinces.add("Selecciona provincia");
        provinces.addAll(PROVINCE_CITIES.keySet());
        provinceSpinner.setAdapter(buildDialogSpinnerAdapter(provinces));
        citySpinner.setAdapter(buildDialogSpinnerAdapter(Collections.singletonList("Selecciona ciudad")));

        provinceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0) {
                    citySpinner.setAdapter(buildDialogSpinnerAdapter(Collections.singletonList("Selecciona ciudad")));
                    return;
                }
                String province = provinces.get(position);
                List<String> cities = PROVINCE_CITIES.get(province);
                List<String> cityItems = new ArrayList<>();
                cityItems.add("Selecciona ciudad");
                if (cities != null) {
                    cityItems.addAll(cities);
                }
                citySpinner.setAdapter(buildDialogSpinnerAdapter(cityItems));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String getSelectedSpinnerValue(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) return "";
        int position = spinner.getSelectedItemPosition();
        if (position <= 0) return "";
        return spinner.getSelectedItem().toString().trim();
    }

    private ArrayAdapter<String> buildDialogSpinnerAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items) {
            @Override
            public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(requireContext().getColor(position == 0 ? R.color.text_muted : R.color.text_light));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(requireContext().getColor(R.color.text_light));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static LinkedHashMap<String, List<String>> buildProvinceCityMap() {
        LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
        data.put("A Coruña", Arrays.asList("A Coruña", "Santiago de Compostela", "Ferrol"));
        data.put("Álava", Arrays.asList("Vitoria-Gasteiz", "Llodio", "Amurrio"));
        data.put("Albacete", Arrays.asList("Albacete", "Hellín", "Villarrobledo"));
        data.put("Alicante", Arrays.asList("Alicante", "Elche", "Benidorm"));
        data.put("Almería", Arrays.asList("Almería", "Roquetas de Mar", "El Ejido"));
        data.put("Asturias", Arrays.asList("Oviedo", "Gijón", "Avilés"));
        data.put("Ávila", Arrays.asList("Ávila", "Arévalo", "Cebreros"));
        data.put("Badajoz", Arrays.asList("Badajoz", "Mérida", "Don Benito"));
        data.put("Barcelona", Arrays.asList("Barcelona", "L'Hospitalet de Llobregat", "Badalona"));
        data.put("Burgos", Arrays.asList("Burgos", "Miranda de Ebro", "Aranda de Duero"));
        data.put("Cáceres", Arrays.asList("Cáceres", "Plasencia", "Navalmoral de la Mata"));
        data.put("Cádiz", Arrays.asList("Cádiz", "Jerez de la Frontera", "Algeciras"));
        data.put("Cantabria", Arrays.asList("Santander", "Torrelavega", "Castro-Urdiales"));
        data.put("Castellón", Arrays.asList("Castellón de la Plana", "Vila-real", "Burriana"));
        data.put("Ciudad Real", Arrays.asList("Ciudad Real", "Puertollano", "Tomelloso"));
        data.put("Córdoba", Arrays.asList("Córdoba", "Lucena", "Puente Genil"));
        data.put("Cuenca", Arrays.asList("Cuenca", "Tarancón", "San Clemente"));
        data.put("Girona", Arrays.asList("Girona", "Figueres", "Blanes"));
        data.put("Granada", Arrays.asList("Granada", "Motril", "Armilla"));
        data.put("Guadalajara", Arrays.asList("Guadalajara", "Azuqueca de Henares", "Molina de Aragón"));
        data.put("Guipúzcoa", Arrays.asList("San Sebastián", "Irún", "Eibar"));
        data.put("Huelva", Arrays.asList("Huelva", "Lepe", "Almonte"));
        data.put("Huesca", Arrays.asList("Huesca", "Barbastro", "Jaca"));
        data.put("Illes Balears", Arrays.asList("Palma", "Calvià", "Eivissa"));
        data.put("Jaén", Arrays.asList("Jaén", "Linares", "Andújar"));
        data.put("La Rioja", Arrays.asList("Logroño", "Calahorra", "Arnedo"));
        data.put("Las Palmas", Arrays.asList("Las Palmas de Gran Canaria", "Telde", "Arrecife"));
        data.put("León", Arrays.asList("León", "Ponferrada", "San Andrés del Rabanedo"));
        data.put("Lleida", Arrays.asList("Lleida", "Balaguer", "La Seu d'Urgell"));
        data.put("Lugo", Arrays.asList("Lugo", "Monforte de Lemos", "Viveiro"));
        data.put("Madrid", Arrays.asList("Madrid", "Móstoles", "Alcalá de Henares"));
        data.put("Málaga", Arrays.asList("Málaga", "Marbella", "Fuengirola"));
        data.put("Murcia", Arrays.asList("Murcia", "Cartagena", "Lorca"));
        data.put("Navarra", Arrays.asList("Pamplona", "Tudela", "Estella"));
        data.put("Ourense", Arrays.asList("Ourense", "Verín", "O Barco de Valdeorras"));
        data.put("Palencia", Arrays.asList("Palencia", "Aguilar de Campoo", "Guardo"));
        data.put("Pontevedra", Arrays.asList("Pontevedra", "Vigo", "Vilagarcía de Arousa"));
        data.put("Salamanca", Arrays.asList("Salamanca", "Béjar", "Ciudad Rodrigo"));
        data.put("Santa Cruz de Tenerife", Arrays.asList("Santa Cruz de Tenerife", "San Cristóbal de La Laguna", "Arona"));
        data.put("Segovia", Arrays.asList("Segovia", "Cuéllar", "El Espinar"));
        data.put("Sevilla", Arrays.asList("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra"));
        data.put("Soria", Arrays.asList("Soria", "Almazán", "El Burgo de Osma"));
        data.put("Tarragona", Arrays.asList("Tarragona", "Reus", "Tortosa"));
        data.put("Teruel", Arrays.asList("Teruel", "Alcañiz", "Andorra"));
        data.put("Toledo", Arrays.asList("Toledo", "Talavera de la Reina", "Illescas"));
        data.put("Valencia", Arrays.asList("València", "Torrent", "Gandia"));
        data.put("Valladolid", Arrays.asList("Valladolid", "Medina del Campo", "Laguna de Duero"));
        data.put("Vizcaya", Arrays.asList("Bilbao", "Barakaldo", "Getxo"));
        data.put("Zamora", Arrays.asList("Zamora", "Benavente", "Toro"));
        data.put("Zaragoza", Arrays.asList("Zaragoza", "Calatayud", "Utebo"));
        return data;
    }

    private interface SingleInputAction {
        boolean onConfirm(String value);
    }

    private interface MemberLabelsCallback {
        void onResolved(List<String> labels);
    }

    private static class GroupItem {
        final String id;
        final String name;
        final int members;
        final String description;
        final boolean isOwner;

        GroupItem(String id, String name, int members, String description, boolean isOwner) {
            this.id = id;
            this.name = name;
            this.members = members;
            this.description = description;
            this.isOwner = isOwner;
        }
    }

    private static class RoomDraft {
        final int roomNumber;
        final String name;
        final int capacity;
        final double monthlyCost;

        RoomDraft(int roomNumber, String name, int capacity, double monthlyCost) {
            this.roomNumber = roomNumber;
            this.name = name;
            this.capacity = capacity;
            this.monthlyCost = monthlyCost;
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


