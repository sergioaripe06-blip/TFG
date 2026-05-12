package com.sergio.flatshare.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.sergio.flatshare.util.DialogUtils;
import com.sergio.flatshare.util.SessionStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerRoomsActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "extra_group_id";
    public static final String EXTRA_OPEN_WORKSPACE = "open_workspace";

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
            Toast.makeText(this, "No se encontró el piso", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "El piso no existe", Toast.LENGTH_SHORT).show();
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
                        rooms.add(new RoomItem(
                                doc.getId(),
                                name == null || name.trim().isEmpty() ? "Habitación" : name,
                                roomNumber == null ? 0 : roomNumber.intValue(),
                                capacity == null ? 0 : capacity.intValue(),
                                monthlyCost == null ? 0.0 : monthlyCost,
                                castEmails(doc.get("memberEmails"))
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
        roomNameEt.setHint("Nombre de la habitación:");

        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "Nueva habitación",
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
                Toast.makeText(this, "Completa todos los datos de la habitación", Toast.LENGTH_SHORT).show();
                return;
            }

            int roomNumber = rooms.size() + 1;
            int capacity;
            double monthlyCost;
            try {
                capacity = Integer.parseInt(capacityText);
                monthlyCost = Double.parseDouble(costText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Capacidad o coste no válidos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (capacity <= 0) {
                Toast.makeText(this, "La capacidad debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                return;
            }
            if (monthlyCost < 0) {
                Toast.makeText(this, "El coste no puede ser negativo", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Map<String, Object> room = new HashMap<>();
            room.put("groupId", groupId);
            room.put("roomNumber", roomNumber);
            room.put("name", roomName);
            room.put("capacity", capacity);
            room.put("monthlyCost", monthlyCost);
            room.put("memberEmails", new ArrayList<String>());
            room.put("memberCount", 0);
            room.put("createdByUid", uid);
            room.put("updatedByUid", uid);
            room.put("createdAt", FieldValue.serverTimestamp());
            room.put("updatedAt", FieldValue.serverTimestamp());

            db.collection("rooms_groups")
                    .add(room)
                    .addOnSuccessListener(v2 -> {
                        Toast.makeText(this, "Habitación creada", Toast.LENGTH_SHORT).show();
                        loadRooms();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "No se pudo crear", Toast.LENGTH_SHORT).show());
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
        if (groupMembers.isEmpty()) {
            Toast.makeText(this, "No hay residentes en el piso todavía", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] memberItems = new String[groupMembers.size()];
        boolean[] checkedItems = new boolean[groupMembers.size()];
        for (int i = 0; i < groupMembers.size(); i++) {
            memberItems[i] = displayNameForEmail(groupMembers.get(i));
            checkedItems[i] = room.memberEmails.contains(groupMembers.get(i));
        }

        new AlertDialog.Builder(this, R.style.ThemeOverlay_FlatShare_Dialog)
                .setTitle("Asignar residentes")
                .setMultiChoiceItems(memberItems, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selected.add(groupMembers.get(i));
                        }
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("memberEmails", selected);
                    updates.put("memberCount", selected.size());
                    updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    updates.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("rooms_groups").document(room.id)
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Habitación actualizada", Toast.LENGTH_SHORT).show();
                                loadRooms();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show());
                })
                .show();
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
                        Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", newName);
                    updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    updates.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("rooms_groups").document(room.id)
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show();
                                loadRooms();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show());
                    return true;
                }
        );
    }

    private void deleteRoomDialog(RoomItem room) {
        View content = DialogUtils.createMessageView(this, "Esta acción eliminará la habitación y su asignación de residentes.");
        DialogUtils.Shell shell = DialogUtils.buildShell(
                this,
                "Eliminar habitación",
                room.name,
                content,
                "Cancelar",
                "Eliminar"
        );
        AlertDialog dialog = DialogUtils.show(this, shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            db.collection("rooms_groups").document(room.id)
                    .delete()
                    .addOnSuccessListener(v2 -> {
                        Toast.makeText(this, "Habitación eliminada", Toast.LENGTH_SHORT).show();
                        loadRooms();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show());
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
                "Invitar al piso",
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
                "Comparte este QR para invitar residentes.",
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
                "Invitar por email",
                "Envía una invitación directa por correo.",
                "Email del invitado:",
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                "Enviar",
                value -> {
                    String invitedEmail = value.trim().toLowerCase(Locale.ROOT);
                    if (invitedEmail.isEmpty()) {
                        Toast.makeText(this, "Debes indicar un email", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Map<String, Object> invitation = new HashMap<>();
                    invitation.put("groupId", groupId);
                    invitation.put("invitedEmail", invitedEmail);
                    invitation.put("inviterUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    invitation.put("status", "pending");
                    invitation.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("invitations").add(invitation)
                            .addOnSuccessListener(v -> Toast.makeText(this, "Invitación enviada", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "No se pudo enviar", Toast.LENGTH_SHORT).show());
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

    private void showSingleInputDialog(String title, String subtitle, String hint, int inputType, String actionLabel, SingleInputAction action) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_single_input, null, false);
        TextView titleTv = form.findViewById(R.id.dialogTitleTv);
        TextView subtitleTv = form.findViewById(R.id.dialogSubtitleTv);
        TextView inputLabelTv = form.findViewById(R.id.dialogInputLabelTv);
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
                        String displayName = userDoc.getString("displayName");
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

    private static class RoomItem {
        final String id;
        final String name;
        final int roomNumber;
        final int capacity;
        final double monthlyCost;
        final List<String> memberEmails;

        RoomItem(String id, String name, int roomNumber, int capacity, double monthlyCost, List<String> memberEmails) {
            this.id = id;
            this.name = name;
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.monthlyCost = monthlyCost;
            this.memberEmails = memberEmails;
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
                List<String> labels = new ArrayList<>();
                for (String email : room.memberEmails) {
                    labels.add(displayNameForEmail(email));
                }
                roomMembersTv.setText("Residentes: " + String.join(", ", labels));
            }
            roomMetaTv.setText("Capacidad: " + room.capacity + " personas | Coste: " + String.format(Locale.ROOT, "%.2f EUR", room.monthlyCost));
            roomCountBadgeTv.setText(room.memberEmails.size() + " residentes");
            return view;
        }
    }
}
