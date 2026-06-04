package com.sergio.flatshare.features.workspace;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.ui.DateInputUtils;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;
import com.sergio.flatshare.features.workspace.services.MemberLabelFormatter;
import com.sergio.flatshare.features.workspace.services.RentalTextFormatter;
import com.sergio.flatshare.core.notifications.ReminderScheduler;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.features.workspace.services.CollectionsService;
import com.sergio.flatshare.features.workspace.services.ContractsService;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class RentalManagementFragment extends Fragment {
    private static final String MODULE_RULES = "house_rules";
    private static final String MODULE_SCHEDULES = "schedules";
    private static final String MODULE_CONTRACT = "contract";
    private static final String MODULE_RENT = "rent";
    private static final String MODULE_MAINTENANCE = "maintenance";
    private static final String MODULE_DOCUMENTS = "documents";
    private static final String MODULE_AUDIT = "audit";
    private static final String MODULE_AUTOMATION = "automation";
    private static final String MODULE_REMINDER_RULES = "reminder_rules";

    private static final String EVENT_RENT_DUE = "rent_due";
    private static final String EVENT_RENT_OVERDUE = "rent_overdue";
    private static final String EVENT_CONTRACT_ENDING = "contract_ending";
    private static final String EVENT_INCIDENT_PENDING = "incident_pending";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ContractsService contractsService = new ContractsService(db);
    private final CollectionsService collectionsService = new CollectionsService(db);
    private final List<ModuleDef> modules = new ArrayList<>();
    private final List<ManagementRow> rows = new ArrayList<>();
    private final List<String> groupMemberEmails = new ArrayList<>();
    private final Map<String, String> groupMemberNamesByEmail = new HashMap<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("0.00");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
    private final Random random = new Random();

    private Spinner moduleSpinner;
    private TextView moduleHintTv;
    private Button primaryActionBtn;
    private TextView emptyTv;
    private ListView rowsLv;
    private RowsAdapter adapter;

    private String currentGroupId;
    private String selectedModuleId = MODULE_MAINTENANCE;
    private boolean currentUserIsGroupOwner = false;
    @Nullable private String currentRoomFilterId;
    @Nullable private String currentRoomFilterName;
    private final List<String> currentRoomFilterMembers = new ArrayList<>();
    @Nullable private Uri pendingDocumentFileUri;
    @Nullable private TextView pendingDocumentStatusTv;
    @Nullable private EditText pendingDocumentReferenceEt;
    private final ActivityResultLauncher<String[]> documentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleDocumentFileSelected);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rental_management, container, false);
        moduleSpinner = view.findViewById(R.id.managementModuleSpinner);
        moduleHintTv = view.findViewById(R.id.managementModuleHintTv);
        primaryActionBtn = view.findViewById(R.id.managementPrimaryActionBtn);
        emptyTv = view.findViewById(R.id.managementEmptyTv);
        rowsLv = view.findViewById(R.id.managementRowsLv);
        adapter = new RowsAdapter();
        rowsLv.setAdapter(adapter);

        buildModules();
        setupModuleSpinner();
        rowsLv.setOnItemClickListener((parent, itemView, position, id) -> openRowDetails(rows.get(position)));
        primaryActionBtn.setOnClickListener(v -> handlePrimaryAction());

        refreshData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    public void refreshData() {
        if (!isAdded()) return;
        currentGroupId = SessionStore.getCurrentGroup(requireContext());
        if (currentGroupId == null || currentGroupId.trim().isEmpty()) {
            rows.clear();
            adapter.notifyDataSetChanged();
            currentUserIsGroupOwner = false;
            primaryActionBtn.setEnabled(false);
            emptyTv.setText("Selecciona un piso para gestionar alquileres.");
            emptyTv.setVisibility(View.VISIBLE);
            return;
        }
        primaryActionBtn.setEnabled(true);
        primaryActionBtn.setAlpha(1f);
        loadGroupMembersAndThen(this::loadCurrentModuleRows);
    }

    private void buildModules() {
        modules.clear();
        modules.add(new ModuleDef(MODULE_MAINTENANCE, "Incidencias", "Averías, responsable, coste, estado e historial", "Nueva incidencia"));
        modules.add(new ModuleDef(MODULE_RULES, "Reglas", "Normas del piso para todos, una persona concreta o una habitación", "Nueva regla"));
        modules.add(new ModuleDef(MODULE_SCHEDULES, "Horarios", "Turnos y usos recurrentes por persona, zona o habitación", "Nuevo horario"));
    }

    private void setupModuleSpinner() {
        String[] labels = new String[modules.size()];
        for (int i = 0; i < modules.size(); i++) labels[i] = modules.get(i).label;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_selected,
                labels
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
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        moduleSpinner.setAdapter(adapter);
        moduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= modules.size()) return;
                ModuleDef module = modules.get(position);
                selectedModuleId = module.id;
                updateModuleHeaderUi(module);
                loadCurrentModuleRows();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadGroupMembersAndThen(Runnable done) {
        groupMemberEmails.clear();
        groupMemberNamesByEmail.clear();
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            currentUserIsGroupOwner = isCurrentUserGroupOwner(doc);
            List<String> emails = castEmails(doc.get("memberEmails"));
            groupMemberEmails.addAll(emails);
            resolveGroupMemberNames(emails, () -> loadCurrentRoomFilterContext(done));
        }).addOnFailureListener(e -> {
            currentUserIsGroupOwner = false;
            clearCurrentRoomFilterContext();
            if (done != null) done.run();
        });
    }

    private void updateModuleHeaderUi(@NonNull ModuleDef module) {
        boolean ownerOnlyModule = MODULE_RULES.equals(module.id) || MODULE_SCHEDULES.equals(module.id);
        boolean canManage = !ownerOnlyModule || currentUserIsGroupOwner;
        String baseDescription = module.description;
        if (ownerOnlyModule && !currentUserIsGroupOwner) {
            moduleHintTv.setText(baseDescription + "\nSolo el propietario puede crear, editar o eliminar.");
            primaryActionBtn.setText("Solo lectura");
            primaryActionBtn.setEnabled(false);
            primaryActionBtn.setAlpha(0.5f);
            return;
        }
        moduleHintTv.setText(baseDescription);
        primaryActionBtn.setText(module.actionLabel);
        primaryActionBtn.setEnabled(canManage);
        primaryActionBtn.setAlpha(1f);
    }

    private void resolveGroupMemberNames(List<String> emails, @Nullable Runnable done) {
        List<String> normalizedEmails = new ArrayList<>();
        for (String email : emails) {
            String normalized = normalizeEmail(email);
            if (normalized.isEmpty() || normalizedEmails.contains(normalized)) continue;
            normalizedEmails.add(normalized);
        }
        if (normalizedEmails.isEmpty()) {
            if (done != null) done.run();
            return;
        }

        List<Task<?>> tasks = new ArrayList<>();
        int chunkSize = 10;
        for (int start = 0; start < normalizedEmails.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, normalizedEmails.size());
            List<String> chunk = new ArrayList<>(normalizedEmails.subList(start, end));
            Task<?> task = db.collection("users")
                    .whereIn("email", chunk)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot userDoc : snapshot.getDocuments()) {
                            String email = normalizeEmail(userDoc.getString("email"));
                            if (email.isEmpty()) continue;
                            String displayName = safe(userDoc.getString("name"));
                            if (displayName.isEmpty()) displayName = safe(userDoc.getString("displayName"));
                            if (displayName.isEmpty()) displayName = safe(userDoc.getString("username"));
                            if (displayName.isEmpty()) displayName = fallbackNameFromEmail(email);
                            groupMemberNamesByEmail.put(email, displayName);
                        }
                    });
            tasks.add(task);
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(doneTask -> {
                    if (done != null) done.run();
                });
    }

    private void loadCurrentModuleRows() {
        if (!isAdded() || currentGroupId == null || currentGroupId.trim().isEmpty()) return;
        ModuleDef selectedModule = findModuleById(selectedModuleId);
        if (selectedModule != null) {
            updateModuleHeaderUi(selectedModule);
        }
        switch (selectedModuleId) {
            case MODULE_MAINTENANCE -> loadMaintenanceRows();
            case MODULE_RULES -> loadHouseRuleRows();
            case MODULE_SCHEDULES -> loadScheduleRows();
            default -> loadMaintenanceRows();
        }
    }

    private void loadCurrentRoomFilterContext(@Nullable Runnable done) {
        currentRoomFilterId = SessionStore.getCurrentRoomId(requireContext());
        currentRoomFilterName = SessionStore.getCurrentRoomName(requireContext());
        currentRoomFilterMembers.clear();
        if (currentRoomFilterId == null || currentRoomFilterId.trim().isEmpty()) {
            if (done != null) done.run();
            return;
        }
        db.collection("rooms_groups").document(currentRoomFilterId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        clearCurrentRoomFilterContext();
                    } else {
                        currentRoomFilterMembers.clear();
                        currentRoomFilterMembers.addAll(castEmails(doc.get("memberEmails")));
                        String roomName = safe(doc.getString("name"));
                        if (!roomName.isEmpty()) {
                            currentRoomFilterName = roomName;
                        }
                    }
                    if (done != null) done.run();
                })
                .addOnFailureListener(e -> {
                    clearCurrentRoomFilterContext();
                    if (done != null) done.run();
                });
    }

    private void clearCurrentRoomFilterContext() {
        currentRoomFilterId = null;
        currentRoomFilterName = null;
        currentRoomFilterMembers.clear();
    }

    private void handlePrimaryAction() {
        if (currentGroupId == null || currentGroupId.trim().isEmpty()) {
            NoticeUtils.show(requireContext(), "Selecciona un piso primero");
            return;
        }
        if ((MODULE_RULES.equals(selectedModuleId) || MODULE_SCHEDULES.equals(selectedModuleId)) && !currentUserIsGroupOwner) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede gestionar este apartado");
            return;
        }
        switch (selectedModuleId) {
            case MODULE_MAINTENANCE -> openMaintenanceDialog(null);
            case MODULE_RULES -> openHouseRuleDialog(null);
            case MODULE_SCHEDULES -> openScheduleDialog(null);
            default -> openMaintenanceDialog(null);
        }
    }

    private void openRowDetails(ManagementRow row) {
        if (MODULE_MAINTENANCE.equals(row.moduleId)) {
            openMaintenanceDialog(row.snapshot);
        } else if (MODULE_RULES.equals(row.moduleId)) {
            if (currentUserIsGroupOwner) {
                openHouseRuleDialog(row.snapshot);
            } else {
                showReadOnlyDetails(row);
            }
        } else if (MODULE_SCHEDULES.equals(row.moduleId)) {
            if (currentUserIsGroupOwner) {
                showScheduleActions(row);
            } else {
                showReadOnlyDetails(row);
            }
        } else {
            showReadOnlyDetails(row);
        }
    }

    private void showReadOnlyDetails(ManagementRow row) {
        StringBuilder detailBuilder = new StringBuilder();
        if (row.subtitle != null && !row.subtitle.trim().isEmpty()) {
            detailBuilder.append(row.subtitle.trim());
        }
        if (row.amount != null && !row.amount.trim().isEmpty()) {
            if (detailBuilder.length() > 0) detailBuilder.append("\n\n");
            detailBuilder.append(row.amount.trim());
        }
        if (row.detail != null && !row.detail.trim().isEmpty()) {
            if (detailBuilder.length() > 0) detailBuilder.append("\n\n");
            detailBuilder.append(row.detail.trim());
        }
        if (detailBuilder.length() == 0) {
            detailBuilder.append("Sin detalle.");
        }
        View content = DialogUtils.createMessageView(requireContext(), detailBuilder.toString());
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Detalle",
                content,
                null,
                "Cerrar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
    }

    private void showScheduleActions(@NonNull ManagementRow row) {
        LinearLayout content = DialogUtils.createVerticalActions(requireContext());
        Button infoBtn = DialogUtils.createActionButton(requireContext(), "Ver detalle", true);
        Button editBtn = DialogUtils.createActionButton(requireContext(), "Editar horario", false);
        Button deleteBtn = DialogUtils.createActionButton(requireContext(), "Eliminar horario", false);
        content.addView(infoBtn);
        content.addView(editBtn);
        content.addView(deleteBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Gestiona este horario del piso.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        infoBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showReadOnlyDetails(row);
        });
        editBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openScheduleDialog(row.snapshot);
        });
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            requestScheduleDeletion(row);
        });
    }

    private void requestScheduleDeletion(@NonNull ManagementRow row) {
        if (!currentUserIsGroupOwner) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede eliminar horarios");
            return;
        }
        View content = DialogUtils.createMessageView(
                requireContext(),
                "Se eliminará el horario \"" + row.title + "\". Esta acción no se puede deshacer."
        );
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Eliminar horario",
                "Confirma la eliminación del horario.",
                content,
                "Cancelar",
                "Eliminar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            db.collection("group_schedules").document(row.id)
                    .delete()
                    .addOnSuccessListener(done -> {
                        writeAudit("Horarios", "eliminar", row.title, row.id);
                        dialog.dismiss();
                        loadScheduleRows();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo eliminar el horario", Toast.LENGTH_SHORT).show());
        });
    }

    private void loadContractRows() {
        contractsService.loadContractsByGroup(
                currentGroupId,
                docs -> {
                    rows.clear();
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String start = normalizeDateText(doc.getString("startDate"));
                        String end = normalizeDateText(doc.getString("endDate"));
                        double deposit = safeDouble(doc.getDouble("depositAmount"));
                        long extMonths = safeLong(doc.getLong("extensionMonths"));
                        String signersSummary = buildContractSignersSummary(doc);
                        rows.add(new ManagementRow(
                                MODULE_CONTRACT,
                                doc.getId(),
                                "Contrato " + (start.isEmpty() ? "-" : start) + " -> " + (end.isEmpty() ? "-" : end),
                                "Fianza " + moneyFormat.format(deposit) + " EUR · Prórrogas: " + extMonths + " meses",
                                signersSummary,
                                doc,
                                "Contrato"
                        ));
                    }
                    onRowsReady("Aún no has creado contrato para este piso.");
                },
                error -> onLoadError("No se pudieron cargar contratos")
        );
    }

    private void loadRentRows() {
        collectionsService.loadCollectionsByGroup(
                currentGroupId,
                docs -> {
                    rows.clear();
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String monthKey = safe(doc.getString("monthKey"));
                        String room = safe(doc.getString("roomName"));
                        String tenant = safe(doc.getString("tenantEmail"));
                        String status = safe(doc.getString("status"));
                        double amountBase = safeDouble(doc.getDouble("amountBase"));
                        double surcharge = safeDouble(doc.getDouble("surcharge"));
                        double amountPaid = safeDouble(doc.getDouble("amountPaid"));
                        double total = amountBase + surcharge;
                        String tenantLabel = tenant.isEmpty() ? "Sin inquilino" : formatMemberTwoLines(tenant);
                        String subtitle = "Mes " + (monthKey.isEmpty() ? "-" : DateInputUtils.normalizeMonthKeyToDisplay(monthKey))
                                + " - " + (room.isEmpty() ? "Sin habitación" : room)
                                + "\nInquilino:\n" + tenantLabel;
                        String amount = moneyFormat.format(amountPaid) + " / " + moneyFormat.format(total) + " EUR";
                        String detail = "Estado: " + (status.isEmpty() ? "pendiente" : status)
                                + "\nVence: " + normalizeDateText(doc.getString("dueDateText"))
                                + "\nRecargo: " + moneyFormat.format(surcharge) + " EUR";
                        rows.add(new ManagementRow(MODULE_RENT, doc.getId(), subtitle, detail, amount, doc, "Cobro"));
                    }
                    onRowsReady("No hay cobros mensuales registrados.");
                },
                error -> onLoadError("No se pudieron cargar cobros")
        );
    }

    private void loadMaintenanceRows() {
        db.collection("maintenance_tickets")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String title = safe(doc.getString("title"));
                        String room = safe(doc.getString("roomName"));
                        String responsible = safe(doc.getString("responsibleEmail"));
                        String status = safe(doc.getString("status"));
                        double cost = safeDouble(doc.getDouble("finalCost"));
                        if (cost <= 0) cost = safeDouble(doc.getDouble("estimatedCost"));
                        if (!matchesManagementRoomFilter(doc, room, responsible, "")) continue;
                        String subtitle = (room.isEmpty() ? "Sin habitación" : room)
                                + "\nResponsable:\n" + (responsible.isEmpty() ? "Sin asignar" : formatMemberTwoLines(responsible));
                        String detail = "Estado: " + (status.isEmpty() ? "abierta" : status)
                                + " · Coste: " + moneyFormat.format(cost) + " EUR";
                        rows.add(new ManagementRow(MODULE_MAINTENANCE, doc.getId(), title.isEmpty() ? "Incidencia" : title, subtitle, detail, doc, "Incidencia"));
                    }
                    onRowsReady("No hay incidencias registradas.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar incidencias"));
    }

    private void loadHouseRuleRows() {
        db.collection("house_rules")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String title = safe(doc.getString("title"));
                        String scopeSummary = buildRuleScopeSummary(doc);
                        String description = safe(doc.getString("description"));
                        if (!matchesManagementRoomFilter(
                                doc,
                                safe(doc.getString("roomName")),
                                safe(doc.getString("targetMemberEmail")),
                                safe(doc.getString("scopeType"))
                        )) continue;
                        String ruleDetail = description.isEmpty()
                                ? "Descripción: Sin detalle adicional"
                                : "Descripción: " + description;
                        rows.add(new ManagementRow(
                                MODULE_RULES,
                                doc.getId(),
                                title.isEmpty() ? "Regla del piso" : title,
                                "Aplica a: " + scopeSummary,
                                ruleDetail,
                                doc,
                                "Regla"
                        ));
                    }
                    onRowsReady("No hay reglas creadas para este piso.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar las reglas"));
    }

    private void loadScheduleRows() {
        db.collection("group_schedules")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String title = safe(doc.getString("title"));
                        String frequency = safe(doc.getString("frequency"));
                        String startTime = safe(doc.getString("startTimeText"));
                        String endTime = safe(doc.getString("endTimeText"));
                        if (!matchesManagementRoomFilter(
                                doc,
                                safe(doc.getString("roomName")),
                                safe(doc.getString("targetMemberEmail")),
                                safe(doc.getString("scopeType"))
                        )) continue;
                        String subtitle = "Aplica a: " + buildRuleScopeSummary(doc);
                        String amount = (startTime.isEmpty() ? "--:--" : startTime)
                                + " - "
                                + (endTime.isEmpty() ? "--:--" : endTime);
                        String scheduleDetail = buildScheduleSummary(doc);
                    rows.add(new ManagementRow(
                            MODULE_SCHEDULES,
                            doc.getId(),
                            title.isEmpty() ? "Horario" : title,
                            subtitle,
                            amount,
                            doc,
                            scheduleDetail
                    ));
                    }
                    onRowsReady("No hay horarios definidos para este piso.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar los horarios"));
    }

    private void loadDocumentRows() {
        db.collection("group_documents")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String type = safe(doc.getString("type"));
                        String title = safe(doc.getString("title"));
                        String date = normalizeDateText(doc.getString("documentDate"));
                        String ref = safe(doc.getString("referenceUri"));
                        String notes = safe(doc.getString("notes"));
                        String subtitle = "Tipo: " + normalizeDocumentType(type) + " · Fecha: " + (date.isEmpty() ? "-" : date);
                        String detail = "Referencia: " + (ref.isEmpty() ? "-" : ref);
                        rows.add(new ManagementRow(MODULE_DOCUMENTS, doc.getId(), title.isEmpty() ? "Documento" : title, subtitle, detail, doc, notes));
                    }
                    onRowsReady("No hay documentos cargados para el piso.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar documentos"));
    }

    private void loadAuditRows() {
        db.collection("audit_events")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "createdAt", "updatedAt"));
                    for (DocumentSnapshot doc : docs) {
                        String module = safe(doc.getString("module"));
                        String action = safe(doc.getString("action"));
                        String actor = safe(doc.getString("actorEmail"));
                        String date = timestampToDateText(doc.getTimestamp("createdAt"));
                        String details = safe(doc.getString("details"));
                        rows.add(new ManagementRow(
                                MODULE_AUDIT,
                                doc.getId(),
                                module + " · " + action,
                                "Usuario: " + (actor.isEmpty() ? "-" : actor),
                                date,
                                doc,
                                details
                        ));
                    }
                    onRowsReady("No hay eventos de auditoría todavía.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudo cargar auditoría"));
    }

    private void loadAutomationRows() {
        db.collection("rent_automations")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String room = safe(doc.getString("roomName"));
                        String tenant = safe(doc.getString("tenantEmail"));
                        double amount = safeDouble(doc.getDouble("monthlyRent"));
                        long day = safeLong(doc.getLong("billingDay"));
                        String start = normalizeDateText(doc.getString("startDate"));
                        String end = normalizeDateText(doc.getString("endDate"));
                        String subtitle = (room.isEmpty() ? "Sin habitación" : room)
                                + "\nInquilino:\n" + (tenant.isEmpty() ? "Sin inquilino" : formatMemberTwoLines(tenant));
                        String detail = "Cobro día " + day + " · " + moneyFormat.format(amount) + " EUR/mes";
                        rows.add(new ManagementRow(MODULE_AUTOMATION, doc.getId(), start + " -> " + (end.isEmpty() ? "sin fin" : end), subtitle, detail, doc, "Regla de automatizacion"));
                    }
                    onRowsReady("No hay reglas de automatización todavía.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar automatizaciones"));
    }

    private void loadReminderRuleRows() {
        db.collection("event_reminder_rules")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    rows.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String eventType = safe(doc.getString("eventType"));
                        String titleTemplate = safe(doc.getString("titleTemplate"));
                        String bodyTemplate = safe(doc.getString("bodyTemplate"));
                        boolean enabled = Boolean.TRUE.equals(doc.getBoolean("enabled"));
                        long offset = safeLong(doc.getLong("offsetDays"));
                        String subtitle = eventTypeLabel(eventType)
                                + " · " + (enabled ? "Activa" : "Pausada")
                                + " · offset " + offset + " días";
                        rows.add(new ManagementRow(
                                MODULE_REMINDER_RULES,
                                doc.getId(),
                                titleTemplate.isEmpty() ? "Regla sin título" : titleTemplate,
                                subtitle,
                                bodyTemplate,
                                doc,
                                "Regla de recordatorio"
                        ));
                    }
                    onRowsReady("No hay reglas de notificación por eventos.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar reglas de recordatorio"));
    }

    private void openContractDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        EditText startEt = addLabeledEditText(form, "Fecha inicio (DD/MM/AAAA)", "01/01/2026", InputType.TYPE_CLASS_TEXT);
        EditText endEt = addLabeledEditText(form, "Fecha fin (DD/MM/AAAA)", "31/12/2026", InputType.TYPE_CLASS_TEXT);
        EditText depositEt = addLabeledEditText(form, "Fianza (EUR)", "1000", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText extensionEt = addLabeledEditText(form, "Prórrogas (meses)", "0", InputType.TYPE_CLASS_NUMBER);
        EditText clausesEt = addLabeledEditText(form, "Cláusulas", "Normas y condiciones del alquiler", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        EditText ownerSignerEt = addLabeledEditText(form, "Firmante propietario (obligatorio)", "propietario@correo.com", InputType.TYPE_CLASS_TEXT);
        EditText tenantSignerEt = addLabeledEditText(form, "Firmante inquilino (obligatorio)", "inquilino@correo.com", InputType.TYPE_CLASS_TEXT);
        Spinner coSignerEnabledSp = addLabeledSpinner(form, "Añadir cotitular", new String[]{"No", "Sí"});
        EditText coSignerEt = addLabeledEditText(form, "Firmante cotitular (opcional)", "cotitular@correo.com", InputType.TYPE_CLASS_TEXT);
        Spinner guarantorEnabledSp = addLabeledSpinner(form, "Añadir avalista", new String[]{"No", "Sí"});
        EditText guarantorEt = addLabeledEditText(form, "Firmante avalista (opcional)", "avalista@correo.com", InputType.TYPE_CLASS_TEXT);
        setupDatePicker(startEt);
        setupDatePicker(endEt);
        coSignerEt.setVisibility(View.GONE);
        guarantorEt.setVisibility(View.GONE);

        coSignerEnabledSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                coSignerEt.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                if (position == 0) coSignerEt.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        guarantorEnabledSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                guarantorEt.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                if (position == 0) guarantorEt.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (existingDoc != null) {
            startEt.setText(normalizeDateText(existingDoc.getString("startDate")));
            endEt.setText(normalizeDateText(existingDoc.getString("endDate")));
            depositEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("depositAmount"))));
            extensionEt.setText(String.valueOf(safeLong(existingDoc.getLong("extensionMonths"))));
            clausesEt.setText(safe(existingDoc.getString("clauses")));

            List<String> legacySigners = castStrings(existingDoc.get("signers"));
            String ownerSigner = safe(existingDoc.getString("ownerSignerEmail"));
            if (ownerSigner.isEmpty() && !legacySigners.isEmpty()) ownerSigner = legacySigners.get(0);
            String tenantSigner = safe(existingDoc.getString("tenantSignerEmail"));
            if (tenantSigner.isEmpty() && legacySigners.size() > 1) tenantSigner = legacySigners.get(1);
            String coSigner = safe(existingDoc.getString("coSignerEmail"));
            if (coSigner.isEmpty() && legacySigners.size() > 2) coSigner = legacySigners.get(2);
            String guarantor = safe(existingDoc.getString("guarantorEmail"));
            if (guarantor.isEmpty() && legacySigners.size() > 3) guarantor = legacySigners.get(3);

            ownerSignerEt.setText(ownerSigner);
            tenantSignerEt.setText(tenantSigner);
            if (!coSigner.isEmpty()) {
                coSignerEnabledSp.setSelection(1);
                coSignerEt.setVisibility(View.VISIBLE);
                coSignerEt.setText(coSigner);
            }
            if (!guarantor.isEmpty()) {
                guarantorEnabledSp.setSelection(1);
                guarantorEt.setVisibility(View.VISIBLE);
                guarantorEt.setText(guarantor);
            }
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                existingDoc == null ? "Nuevo contrato" : "Editar contrato",
                "Guarda fechas, fianza, cláusulas y firmantes.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String start = normalizeDateText(startEt.getText().toString().trim());
            String end = normalizeDateText(endEt.getText().toString().trim());
            if (!isValidDate(start) || !isValidDate(end)) {
                NoticeUtils.show(requireContext(), "Revisa formato de fechas (DD/MM/AAAA)");
                return;
            }
            double deposit = parseDouble(depositEt.getText().toString().trim(), -1);
            long extensions = parseLong(extensionEt.getText().toString().trim(), -1);
            if (deposit < 0 || extensions < 0) {
                NoticeUtils.show(requireContext(), "Fianza o prórrogas inválidas");
                return;
            }
            String ownerSigner = normalizeEmail(ownerSignerEt.getText().toString());
            String tenantSigner = normalizeEmail(tenantSignerEt.getText().toString());
            String coSigner = coSignerEnabledSp.getSelectedItemPosition() == 1
                    ? normalizeEmail(coSignerEt.getText().toString())
                    : "";
            String guarantor = guarantorEnabledSp.getSelectedItemPosition() == 1
                    ? normalizeEmail(guarantorEt.getText().toString())
                    : "";

            if (!isLikelyEmail(ownerSigner) || !isLikelyEmail(tenantSigner)) {
                NoticeUtils.show(requireContext(), "Propietario e inquilino deben tener email válido");
                return;
            }
            if (!coSigner.isEmpty() && !isLikelyEmail(coSigner)) {
                NoticeUtils.show(requireContext(), "El cotitular no tiene email válido");
                return;
            }
            if (!guarantor.isEmpty() && !isLikelyEmail(guarantor)) {
                NoticeUtils.show(requireContext(), "El avalista no tiene email válido");
                return;
            }

            List<String> signers = new ArrayList<>();
            signers.add(ownerSigner);
            signers.add(tenantSigner);
            if (!coSigner.isEmpty()) signers.add(coSigner);
            if (!guarantor.isEmpty()) signers.add(guarantor);

            String uid = authUid();
            String email = authEmail();
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("startDate", start);
            data.put("endDate", end);
            data.put("depositAmount", deposit);
            data.put("extensionMonths", extensions);
            data.put("clauses", clausesEt.getText().toString().trim());
            data.put("ownerSignerEmail", ownerSigner);
            data.put("tenantSignerEmail", tenantSigner);
            data.put("coSignerEmail", coSigner);
            data.put("guarantorEmail", guarantor);
            data.put("signers", signers);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedByUid", uid);
            data.put("updatedByEmail", email);
            if (existingDoc == null) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("createdByUid", uid);
                data.put("createdByEmail", email);
            }

            Task<?> writeTask = existingDoc == null
                    ? db.collection("rental_contracts").add(data)
                    : db.collection("rental_contracts").document(existingDoc.getId()).update(data);

            writeTask.addOnSuccessListener(done -> {
                writeAudit("Contrato", existingDoc == null ? "crear" : "editar", "Contrato guardado: " + start + " -> " + end, existingDoc == null ? "" : existingDoc.getId());
                applyEventRules(EVENT_CONTRACT_ENDING, parseDate(end), null, "Fin de contrato cercano");
                dialog.dismiss();
                loadContractRows();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar contrato", Toast.LENGTH_SHORT).show());
        });
    }

    private void openRentDialog(@Nullable DocumentSnapshot existingDoc) {
        loadGroupRoomLabels(roomLabels -> {
            if (!isAdded()) return;
            List<String> roomOptions = new ArrayList<>(roomLabels);
            String existingRoom = existingDoc == null ? "" : safe(existingDoc.getString("roomName"));
            if (!existingRoom.isEmpty() && !roomOptions.contains(existingRoom)) {
                roomOptions.add(0, existingRoom);
            }
            if (roomOptions.isEmpty()) {
                roomOptions.add("Sin habitaciones");
            }

            LinearLayout form = buildVerticalForm();
            EditText monthEt = addLabeledEditText(form, "Mes (YYYY-MM)", "2026-05", InputType.TYPE_CLASS_TEXT);
            Spinner roomSpinner = addLabeledSpinner(form, "Habitación", roomOptions.toArray(new String[0]));
            Spinner tenantSpinner = addLabeledMemberInlineSpinner(form, "Inquilino");
            EditText amountEt = addLabeledEditText(form, "Base mensual (EUR)", "450", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            EditText paidEt = addLabeledEditText(form, "Pagado hasta ahora (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            EditText surchargeEt = addLabeledEditText(form, "Recargo (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            Spinner statusSpinner = addLabeledSpinner(form, "Estado", new String[]{"pendiente", "parcial", "pagado", "atrasado"});
            EditText dueDateEt = addLabeledEditText(form, "Vencimiento (DD/MM/AAAA)", "05/05/2026", InputType.TYPE_CLASS_TEXT);
            setupDatePicker(dueDateEt);

            if (existingDoc != null) {
                monthEt.setText(safe(existingDoc.getString("monthKey")));
                selectSpinnerValue(roomSpinner, safe(existingDoc.getString("roomName")));
                selectMemberSpinnerEmail(tenantSpinner, safe(existingDoc.getString("tenantEmail")));
                amountEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("amountBase"))));
                paidEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("amountPaid"))));
                surchargeEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("surcharge"))));
                selectSpinnerValue(statusSpinner, safe(existingDoc.getString("status")));
                dueDateEt.setText(normalizeDateText(existingDoc.getString("dueDateText")));
            }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    existingDoc == null ? "Nuevo cobro mensual" : "Editar cobro",
                    "Controla estado, recargos y vencimiento.",
                    form,
                    "Cancelar",
                    "Guardar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            shell.confirmBtn.setOnClickListener(v -> {
                String monthKey = monthEt.getText().toString().trim();
                if (!isValidMonth(monthKey)) {
                    NoticeUtils.show(requireContext(), "Mes inválido. Usa YYYY-MM");
                    return;
                }
            String dueDate = normalizeDateText(dueDateEt.getText().toString().trim());
                if (!isValidDate(dueDate)) {
                    NoticeUtils.show(requireContext(), "Fecha de vencimiento inválida");
                    return;
                }
                double baseAmount = parseDouble(amountEt.getText().toString().trim(), -1);
                double paidAmount = parseDouble(paidEt.getText().toString().trim(), -1);
                double surcharge = parseDouble(surchargeEt.getText().toString().trim(), -1);
                if (baseAmount < 0 || paidAmount < 0 || surcharge < 0) {
                    NoticeUtils.show(requireContext(), "Importes inválidos");
                    return;
                }
                String tenant = selectedMemberEmail(tenantSpinner);
                if (tenant.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Selecciona un inquilino válido");
                    return;
                }
                String status = selectedSpinnerValue(statusSpinner);
                String roomName = selectedSpinnerValue(roomSpinner);
                if (roomName.isEmpty() || "Sin habitaciones".equalsIgnoreCase(roomName)) {
                    NoticeUtils.show(requireContext(), "Selecciona una habitación válida");
                    return;
                }
                String uniqueKey = currentGroupId + "|" + monthKey + "|" + roomName.toLowerCase(Locale.ROOT) + "|" + tenant.toLowerCase(Locale.ROOT);

                Map<String, Object> data = new HashMap<>();
                data.put("groupId", currentGroupId);
                data.put("monthKey", monthKey);
                data.put("roomName", roomName);
                data.put("tenantEmail", tenant);
                data.put("amountBase", baseAmount);
                data.put("amountPaid", paidAmount);
                data.put("surcharge", surcharge);
                data.put("status", status);
                data.put("dueDateText", dueDate);
                data.put("dueAt", toTimestamp(dueDate));
                data.put("uniqueKey", uniqueKey);
                data.put("updatedAt", FieldValue.serverTimestamp());
                data.put("updatedByUid", authUid());
                data.put("updatedByEmail", authEmail());
                if (existingDoc == null) {
                    data.put("createdAt", FieldValue.serverTimestamp());
                    data.put("createdByUid", authUid());
                    data.put("createdByEmail", authEmail());
                }

                Task<?> saveTask = existingDoc == null
                        ? db.collection("rent_collections").add(data)
                        : db.collection("rent_collections").document(existingDoc.getId()).update(data);

                saveTask.addOnSuccessListener(done -> {
                    writeAudit("Cobros", existingDoc == null ? "crear" : "editar", "Cobro " + monthKey + " (" + status + ")", existingDoc == null ? "" : existingDoc.getId());
                    Date dueAt = parseDate(dueDate);
                    applyEventRules(EVENT_RENT_DUE, dueAt, tenant, "Vence renta de " + roomName);
                    if ("atrasado".equalsIgnoreCase(status)) {
                        applyEventRules(EVENT_RENT_OVERDUE, new Date(), tenant, "Impago de renta en " + roomName);
                    } else if (dueAt != null) {
                        Calendar c = Calendar.getInstance();
                        c.setTime(dueAt);
                        c.add(Calendar.DAY_OF_MONTH, 1);
                        applyEventRules(EVENT_RENT_OVERDUE, c.getTime(), tenant, "Impago de renta en " + roomName);
                    }
                    dialog.dismiss();
                    loadRentRows();
                }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar cobro", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void openMaintenanceDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        EditText titleEt = addLabeledEditText(form, "Incidencia", "Ejemplo: Fuga en baño", InputType.TYPE_CLASS_TEXT);
        EditText roomEt = addLabeledEditText(form, "Habitación/Zona", "Ejemplo: Cocina", InputType.TYPE_CLASS_TEXT);
        Spinner responsibleSpinner = addLabeledMemberSpinner(form, "Responsable");
        Spinner statusSpinner = addLabeledSpinner(form, "Estado", new String[]{"abierta", "en_progreso", "resuelta", "cancelada"});
        EditText estimatedEt = addLabeledEditText(form, "Coste estimado (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText finalEt = addLabeledEditText(form, "Coste final (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText detailEt = addLabeledEditText(form, "Descripción", "Describe la avería y el trabajo realizado", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        if (existingDoc != null) {
            titleEt.setText(safe(existingDoc.getString("title")));
            roomEt.setText(safe(existingDoc.getString("roomName")));
            selectMemberSpinnerEmail(responsibleSpinner, safe(existingDoc.getString("responsibleEmail")));
            selectSpinnerValue(statusSpinner, safe(existingDoc.getString("status")));
            estimatedEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("estimatedCost"))));
            finalEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("finalCost"))));
            detailEt.setText(safe(existingDoc.getString("description")));
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                existingDoc == null ? "Nueva incidencia" : "Editar incidencia",
                "Actualiza estado e historial de mantenimiento.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String title = titleEt.getText().toString().trim();
            if (title.isEmpty()) {
                NoticeUtils.show(requireContext(), "Indica un título para la incidencia");
                return;
            }
            String status = selectedSpinnerValue(statusSpinner);
            String responsible = selectedMemberEmail(responsibleSpinner);
            if (responsible.isEmpty()) {
                NoticeUtils.show(requireContext(), "Selecciona una persona responsable");
                return;
            }
            double estimatedCost = parseDouble(estimatedEt.getText().toString().trim(), -1);
            double finalCost = parseDouble(finalEt.getText().toString().trim(), -1);
            if (estimatedCost < 0 || finalCost < 0) {
                NoticeUtils.show(requireContext(), "Los costes no pueden ser negativos");
                return;
            }

            List<Map<String, Object>> history = existingDoc == null
                    ? new ArrayList<>()
                    : castMapList(existingDoc.get("history"));
            Map<String, Object> event = new HashMap<>();
            event.put("at", new Timestamp(new Date()));
            event.put("actorUid", authUid());
            event.put("actorEmail", authEmail());
            event.put("action", existingDoc == null ? "crear" : "editar");
            event.put("status", status);
            history.add(event);

            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("title", title);
            data.put("roomName", roomEt.getText().toString().trim());
            data.put("responsibleEmail", responsible);
            data.put("status", status);
            data.put("estimatedCost", estimatedCost);
            data.put("finalCost", finalCost);
            data.put("description", detailEt.getText().toString().trim());
            data.put("history", history);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedByUid", authUid());
            data.put("updatedByEmail", authEmail());
            if (existingDoc == null) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("createdByUid", authUid());
                data.put("createdByEmail", authEmail());
            }

            Task<?> saveTask = existingDoc == null
                    ? db.collection("maintenance_tickets").add(data)
                    : db.collection("maintenance_tickets").document(existingDoc.getId()).update(data);
            saveTask.addOnSuccessListener(done -> {
                writeAudit("Incidencias", existingDoc == null ? "crear" : "editar", title + " · " + status, existingDoc == null ? "" : existingDoc.getId());
                if ("abierta".equalsIgnoreCase(status)) {
                    applyEventRules(EVENT_INCIDENT_PENDING, new Date(), responsible, "Incidencia abierta: " + title);
                }
                dialog.dismiss();
                loadMaintenanceRows();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar incidencia", Toast.LENGTH_SHORT).show());
        });
    }

    private void openHouseRuleDialog(@Nullable DocumentSnapshot existingDoc) {
        if (!currentUserIsGroupOwner) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede crear o editar reglas");
            return;
        }
        loadGroupRoomLabels(roomLabels -> {
            if (!isAdded()) return;
            LinearLayout form = buildVerticalForm();
            EditText titleEt = addLabeledEditText(form, "Título", "Ejemplo: Silencio por la noche", InputType.TYPE_CLASS_TEXT);
            EditText detailEt = addLabeledEditText(form, "Detalle", "Explica la norma de la casa", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            Spinner scopeSpinner = addLabeledSpinner(form, "Aplicar a", new String[]{"Todos", "Persona", "Habitación"});
            Spinner memberSpinner = addLabeledMemberInlineSpinner(form, "Persona");
            Spinner roomSpinner = addLabeledSpinner(form, "Habitación", buildRoomOptions(roomLabels));

            updateScopeInputs(scopeSpinner, memberSpinner, roomSpinner);
            scopeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateScopeInputs(scopeSpinner, memberSpinner, roomSpinner);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            if (existingDoc != null) {
                titleEt.setText(safe(existingDoc.getString("title")));
                detailEt.setText(safe(existingDoc.getString("description")));
                selectScopeFromDoc(scopeSpinner, existingDoc);
                selectMemberSpinnerEmail(memberSpinner, safe(existingDoc.getString("targetMemberEmail")));
                selectSpinnerValue(roomSpinner, safe(existingDoc.getString("roomName")));
                updateScopeInputs(scopeSpinner, memberSpinner, roomSpinner);
            }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    existingDoc == null ? "Nueva regla" : "Editar regla",
                    "Define normas generales, por persona o por habitación.",
                    form,
                    "Cancelar",
                    "Guardar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            shell.confirmBtn.setOnClickListener(v -> {
                String title = titleEt.getText().toString().trim();
                String detail = detailEt.getText().toString().trim();
                if (title.isEmpty() || detail.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Completa título y detalle de la regla");
                    return;
                }

                ScopeSelection scope = resolveScopeSelection(scopeSpinner, memberSpinner, roomSpinner);
                if (!scope.valid) {
                    NoticeUtils.show(requireContext(), scope.errorMessage);
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("groupId", currentGroupId);
                data.put("title", title);
                data.put("description", detail);
                data.put("scopeType", scope.scopeType);
                data.put("targetMemberEmail", scope.memberEmail);
                data.put("targetEmails", scope.memberEmail.isEmpty() ? new ArrayList<>() : Collections.singletonList(scope.memberEmail));
                data.put("roomName", scope.roomName);
                data.put("updatedAt", FieldValue.serverTimestamp());
                data.put("updatedByUid", authUid());
                data.put("updatedByEmail", authEmail());
                if (existingDoc == null) {
                    data.put("createdAt", FieldValue.serverTimestamp());
                    data.put("createdByUid", authUid());
                    data.put("createdByEmail", authEmail());
                }

                Task<?> saveTask = existingDoc == null
                        ? db.collection("house_rules").add(data)
                        : db.collection("house_rules").document(existingDoc.getId()).update(data);
                saveTask.addOnSuccessListener(done -> {
                    writeAudit("Reglas", existingDoc == null ? "crear" : "editar", title + " · " + scope.summary, existingDoc == null ? "" : existingDoc.getId());
                    dialog.dismiss();
                    loadHouseRuleRows();
                }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar la regla", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void openScheduleDialog(@Nullable DocumentSnapshot existingDoc) {
        if (!currentUserIsGroupOwner) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede crear o editar horarios");
            return;
        }
        loadGroupRoomLabels(roomLabels -> {
            if (!isAdded()) return;
            LinearLayout form = buildVerticalForm();
            EditText titleEt = addLabeledEditText(form, "Título o zona", "Ejemplo: Baño principal", InputType.TYPE_CLASS_TEXT);
            EditText detailEt = addLabeledEditText(form, "Qué ocurre", "Ejemplo: Uso exclusivo de Sergio", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            Spinner scopeSpinner = addLabeledSpinner(form, "Aplicar a", new String[]{"Todos", "Persona", "Habitación"});
            Spinner memberSpinner = addLabeledMemberInlineSpinner(form, "Persona");
            Spinner roomSpinner = addLabeledSpinner(form, "Habitación", buildRoomOptions(roomLabels));
            Spinner frequencySpinner = addLabeledSpinner(form, "Frecuencia", new String[]{"diario", "semanal", "mensual"});
            EditText startDateEt = addLabeledEditText(form, "Empieza (DD/MM/AAAA)", "04/06/2026", InputType.TYPE_CLASS_TEXT);
            EditText endDateEt = addLabeledEditText(form, "Termina (opcional) (DD/MM/AAAA)", "", InputType.TYPE_CLASS_TEXT);
            EditText startTimeEt = addLabeledEditText(form, "Hora inicio (HH:MM)", "17:00", InputType.TYPE_CLASS_TEXT);
            EditText endTimeEt = addLabeledEditText(form, "Hora fin (HH:MM)", "19:00", InputType.TYPE_CLASS_TEXT);
            setupDatePicker(startDateEt);
            setupDatePicker(endDateEt);
            setupTimePicker(startTimeEt);
            setupTimePicker(endTimeEt);
            startTimeEt.setText("17:00");
            endTimeEt.setText("19:00");

            updateScopeInputs(scopeSpinner, memberSpinner, roomSpinner);
            scopeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateScopeInputs(scopeSpinner, memberSpinner, roomSpinner);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            if (existingDoc != null) {
                titleEt.setText(safe(existingDoc.getString("title")));
                detailEt.setText(safe(existingDoc.getString("description")));
                selectScopeFromDoc(scopeSpinner, existingDoc);
                selectMemberSpinnerEmail(memberSpinner, safe(existingDoc.getString("targetMemberEmail")));
                selectSpinnerValue(roomSpinner, safe(existingDoc.getString("roomName")));
                selectSpinnerValue(frequencySpinner, safe(existingDoc.getString("frequency")));
                startDateEt.setText(normalizeDateText(existingDoc.getString("startDateText")));
                endDateEt.setText(normalizeDateText(existingDoc.getString("endDateText")));
                startTimeEt.setText(safe(existingDoc.getString("startTimeText")));
                endTimeEt.setText(safe(existingDoc.getString("endTimeText")));
                updateScopeInputs(scopeSpinner, memberSpinner, roomSpinner);
            }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    existingDoc == null ? "Nuevo horario" : "Editar horario",
                    "Reserva zonas y turnos recurrentes del piso.",
                    form,
                    "Cancelar",
                    "Guardar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            shell.confirmBtn.setOnClickListener(v -> {
                String title = titleEt.getText().toString().trim();
                String detail = detailEt.getText().toString().trim();
                String startDateText = normalizeDateText(startDateEt.getText().toString().trim());
                String endDateText = normalizeDateText(endDateEt.getText().toString().trim());
                String startTimeText = normalizeTimeText(startTimeEt.getText().toString().trim());
                String endTimeText = normalizeTimeText(endTimeEt.getText().toString().trim());
                if (title.isEmpty() || detail.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Completa el título y la descripción del horario");
                    return;
                }
                if (!isValidDate(startDateText)) {
                    NoticeUtils.show(requireContext(), "La fecha de inicio no es válida");
                    return;
                }
                if (!endDateText.isEmpty() && !isValidDate(endDateText)) {
                    NoticeUtils.show(requireContext(), "La fecha de fin no es válida");
                    return;
                }
                if (!isValidTime(startTimeText) || !isValidTime(endTimeText)) {
                    NoticeUtils.show(requireContext(), "Las horas deben tener formato HH:MM");
                    return;
                }
                int startMinutes = parseTimeToMinutes(startTimeText);
                int endMinutes = parseTimeToMinutes(endTimeText);
                if (endMinutes <= startMinutes) {
                    NoticeUtils.show(requireContext(), "La hora de fin debe ser posterior a la de inicio");
                    return;
                }
                startTimeEt.setText(startTimeText);
                endTimeEt.setText(endTimeText);

                Date startAt = combineDateAndTime(startDateText, startTimeText);
                Date endAt = endDateText.isEmpty() ? null : combineDateAndTime(endDateText, endTimeText);
                if (startAt == null || (!endDateText.isEmpty() && endAt == null)) {
                    NoticeUtils.show(requireContext(), "No se pudo interpretar la fecha del horario");
                    return;
                }
                if (endAt != null && endAt.before(startAt)) {
                    NoticeUtils.show(requireContext(), "La fecha de fin no puede ser anterior al inicio");
                    return;
                }

                ScopeSelection scope = resolveScopeSelection(scopeSpinner, memberSpinner, roomSpinner);
                if (!scope.valid) {
                    NoticeUtils.show(requireContext(), scope.errorMessage);
                    return;
                }

                String frequency = selectedSpinnerValue(frequencySpinner).toLowerCase(Locale.ROOT);
                Map<String, Object> data = new HashMap<>();
                data.put("groupId", currentGroupId);
                data.put("title", title);
                data.put("description", detail);
                data.put("scopeType", scope.scopeType);
                data.put("targetMemberEmail", scope.memberEmail);
                data.put("targetEmails", scope.memberEmail.isEmpty() ? new ArrayList<>() : Collections.singletonList(scope.memberEmail));
                data.put("roomName", scope.roomName);
                data.put("frequency", frequency);
                data.put("startAt", startAt);
                data.put("startDateText", startDateText);
                data.put("endAt", endAt);
                data.put("endDateText", endDateText);
                data.put("startTimeText", startTimeText);
                data.put("endTimeText", endTimeText);
                data.put("updatedAt", FieldValue.serverTimestamp());
                data.put("updatedByUid", authUid());
                data.put("updatedByEmail", authEmail());
                if (existingDoc == null) {
                    data.put("createdAt", FieldValue.serverTimestamp());
                    data.put("createdByUid", authUid());
                    data.put("createdByEmail", authEmail());
                }

                Task<?> saveTask = existingDoc == null
                        ? db.collection("group_schedules").add(data)
                        : db.collection("group_schedules").document(existingDoc.getId()).update(data);
                saveTask.addOnSuccessListener(done -> {
                    writeAudit("Horarios", existingDoc == null ? "crear" : "editar", title + " · " + startTimeText + "-" + endTimeText, existingDoc == null ? "" : existingDoc.getId());
                    dialog.dismiss();
                    loadScheduleRows();
                }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar el horario", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void openDocumentDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        Spinner typeSpinner = addLabeledSpinner(form, "Tipo", new String[]{"contrato", "factura", "inventario", "foto", "acta"});
        EditText titleEt = addLabeledEditText(form, "Título", "Ejemplo: Contrato alquiler 2026", InputType.TYPE_CLASS_TEXT);
        EditText dateEt = addLabeledEditText(form, "Fecha (DD/MM/AAAA)", "13/05/2026", InputType.TYPE_CLASS_TEXT);
        EditText refEt = addLabeledEditText(form, "Referencia/URL (opcional)", "https://...", InputType.TYPE_CLASS_TEXT);
        EditText notesEt = addLabeledEditText(form, "Notas", "Detalle opcional", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        Button attachBtn = new Button(requireContext());
        attachBtn.setText("Adjuntar archivo del móvil");
        attachBtn.setAllCaps(false);
        attachBtn.setBackgroundResource(R.drawable.bg_button_pill_secondary);
        attachBtn.setTextColor(requireContext().getColor(R.color.text_light));
        LinearLayout.LayoutParams attachLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        attachLp.topMargin = dp(10);
        attachBtn.setLayoutParams(attachLp);
        form.addView(attachBtn);

        TextView attachStatusTv = new TextView(requireContext());
        attachStatusTv.setText("Sin archivo adjunto. También puedes guardar solo una URL.");
        attachStatusTv.setTextColor(requireContext().getColor(R.color.text_muted));
        attachStatusTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        form.addView(attachStatusTv);
        setupDatePicker(dateEt);

        pendingDocumentFileUri = null;
        pendingDocumentReferenceEt = refEt;
        pendingDocumentStatusTv = attachStatusTv;

        if (existingDoc != null) {
            selectSpinnerValue(typeSpinner, safe(existingDoc.getString("type")));
            titleEt.setText(safe(existingDoc.getString("title")));
            dateEt.setText(normalizeDateText(existingDoc.getString("documentDate")));
            refEt.setText(safe(existingDoc.getString("referenceUri")));
            notesEt.setText(safe(existingDoc.getString("notes")));
            if (!safe(existingDoc.getString("referenceUri")).isEmpty()) {
                attachStatusTv.setText("URL actual cargada. Puedes reemplazarla adjuntando un archivo.");
            }
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                existingDoc == null ? "Nuevo documento" : "Editar documento",
                "Guarda contrato, factura, inventario, foto o acta con archivo o URL.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        attachBtn.setOnClickListener(v -> documentPickerLauncher.launch(new String[]{"*/*"}));
        shell.cancelBtn.setOnClickListener(v -> {
            pendingDocumentFileUri = null;
            pendingDocumentReferenceEt = null;
            pendingDocumentStatusTv = null;
            dialog.dismiss();
        });
        shell.confirmBtn.setOnClickListener(v -> {
            String date = normalizeDateText(dateEt.getText().toString().trim());
            if (!date.isEmpty() && !isValidDate(date)) {
                NoticeUtils.show(requireContext(), "Fecha inválida. Usa DD/MM/AAAA");
                return;
            }
            String title = titleEt.getText().toString().trim();
            if (title.isEmpty()) {
                NoticeUtils.show(requireContext(), "El título es obligatorio");
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("type", selectedSpinnerValue(typeSpinner));
            data.put("title", title);
            data.put("documentDate", date);
            data.put("notes", notesEt.getText().toString().trim());
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedByUid", authUid());
            data.put("updatedByEmail", authEmail());
            if (existingDoc == null) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("createdByUid", authUid());
                data.put("createdByEmail", authEmail());
            }

            String manualReference = refEt.getText().toString().trim();
            if (pendingDocumentFileUri != null) {
                shell.confirmBtn.setEnabled(false);
                shell.cancelBtn.setEnabled(false);
                attachBtn.setEnabled(false);
                attachStatusTv.setText("Subiendo archivo...");
                uploadDocumentFileAndSave(existingDoc, data, title, manualReference, dialog, shell.confirmBtn, shell.cancelBtn, attachBtn, attachStatusTv);
            } else {
                data.put("referenceUri", manualReference);
                persistDocumentData(existingDoc, data, title, dialog);
            }
        });
    }

    private void handleDocumentFileSelected(@Nullable Uri uri) {
        if (uri == null || !isAdded()) return;
        pendingDocumentFileUri = uri;
        if (pendingDocumentReferenceEt != null) pendingDocumentReferenceEt.setText("");
        if (pendingDocumentStatusTv != null) {
            pendingDocumentStatusTv.setText("Archivo seleccionado: " + resolveFileName(uri));
        }
    }

    private void uploadDocumentFileAndSave(
            @Nullable DocumentSnapshot existingDoc,
            Map<String, Object> data,
            String title,
            String manualReference,
            AlertDialog dialog,
            Button confirmBtn,
            Button cancelBtn,
            Button attachBtn,
            TextView attachStatusTv
    ) {
        Uri uri = pendingDocumentFileUri;
        if (uri == null) {
            data.put("referenceUri", manualReference);
            persistDocumentData(existingDoc, data, title, dialog);
            return;
        }
        String safeName = sanitizeStorageSegment(resolveFileName(uri));
        if (safeName.isEmpty()) safeName = "documento";
        String objectPath = "group_documents/" + currentGroupId + "/" + System.currentTimeMillis() + "_" + safeName;
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(objectPath);
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    data.put("referenceUri", downloadUri.toString());
                    persistDocumentData(existingDoc, data, title, dialog);
                })
                .addOnFailureListener(e -> {
                    confirmBtn.setEnabled(true);
                    cancelBtn.setEnabled(true);
                    attachBtn.setEnabled(true);
                    attachStatusTv.setText("No se pudo subir el archivo. Revisa permisos o conexión.");
                    Toast.makeText(requireContext(), "No se pudo subir el archivo", Toast.LENGTH_SHORT).show();
                });
    }

    private void persistDocumentData(
            @Nullable DocumentSnapshot existingDoc,
            Map<String, Object> data,
            String title,
            AlertDialog dialog
    ) {
        Task<?> saveTask = existingDoc == null
                ? db.collection("group_documents").add(data)
                : db.collection("group_documents").document(existingDoc.getId()).update(data);
        saveTask.addOnSuccessListener(done -> {
            writeAudit("Documentos", existingDoc == null ? "crear" : "editar", title, existingDoc == null ? "" : existingDoc.getId());
            pendingDocumentFileUri = null;
            pendingDocumentReferenceEt = null;
            pendingDocumentStatusTv = null;
            dialog.dismiss();
            loadDocumentRows();
        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar documento", Toast.LENGTH_SHORT).show());
    }

    private String resolveFileName(@NonNull Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIdx >= 0) {
                    String value = safe(cursor.getString(nameIdx));
                    if (!value.isEmpty()) return value;
                }
            }
        } catch (Exception ignored) {
        }
        String path = uri.getLastPathSegment();
        if (path == null || path.trim().isEmpty()) return "documento";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private String sanitizeStorageSegment(String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) return "";
        return safeValue
                .replace("\\", "_")
                .replace("/", "_")
                .replace("#", "_")
                .replace("?", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace(":", "_");
    }
    private void showAutomationActionsDialog() {
        LinearLayout form = buildVerticalForm();
        Spinner actionSpinner = addLabeledSpinner(form, "Acción", new String[]{"Crear regla", "Ejecutar reglas ahora"});
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Automatización",
                "Elige una acción para rentas recurrentes.",
                form,
                "Cancelar",
                "Continuar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String selected = selectedSpinnerValue(actionSpinner);
            dialog.dismiss();
            if ("Ejecutar reglas ahora".equals(selected)) {
                runRentAutomationsNow();
            } else {
                openAutomationDialog(null);
            }
        });
    }

    private void openAutomationDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        EditText roomEt = addLabeledEditText(form, "Habitación", "Habitación 1", InputType.TYPE_CLASS_TEXT);
        Spinner tenantSpinner = addLabeledMemberSpinner(form, "Inquilino");
        EditText rentEt = addLabeledEditText(form, "Renta mensual (EUR)", "450", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText billingDayEt = addLabeledEditText(form, "Día de cobro (1-28)", "5", InputType.TYPE_CLASS_NUMBER);
        EditText startEt = addLabeledEditText(form, "Inicio ocupación (DD/MM/AAAA)", "01/05/2026", InputType.TYPE_CLASS_TEXT);
        EditText endEt = addLabeledEditText(form, "Fin ocupación (opcional, DD/MM/AAAA)", "", InputType.TYPE_CLASS_TEXT);
        setupDatePicker(startEt);
        setupDatePicker(endEt);

        if (existingDoc != null) {
            roomEt.setText(safe(existingDoc.getString("roomName")));
            selectMemberSpinnerEmail(tenantSpinner, safe(existingDoc.getString("tenantEmail")));
            rentEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("monthlyRent"))));
            billingDayEt.setText(String.valueOf(safeLong(existingDoc.getLong("billingDay"))));
            startEt.setText(normalizeDateText(existingDoc.getString("startDate")));
            endEt.setText(normalizeDateText(existingDoc.getString("endDate")));
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                existingDoc == null ? "Nueva regla automática" : "Editar regla automática",
                "Rentas recurrentes con prorrateo por entrada/salida.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String startDate = normalizeDateText(startEt.getText().toString().trim());
            String endDate = normalizeDateText(endEt.getText().toString().trim());
            if (!isValidDate(startDate) || (!endDate.isEmpty() && !isValidDate(endDate))) {
                NoticeUtils.show(requireContext(), "Revisa fechas de ocupación");
                return;
            }
            long billingDay = parseLong(billingDayEt.getText().toString().trim(), -1);
            if (billingDay < 1 || billingDay > 28) {
                NoticeUtils.show(requireContext(), "El día de cobro debe estar entre 1 y 28");
                return;
            }
            double rent = parseDouble(rentEt.getText().toString().trim(), -1);
            if (rent <= 0) {
                NoticeUtils.show(requireContext(), "La renta mensual debe ser mayor que 0");
                return;
            }
            String tenantEmail = selectedMemberEmail(tenantSpinner);
            if (tenantEmail.isEmpty()) {
                NoticeUtils.show(requireContext(), "Selecciona un inquilino valido");
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("roomName", roomEt.getText().toString().trim());
            data.put("tenantEmail", tenantEmail);
            data.put("monthlyRent", rent);
            data.put("billingDay", billingDay);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedByUid", authUid());
            data.put("updatedByEmail", authEmail());
            if (existingDoc == null) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("createdByUid", authUid());
                data.put("createdByEmail", authEmail());
            }

            Task<?> saveTask = existingDoc == null
                    ? db.collection("rent_automations").add(data)
                    : db.collection("rent_automations").document(existingDoc.getId()).update(data);

            saveTask.addOnSuccessListener(done -> {
                writeAudit("Automatización", existingDoc == null ? "crear" : "editar", "Regla para " + data.get("tenantEmail"), existingDoc == null ? "" : existingDoc.getId());
                dialog.dismiss();
                loadAutomationRows();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar automatización", Toast.LENGTH_SHORT).show());
        });
    }

    private void runRentAutomationsNow() {
        db.collection("rent_automations")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(rulesSnapshot -> {
                    List<DocumentSnapshot> rules = rulesSnapshot.getDocuments();
                    if (rules.isEmpty()) {
                        NoticeUtils.show(requireContext(), "No hay reglas de automatización");
                        return;
                    }
                    db.collection("rent_collections")
                            .whereEqualTo("groupId", currentGroupId)
                            .get()
                            .addOnSuccessListener(chargesSnapshot -> {
                                List<String> existingKeys = new ArrayList<>();
                                for (DocumentSnapshot charge : chargesSnapshot.getDocuments()) {
                                    existingKeys.add(safe(charge.getString("uniqueKey")));
                                }
                                List<Task<?>> tasks = new ArrayList<>();
                                int created = 0;
                                for (DocumentSnapshot rule : rules) {
                                    created += buildChargesFromRule(rule, existingKeys, tasks);
                                }
                                if (tasks.isEmpty()) {
                                    NoticeUtils.show(requireContext(), "No hay nuevos cobros por generar");
                                    return;
                                }
                                int createdFinal = created;
                                Tasks.whenAllComplete(tasks).addOnSuccessListener(done -> {
                                    writeAudit("Automatización", "ejecutar", "Cobros generados: " + createdFinal, "");
                                    NoticeUtils.show(requireContext(), "Cobros generados: " + createdFinal);
                                    loadRentRows();
                                }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Error generando cobros automáticos", Toast.LENGTH_SHORT).show());
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudieron cargar automatizaciones", Toast.LENGTH_SHORT).show());
    }

    private int buildChargesFromRule(DocumentSnapshot rule, List<String> existingKeys, List<Task<?>> tasks) {
        String roomName = safe(rule.getString("roomName"));
        String tenant = safe(rule.getString("tenantEmail"));
        double monthlyRent = safeDouble(rule.getDouble("monthlyRent"));
        long billingDay = safeLong(rule.getLong("billingDay"));
        Date startDate = parseDate(safe(rule.getString("startDate")));
        Date endDate = parseDate(safe(rule.getString("endDate")));
        if (startDate == null || monthlyRent <= 0 || billingDay < 1 || billingDay > 28) return 0;

        Calendar monthCursor = Calendar.getInstance();
        monthCursor.setTime(startDate);
        monthCursor.set(Calendar.DAY_OF_MONTH, 1);
        zeroTime(monthCursor);

        Calendar monthLimit = Calendar.getInstance();
        monthLimit.set(Calendar.DAY_OF_MONTH, 1);
        zeroTime(monthLimit);

        int created = 0;
        while (!monthCursor.after(monthLimit)) {
            int year = monthCursor.get(Calendar.YEAR);
            int month = monthCursor.get(Calendar.MONTH);
            String monthKey = String.format(Locale.ROOT, "%04d-%02d", year, month + 1);

            String uniqueKey = currentGroupId + "|" + monthKey + "|" + roomName.toLowerCase(Locale.ROOT) + "|" + tenant.toLowerCase(Locale.ROOT);
            if (!existingKeys.contains(uniqueKey)) {
                ProrationResult proration = calculateProration(startDate, endDate, year, month);
                if (proration.occupiedDays > 0) {
                    double amountBase = round2(monthlyRent * proration.ratio);
                    Calendar dueCalendar = Calendar.getInstance();
                    dueCalendar.set(year, month, (int) billingDay, 10, 0, 0);
                    dueCalendar.set(Calendar.MILLISECOND, 0);
                    String dueDateText = dateFormat.format(dueCalendar.getTime());
                    String status = dueCalendar.getTime().before(new Date()) ? "atrasado" : "pendiente";

                    Map<String, Object> charge = new LinkedHashMap<>();
                    charge.put("groupId", currentGroupId);
                    charge.put("monthKey", monthKey);
                    charge.put("roomName", roomName);
                    charge.put("tenantEmail", tenant);
                    charge.put("amountBase", amountBase);
                    charge.put("amountPaid", 0.0);
                    charge.put("surcharge", 0.0);
                    charge.put("status", status);
                    charge.put("dueDateText", dueDateText);
                    charge.put("dueAt", new Timestamp(dueCalendar.getTime()));
                    charge.put("prorated", proration.ratio < 0.999d);
                    charge.put("occupiedDays", proration.occupiedDays);
                    charge.put("daysInMonth", proration.daysInMonth);
                    charge.put("generatedByRuleId", rule.getId());
                    charge.put("uniqueKey", uniqueKey);
                    charge.put("createdAt", FieldValue.serverTimestamp());
                    charge.put("createdByUid", authUid());
                    charge.put("createdByEmail", authEmail());
                    charge.put("updatedAt", FieldValue.serverTimestamp());
                    charge.put("updatedByUid", authUid());
                    charge.put("updatedByEmail", authEmail());

                    tasks.add(db.collection("rent_collections").add(charge));
                    applyEventRules(EVENT_RENT_DUE, dueCalendar.getTime(), tenant, "Vence renta de " + roomName + " (" + monthKey + ")");
                    if ("atrasado".equalsIgnoreCase(status)) {
                        applyEventRules(EVENT_RENT_OVERDUE, new Date(), tenant, "Impago en " + roomName + " (" + monthKey + ")");
                    }
                    existingKeys.add(uniqueKey);
                    created++;
                }
            }
            monthCursor.add(Calendar.MONTH, 1);
        }
        return created;
    }

    private void openReminderRuleDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        Spinner eventSpinner = addLabeledSpinner(
                form,
                "Evento",
                new String[]{"rent_due", "rent_overdue", "contract_ending", "incident_pending"}
        );
        EditText offsetEt = addLabeledEditText(form, "Offset días (puede ser negativo)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        EditText titleEt = addLabeledEditText(form, "Título push", "Recordatorio de alquiler", InputType.TYPE_CLASS_TEXT);
        EditText bodyEt = addLabeledEditText(form, "Mensaje push", "Revisa el evento del piso", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        Spinner enabledSpinner = addLabeledSpinner(form, "Activa", new String[]{"si", "no"});

        if (existingDoc != null) {
            selectSpinnerValue(eventSpinner, safe(existingDoc.getString("eventType")));
            offsetEt.setText(String.valueOf(safeLong(existingDoc.getLong("offsetDays"))));
            titleEt.setText(safe(existingDoc.getString("titleTemplate")));
            bodyEt.setText(safe(existingDoc.getString("bodyTemplate")));
            selectSpinnerValue(enabledSpinner, Boolean.TRUE.equals(existingDoc.getBoolean("enabled")) ? "si" : "no");
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                existingDoc == null ? "Nueva regla push" : "Editar regla push",
                "Dispara notificaciones por vencimientos, impagos y eventos clave.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            long offsetDays = parseLong(offsetEt.getText().toString().trim(), Long.MIN_VALUE);
            if (offsetDays == Long.MIN_VALUE) {
                NoticeUtils.show(requireContext(), "Offset inválido");
                return;
            }
            String title = titleEt.getText().toString().trim();
            String body = bodyEt.getText().toString().trim();
            if (title.isEmpty() || body.isEmpty()) {
                NoticeUtils.show(requireContext(), "Título y mensaje son obligatorios");
                return;
            }
            boolean enabled = "si".equalsIgnoreCase(selectedSpinnerValue(enabledSpinner));
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("eventType", selectedSpinnerValue(eventSpinner));
            data.put("offsetDays", offsetDays);
            data.put("titleTemplate", title);
            data.put("bodyTemplate", body);
            data.put("enabled", enabled);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedByUid", authUid());
            data.put("updatedByEmail", authEmail());
            if (existingDoc == null) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("createdByUid", authUid());
                data.put("createdByEmail", authEmail());
            }

            Task<?> saveTask = existingDoc == null
                    ? db.collection("event_reminder_rules").add(data)
                    : db.collection("event_reminder_rules").document(existingDoc.getId()).update(data);
            saveTask.addOnSuccessListener(done -> {
                writeAudit("Reglas push", existingDoc == null ? "crear" : "editar", "Regla " + selectedSpinnerValue(eventSpinner), existingDoc == null ? "" : existingDoc.getId());
                dialog.dismiss();
                loadReminderRuleRows();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar regla", Toast.LENGTH_SHORT).show());
        });
    }

    private void applyEventRules(String eventType, @Nullable Date baseDate, @Nullable String targetEmail, @NonNull String fallbackBody) {
        if (baseDate == null || !isAdded() || currentGroupId == null) return;
        db.collection("event_reminder_rules")
                .whereEqualTo("groupId", currentGroupId)
                .whereEqualTo("eventType", eventType)
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot rule : snapshot.getDocuments()) {
                        long offset = safeLong(rule.getLong("offsetDays"));
                        Calendar trigger = Calendar.getInstance();
                        trigger.setTime(baseDate);
                        trigger.add(Calendar.DAY_OF_MONTH, (int) offset);
                        if (trigger.getTime().before(new Date())) {
                            trigger.setTime(new Date());
                            trigger.add(Calendar.MINUTE, 1);
                        }
                        String title = safe(rule.getString("titleTemplate"));
                        String body = safe(rule.getString("bodyTemplate"));
                        if (title.isEmpty()) title = "Recordatorio";
                        if (body.isEmpty()) body = fallbackBody;

                        int reminderCode = random.nextInt(Integer.MAX_VALUE - 1) + 1;
                        ReminderScheduler.scheduleOneTime(requireContext(), reminderCode, title, body, trigger.getTimeInMillis());

                        Map<String, Object> job = new HashMap<>();
                        job.put("groupId", currentGroupId);
                        job.put("ruleId", rule.getId());
                        job.put("eventType", eventType);
                        job.put("targetEmail", safe(targetEmail));
                        job.put("title", title);
                        job.put("body", body);
                        job.put("triggerAt", new Timestamp(trigger.getTime()));
                        job.put("reminderCode", reminderCode);
                        job.put("createdAt", FieldValue.serverTimestamp());
                        db.collection("event_reminder_jobs").add(job);
                    }
                });
    }

    private void writeAudit(String module, String action, String details, String entityId) {
        if (!isAdded() || currentGroupId == null) return;
        Map<String, Object> log = new HashMap<>();
        log.put("groupId", currentGroupId);
        log.put("module", module);
        log.put("action", action);
        log.put("details", details);
        log.put("entityId", entityId == null ? "" : entityId);
        log.put("actorUid", authUid());
        log.put("actorEmail", authEmail());
        log.put("createdAt", FieldValue.serverTimestamp());
        db.collection("audit_events").add(log);
    }

    private void onRowsReady(String emptyMessage) {
        adapter.notifyDataSetChanged();
        if (rows.isEmpty()) {
            emptyTv.setText(emptyMessage);
            emptyTv.setVisibility(View.VISIBLE);
        } else {
            emptyTv.setVisibility(View.GONE);
        }
    }

    private void onLoadError(String message) {
        rows.clear();
        adapter.notifyDataSetChanged();
        emptyTv.setText(message);
        emptyTv.setVisibility(View.VISIBLE);
    }

    private boolean hasRoomFilterContext() {
        return currentRoomFilterId != null && !currentRoomFilterId.trim().isEmpty();
    }

    private boolean matchesManagementRoomFilter(
            @NonNull DocumentSnapshot doc,
            @Nullable String roomName,
            @Nullable String memberEmail,
            @Nullable String scopeType
    ) {
        if (!hasRoomFilterContext()) return true;
        String normalizedScope = safe(scopeType).trim().toLowerCase(Locale.ROOT);
        if ("room".equals(normalizedScope)) {
            return sameRoomName(roomName, currentRoomFilterName);
        }
        if ("member".equals(normalizedScope)) {
            String normalizedMember = normalizeEmail(memberEmail);
            return !normalizedMember.isEmpty() && currentRoomFilterMembers.contains(normalizedMember);
        }
        String directRoomId = safe(doc.getString("roomId"));
        if (!directRoomId.isEmpty() && directRoomId.equals(currentRoomFilterId)) {
            return true;
        }
        if (!safe(roomName).isEmpty()) {
            return sameRoomName(roomName, currentRoomFilterName);
        }
        if (memberEmail != null && !memberEmail.trim().isEmpty()) {
            String normalizedMember = normalizeEmail(memberEmail);
            return !normalizedMember.isEmpty() && currentRoomFilterMembers.contains(normalizedMember);
        }
        return "everyone".equals(normalizedScope) || normalizedScope.isEmpty();
    }

    private boolean sameRoomName(@Nullable String left, @Nullable String right) {
        String safeLeft = safe(left).trim();
        String safeRight = safe(right).trim();
        if (safeLeft.isEmpty() || safeRight.isEmpty()) return false;
        return safeLeft.equalsIgnoreCase(safeRight);
    }

    private int compareByTimestampDesc(DocumentSnapshot a, DocumentSnapshot b, String primary, String secondary) {
        Timestamp ta = a.getTimestamp(primary);
        if (ta == null) ta = a.getTimestamp(secondary);
        Timestamp tb = b.getTimestamp(primary);
        if (tb == null) tb = b.getTimestamp(secondary);
        long va = ta == null ? 0L : ta.toDate().getTime();
        long vb = tb == null ? 0L : tb.toDate().getTime();
        return Long.compare(vb, va);
    }

    private String selectedSpinnerValue(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        return selected == null ? "" : selected.toString().trim();
    }

    private String selectedMemberEmail(Spinner spinner) {
        if (groupMemberEmails.isEmpty()) return "";
        int index = spinner.getSelectedItemPosition();
        if (index < 0 || index >= groupMemberEmails.size()) return "";
        return groupMemberEmails.get(index);
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && value.equalsIgnoreCase(item.toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void selectMemberSpinnerEmail(Spinner spinner, @Nullable String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return;
        for (int i = 0; i < groupMemberEmails.size(); i++) {
            if (normalized.equals(groupMemberEmails.get(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private LinearLayout buildVerticalForm() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private EditText addLabeledEditText(LinearLayout parent, String label, String hint, int inputType) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextColor(requireContext().getColor(R.color.text_light));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        if (parent.getChildCount() > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            tv.setLayoutParams(lp);
        }
        parent.addView(tv);

        EditText et = new EditText(requireContext());
        et.setBackgroundResource(R.drawable.bg_input_dark_round);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setPadding(dp(14), dp(10), dp(14), dp(10));
        et.setTextColor(requireContext().getColor(R.color.text_light));
        et.setHintTextColor(requireContext().getColor(R.color.text_muted));
        parent.addView(et);
        return et;
    }

    private Spinner addLabeledSpinner(LinearLayout parent, String label, String[] values) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextColor(requireContext().getColor(R.color.text_light));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        if (parent.getChildCount() > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            tv.setLayoutParams(lp);
        }
        parent.addView(tv);

        Spinner spinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        spinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        spinner.setPadding(dp(10), 0, dp(10), 0);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_selected,
                values
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
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
        parent.addView(spinner);
        return spinner;
    }

    private Spinner addLabeledMemberSpinner(LinearLayout parent, String label) {
        if (groupMemberEmails.isEmpty()) {
            return addLabeledSpinner(parent, label, new String[]{"Sin miembros"});
        }
        String[] labels = new String[groupMemberEmails.size()];
        for (int i = 0; i < groupMemberEmails.size(); i++) {
            labels[i] = formatMemberTwoLines(groupMemberEmails.get(i));
        }
        return addLabeledSpinner(parent, label, labels);
    }

    private Spinner addLabeledMemberInlineSpinner(LinearLayout parent, String label) {
        if (groupMemberEmails.isEmpty()) {
            return addLabeledSpinner(parent, label, new String[]{"Sin miembros"});
        }
        String[] labels = new String[groupMemberEmails.size()];
        for (int i = 0; i < groupMemberEmails.size(); i++) {
            labels[i] = formatMemberInline(groupMemberEmails.get(i));
        }
        return addLabeledSpinner(parent, label, labels);
    }

    private String[] buildRoomOptions(@NonNull List<String> roomLabels) {
        if (roomLabels.isEmpty()) {
            return new String[]{"Sin habitaciones"};
        }
        return roomLabels.toArray(new String[0]);
    }

    private void updateScopeInputs(@NonNull Spinner scopeSpinner, @NonNull Spinner memberSpinner, @NonNull Spinner roomSpinner) {
        String scope = selectedSpinnerValue(scopeSpinner).toLowerCase(Locale.ROOT);
        memberSpinner.setVisibility("persona".equals(scope) ? View.VISIBLE : View.GONE);
        roomSpinner.setVisibility("habitación".equals(scope) || "habitacion".equals(scope) ? View.VISIBLE : View.GONE);
    }

    private void selectScopeFromDoc(@NonNull Spinner scopeSpinner, @NonNull DocumentSnapshot doc) {
        String scopeType = safe(doc.getString("scopeType")).toLowerCase(Locale.ROOT);
        if ("member".equals(scopeType)) {
            selectSpinnerValue(scopeSpinner, "Persona");
            return;
        }
        if ("room".equals(scopeType)) {
            selectSpinnerValue(scopeSpinner, "Habitación");
            return;
        }
        selectSpinnerValue(scopeSpinner, "Todos");
    }

    @NonNull
    private ScopeSelection resolveScopeSelection(@NonNull Spinner scopeSpinner, @NonNull Spinner memberSpinner, @NonNull Spinner roomSpinner) {
        String scope = selectedSpinnerValue(scopeSpinner).toLowerCase(Locale.ROOT);
        if ("persona".equals(scope)) {
            String memberEmail = selectedMemberEmail(memberSpinner);
            if (memberEmail.isEmpty()) {
                return ScopeSelection.invalid("Selecciona una persona válida");
            }
            return ScopeSelection.member(memberEmail, formatMemberInline(memberEmail));
        }
        if ("habitación".equals(scope) || "habitacion".equals(scope)) {
            String roomName = selectedSpinnerValue(roomSpinner);
            if (roomName.isEmpty() || "Sin habitaciones".equalsIgnoreCase(roomName)) {
                return ScopeSelection.invalid("Selecciona una habitación válida");
            }
            return ScopeSelection.room(roomName);
        }
        return ScopeSelection.everyone();
    }

    private String buildRuleScopeSummary(@NonNull DocumentSnapshot doc) {
        String scopeType = safe(doc.getString("scopeType")).toLowerCase(Locale.ROOT);
        if ("member".equals(scopeType)) {
            String email = safe(doc.getString("targetMemberEmail"));
            return "Para: " + (email.isEmpty() ? "Persona" : formatMemberInline(email));
        }
        if ("room".equals(scopeType)) {
            String roomName = safe(doc.getString("roomName"));
            return "Habitación: " + (roomName.isEmpty() ? "Sin definir" : roomName);
        }
        return "Para todo el piso";
    }

    private String buildScheduleSummary(@NonNull DocumentSnapshot doc) {
        String scopeSummary = buildRuleScopeSummary(doc);
        String frequency = capitalizeLabel(safe(doc.getString("frequency")));
        String startDate = normalizeDateText(doc.getString("startDateText"));
        String endDate = normalizeDateText(doc.getString("endDateText"));
        String dateSummary = startDate.isEmpty() ? "" : (" · Desde " + startDate + (endDate.isEmpty() ? "" : " hasta " + endDate));
        return scopeSummary + " · " + frequency + dateSummary;
    }

    private String capitalizeLabel(@Nullable String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) return "";
        return Character.toUpperCase(safeValue.charAt(0)) + safeValue.substring(1);
    }

    private void loadGroupRoomLabels(@NonNull StringListCallback callback) {
        if (currentGroupId == null || currentGroupId.trim().isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }
        db.collection("rooms_groups")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort((a, b) -> {
                        Long n1 = a.getLong("roomNumber");
                        Long n2 = b.getLong("roomNumber");
                        long v1 = n1 == null ? Long.MAX_VALUE : n1;
                        long v2 = n2 == null ? Long.MAX_VALUE : n2;
                        return Long.compare(v1, v2);
                    });

                    List<String> roomLabels = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        String name = safe(doc.getString("name"));
                        Long roomNumber = doc.getLong("roomNumber");
                        String label;
                        if (roomNumber == null || roomNumber <= 0) {
                            label = name.isEmpty() ? "Habitación" : name;
                        } else {
                            label = "Hab. " + roomNumber + " - " + (name.isEmpty() ? "Habitación" : name);
                        }
                        if (!roomLabels.contains(label)) roomLabels.add(label);
                    }
                    callback.onLoaded(roomLabels);
                })
                .addOnFailureListener(e -> callback.onLoaded(new ArrayList<>()));
    }

    private void setupDatePicker(EditText target) {
        target.setFocusable(false);
        target.setClickable(true);
        target.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String current = target.getText() == null ? "" : target.getText().toString().trim();
            Date currentDate = parseDate(current);
            if (currentDate != null) {
                calendar.setTime(currentDate);
            }
            DatePickerDialog picker = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, day) -> target.setText(String.format(Locale.ROOT, "%02d/%02d/%04d", day, month + 1, year)),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });
    }

    private void setupTimePicker(EditText target) {
        target.setFocusable(false);
        target.setClickable(true);
        target.setOnClickListener(v -> {
            int hour = 17;
            int minute = 0;
            String current = target.getText() == null ? "" : target.getText().toString().trim();
            int parsedMinutes = parseTimeToMinutes(current);
            if (parsedMinutes >= 0) {
                hour = parsedMinutes / 60;
                minute = parsedMinutes % 60;
            }
            TimePickerDialog picker = new TimePickerDialog(
                    requireContext(),
                    (view, selectedHour, selectedMinute) -> target.setText(String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute)),
                    hour,
                    minute,
                    true
            );
            picker.show();
        });
    }

    private boolean isValidDate(String isoDate) {
        return parseDate(isoDate) != null;
    }

    private boolean isValidMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) return false;
        String[] p = month.split("-");
        int mm = Integer.parseInt(p[1]);
        return mm >= 1 && mm <= 12;
    }

    private boolean isValidTime(String value) {
        return parseTimeToMinutes(value) >= 0;
    }

    @NonNull
    private String normalizeTimeText(@Nullable String value) {
        if (value == null) return "";
        String normalized = value.trim()
                .replace('：', ':')
                .replace('.', ':')
                .replace('·', ':')
                .replace('∙', ':')
                .replaceAll("\\s+", "");
        normalized = normalized.replaceAll("[^0-9:]", "");
        if (normalized.matches("^\\d{1,2}$")) {
            int hour = (int) parseLong(normalized, -1);
            if (hour >= 0 && hour <= 23) {
                return String.format(Locale.ROOT, "%02d:00", hour);
            }
        }
        if (normalized.matches("^\\d{3,4}$")) {
            int split = normalized.length() - 2;
            int hour = (int) parseLong(normalized.substring(0, split), -1);
            int minute = (int) parseLong(normalized.substring(split), -1);
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
            }
        }
        if (normalized.matches("^\\d{1,2}:\\d{1,2}$")) {
            String[] parts = normalized.split(":");
            int hour = (int) parseLong(parts[0], -1);
            int minute = (int) parseLong(parts[1], -1);
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
            }
        }
        return normalized;
    }

    private int parseTimeToMinutes(@Nullable String value) {
        String normalized = normalizeTimeText(value);
        if (!normalized.matches("^\\d{2}:\\d{2}$")) return -1;
        String[] parts = normalized.split(":");
        int hour = (int) parseLong(parts[0], -1);
        int minute = (int) parseLong(parts[1], -1);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return -1;
        return (hour * 60) + minute;
    }

    @Nullable
    private Date parseDate(String value) {
        return DateInputUtils.parseDayOrNull(value);
    }

    @Nullable
    private Timestamp toTimestamp(String date) {
        Date parsed = parseDate(date);
        return parsed == null ? null : new Timestamp(parsed);
    }

    @Nullable
    private Date combineDateAndTime(@Nullable String isoDate, @Nullable String timeText) {
        Date date = parseDate(isoDate == null ? "" : isoDate.trim());
        int minutes = parseTimeToMinutes(timeText);
        if (date == null || minutes < 0) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60);
        calendar.set(Calendar.MINUTE, minutes % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String timestampToDateText(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "-";
        return dateFormat.format(timestamp.toDate());
    }

    @NonNull
    private String normalizeDateText(@Nullable String value) {
        return DateInputUtils.normalizeToDisplay(value);
    }

    private String authUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return "";
        return safe(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private String authEmail() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || FirebaseAuth.getInstance().getCurrentUser().getEmail() == null) {
            return "";
        }
        return FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT).trim();
    }

    private boolean isCurrentUserGroupOwner(@Nullable DocumentSnapshot groupDoc) {
        if (groupDoc == null) return false;
        String ownerId = safe(groupDoc.getString("ownerId"));
        if (!ownerId.isEmpty() && ownerId.equals(authUid())) {
            return true;
        }
        Object rolesRaw = groupDoc.get("roles");
        if (rolesRaw instanceof Map<?, ?> roles) {
            Object role = roles.get(authUid());
            if (role != null && "admin".equalsIgnoreCase(role.toString().trim())) {
                return true;
            }
        }
        String ownerEmail = normalizeEmail(groupDoc.getString("ownerEmail"));
        return !ownerEmail.isEmpty() && ownerEmail.equals(authEmail());
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private List<String> castStrings(Object raw) {
        List<String> values = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) values.add(item.toString());
            }
        }
        return values;
    }

    private List<String> castEmails(Object raw) {
        List<String> emails = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) emails.add(item.toString().toLowerCase(Locale.ROOT));
            }
        }
        return emails;
    }

    private List<Map<String, Object>> castMapList(Object raw) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item instanceof Map<?, ?>) {
                    Map<String, Object> copy = new HashMap<>();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                        if (entry.getKey() != null) {
                            copy.put(entry.getKey().toString(), entry.getValue());
                        }
                    }
                    list.add(copy);
                }
            }
        }
        return list;
    }

    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private long safeLong(@Nullable Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(@Nullable Double value) {
        return value == null ? 0.0 : value;
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            String normalized = value.trim().replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String normalizeEmail(String value) {
        return MemberLabelFormatter.normalizeEmail(value);
    }

    private String formatMemberTwoLines(@Nullable String email) {
        return MemberLabelFormatter.formatTwoLines(email, groupMemberNamesByEmail);
    }

    private String formatMemberInline(@Nullable String email) {
        return MemberLabelFormatter.formatInline(email, groupMemberNamesByEmail);
    }

    private String fallbackNameFromEmail(@Nullable String email) {
        return MemberLabelFormatter.fallbackNameFromEmail(email);
    }

    private boolean isLikelyEmail(String value) {
        if (value == null) return false;
        String email = value.trim();
        return !email.isEmpty()
                && email.contains("@")
                && !email.startsWith("@")
                && !email.endsWith("@");
    }

    private String buildContractSignersSummary(DocumentSnapshot doc) {
        List<String> legacySigners = castStrings(doc.get("signers"));
        String owner = safe(doc.getString("ownerSignerEmail"));
        if (owner.isEmpty() && !legacySigners.isEmpty()) owner = legacySigners.get(0);
        String tenant = safe(doc.getString("tenantSignerEmail"));
        if (tenant.isEmpty() && legacySigners.size() > 1) tenant = legacySigners.get(1);
        String coSigner = safe(doc.getString("coSignerEmail"));
        if (coSigner.isEmpty() && legacySigners.size() > 2) coSigner = legacySigners.get(2);
        String guarantor = safe(doc.getString("guarantorEmail"));
        if (guarantor.isEmpty() && legacySigners.size() > 3) guarantor = legacySigners.get(3);

        List<String> parts = new ArrayList<>();
        parts.add("Propietario: " + (owner.isEmpty() ? "-" : owner));
        parts.add("Inquilino: " + (tenant.isEmpty() ? "-" : tenant));
        if (!coSigner.isEmpty()) parts.add("Cotitular: " + coSigner);
        if (!guarantor.isEmpty()) parts.add("Avalista: " + guarantor);
        return String.join(" · ", parts);
    }

    private String joinList(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) return fallback;
        return String.join(", ", values);
    }

    private String normalizeDocumentType(String type) {
        return RentalTextFormatter.normalizeDocumentType(type);
    }

    private String eventTypeLabel(String eventType) {
        return RentalTextFormatter.eventTypeLabel(eventType, EVENT_RENT_DUE, EVENT_RENT_OVERDUE, EVENT_CONTRACT_ENDING, EVENT_INCIDENT_PENDING);
    }

    private void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    @Nullable
    private ModuleDef findModuleById(@Nullable String moduleId) {
        if (moduleId == null) return null;
        for (ModuleDef module : modules) {
            if (moduleId.equals(module.id)) return module;
        }
        return null;
    }

    private ProrationResult calculateProration(Date occupancyStart, @Nullable Date occupancyEnd, int year, int month) {
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(year, month, 1, 0, 0, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        Calendar monthEnd = Calendar.getInstance();
        monthEnd.set(year, month, monthStart.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        monthEnd.set(Calendar.MILLISECOND, 999);

        Date intervalStart = occupancyStart.after(monthStart.getTime()) ? occupancyStart : monthStart.getTime();
        Date intervalEnd = occupancyEnd == null
                ? monthEnd.getTime()
                : (occupancyEnd.before(monthEnd.getTime()) ? occupancyEnd : monthEnd.getTime());

        if (intervalStart.after(intervalEnd)) {
            return new ProrationResult(0, monthStart.getActualMaximum(Calendar.DAY_OF_MONTH), 0d);
        }
        Calendar start = Calendar.getInstance();
        start.setTime(intervalStart);
        Calendar end = Calendar.getInstance();
        end.setTime(intervalEnd);

        int occupiedDays = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH) + 1;
        int daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);
        double ratio = occupiedDays <= 0 ? 0d : (double) occupiedDays / (double) daysInMonth;
        return new ProrationResult(occupiedDays, daysInMonth, ratio);
    }

    private static class ModuleDef {
        final String id;
        final String label;
        final String description;
        final String actionLabel;

        ModuleDef(String id, String label, String description, String actionLabel) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.actionLabel = actionLabel;
        }
    }

    private static class ManagementRow {
        final String moduleId;
        final String id;
        final String title;
        final String subtitle;
        final String amount;
        final DocumentSnapshot snapshot;
        final String detail;

        ManagementRow(String moduleId, String id, String title, String subtitle, String amount, @Nullable DocumentSnapshot snapshot, String detail) {
            this.moduleId = moduleId;
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.amount = amount;
            this.snapshot = snapshot;
            this.detail = detail;
        }
    }

    private static class ProrationResult {
        final int occupiedDays;
        final int daysInMonth;
        final double ratio;

        ProrationResult(int occupiedDays, int daysInMonth, double ratio) {
            this.occupiedDays = occupiedDays;
            this.daysInMonth = daysInMonth;
            this.ratio = ratio;
        }
    }

    private static class ScopeSelection {
        final boolean valid;
        final String errorMessage;
        final String scopeType;
        final String memberEmail;
        final String roomName;
        final String summary;

        ScopeSelection(boolean valid, String errorMessage, String scopeType, String memberEmail, String roomName, String summary) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.scopeType = scopeType;
            this.memberEmail = memberEmail;
            this.roomName = roomName;
            this.summary = summary;
        }

        static ScopeSelection invalid(String message) {
            return new ScopeSelection(false, message, "", "", "", "");
        }

        static ScopeSelection everyone() {
            return new ScopeSelection(true, "", "everyone", "", "", "Todo el piso");
        }

        static ScopeSelection member(String email, String summary) {
            return new ScopeSelection(true, "", "member", email, "", summary);
        }

        static ScopeSelection room(String roomName) {
            return new ScopeSelection(true, "", "room", "", roomName, roomName);
        }
    }

    private interface StringListCallback {
        void onLoaded(@NonNull List<String> values);
    }

    private class RowsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workspace_row, parent, false);
            }
            ManagementRow row = rows.get(position);
            TextView titleTv = view.findViewById(R.id.rowTitleTv);
            TextView subtitleTv = view.findViewById(R.id.rowSubtitleTv);
            TextView detailTv = view.findViewById(R.id.rowDetailTv);
            TextView amountTv = view.findViewById(R.id.rowAmountTv);
            titleTv.setText(row.title);
            subtitleTv.setText(row.subtitle);
            String detail = row.detail == null ? "" : row.detail.trim();
            if (detail.isEmpty()) {
                detailTv.setVisibility(View.GONE);
                detailTv.setText("");
            } else {
                detailTv.setVisibility(View.VISIBLE);
                detailTv.setText(detail);
            }

            String amount = row.amount == null ? "" : row.amount.trim();
            if (amount.isEmpty()) {
                amountTv.setVisibility(View.GONE);
                amountTv.setText("");
            } else {
                amountTv.setVisibility(View.VISIBLE);
                amountTv.setText(amount);
            }
            return view;
        }
    }
}




