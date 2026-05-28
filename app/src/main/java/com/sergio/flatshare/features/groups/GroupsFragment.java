package com.sergio.flatshare.features.groups;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
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
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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
import com.sergio.flatshare.features.groups.services.GroupService;
import com.sergio.flatshare.features.groups.services.InitialRoomsSetupFlow;
import com.sergio.flatshare.features.groups.services.InvitationService;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.sound.AppSoundFx;
import com.sergio.flatshare.features.shell.MainActivity;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;

public class GroupsFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final GroupService groupService = new GroupService(db);
    private final InvitationService invitationService = new InvitationService(db);
    private final InitialRoomsSetupFlow initialRoomsSetupFlow = new InitialRoomsSetupFlow(db);
    private final List<GroupItem> groups = new ArrayList<>();
    private final List<GroupItem> allGroups = new ArrayList<>();
    private final SparseBooleanArray animatedPositions = new SparseBooleanArray();
    private static final LinkedHashMap<String, List<String>> PROVINCE_CITIES = buildProvinceCityMap();
    private static final String RENT_MODE_FIXED = "fixed";
    private static final String RENT_MODE_VARIABLE = "variable";
    private static final String VARIABLE_SPLIT_EQUAL = "equal";
    private static final String VARIABLE_SPLIT_PERCENTAGE = "percentage";

    private GroupsAdapter adapter;
    private View detailsCard;
    private TextView emptyGroupsTv;
    private TextView detailNameTv;
    private TextView detailDescTv;
    private TextView detailMembersTv;
    private TextView totalGroupsTv;
    private TextView totalMembersTv;
    private Button deleteGroupBtn;
    private Button editGroupBtn;

    private String selectedGroupId;
    private String currentGroupSearchQuery = "";
    private boolean selectedGroupIsOwner = false;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (!isAdded()) return;
                if (result == null || result.getContents() == null) return;
                String raw = result.getContents().trim();
                String code = extractGroupCode(raw);
                if (code.isEmpty()) {
                    NoticeUtils.show(requireContext(), "QR no válido para unirse al piso");
                    return;
                }
                joinGroupByCode(code);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        ListView listView = view.findViewById(R.id.groupsLv);
        EditText groupsSearchEt = view.findViewById(R.id.groupsSearchEt);
        Button addGroupBtn = view.findViewById(R.id.addGroupBtn);
        Button joinGroupBtn = view.findViewById(R.id.joinGroupBtn);
        detailsCard = view.findViewById(R.id.groupDetailsCard);
        emptyGroupsTv = view.findViewById(R.id.emptyGroupsTv);
        totalGroupsTv = view.findViewById(R.id.totalGroupsTv);
        totalMembersTv = view.findViewById(R.id.totalMembersTv);
        detailNameTv = view.findViewById(R.id.detailGroupNameTv);
        detailDescTv = view.findViewById(R.id.detailGroupDescTv);
        detailMembersTv = view.findViewById(R.id.detailGroupMembersTv);
        deleteGroupBtn = view.findViewById(R.id.deleteGroupBtn);
        editGroupBtn = view.findViewById(R.id.editGroupBtn);
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
            GroupItem selected = groups.get(position);
            selectedGroupId = selected.id;
            showGroupLongPressActions(selected);
            return true;
        });

        groupsSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentGroupSearchQuery = s == null ? "" : s.toString().trim();
                applyGroupSearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editGroupBtn.setOnClickListener(v -> editSelectedGroup());
        inviteBtn.setOnClickListener(v -> showInviteOptionsDialog(selectedGroupId));
        deleteGroupBtn.setOnClickListener(v -> requestDeleteSelectedGroup());
        closeBtn.setOnClickListener(v -> {
            selectedGroupId = null;
            selectedGroupIsOwner = false;
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
        LinearLayout stepOneContainer = form.findViewById(R.id.createGroupStepOneContainer);
        LinearLayout stepTwoContainer = form.findViewById(R.id.createGroupStepTwoContainer);
        TextView stepIndicatorTv = form.findViewById(R.id.createGroupStepIndicatorTv);
        EditText groupNameEt = form.findViewById(R.id.groupNameEt);
        EditText streetEt = form.findViewById(R.id.streetEt);
        EditText portalEt = form.findViewById(R.id.portalEt);
        EditText numberEt = form.findViewById(R.id.numberEt);
        EditText postalCodeEt = form.findViewById(R.id.postalCodeEt);
        AutoCompleteTextView provinceInput = form.findViewById(R.id.provinceInput);
        EditText cityInput = form.findViewById(R.id.cityInput);

        EditText roomCountEt = form.findViewById(R.id.roomCountEt);
        TextView variableSplitLabelTv = form.findViewById(R.id.variableSplitLabelTv);
        Spinner rentModeSpinner = form.findViewById(R.id.rentModeSpinner);
        Spinner variableSplitSpinner = form.findViewById(R.id.variableSplitSpinner);

        setupProvinceAutocomplete(provinceInput);
        setupRentModeSpinners(rentModeSpinner, variableSplitSpinner, variableSplitLabelTv);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nuevo piso",
                "Paso 1 de 2: datos del piso.",
                form,
                "Cancelar",
                "Siguiente"
        );

        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        final boolean[] inStepTwo = {false};

        Runnable showStepOne = () -> {
            inStepTwo[0] = false;
            stepOneContainer.setVisibility(View.VISIBLE);
            stepTwoContainer.setVisibility(View.GONE);
            stepIndicatorTv.setText("Paso 1 de 2 - Datos básicos");
            shell.cancelBtn.setText("Cancelar");
            shell.confirmBtn.setText("Siguiente");
        };

        Runnable showStepTwo = () -> {
            inStepTwo[0] = true;
            stepOneContainer.setVisibility(View.GONE);
            stepTwoContainer.setVisibility(View.VISIBLE);
            stepIndicatorTv.setText("Paso 2 de 2 - Configuración");
            shell.cancelBtn.setText("Atrás");
            shell.confirmBtn.setText("Crear");
        };

        shell.cancelBtn.setOnClickListener(v -> {
            if (inStepTwo[0]) {
                showStepOne.run();
            } else {
                dialog.dismiss();
            }
        });

        shell.confirmBtn.setOnClickListener(v -> {
            if (!inStepTwo[0]) {
                String name = groupNameEt.getText().toString().trim();
                String street = streetEt.getText().toString().trim();
                String portal = portalEt.getText().toString().trim();
                String number = numberEt.getText().toString().trim();
                String postalCode = postalCodeEt.getText().toString().trim();
                String province = getSelectedProvinceValue(provinceInput);
                String city = getInputValue(cityInput);

                if (name.isEmpty() || street.isEmpty() || portal.isEmpty() || number.isEmpty() || postalCode.isEmpty() || city.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Completa todos los datos del piso");
                    return;
                }
                if (province.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Indica una provincia");
                    return;
                }
                showStepTwo.run();
                return;
            }

            String name = groupNameEt.getText().toString().trim();
            String street = streetEt.getText().toString().trim();
            String portal = portalEt.getText().toString().trim();
            String number = numberEt.getText().toString().trim();
            String postalCode = postalCodeEt.getText().toString().trim();
            String province = getSelectedProvinceValue(provinceInput);
            String city = getInputValue(cityInput);
            String roomCountText = roomCountEt.getText().toString().trim();
            int rentModeSelection = rentModeSpinner.getSelectedItemPosition();
            int splitModeSelection = variableSplitSpinner.getSelectedItemPosition();

            if (roomCountText.isEmpty()) {
                NoticeUtils.show(requireContext(), "Indica el número de habitaciones");
                return;
            }
            if (rentModeSelection <= 0) {
                NoticeUtils.show(requireContext(), "Selecciona el tipo de alquiler");
                return;
            }
            if (rentModeSelection == 2 && splitModeSelection <= 0) {
                NoticeUtils.show(requireContext(), "Selecciona el reparto del alquiler variable");
                return;
            }

            int roomCount;
            try {
                roomCount = Integer.parseInt(roomCountText);
            } catch (NumberFormatException e) {
                NoticeUtils.show(requireContext(), "El número de habitaciones no es válido");
                return;
            }
            if (roomCount <= 0) {
                NoticeUtils.show(requireContext(), "Debe haber al menos una habitación");
                return;
            }

            String rentMode = rentModeSelection == 1 ? RENT_MODE_FIXED : RENT_MODE_VARIABLE;
            String variableSplitMode = splitModeSelection == 2 ? VARIABLE_SPLIT_PERCENTAGE : VARIABLE_SPLIT_EQUAL;

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
            group.put("billingModel", rentMode);
            group.put("variableSplitMode", variableSplitMode);
            group.put("createdAt", FieldValue.serverTimestamp());

            groupService.createGroup(
                    group,
                    uid,
                    name,
                    groupId -> {
                selectedGroupId = groupId;
                SessionStore.setCurrentGroup(requireContext(), selectedGroupId);
                loadGroups();
                dialog.dismiss();
                AppSoundFx.playByName(requireContext(), AppSoundFx.FX_GROUP_CREATED);
                NoticeUtils.show(requireContext(), "Piso creado. Define las habitaciones.");
                startInitialRoomsSetup(selectedGroupId, roomCount);
                    },
                    error -> Toast.makeText(requireContext(), "Error creando piso: " + error, Toast.LENGTH_LONG).show()
            );
        });

        showStepOne.run();
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
        roomNameEt.setHint("Nombre de la habitación " + currentNumber + ":");

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Habitación " + currentNumber + " de " + totalRooms,
                "Completa esta habitación para continuar con la siguiente.",
                form,
                "Cerrar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        shell.cancelBtn.setOnClickListener(v -> {
            View content = DialogUtils.createMessageView(
                    requireContext(),
                    "Si sales ahora, se eliminará el piso completo porque no se han terminado las habitaciones."
            );
            DialogUtils.Shell confirmShell = DialogUtils.buildShell(
                    requireContext(),
                    "Cancelar creación del piso",
                    "Esta acción no se puede deshacer.",
                    content,
                    "Volver",
                    "Eliminar piso"
            );
            AlertDialog confirmDialog = DialogUtils.show(requireContext(), confirmShell.root);
            confirmShell.cancelBtn.setOnClickListener(v2 -> confirmDialog.dismiss());
            confirmShell.confirmBtn.setOnClickListener(v2 -> {
                confirmDialog.dismiss();
                dialog.dismiss();
                deleteGroupCascade(groupId);
                NoticeUtils.show(requireContext(), "Se canceló la creación y se eliminó el piso");
            });
        });
        shell.confirmBtn.setOnClickListener(v -> {
            String name = roomNameEt.getText().toString().trim();
            String capacityText = roomCapacityEt.getText().toString().trim();
            String costText = roomCostEt.getText().toString().trim();
            if (name.isEmpty() || capacityText.isEmpty() || costText.isEmpty()) {
                NoticeUtils.show(requireContext(), "Completa todos los campos de la habitación");
                return;
            }
            int capacity;
            double monthlyCost;
            try {
                capacity = Integer.parseInt(capacityText);
                monthlyCost = Double.parseDouble(costText);
            } catch (NumberFormatException e) {
                NoticeUtils.show(requireContext(), "Capacidad o coste no válidos");
                return;
            }
            if (capacity <= 0) {
                NoticeUtils.show(requireContext(), "La capacidad debe ser mayor que 0");
                return;
            }
            if (monthlyCost < 0) {
                NoticeUtils.show(requireContext(), "El coste no puede ser negativo");
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
        List<InitialRoomsSetupFlow.RoomDraftInput> inputs = new ArrayList<>();
        for (RoomDraft draft : drafts) {
            inputs.add(new InitialRoomsSetupFlow.RoomDraftInput(
                    draft.roomNumber,
                    draft.name,
                    draft.capacity,
                    draft.monthlyCost
            ));
        }
        initialRoomsSetupFlow.saveInitialRooms(
                groupId,
                uid,
                inputs,
                () -> {
                    NoticeUtils.show(requireContext(), "Habitaciones guardadas");
                    openCurrentGroupWorkspace();
                },
                error -> {
                    NoticeUtils.show(requireContext(), "No se pudieron guardar todas las habitaciones: " + error);
                    openCurrentGroupWorkspace();
                }
        );
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
                    "Código del piso:",
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
        TextView closeXBtn = form.findViewById(R.id.dialogCloseXBtn);
        Button cancelBtn = form.findViewById(R.id.dialogCancelBtn);
        Button confirmBtn = form.findViewById(R.id.dialogConfirmBtn);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_FlatShare_Dialog)
                .setView(form)
                .create();

        closeXBtn.setOnClickListener(v -> dialog.dismiss());
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
        TextView inputLabelTv = form.findViewById(R.id.dialogInputLabelTv);
        EditText inputEt = form.findViewById(R.id.dialogInputEt);
        Button confirmBtn = form.findViewById(R.id.dialogConfirmBtn);

        titleTv.setText(title);
        subtitleTv.setText(subtitle);
        inputLabelTv.setText(hint);
        inputEt.setHint("");
        inputEt.setInputType(inputType);
        confirmBtn.setText(actionLabel);
        return form;
    }

    private void joinGroupByCode(String code) {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        groupService.joinGroupByCode(
                code,
                uid,
                email,
                groupId -> {
                    selectedGroupId = groupId;
                    SessionStore.setCurrentGroup(requireContext(), groupId);
                    SessionStore.clearCurrentRoom(requireContext());
                    loadGroups();
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
                    }
                },
                error -> Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        );
    }

    private void showInviteOptionsDialog(String groupId) {
        if (groupId == null) {
            NoticeUtils.show(requireContext(), "Selecciona un grupo primero");
            return;
        }
        View content = DialogUtils.createVerticalActions(requireContext());
        Button codeBtn = DialogUtils.createActionButton(requireContext(), "Añadir inquilinos por código", true);
        Button qrBtn = DialogUtils.createActionButton(requireContext(), "Añadir inquilinos por QR", false);
        Button emailBtn = DialogUtils.createActionButton(requireContext(), "Añadir inquilinos por email", false);
        ((LinearLayout) content).addView(codeBtn);
        ((LinearLayout) content).addView(qrBtn);
        ((LinearLayout) content).addView(emailBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Añadir inquilinos",
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
                "Añadir inquilinos por email",
                "Comparte el piso por correo con otra persona.",
                "Email del invitado:",
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                "Enviar",
                value -> {
                    String invitedEmail = value.trim().toLowerCase(Locale.ROOT);
                    if (invitedEmail.isEmpty() || !invitedEmail.contains("@")) {
                        NoticeUtils.show(requireContext(), "Email invalido");
                        return false;
                    }

                    String inviterUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String inviterEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail() == null
                            ? ""
                            : FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);

                    invitationService.createEmailInvitation(
                            groupId,
                            invitedEmail,
                            inviterUid,
                            inviterEmail,
                            () -> Toast.makeText(requireContext(), "Invitacion creada", Toast.LENGTH_SHORT).show(),
                            error -> Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    );
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
                    "Comparte este QR para añadir a otra persona.",
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
                    showInvitationDecision(res.getDocuments(), 0);
                });
    }

    private void showInvitationDecision(List<DocumentSnapshot> pendingInvitations, int index) {
        if (!isAdded() || index >= pendingInvitations.size()) return;
        DocumentSnapshot invitation = pendingInvitations.get(index);
        String invitationId = invitation.getId();
        String groupId = stringValue(invitation.get("groupId"));
        String invitationGroupName = stringValue(invitation.get("groupName"));
        String invitationShareCode = stringValue(invitation.get("shareCode")).toUpperCase(Locale.ROOT);
        String inviterEmail = stringValue(invitation.get("inviterEmail")).toLowerCase(Locale.ROOT);

        if (groupId.isEmpty()) {
            db.collection("invitations").document(invitationId).update("status", "rejected")
                    .addOnCompleteListener(t -> showInvitationDecision(pendingInvitations, index + 1));
            return;
        }

        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            if (!isAdded()) return;

            String groupName = invitationGroupName.isEmpty() ? "Piso" : invitationGroupName;
            String shareCode = invitationShareCode;
            String ownerLabel = "Sin datos";
            String locationLabel = "Sin ubicacion";
            List<String> memberIds = new ArrayList<>();
            List<String> memberEmails = new ArrayList<>();

            if (groupDoc.exists()) {
                String liveName = stringValue(groupDoc.getString("name"));
                if (!liveName.isEmpty()) groupName = liveName;

                String liveCode = stringValue(groupDoc.getString("shareCode")).toUpperCase(Locale.ROOT);
                if (!liveCode.isEmpty()) {
                    shareCode = liveCode;
                } else if (shareCode.isEmpty()) {
                    shareCode = groupId.toUpperCase(Locale.ROOT);
                }

                memberIds = castStrings(groupDoc.get("members"));
                memberEmails = castStrings(groupDoc.get("memberEmails"));
                String resolvedOwner = resolveOwnerEmail(groupDoc, memberIds, memberEmails);
                if (!resolvedOwner.isEmpty()) ownerLabel = resolvedOwner;
                locationLabel = buildLocationLabel(groupDoc);
            } else if (shareCode.isEmpty()) {
                shareCode = groupId.toUpperCase(Locale.ROOT);
            }

            String invitedByLabel = inviterEmail.isEmpty() ? "Sin datos" : inviterEmail;
            final String finalGroupName = groupName;
            final String finalOwnerLabel = ownerLabel;
            final String finalLocationLabel = locationLabel;
            final String finalShareCode = shareCode;
            final List<String> finalMemberIds = new ArrayList<>(memberIds);
            final List<String> finalMemberEmails = new ArrayList<>(memberEmails);

            if (finalMemberEmails.isEmpty()) {
                openInvitationDecisionDialog(
                        invitationId,
                        groupId,
                        pendingInvitations,
                        index,
                        finalGroupName,
                        finalOwnerLabel,
                        "Sin datos",
                        finalLocationLabel,
                        finalShareCode,
                        invitedByLabel
                );
                return;
            }

            resolveMemberDisplayNames(finalMemberIds, finalMemberEmails, labels -> {
                if (!isAdded()) return;
                String membersLabel = buildDetailedMembersLabel(labels, finalMemberEmails);
                String resolvedOwnerLabel = buildOwnerDetailedLabel(finalOwnerLabel, labels, finalMemberEmails);
                openInvitationDecisionDialog(
                        invitationId,
                        groupId,
                        pendingInvitations,
                        index,
                        finalGroupName,
                        resolvedOwnerLabel,
                        membersLabel,
                        finalLocationLabel,
                        finalShareCode,
                        invitedByLabel
                );
            });
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            NoticeUtils.show(requireContext(), "No se pudo leer la invitacion");
            showInvitationDecision(pendingInvitations, index + 1);
        });
    }

    private void openInvitationDecisionDialog(
            String invitationId,
            String groupId,
            List<DocumentSnapshot> pendingInvitations,
            int index,
            String groupName,
            String ownerLabel,
            String membersLabel,
            String locationLabel,
            String shareCode,
            String invitedByLabel
    ) {
        String details = "Piso: " + groupName
                + "\nPropietario: " + ownerLabel
                + "\nMiembros:\n" + membersLabel
                + "\nUbicación: " + locationLabel
                + "\nCódigo: " + shareCode
                + "\nInvitado por: " + invitedByLabel;

        View content = DialogUtils.createMessageView(requireContext(), details);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Invitacion pendiente",
                "Te han invitado a este piso. Quieres unirte ahora?",
                content,
                "Rechazar",
                "Unirme"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> {
            invitationService.rejectInvitation(
                    invitationId,
                    () -> {
                        dialog.dismiss();
                        showInvitationDecision(pendingInvitations, index + 1);
                    },
                    error -> {
                        dialog.dismiss();
                        showInvitationDecision(pendingInvitations, index + 1);
                    }
            );
        });
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            acceptInvitation(invitationId, groupId);
        });
    }

    private void acceptInvitation(String invitationId, String groupId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        invitationService.acceptInvitation(
                invitationId,
                groupId,
                uid,
                email,
                () -> {
                    SessionStore.setCurrentGroup(requireContext(), groupId);
                    SessionStore.clearCurrentRoom(requireContext());
                    loadGroups();
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).openCurrentGroupWorkspace();
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    NoticeUtils.show(requireContext(), "No se pudo aceptar la invitacion");
                }
        );
    }

    private String buildDetailedMembersLabel(List<String> displayNames, List<String> memberEmails) {
        if (memberEmails == null || memberEmails.isEmpty()) {
            return "Sin datos";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < memberEmails.size(); i++) {
            String email = stringValue(memberEmails.get(i)).toLowerCase(Locale.ROOT);
            if (email.isEmpty()) continue;
            String name = email;
            if (displayNames != null && i < displayNames.size()) {
                String candidate = stringValue(displayNames.get(i));
                if (!candidate.isEmpty()) name = candidate;
            }
            if (out.length() > 0) out.append("\n");
            out.append("Miembro ").append(i + 1).append(":");
            out.append("\nNombre: ").append(name);
            out.append("\nCorreo: ").append(email);
        }
        return out.length() == 0 ? "Sin datos" : out.toString();
    }
    private void loadGroups() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        groupService.loadGroupsForUser(uid, (loadedGroups, membersTotal) -> {
            allGroups.clear();
            groups.clear();
            animatedPositions.clear();
            for (GroupService.GroupSummary group : loadedGroups) {
                allGroups.add(new GroupItem(
                        group.id,
                        group.name,
                        group.members,
                        group.description,
                        group.isOwner
                ));
            }
            applyGroupSearchFilter();
            totalGroupsTv.setText(String.valueOf(allGroups.size()));
            totalMembersTv.setText(String.valueOf(membersTotal));
        }, error -> {
            if (!isAdded()) return;
            NoticeUtils.show(requireContext(), "Error cargando pisos: " + error);
            if (emptyGroupsTv != null) emptyGroupsTv.setVisibility(View.VISIBLE);
        });
    }

    private void applyGroupSearchFilter() {
        groups.clear();
        String query = currentGroupSearchQuery == null ? "" : currentGroupSearchQuery.trim().toLowerCase(Locale.ROOT);
        for (GroupItem group : allGroups) {
            if (matchesGroupSearch(group, query)) {
                groups.add(group);
            }
        }

        animatedPositions.clear();
        adapter.notifyDataSetChanged();

        if (emptyGroupsTv != null) {
            if (allGroups.isEmpty()) {
                emptyGroupsTv.setText("Aun no tienes pisos. Pulsa Crear piso.");
                emptyGroupsTv.setVisibility(View.VISIBLE);
            } else if (!query.isEmpty() && groups.isEmpty()) {
                emptyGroupsTv.setText("No hay pisos que coincidan con la busqueda.");
                emptyGroupsTv.setVisibility(View.VISIBLE);
            } else {
                emptyGroupsTv.setVisibility(View.GONE);
            }
        }

        if (selectedGroupId != null && detailsCard != null && detailsCard.getVisibility() == View.VISIBLE) {
            boolean visibleInFiltered = false;
            for (GroupItem item : groups) {
                if (selectedGroupId.equals(item.id)) {
                    visibleInFiltered = true;
                    break;
                }
            }
            if (!visibleInFiltered) {
                selectedGroupId = null;
                selectedGroupIsOwner = false;
                detailsCard.setVisibility(View.GONE);
            }
        }
    }

    private boolean matchesGroupSearch(GroupItem group, String query) {
        if (query == null || query.isEmpty()) return true;
        String name = stringValue(group.name).toLowerCase(Locale.ROOT);
        String description = stringValue(group.description).toLowerCase(Locale.ROOT);
        return name.contains(query) || description.contains(query);
    }

    private void loadGroupDetails(String groupId) {
        if (groupId == null) return;
        db.collection("groups").document(groupId).get().addOnSuccessListener(this::renderGroupDetails);
    }

    private void showGroupLongPressActions(GroupItem group) {
        View content = DialogUtils.createVerticalActions(requireContext());
        Button detailsBtn = DialogUtils.createActionButton(requireContext(), "Ver detalles", true);
        ((LinearLayout) content).addView(detailsBtn);
        Button secondaryBtn;
        if (group.isOwner) {
            secondaryBtn = DialogUtils.createActionButton(requireContext(), "Expulsar inquilino", false);
        } else {
            secondaryBtn = DialogUtils.createActionButton(requireContext(), "Salir del piso", false);
        }
        ((LinearLayout) content).addView(secondaryBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                group.name,
                "Elige una acción para este piso.",
                content,
                "Cancelar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        detailsBtn.setOnClickListener(v -> {
            dialog.dismiss();
            loadGroupDetails(group.id);
        });
        secondaryBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (group.isOwner) {
                showKickMemberDialog(group);
            } else {
                requestLeaveGroup(group);
            }
        });
    }

    private void requestLeaveGroup(GroupItem group) {
        String message = "Vas a salir de este piso. Perderas acceso a gastos, pagos y habitaciones de este grupo.";
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Salir del piso",
                "Confirmacion",
                DialogUtils.createMessageView(requireContext(), message),
                "Cancelar",
                "Salir"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            leaveGroup(group.id);
        });
    }

    @SuppressWarnings("unchecked")
    private void leaveGroup(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email == null || email.trim().isEmpty()) {
            NoticeUtils.show(requireContext(), "No se pudo obtener tu correo.");
            return;
        }
        final String emailLc = email.trim().toLowerCase(Locale.ROOT);

        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            if (!isAdded()) return;
            if (!groupDoc.exists()) {
                NoticeUtils.show(requireContext(), "El piso ya no existe.");
                return;
            }
            String ownerId = stringValue(groupDoc.getString("ownerId"));
            if (uid.equals(ownerId)) {
                NoticeUtils.show(requireContext(), "El propietario no puede salir desde aqui.");
                return;
            }

            List<String> members = castStrings(groupDoc.get("members"));
            List<String> memberEmails = castStrings(groupDoc.get("memberEmails"));
            Map<String, Object> roles = groupDoc.get("roles") instanceof Map
                    ? new HashMap<>((Map<String, Object>) groupDoc.get("roles"))
                    : new HashMap<>();

            members.remove(uid);
            memberEmails.removeIf(value -> emailLc.equals(stringValue(value).toLowerCase(Locale.ROOT)));
            roles.remove(uid);

            Map<String, Object> updates = new HashMap<>();
            updates.put("members", members);
            updates.put("memberEmails", memberEmails);
            updates.put("roles", roles);

            db.collection("groups").document(groupId).update(updates)
                    .addOnSuccessListener(v -> {
                        removeMemberFromGroupRooms(groupId, emailLc, () -> {
                            if (!isAdded()) return;
                            String currentGroup = SessionStore.getCurrentGroup(requireContext());
                            if (groupId.equals(currentGroup)) {
                                SessionStore.clearCurrentGroup(requireContext());
                                SessionStore.clearCurrentRoom(requireContext());
                            }
                            selectedGroupId = null;
                            selectedGroupIsOwner = false;
                            detailsCard.setVisibility(View.GONE);
                            NoticeUtils.show(requireContext(), "Has salido del piso.");
                            loadGroups();
                        });
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        NoticeUtils.show(requireContext(), "No se pudo salir del piso: " + e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            NoticeUtils.show(requireContext(), "No se pudo cargar el piso: " + e.getMessage());
        });
    }

    @SuppressWarnings("unchecked")
    private void showKickMemberDialog(GroupItem group) {
        if (group == null || group.id == null) return;
        db.collection("groups").document(group.id).get().addOnSuccessListener(groupDoc -> {
            if (!isAdded()) return;
            if (!groupDoc.exists()) {
                NoticeUtils.show(requireContext(), "El piso ya no existe.");
                return;
            }

            String ownerId = stringValue(groupDoc.getString("ownerId"));
            List<String> members = castStrings(groupDoc.get("members"));
            List<String> memberEmails = castStrings(groupDoc.get("memberEmails"));
            resolveMemberDisplayNames(members, memberEmails, labels -> {
                if (!isAdded()) return;
                int size = Math.min(members.size(), memberEmails.size());
                List<KickCandidate> candidates = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    String memberUid = stringValue(members.get(i));
                    String memberEmail = stringValue(memberEmails.get(i)).toLowerCase(Locale.ROOT);
                    if (memberUid.isEmpty() || memberUid.equals(ownerId)) continue;
                    String memberName = i < labels.size() ? stringValue(labels.get(i)) : "";
                    if (memberName.isEmpty() || memberName.equalsIgnoreCase(memberEmail)) {
                        memberName = fallbackMemberName(memberEmail);
                    }
                    if (memberName.isEmpty()) memberName = "Sin datos";
                    candidates.add(new KickCandidate(memberUid, memberEmail, memberName));
                }

                if (candidates.isEmpty()) {
                    NoticeUtils.show(requireContext(), "No hay inquilinos para expulsar.");
                    return;
                }

                View content = DialogUtils.createVerticalActions(requireContext());
                final AlertDialog[] pickerDialogRef = new AlertDialog[1];
                for (int i = 0; i < candidates.size(); i++) {
                    final int idx = i;
                    KickCandidate candidate = candidates.get(i);
                    Button actionBtn = DialogUtils.createActionButton(
                            requireContext(),
                            formatMemberTwoLines(candidate.displayName, candidate.email),
                            i == 0
                    );
                    actionBtn.setSingleLine(false);
                    actionBtn.setMaxLines(3);
                    actionBtn.setMinHeight(dp(56));
                    actionBtn.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    actionBtn.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                    ViewGroup.LayoutParams params = actionBtn.getLayoutParams();
                    if (params != null) {
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        actionBtn.setLayoutParams(params);
                    }
                    actionBtn.setPadding(dp(14), dp(10), dp(14), dp(10));
                    actionBtn.setOnClickListener(v -> {
                        if (pickerDialogRef[0] != null) pickerDialogRef[0].dismiss();
                        KickCandidate target = candidates.get(idx);
                        requestKickMember(group.id, target.uid, target.displayName, target.email);
                    });
                    ((LinearLayout) content).addView(actionBtn);
                }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Expulsar inquilino",
                    "Selecciona a quién quieres expulsar.",
                    content,
                    "Cancelar",
                    null
            );
            AlertDialog pickerDialog = DialogUtils.show(requireContext(), shell.root);
            pickerDialogRef[0] = pickerDialog;
            shell.cancelBtn.setOnClickListener(v -> pickerDialog.dismiss());
            });
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            NoticeUtils.show(requireContext(), "No se pudo cargar el piso: " + e.getMessage());
        });
    }

    @SuppressWarnings("unchecked")
    private void requestKickMember(String groupId, String memberUid, String memberName, String memberEmail) {
        String message = "Se expulsara a:\n"
                + memberName
                + "\n"
                + (memberEmail == null || memberEmail.trim().isEmpty() ? "sin correo" : memberEmail.trim().toLowerCase(Locale.ROOT))
                + "\n\ndel piso.";
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Confirmar expulsion",
                "Esta accion se aplica al instante",
                DialogUtils.createMessageView(requireContext(), message),
                "Cancelar",
                "Expulsar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
                if (!isAdded()) return;
                if (!groupDoc.exists()) {
                    NoticeUtils.show(requireContext(), "El piso ya no existe.");
                    return;
                }

                List<String> members = castStrings(groupDoc.get("members"));
                List<String> memberEmails = castStrings(groupDoc.get("memberEmails"));
                Map<String, Object> roles = groupDoc.get("roles") instanceof Map
                        ? new HashMap<>((Map<String, Object>) groupDoc.get("roles"))
                        : new HashMap<>();

                int memberIndex = members.indexOf(memberUid);
                String emailToRemove = "";
                if (memberIndex >= 0 && memberIndex < memberEmails.size()) {
                    emailToRemove = stringValue(memberEmails.get(memberIndex)).toLowerCase(Locale.ROOT);
                }
                members.remove(memberUid);
                roles.remove(memberUid);
                if (!emailToRemove.isEmpty()) {
                    final String emailLc = emailToRemove;
                    memberEmails.removeIf(value -> emailLc.equals(stringValue(value).toLowerCase(Locale.ROOT)));
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("members", members);
                updates.put("memberEmails", memberEmails);
                updates.put("roles", roles);
                final String emailToRemoveFinal = emailToRemove;

                db.collection("groups").document(groupId).update(updates)
                        .addOnSuccessListener(v2 -> {
                            removeMemberFromGroupRooms(groupId, emailToRemoveFinal, () -> {
                                if (!isAdded()) return;
                                NoticeUtils.show(requireContext(), "Inquilino expulsado: " + memberName);
                                loadGroups();
                                if (groupId.equals(selectedGroupId)) {
                                    loadGroupDetails(groupId);
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            NoticeUtils.show(requireContext(), "No se pudo expulsar: " + e.getMessage());
                        });
            }).addOnFailureListener(e -> {
                if (!isAdded()) return;
                NoticeUtils.show(requireContext(), "No se pudo cargar el piso: " + e.getMessage());
            });
        });
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
        selectedGroupIsOwner = isOwner;

        detailNameTv.setText(name == null ? "Grupo" : name);
        detailDescTv.setText(desc);
        resolveMemberDisplayNames(memberIds, emails, labels -> {
            if (!isAdded()) return;
            if (labels.isEmpty() || emails.isEmpty()) {
                detailMembersTv.setText("Inquilinos\nSin datos");
            } else {
                StringBuilder membersText = new StringBuilder("Inquilinos");
                int count = Math.min(labels.size(), emails.size());
                for (int i = 0; i < count; i++) {
                    String label = labels.get(i) == null ? "" : labels.get(i).trim();
                    String email = emails.get(i) == null ? "" : emails.get(i).trim().toLowerCase(Locale.ROOT);
                    if (label.isEmpty()) label = email.isEmpty() ? "Sin datos" : email;

                    membersText.append("\n\n• ").append(label);
                    membersText.append("\n  ").append(email.isEmpty() ? "sin correo" : email);
                }
                detailMembersTv.setText(membersText.toString());
            }
            detailMembersTv.setLineSpacing(0f, 1.15f);
        });

        editGroupBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        deleteGroupBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        detailsCard.setVisibility(View.VISIBLE);
    }

    private String buildOwnerDetailedLabel(String ownerEmailOrLabel, List<String> displayNames, List<String> memberEmails) {
        String normalized = stringValue(ownerEmailOrLabel).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || "sin datos".equals(normalized)) {
            return "Sin datos";
        }
        if (!normalized.contains("@")) {
            return ownerEmailOrLabel;
        }
        for (int i = 0; i < memberEmails.size(); i++) {
            String email = stringValue(memberEmails.get(i)).toLowerCase(Locale.ROOT);
            if (!normalized.equals(email)) continue;
            String name = "";
            if (displayNames != null && i < displayNames.size()) {
                name = stringValue(displayNames.get(i));
            }
            if (name.isEmpty() || name.equalsIgnoreCase(email)) {
                return email;
            }
            return name + " (" + email + ")";
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private String buildLocationLabel(DocumentSnapshot doc) {
        Object locationRaw = doc.get("location");
        if (!(locationRaw instanceof Map)) {
            return "Sin ubicacion";
        }
        Map<String, Object> location = (Map<String, Object>) locationRaw;
        String street = stringValue(location.get("street"));
        String portal = stringValue(location.get("portal"));
        String postalCode = stringValue(location.get("postalCode"));
        String city = stringValue(location.get("city"));
        String province = stringValue(location.get("province"));

        StringBuilder out = new StringBuilder();
        if (!street.isEmpty()) out.append(street);
        if (!portal.isEmpty()) {
            if (out.length() > 0) out.append(", ");
            out.append(portal);
        }
        if (!postalCode.isEmpty() || !city.isEmpty()) {
            if (out.length() > 0) out.append(" - ");
            if (!postalCode.isEmpty()) out.append(postalCode).append(" ");
            if (!city.isEmpty()) out.append(city);
        }
        if (!province.isEmpty()) {
            if (out.length() > 0) out.append(" (");
            out.append(province);
            if (out.length() > 0 && out.charAt(out.length() - 1) != ')') out.append(")");
        }
        return out.length() == 0 ? "Sin ubicacion" : out.toString();
    }

    private String resolveOwnerEmail(DocumentSnapshot groupDoc, List<String> memberIds, List<String> memberEmails) {
        String ownerId = groupDoc.getString("ownerId");
        if (ownerId == null || ownerId.trim().isEmpty()) return "";

        int size = Math.min(memberIds.size(), memberEmails.size());
        for (int i = 0; i < size; i++) {
            String uid = memberIds.get(i);
            if (ownerId.equals(uid)) {
                String email = memberEmails.get(i);
                return email == null ? "" : email.toLowerCase(Locale.ROOT);
            }
        }
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (ownerId.equals(myUid)) {
            String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            return myEmail == null ? "" : myEmail.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private String stringValue(Object value) {
        if (value == null) return "";
        return value.toString().trim();
    }

    private String formatMemberTwoLines(@Nullable String displayName, @Nullable String email) {
        String normalizedEmail = stringValue(email).toLowerCase(Locale.ROOT);
        String resolvedName = stringValue(displayName);
        if (resolvedName.isEmpty() || resolvedName.equalsIgnoreCase(normalizedEmail)) {
            resolvedName = fallbackMemberName(normalizedEmail);
        }
        if (resolvedName.isEmpty()) resolvedName = "Sin datos";
        String secondLine = normalizedEmail.isEmpty() ? "sin correo" : normalizedEmail;
        return resolvedName + "\n" + secondLine;
    }

    private String fallbackMemberName(@Nullable String email) {
        String normalizedEmail = stringValue(email).toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) return "";
        int at = normalizedEmail.indexOf('@');
        if (at <= 0) return normalizedEmail;
        String local = normalizedEmail.substring(0, at)
                .replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();
        if (local.isEmpty()) return normalizedEmail;
        String[] parts = local.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(" ");
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.length() == 0 ? normalizedEmail : out.toString();
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
                        String displayName = userDoc.getString("name");
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = userDoc.getString("displayName");
                        }
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
            NoticeUtils.show(requireContext(), "Selecciona un grupo primero");
            return;
        }
        if (!selectedGroupIsOwner) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede editar este piso");
            return;
        }
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_group, null, false);
        EditText nameEt = form.findViewById(R.id.editGroupNameEt);
        EditText descEt = form.findViewById(R.id.editGroupDescEt);
        TextView variableSplitLabelTv = form.findViewById(R.id.editGroupVariableSplitLabelTv);
        Spinner rentModeSpinner = form.findViewById(R.id.editGroupRentModeSpinner);
        Spinner variableSplitSpinner = form.findViewById(R.id.editGroupVariableSplitSpinner);
        nameEt.setText(detailNameTv.getText());
        descEt.setText(detailDescTv.getText());
        setupRentModeSpinners(rentModeSpinner, variableSplitSpinner, variableSplitLabelTv);

        db.collection("groups").document(selectedGroupId).get().addOnSuccessListener(groupDoc -> {
            String billingModel = groupDoc.getString("billingModel");
            String variableSplitMode = groupDoc.getString("variableSplitMode");
            if (RENT_MODE_FIXED.equalsIgnoreCase(billingModel)) {
                rentModeSpinner.setSelection(1);
                variableSplitSpinner.setSelection(0);
            } else {
                rentModeSpinner.setSelection(2);
                if (VARIABLE_SPLIT_PERCENTAGE.equalsIgnoreCase(variableSplitMode)) {
                    variableSplitSpinner.setSelection(2);
                } else {
                    variableSplitSpinner.setSelection(1);
                }
            }
        });

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Editar piso",
                "Actualiza nombre, descripción y tipo de alquiler.",
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
            int rentModeSelection = rentModeSpinner.getSelectedItemPosition();
            int splitModeSelection = variableSplitSpinner.getSelectedItemPosition();
            if (rentModeSelection <= 0) {
                NoticeUtils.show(requireContext(), "Selecciona el tipo de alquiler");
                return;
            }
            if (rentModeSelection == 2 && splitModeSelection <= 0) {
                NoticeUtils.show(requireContext(), "Selecciona el reparto del alquiler variable");
                return;
            }
            String rentMode = rentModeSelection == 1 ? RENT_MODE_FIXED : RENT_MODE_VARIABLE;
            String variableSplitMode = splitModeSelection == 2 ? VARIABLE_SPLIT_PERCENTAGE : VARIABLE_SPLIT_EQUAL;
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("description", newDesc.isEmpty() ? "Sin descripción" : newDesc);
            updates.put("billingModel", rentMode);
            updates.put("variableSplitMode", variableSplitMode);
            db.collection("groups").document(selectedGroupId).update(updates).addOnSuccessListener(task -> {
                loadGroups();
                loadGroupDetails(selectedGroupId);
                dialog.dismiss();
            });
        });
    }

    private void requestDeleteSelectedGroup() {
        if (selectedGroupId == null) {
            NoticeUtils.show(requireContext(), "Selecciona un piso primero");
            return;
        }
        if (!selectedGroupIsOwner) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede eliminar este piso");
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
        queries.add(db.collection("activity_logs").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("rental_contracts").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("rent_collections").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("maintenance_tickets").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("group_documents").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("audit_events").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("rent_automations").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("event_reminder_rules").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("event_reminder_jobs").whereEqualTo("groupId", groupId).get());
        queries.add(db.collection("invitations").whereEqualTo("groupId", groupId).get());

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> Tasks.whenAllSuccess(queries)
                        .addOnSuccessListener(results -> {
                            QuerySnapshot roomsSnapshot = (QuerySnapshot) results.get(0);
                            List<String> occupiedRoomNames = new ArrayList<>();
                            for (DocumentSnapshot roomDoc : roomsSnapshot.getDocuments()) {
                                List<String> roomMembers = castStrings(roomDoc.get("memberEmails"));
                                if (!roomMembers.isEmpty()) {
                                    String roomName = stringValue(roomDoc.getString("name"));
                                    if (roomName.isEmpty()) {
                                        roomName = "Habitación";
                                    }
                                    occupiedRoomNames.add(roomName);
                                }
                            }
                            if (!occupiedRoomNames.isEmpty()) {
                                StringBuilder details = new StringBuilder("No puedes eliminar el piso porque hay inquilinos asignados en habitaciones.\n\n");
                                details.append("Primero quitalos o cambialos de habitación en:\nPiso > Habitaciones.\n\n");
                                details.append("Habitaciones ocupadas:\n");
                                for (String roomName : occupiedRoomNames) {
                                    details.append("• ").append(roomName).append("\n");
                                }
                                NoticeUtils.show(requireContext(), details.toString().trim());
                                return;
                            }

                            List<DocumentReference> refsToDelete = new ArrayList<>();
                            for (Object result : results) {
                                QuerySnapshot snapshot = (QuerySnapshot) result;
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    refsToDelete.add(doc.getReference());
                                }
                            }

                            String shareCode = normalizeShareCode(groupDoc.getString("shareCode"));
                            if (shareCode.isEmpty()) {
                                shareCode = groupDoc.getId().toUpperCase(Locale.ROOT);
                            }
                            refsToDelete.add(db.collection("group_codes").document(shareCode));
                            refsToDelete.add(db.collection("groups").document(groupId));

                            deleteDocumentsInChunks(refsToDelete)
                                    .addOnSuccessListener(v -> {
                                        if (groupId.equals(SessionStore.getCurrentGroup(requireContext()))) {
                                            SessionStore.clearCurrentGroup(requireContext());
                                            SessionStore.clearCurrentRoom(requireContext());
                                        }
                                        selectedGroupId = null;
                                        detailsCard.setVisibility(View.GONE);
                                        NoticeUtils.show(requireContext(), "Piso eliminado");
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
                        ).show()))
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "No se pudo leer la información del piso: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void removeMemberFromGroupRooms(String groupId, String memberEmail, Runnable onDone) {
        if (groupId == null || groupId.trim().isEmpty() || memberEmail == null || memberEmail.trim().isEmpty()) {
            onDone.run();
            return;
        }
        final String memberEmailLc = memberEmail.trim().toLowerCase(Locale.ROOT);
        db.collection("rooms_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Task<Void>> updates = new ArrayList<>();
                    for (DocumentSnapshot roomDoc : snapshot.getDocuments()) {
                        List<String> roomMembers = castStrings(roomDoc.get("memberEmails"));
                        boolean existed = roomMembers.removeIf(value -> memberEmailLc.equals(stringValue(value).toLowerCase(Locale.ROOT)));
                        if (!existed) continue;

                        Map<String, Object> roomUpdates = new HashMap<>();
                        roomUpdates.put("memberEmails", roomMembers);
                        roomUpdates.put("memberCount", roomMembers.size());
                        roomUpdates.put("updatedAt", FieldValue.serverTimestamp());
                        roomUpdates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        updates.add(roomDoc.getReference().update(roomUpdates));
                    }
                    if (updates.isEmpty()) {
                        onDone.run();
                        return;
                    }
                    Tasks.whenAllComplete(updates)
                            .addOnSuccessListener(done -> onDone.run())
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private String normalizeShareCode(String shareCode) {
        return shareCode == null ? "" : shareCode.trim().toUpperCase(Locale.ROOT);
    }

    private Task<Void> deleteDocumentsInChunks(List<DocumentReference> refs) {
        if (refs == null || refs.isEmpty()) {
            return Tasks.forResult(null);
        }

        final int chunkSize = 450;
        List<Task<Void>> commits = new ArrayList<>();
        WriteBatch batch = db.batch();
        int count = 0;
        for (DocumentReference ref : refs) {
            if (ref == null) continue;
            batch.delete(ref);
            count++;
            if (count == chunkSize) {
                commits.add(batch.commit());
                batch = db.batch();
                count = 0;
            }
        }

        if (count > 0) {
            commits.add(batch.commit());
        }
        if (commits.isEmpty()) {
            return Tasks.forResult(null);
        }
        return Tasks.whenAll(commits);
    }

    private void setupProvinceAutocomplete(AutoCompleteTextView provinceInput) {
        List<String> provinces = new ArrayList<>(PROVINCE_CITIES.keySet());
        provinceInput.setAdapter(buildDialogAutocompleteAdapter(provinces));
        provinceInput.setThreshold(1);

        provinceInput.setOnClickListener(v -> provinceInput.showDropDown());
        provinceInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                provinceInput.post(provinceInput::showDropDown);
            }
        });
        provinceInput.setOnItemClickListener((parent, view, position, id) ->
                provinceInput.post(provinceInput::dismissDropDown)
        );
    }

    private void setupRentModeSpinners(Spinner rentModeSpinner, Spinner variableSplitSpinner, TextView variableSplitLabelTv) {
        List<String> rentModes = Arrays.asList(
                "Selecciona tipo de alquiler",
                "Alquiler fijo",
                "Alquiler variable"
        );
        List<String> variableSplitModes = Arrays.asList(
                "Selecciona reparto",
                "Equitativo",
                "Porcentual"
        );

        rentModeSpinner.setAdapter(buildDialogSpinnerAdapter(rentModes));
        variableSplitSpinner.setAdapter(buildDialogSpinnerAdapter(variableSplitModes));
        variableSplitSpinner.setSelection(0);
        variableSplitSpinner.setVisibility(View.GONE);
        variableSplitLabelTv.setVisibility(View.GONE);

        rentModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isVariable = position == 2;
                variableSplitLabelTv.setVisibility(isVariable ? View.VISIBLE : View.GONE);
                variableSplitSpinner.setVisibility(isVariable ? View.VISIBLE : View.GONE);
                if (!isVariable) {
                    variableSplitSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String getSelectedProvinceValue(AutoCompleteTextView provinceInput) {
        String typedProvince = getInputValue(provinceInput);
        if (typedProvince.isEmpty()) return "";
        for (String province : PROVINCE_CITIES.keySet()) {
            if (province.equalsIgnoreCase(typedProvince)) {
                return province;
            }
        }
        return typedProvince;
    }

    private String getInputValue(TextView input) {
        return input == null ? "" : input.getText().toString().trim();
    }

    private ArrayAdapter<String> buildDialogAutocompleteAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, items) {
            @Override
            public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(requireContext().getColor(R.color.text_light));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(requireContext().getColor(R.color.text_light));
                    tv.setBackgroundColor(requireContext().getColor(R.color.surface_dark_alt));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        return adapter;
    }

    private ArrayAdapter<String> buildDialogSpinnerAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, items) {
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
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
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

    private static class KickCandidate {
        final String uid;
        final String email;
        final String displayName;

        KickCandidate(String uid, String email, String displayName) {
            this.uid = uid;
            this.email = email;
            this.displayName = displayName;
        }
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

