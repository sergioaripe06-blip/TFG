package com.sergio.flatshare.features.groups;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.BaseAdapter;
import android.widget.Toast;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.features.shell.MainActivity;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OwnerRoomsActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "extra_group_id";
    public static final String EXTRA_OPEN_WORKSPACE = "open_workspace";
    private static final String ROOM_SPLIT_EQUAL = "equal";
    private static final String ROOM_SPLIT_PERCENTAGE = "percentage";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<RoomItem> rooms = new ArrayList<>();
    private final List<String> groupMembers = new ArrayList<>();
    private final Map<String, String> memberDisplayNamesByEmail = new HashMap<>();

    private RoomsAdapter adapter;
    private TextView roomsTitleTv;
    private TextView roomsSubtitleTv;
    private TextView shareCodeTv;
    private TextView emptyRoomsTv;
    private Button createRoomBtn;
    private Button inviteCodeBtn;
    private Button inviteEmailBtn;

    private String groupId;
    private String groupName = "Piso";
    private String shareCode = "";
    private boolean isOwner = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_rooms);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null || groupId.trim().isEmpty()) {
            NoticeUtils.show(this, "No se encontró el piso");
            finish();
            return;
        }

        roomsTitleTv = findViewById(R.id.roomsTitleTv);
        roomsSubtitleTv = findViewById(R.id.roomsSubtitleTv);
        shareCodeTv = findViewById(R.id.shareCodeTv);
        emptyRoomsTv = findViewById(R.id.emptyRoomsTv);
        createRoomBtn = findViewById(R.id.createRoomBtn);
        inviteCodeBtn = findViewById(R.id.inviteCodeBtn);
        inviteEmailBtn = findViewById(R.id.inviteEmailBtn);
        Button finishSetupBtn = findViewById(R.id.finishSetupBtn);

        ListView roomsLv = findViewById(R.id.roomsLv);
        adapter = new RoomsAdapter();
        roomsLv.setAdapter(adapter);
        roomsLv.setOnItemClickListener((parent, view, position, id) -> {
            RoomItem selectedRoom = rooms.get(position);
            SessionStore.setCurrentGroup(this, groupId);
            SessionStore.setCurrentRoom(this, selectedRoom.id, selectedRoom.name);
            openWorkspace();
        });
        roomsLv.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!isOwner) return true;
            showRoomActions(rooms.get(position));
            return true;
        });

        createRoomBtn.setOnClickListener(v -> createRoomDialog());
        inviteCodeBtn.setOnClickListener(v -> showShareOptionsDialog());
        inviteEmailBtn.setOnClickListener(v -> inviteByEmailDialog());
        finishSetupBtn.setOnClickListener(v -> {
            SessionStore.setCurrentGroup(this, groupId);
            SessionStore.clearCurrentRoom(this);
            openWorkspace();
        });

        loadGroupData();
        loadRooms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroupData();
        loadRooms();
    }

    private void loadGroupData() {
        db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                NoticeUtils.show(this, "El piso no existe");
                finish();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String ownerId = doc.getString("ownerId");
            isOwner = uid.equals(ownerId);
            updateOwnerActions();

            String loadedName = doc.getString("name");
            if (loadedName != null && !loadedName.trim().isEmpty()) {
                groupName = loadedName;
            }
            String loadedCode = doc.getString("shareCode");
            shareCode = (loadedCode == null || loadedCode.trim().isEmpty())
                    ? doc.getId().toUpperCase(Locale.ROOT)
                    : loadedCode.toUpperCase(Locale.ROOT);

            groupMembers.clear();
            groupMembers.addAll(castEmails(doc.get("memberEmails")));
            resolveMemberDisplayNames(castStrings(doc.get("members")), groupMembers, this::loadRooms);

            roomsTitleTv.setText("Habitaciones de " + groupName);
            roomsSubtitleTv.setText(isOwner
                    ? "Crea habitaciones, invita residentes y asigna quién vive en cada una."
                    : "Vista de habitaciones del piso compartido.");
            shareCodeTv.setText("Código: " + shareCode);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "No se pudo cargar el piso", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateOwnerActions() {
        createRoomBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        inviteCodeBtn.setEnabled(isOwner);
        inviteEmailBtn.setEnabled(isOwner);
    }

    private void loadRooms() {
        db.collection("rooms_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(result -> {
                    rooms.clear();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        String name = doc.getString("name");
                        Long roomNumber = doc.getLong("roomNumber");
                        Long capacity = doc.getLong("capacity");
                        Double monthlyCost = doc.getDouble("monthlyCost");
                        List<String> memberEmails = castEmails(doc.get("memberEmails"));
                        String rentSplitMode = normalizeRoomSplitMode(doc.getString("rentSplitMode"));
                        Map<String, Double> rentSplitPercentages = castPercentages(doc.get("rentSplitPercentages"));
                        List<String> rentSplitOrder = castEmails(doc.get("rentSplitOrder"));
                        rooms.add(new RoomItem(
                                doc.getId(),
                                name == null || name.trim().isEmpty() ? "Habitación" : name,
                                roomNumber == null ? 0 : roomNumber.intValue(),
                                capacity == null ? 0 : capacity.intValue(),
                                monthlyCost == null ? 0.0 : monthlyCost,
                                memberEmails,
                                rentSplitMode,
                                rentSplitPercentages,
                                rentSplitOrder
                        ));
                    }
                    Collections.sort(rooms, Comparator.comparingInt(a -> a.roomNumber <= 0 ? Integer.MAX_VALUE : a.roomNumber));
                    adapter.notifyDataSetChanged();
                    emptyRoomsTv.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "No se pudo cargar habitaciones", Toast.LENGTH_SHORT).show());
    }

    private void createRoomDialog() {
        if (!isOwner) return;
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_room_setup, null, false);
        EditText roomNameEt = form.findViewById(R.id.roomNameEt);
        EditText roomCapacityEt = form.findViewById(R.id.roomCapacityEt);
        EditText roomCostEt = form.findViewById(R.id.roomCostEt);
        roomNameEt.setHint("Nombre de la habitacion:");

        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "Nueva habitacion",
                "Indica nombre, capacidad y coste mensual.",
                form,
                "Cancelar",
                "Crear"
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String roomName = roomNameEt.getText().toString().trim();
            String capacityText = roomCapacityEt.getText().toString().trim();
            String costText = roomCostEt.getText().toString().trim();
            if (roomName.isEmpty() || capacityText.isEmpty() || costText.isEmpty()) {
                NoticeUtils.show(this, "Completa todos los datos de la habitacion");
                return;
            }

            int roomNumber = rooms.size() + 1;
            int capacity;
            double monthlyCost;
            try {
                capacity = Integer.parseInt(capacityText);
                monthlyCost = Double.parseDouble(costText);
            } catch (NumberFormatException e) {
                NoticeUtils.show(this, "Capacidad o coste no validos");
                return;
            }
            if (capacity <= 0) {
                NoticeUtils.show(this, "La capacidad debe ser mayor que 0");
                return;
            }
            if (monthlyCost < 0) {
                NoticeUtils.show(this, "El coste no puede ser negativo");
                return;
            }

            openRoomResidentsConfigDialog(
                    "Configurar inquilinos",
                    "Elige quien ocupa cada plaza y como se reparte el alquiler.",
                    capacity,
                    new ArrayList<>(),
                    ROOM_SPLIT_EQUAL,
                    new HashMap<>(),
                    new ArrayList<>(),
                    result -> {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        Map<String, Object> room = new HashMap<>();
                        room.put("groupId", groupId);
                        room.put("roomNumber", roomNumber);
                        room.put("name", roomName);
                        room.put("capacity", capacity);
                        room.put("monthlyCost", monthlyCost);
                        room.put("memberEmails", result.members);
                        room.put("memberCount", result.members.size());
                        room.put("rentSplitMode", result.splitMode);
                        room.put("rentSplitPercentages", result.splitPercentages);
                        room.put("rentSplitOrder", result.splitOrder);
                        room.put("createdByUid", uid);
                        room.put("updatedByUid", uid);
                        room.put("createdAt", FieldValue.serverTimestamp());
                        room.put("updatedAt", FieldValue.serverTimestamp());

                        db.collection("rooms_groups")
                                .add(room)
                                .addOnSuccessListener(v2 -> {
                                    NoticeUtils.show(this, "Habitacion creada");
                                    loadRooms();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "No se pudo crear", Toast.LENGTH_SHORT).show());
                    }
            );
        });
    }

    private void showRoomActions(RoomItem room) {
        View content = DialogUtils.createVerticalActions(this);
        Button assignBtn = DialogUtils.createActionButton(this, "Asignar residentes", true);
        Button renameBtn = DialogUtils.createActionButton(this, "Renombrar", false);
        Button deleteBtn = DialogUtils.createActionButton(this, "Eliminar", false);
        ((LinearLayout) content).addView(assignBtn);
        ((LinearLayout) content).addView(renameBtn);
        ((LinearLayout) content).addView(deleteBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                room.name,
                "Gestiona esta habitación.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());

        assignBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showAssignResidentsDialog(room);
        });
        renameBtn.setOnClickListener(v -> {
            dialog.dismiss();
            renameRoomDialog(room);
        });
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            deleteRoomDialog(room);
        });
    }

    private void showAssignResidentsDialog(RoomItem room) {
        if (!isOwner) return;
        openRoomResidentsConfigDialog(
                "Editar inquilinos",
                "Elige que inquilino va en cada plaza y ajusta el reparto del alquiler.",
                Math.max(1, room.capacity),
                room.memberEmails,
                room.rentSplitMode,
                room.rentSplitPercentages,
                room.rentSplitOrder,
                result -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("memberEmails", result.members);
                    updates.put("memberCount", result.members.size());
                    updates.put("rentSplitMode", result.splitMode);
                    updates.put("rentSplitPercentages", result.splitPercentages);
                    updates.put("rentSplitOrder", result.splitOrder);
                    updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    updates.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("rooms_groups").document(room.id)
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                NoticeUtils.show(this, "Habitacion actualizada");
                                loadRooms();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show());
                }
        );
    }

    private void renameRoomDialog(RoomItem room) {
        showSingleInputDialog(
                "Renombrar habitación",
                "Escribe el nuevo nombre.",
                room.name,
                InputType.TYPE_CLASS_TEXT,
                "Guardar",
                value -> {
                    String newName = value.trim();
                    if (newName.isEmpty()) {
                        NoticeUtils.show(this, "El nombre es obligatorio");
                        return false;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", newName);
                    updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    updates.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("rooms_groups").document(room.id)
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                NoticeUtils.show(this, "Nombre actualizado");
                                loadRooms();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show());
                    return true;
                }
        );
    }

    private void deleteRoomDialog(RoomItem room) {
        if (room != null && room.memberEmails != null && !room.memberEmails.isEmpty()) {
            NoticeUtils.show(this, "No puedes eliminar una habitacion con inquilinos. Quitalos o cambialos de habitacion primero.");
            return;
        }
        View content = DialogUtils.createMessageView(this, "Esta accion eliminara la habitacion y su asignacion de residentes.");
        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "Eliminar habitacion",
                room.name,
                content,
                "Cancelar",
                "Eliminar"
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            db.collection("rooms_groups").document(room.id).get()
                    .addOnSuccessListener(doc -> {
                        List<String> memberEmails = castEmails(doc.get("memberEmails"));
                        if (!memberEmails.isEmpty()) {
                            NoticeUtils.show(this, "No puedes eliminar una habitacion con inquilinos. Quitalos o cambialos de habitacion primero.");
                            return;
                        }
                        db.collection("rooms_groups").document(room.id)
                                .delete()
                                .addOnSuccessListener(v2 -> {
                                    NoticeUtils.show(this, "Habitacion eliminada");
                                    loadRooms();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "No se pudo validar la habitacion", Toast.LENGTH_SHORT).show());
            dialog.dismiss();
        });
    }

    private void showShareOptionsDialog() {
        View content = DialogUtils.createVerticalActions(this);
        Button codeBtn = DialogUtils.createActionButton(this, "Mostrar código", true);
        Button qrBtn = DialogUtils.createActionButton(this, "Mostrar QR", false);
        ((LinearLayout) content).addView(codeBtn);
        ((LinearLayout) content).addView(qrBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "Añadir al piso",
                "Comparte el acceso con nuevos residentes.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        codeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showShareCodeDialog();
        });
        qrBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showShareQrDialog();
        });
    }

    private void showShareCodeDialog() {
        View content = DialogUtils.createMessageView(this, shareCode);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "Código del piso",
                "Comparte este código para que se unan al piso.",
                content,
                null,
                "Cerrar"
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
    }

    private void showShareQrDialog() {
        String payload = "flatshare://join?code=" + shareCode;
        View content = buildQrContent(payload);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "QR del piso",
                "Comparte este QR para añadir residentes.",
                content,
                null,
                "Cerrar"
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
    }

    private void inviteByEmailDialog() {
        if (!isOwner) return;
        showSingleInputDialog(
                "Añadir por email",
                "Envía una solicitud directa por correo.",
                "Email del invitado:",
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                "Enviar",
                value -> {
                    String invitedEmail = value.trim().toLowerCase(Locale.ROOT);
                    if (invitedEmail.isEmpty() || !invitedEmail.contains("@")) {
                        NoticeUtils.show(this, "Debes indicar un email valido");
                        return false;
                    }
                    String currentShareCode = (shareCode == null || shareCode.trim().isEmpty())
                            ? groupId.toUpperCase(Locale.ROOT)
                            : shareCode;
                    String inviterEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail() == null
                            ? ""
                            : FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);

                    Map<String, Object> invitation = new HashMap<>();
                    invitation.put("groupId", groupId);
                    invitation.put("groupName", groupName == null || groupName.trim().isEmpty() ? "Piso" : groupName);
                    invitation.put("shareCode", currentShareCode);
                    invitation.put("invitedEmail", invitedEmail);
                    invitation.put("inviterUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    invitation.put("inviterEmail", inviterEmail);
                    invitation.put("status", "pending");
                    invitation.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("invitations").add(invitation)
                            .addOnSuccessListener(v -> Toast.makeText(this, "Invitacion creada", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "No se pudo crear la invitacion", Toast.LENGTH_SHORT).show());
                    return true;
                }
        );
    }
    private void openWorkspace() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_OPEN_WORKSPACE, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openRoomResidentsConfigDialog(
            String title,
            String subtitle,
            int capacity,
            List<String> initialMembers,
            String initialSplitMode,
            Map<String, Double> initialSplitPercentages,
            List<String> initialSplitOrder,
            RoomResidentsConfigCallback onConfigured
    ) {
        int safeCapacity = Math.max(0, capacity);
        int maxResidents = Math.min(safeCapacity, groupMembers.size());

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(2), 0, dp(2), 0);
        scroll.addView(content);

        TextView countLabel = new TextView(this);
        countLabel.setText("Número de inquilinos en la habitación");
        countLabel.setTextColor(getColor(R.color.text_light));
        countLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        countLabel.setPadding(0, 0, 0, dp(4));
        content.addView(countLabel);

        Spinner residentsCountSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        residentsCountSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        residentsCountSpinner.setPadding(dp(12), 0, dp(12), 0);
        residentsCountSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        ));
        content.addView(residentsCountSpinner);

        TextView countHintTv = new TextView(this);
        countHintTv.setTextColor(getColor(R.color.text_muted));
        countHintTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        countHintTv.setPadding(0, dp(6), 0, dp(6));
        content.addView(countHintTv);

        LinearLayout slotsContainer = new LinearLayout(this);
        slotsContainer.setOrientation(LinearLayout.VERTICAL);
        slotsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        content.addView(slotsContainer);

        TextView splitModeLabel = new TextView(this);
        splitModeLabel.setText("Reparto del alquiler en esta habitación");
        splitModeLabel.setTextColor(getColor(R.color.text_light));
        splitModeLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        splitModeLabel.setPadding(0, dp(12), 0, dp(4));
        content.addView(splitModeLabel);

        Spinner splitModeSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        splitModeSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        splitModeSpinner.setPadding(dp(12), 0, dp(12), 0);
        splitModeSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        ));
        splitModeSpinner.setAdapter(buildLightSpinnerAdapter(new String[]{"Equitativo", "Porcentual"}));
        splitModeSpinner.setSelection(ROOM_SPLIT_PERCENTAGE.equals(normalizeRoomSplitMode(initialSplitMode)) ? 1 : 0);
        content.addView(splitModeSpinner);

        TextView splitHintTv = new TextView(this);
        splitHintTv.setTextColor(getColor(R.color.text_muted));
        splitHintTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        splitHintTv.setPadding(0, dp(6), 0, dp(6));
        content.addView(splitHintTv);

        LinearLayout percentageRowsContainer = new LinearLayout(this);
        percentageRowsContainer.setOrientation(LinearLayout.VERTICAL);
        percentageRowsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        content.addView(percentageRowsContainer);

        TextView autoPercentPreviewTv = new TextView(this);
        autoPercentPreviewTv.setTextColor(getColor(R.color.text_muted));
        autoPercentPreviewTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        autoPercentPreviewTv.setPadding(0, dp(6), 0, 0);
        content.addView(autoPercentPreviewTv);

        int[] countValues = new int[maxResidents + 1];
        String[] countLabels = new String[maxResidents + 1];
        for (int i = 0; i <= maxResidents; i++) {
            countValues[i] = i;
            countLabels[i] = String.valueOf(i);
        }
        residentsCountSpinner.setAdapter(buildLightSpinnerAdapter(countLabels));

        List<String> orderedInitialMembers = buildOrderedMembers(initialMembers, initialSplitOrder);
        int initialCount = Math.min(orderedInitialMembers.size(), maxResidents);
        residentsCountSpinner.setSelection(initialCount);

        Map<String, Double> draftPercentages = new LinkedHashMap<>(initialSplitPercentages);
        List<Spinner> slotSpinners = new ArrayList<>();
        List<EditText> percentInputs = new ArrayList<>();
        List<String> percentResidents = new ArrayList<>();
        boolean[] isBindingSlots = new boolean[]{false};
        Runnable[] refreshSplitRef = new Runnable[1];

        Runnable rebuildSlots = () -> {
            int selectedCount = countValues[Math.max(0, residentsCountSpinner.getSelectedItemPosition())];
            slotsContainer.removeAllViews();
            slotSpinners.clear();

            if (selectedCount == 0) {
                countHintTv.setText("Sin inquilinos asignados en esta habitación.");
                return;
            }
            countHintTv.setText("Selecciona quién ocupa cada plaza. Puedes elegir quién va en Inquilino 1, 2, 3...");

            List<String> memberLabels = new ArrayList<>();
            for (String email : groupMembers) {
                memberLabels.add(displayNameForEmail(email));
            }
            String[] labelArray = memberLabels.toArray(new String[0]);

            Set<String> used = new HashSet<>();
            for (int i = 0; i < selectedCount; i++) {
                TextView slotLabel = new TextView(this);
                slotLabel.setText("Inquilino " + (i + 1) + ":");
                slotLabel.setTextColor(getColor(R.color.text_light));
                slotLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                slotLabel.setPadding(0, i == 0 ? dp(4) : dp(10), 0, dp(4));
                slotsContainer.addView(slotLabel);

                Spinner slotSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
                slotSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
                slotSpinner.setPadding(dp(12), 0, dp(12), 0);
                slotSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(50)
                ));
                slotSpinner.setAdapter(buildLightSpinnerAdapter(labelArray));
                slotsContainer.addView(slotSpinner);
                slotSpinners.add(slotSpinner);

                String targetEmail = i < orderedInitialMembers.size() ? orderedInitialMembers.get(i) : "";
                int index = groupMembers.indexOf(targetEmail);
                if (index < 0 || used.contains(targetEmail)) {
                    index = -1;
                    for (int j = 0; j < groupMembers.size(); j++) {
                        String candidate = groupMembers.get(j);
                        if (!used.contains(candidate)) {
                            index = j;
                            break;
                        }
                    }
                    if (index < 0) index = 0;
                }
                slotSpinner.setSelection(index);
                used.add(groupMembers.get(index));
                slotSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (isBindingSlots[0]) return;
                        if (refreshSplitRef[0] != null) refreshSplitRef[0].run();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        };

        Runnable refreshSplit = () -> {
            List<String> selectedResidents = new ArrayList<>();
            for (Spinner slotSpinner : slotSpinners) {
                int pos = slotSpinner.getSelectedItemPosition();
                if (pos < 0 || pos >= groupMembers.size()) continue;
                selectedResidents.add(groupMembers.get(pos));
            }

            boolean canSplit = selectedResidents.size() >= 2;
            if (!canSplit && splitModeSpinner.getSelectedItemPosition() == 1) {
                splitModeSpinner.setSelection(0);
            }
            splitModeSpinner.setEnabled(canSplit);
            splitModeSpinner.setAlpha(canSplit ? 1f : 0.55f);

            percentageRowsContainer.removeAllViews();
            percentInputs.clear();
            percentResidents.clear();

            if (!canSplit) {
                splitHintTv.setText("Con menos de 2 inquilinos, el único residente asume el 100%.");
                percentageRowsContainer.setVisibility(View.GONE);
                autoPercentPreviewTv.setVisibility(View.GONE);
                return;
            }

            boolean percentageMode = splitModeSpinner.getSelectedItemPosition() == 1;
            if (!percentageMode) {
                splitHintTv.setText("El alquiler se divide de forma equitativa entre todos.");
                percentageRowsContainer.setVisibility(View.GONE);
                autoPercentPreviewTv.setVisibility(View.GONE);
                return;
            }

            splitHintTv.setText("Modo porcentual: el último inquilino se calcula automáticamente con el porcentaje restante.");
            percentageRowsContainer.setVisibility(View.VISIBLE);
            autoPercentPreviewTv.setVisibility(View.VISIBLE);

            String autoResident = selectedResidents.get(selectedResidents.size() - 1);
            String autoResidentLabel = "Inquilino " + selectedResidents.size() + " (" + displayNameForEmail(autoResident) + ")";

            for (int i = 0; i < selectedResidents.size() - 1; i++) {
                String resident = selectedResidents.get(i);
                TextView residentLabel = new TextView(this);
                residentLabel.setText("Inquilino " + (i + 1) + " (" + displayNameForEmail(resident) + ") %:");
                residentLabel.setTextColor(getColor(R.color.text_light));
                residentLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                residentLabel.setPadding(0, i == 0 ? dp(4) : dp(8), 0, dp(4));
                percentageRowsContainer.addView(residentLabel);

                EditText percentEt = new EditText(this);
                percentEt.setBackgroundResource(R.drawable.bg_input_dark_round);
                percentEt.setTextColor(getColor(R.color.text_light));
                percentEt.setHintTextColor(getColor(R.color.text_muted));
                percentEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                percentEt.setHint("0 - 100");
                percentEt.setPadding(dp(14), dp(12), dp(14), dp(12));
                percentEt.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                Double persisted = draftPercentages.get(resident);
                if (persisted == null || persisted < 0.0) {
                    persisted = 100.0 / selectedResidents.size();
                }
                percentEt.setText(formatPercent(persisted));
                percentInputs.add(percentEt);
                percentResidents.add(resident);
                percentageRowsContainer.addView(percentEt);
            }

            Runnable updateAutoPreview = () -> {
                double sum = 0.0;
                for (int i = 0; i < percentInputs.size(); i++) {
                    double value = parsePercentInput(percentInputs.get(i).getText() == null ? "" : percentInputs.get(i).getText().toString());
                    if (value < 0.0) value = 0.0;
                    draftPercentages.put(percentResidents.get(i), value);
                    sum += value;
                }
                double remaining = 100.0 - sum;
                autoPercentPreviewTv.setText(autoResidentLabel + ": " + formatPercent(Math.max(0.0, remaining)) + "% (automático)");
                autoPercentPreviewTv.setTextColor(getColor(remaining < 0.0 ? R.color.status_danger : R.color.text_muted));
            };

            updateAutoPreview.run();
            for (EditText input : percentInputs) {
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        updateAutoPreview.run();
                    }
                });
            }
        };
        refreshSplitRef[0] = refreshSplit;

        residentsCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isBindingSlots[0] = true;
                rebuildSlots.run();
                isBindingSlots[0] = false;
                refreshSplit.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        splitModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshSplit.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        rebuildSlots.run();
        refreshSplit.run();

        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                title,
                subtitle,
                scroll,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            List<String> selectedResidents = new ArrayList<>();
            for (Spinner slotSpinner : slotSpinners) {
                int pos = slotSpinner.getSelectedItemPosition();
                if (pos < 0 || pos >= groupMembers.size()) continue;
                selectedResidents.add(groupMembers.get(pos));
            }

            Set<String> unique = new HashSet<>(selectedResidents);
            if (unique.size() != selectedResidents.size()) {
                NoticeUtils.show(this, "No repitas inquilinos en varias plazas");
                return;
            }

            String splitMode = normalizeRoomSplitMode(
                    splitModeSpinner.getSelectedItemPosition() == 1 ? ROOM_SPLIT_PERCENTAGE : ROOM_SPLIT_EQUAL
            );
            if (selectedResidents.size() < 2) {
                splitMode = ROOM_SPLIT_EQUAL;
            }

            Map<String, Object> splitPercentages = new LinkedHashMap<>();
            List<String> splitOrder = new ArrayList<>(selectedResidents);
            if (ROOM_SPLIT_PERCENTAGE.equals(splitMode)) {
                if (selectedResidents.isEmpty()) {
                    NoticeUtils.show(this, "No hay inquilinos para repartir");
                    return;
                }
                double sum = 0.0;
                for (int i = 0; i < percentInputs.size(); i++) {
                    double value = parsePercentInput(percentInputs.get(i).getText() == null ? "" : percentInputs.get(i).getText().toString());
                    if (value < 0.0) value = 0.0;
                    sum += value;
                    splitPercentages.put(percentResidents.get(i), round2(value));
                }
                if (sum > 100.0) {
                    NoticeUtils.show(this, "La suma de porcentajes no puede superar 100");
                    return;
                }
                String autoResident = selectedResidents.get(selectedResidents.size() - 1);
                splitPercentages.put(autoResident, round2(100.0 - sum));
            }

            onConfigured.onConfigured(new RoomResidentsConfigResult(
                    selectedResidents,
                    splitMode,
                    splitPercentages,
                    splitOrder
            ));
            dialog.dismiss();
        });
    }

    private List<String> buildOrderedMembers(List<String> currentMembers, List<String> persistedOrder) {
        List<String> normalizedMembers = castEmails(currentMembers);
        List<String> ordered = new ArrayList<>();
        for (String email : castEmails(persistedOrder)) {
            if (normalizedMembers.contains(email) && !ordered.contains(email)) {
                ordered.add(email);
            }
        }
        for (String email : normalizedMembers) {
            if (!ordered.contains(email)) {
                ordered.add(email);
            }
        }
        return ordered;
    }

    private void showSingleInputDialog(String title, String subtitle, String hint, int inputType, String actionLabel, SingleInputAction action) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null, false);
        TextView titleTv = form.findViewById(R.id.dialogTitleTv);
        TextView subtitleTv = form.findViewById(R.id.dialogSubtitleTv);
        TextView inputLabelTv = form.findViewById(R.id.dialogInputLabelTv);
        TextView closeXBtn = form.findViewById(R.id.dialogCloseXBtn);
        EditText inputEt = form.findViewById(R.id.dialogInputEt);
        Button cancelBtn = form.findViewById(R.id.dialogCancelBtn);
        Button confirmBtn = form.findViewById(R.id.dialogConfirmBtn);

        titleTv.setText(title);
        subtitleTv.setText(subtitle);
        inputLabelTv.setText(hint);
        inputEt.setHint("");
        inputEt.setInputType(inputType);
        confirmBtn.setText(actionLabel);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_FlatShare_Dialog)
                .setView(form)
                .create();

        closeXBtn.setOnClickListener(v -> dialog.dismiss());
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        confirmBtn.setOnClickListener(v -> {
            boolean close = action.onConfirm(inputEt.getText().toString());
            if (close) dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private View buildQrContent(String payload) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 8, 0, 0);

        ImageView qrImage = new ImageView(this);
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
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private List<String> castEmails(Object raw) {
        List<String> emails = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    emails.add(item.toString().toLowerCase(Locale.ROOT));
                }
            }
        }
        return emails;
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

    private Map<String, Double> castPercentages(Object raw) {
        Map<String, Double> percentages = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> mapRaw)) {
            return percentages;
        }
        for (Map.Entry<?, ?> entry : mapRaw.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String key = entry.getKey().toString().trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            double value;
            Object rawValue = entry.getValue();
            if (rawValue instanceof Number number) {
                value = number.doubleValue();
            } else {
                try {
                    value = Double.parseDouble(rawValue.toString().trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            if (value < 0.0) continue;
            percentages.put(key, round2(value));
        }
        return percentages;
    }

    private String normalizeRoomSplitMode(@Nullable String raw) {
        return ROOM_SPLIT_PERCENTAGE.equals(raw) ? ROOM_SPLIT_PERCENTAGE : ROOM_SPLIT_EQUAL;
    }

    private double parsePercentInput(String rawValue) {
        if (rawValue == null) return 0.0;
        String normalized = rawValue.trim().replace(',', '.');
        if (normalized.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String formatPercent(double value) {
        double rounded = round2(value);
        if (Math.abs(rounded - Math.rint(rounded)) < 0.01) {
            return String.valueOf((int) Math.rint(rounded));
        }
        return String.format(Locale.ROOT, "%.2f", rounded);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private ArrayAdapter<String> buildLightSpinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, values) {
            @Override
            public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(getColor(R.color.text_light));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(getColor(R.color.text_light));
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }

    private void resolveMemberDisplayNames(List<String> memberIds, List<String> memberEmails, Runnable onDone) {
        memberDisplayNamesByEmail.clear();
        for (String email : memberEmails) {
            if (email != null && !email.trim().isEmpty()) {
                memberDisplayNamesByEmail.put(email.toLowerCase(Locale.ROOT), email.toLowerCase(Locale.ROOT));
            }
        }
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : memberIds) {
            if (uid != null && !uid.trim().isEmpty()) {
                tasks.add(db.collection("users").document(uid).get());
            }
        }
        if (tasks.isEmpty()) {
            onDone.run();
            return;
        }
        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(done -> {
                    for (Task<DocumentSnapshot> task : tasks) {
                        if (!task.isSuccessful() || task.getResult() == null) continue;
                        DocumentSnapshot userDoc = task.getResult();
                        String email = userDoc.getString("email");
                        if (email == null || email.trim().isEmpty()) continue;
                        String displayName = userDoc.getString("name");
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = userDoc.getString("displayName");
                        }
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = userDoc.getString("username");
                        }
                        memberDisplayNamesByEmail.put(
                                email.toLowerCase(Locale.ROOT),
                                (displayName == null || displayName.trim().isEmpty()) ? email.toLowerCase(Locale.ROOT) : displayName.trim()
                        );
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private String displayNameForEmail(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) return "";
        String normalized = email.toLowerCase(Locale.ROOT);
        String displayName = memberDisplayNamesByEmail.get(normalized);
        return displayName == null || displayName.trim().isEmpty() ? normalized : displayName;
    }

    private interface SingleInputAction {
        boolean onConfirm(String value);
    }

    private interface RoomResidentsConfigCallback {
        void onConfigured(RoomResidentsConfigResult result);
    }

    private static class RoomResidentsConfigResult {
        final List<String> members;
        final String splitMode;
        final Map<String, Object> splitPercentages;
        final List<String> splitOrder;

        RoomResidentsConfigResult(
                List<String> members,
                String splitMode,
                Map<String, Object> splitPercentages,
                List<String> splitOrder
        ) {
            this.members = members;
            this.splitMode = splitMode;
            this.splitPercentages = splitPercentages;
            this.splitOrder = splitOrder;
        }
    }

    private static class RoomItem {
        final String id;
        final String name;
        final int roomNumber;
        final int capacity;
        final double monthlyCost;
        final List<String> memberEmails;
        final String rentSplitMode;
        final Map<String, Double> rentSplitPercentages;
        final List<String> rentSplitOrder;

        RoomItem(
                String id,
                String name,
                int roomNumber,
                int capacity,
                double monthlyCost,
                List<String> memberEmails,
                String rentSplitMode,
                Map<String, Double> rentSplitPercentages,
                List<String> rentSplitOrder
        ) {
            this.id = id;
            this.name = name;
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.monthlyCost = monthlyCost;
            this.memberEmails = memberEmails;
            this.rentSplitMode = rentSplitMode;
            this.rentSplitPercentages = rentSplitPercentages;
            this.rentSplitOrder = rentSplitOrder;
        }
    }

    private class RoomsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rooms.size();
        }

        @Override
        public Object getItem(int position) {
            return rooms.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room_row, parent, false);
            }

            RoomItem room = rooms.get(position);
            TextView roomNameTv = view.findViewById(R.id.roomNameTv);
            TextView roomMembersTv = view.findViewById(R.id.roomMembersTv);
            TextView roomMetaTv = view.findViewById(R.id.roomMetaTv);
            TextView roomCountBadgeTv = view.findViewById(R.id.roomCountBadgeTv);

            roomNameTv.setText(room.roomNumber > 0 ? "Hab. " + room.roomNumber + " - " + room.name : room.name);
            if (room.memberEmails.isEmpty()) {
                roomMembersTv.setText("Sin residentes asignados");
            } else {
                StringBuilder residentsText = new StringBuilder("Residentes:");
                int index = 1;
                for (String email : room.memberEmails) {
                    residentsText.append("\n").append(formatMemberNameAndEmail(email, index));
                    index++;
                }
                roomMembersTv.setText(residentsText.toString());
            }
            roomMetaTv.setText("Capacidad: " + room.capacity + " personas | Coste: " + String.format(Locale.ROOT, "%.2f EUR", room.monthlyCost));
            roomCountBadgeTv.setText(room.memberEmails.size() + " residentes");
            return view;
        }
    }

    private String formatMemberNameAndEmail(@Nullable String email, int index) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String name = displayNameForEmail(normalized);
        if (name == null || name.trim().isEmpty()) {
            name = normalized.isEmpty() ? "Sin datos" : normalized;
        }
        String emailLine = normalized.isEmpty() ? "sin correo" : normalized;
        return "Miembro " + index + ":\nNombre: " + name + "\nCorreo: " + emailLine;
    }
}

