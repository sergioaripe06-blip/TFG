package com.sergio.flatshare.features.workspace;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.ui.DialogUtils;
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
    private final DecimalFormat moneyFormat = new DecimalFormat("0.00");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final Random random = new Random();

    private Spinner moduleSpinner;
    private TextView moduleHintTv;
    private Button primaryActionBtn;
    private TextView emptyTv;
    private ListView rowsLv;
    private RowsAdapter adapter;

    private String currentGroupId;
    private String selectedModuleId = MODULE_CONTRACT;

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
            primaryActionBtn.setEnabled(false);
            emptyTv.setText("Selecciona un piso para gestionar alquileres.");
            emptyTv.setVisibility(View.VISIBLE);
            return;
        }
        primaryActionBtn.setEnabled(true);
        loadGroupMembersAndThen(this::loadCurrentModuleRows);
    }

    private void buildModules() {
        modules.clear();
        modules.add(new ModuleDef(MODULE_CONTRACT, "Contrato", "Inicio/fin, fianza, prórrogas, cláusulas y firmantes", "Guardar contrato"));
        modules.add(new ModuleDef(MODULE_RENT, "Cobros", "Estado mensual: pendiente, parcial, pagado o atrasado + recargos", "Nuevo cobro mensual"));
        modules.add(new ModuleDef(MODULE_MAINTENANCE, "Incidencias", "Averías, responsable, coste, estado e historial", "Nueva incidencia"));
        modules.add(new ModuleDef(MODULE_DOCUMENTS, "Documentos", "Contrato, facturas, inventario, fotos y actas", "Nuevo documento"));
        modules.add(new ModuleDef(MODULE_AUDIT, "Auditoría", "Trazabilidad por usuario, fecha y acción", "Actualizar"));
        modules.add(new ModuleDef(MODULE_AUTOMATION, "Automatización", "Rentas recurrentes y prorrateos entrada/salida", "Gestionar automatizaciones"));
        modules.add(new ModuleDef(MODULE_REMINDER_RULES, "Reglas push", "Recordatorios por vencimientos e impagos", "Nueva regla de evento"));
    }

    private void setupModuleSpinner() {
        String[] labels = new String[modules.size()];
        for (int i = 0; i < modules.size(); i++) labels[i] = modules.get(i).label;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moduleSpinner.setAdapter(adapter);
        moduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= modules.size()) return;
                ModuleDef module = modules.get(position);
                selectedModuleId = module.id;
                moduleHintTv.setText(module.description);
                primaryActionBtn.setText(module.actionLabel);
                loadCurrentModuleRows();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadGroupMembersAndThen(Runnable done) {
        groupMemberEmails.clear();
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            List<String> emails = castEmails(doc.get("memberEmails"));
            groupMemberEmails.addAll(emails);
            if (done != null) done.run();
        }).addOnFailureListener(e -> {
            if (done != null) done.run();
        });
    }

    private void loadCurrentModuleRows() {
        if (!isAdded() || currentGroupId == null || currentGroupId.trim().isEmpty()) return;
        switch (selectedModuleId) {
            case MODULE_CONTRACT -> loadContractRows();
            case MODULE_RENT -> loadRentRows();
            case MODULE_MAINTENANCE -> loadMaintenanceRows();
            case MODULE_DOCUMENTS -> loadDocumentRows();
            case MODULE_AUDIT -> loadAuditRows();
            case MODULE_AUTOMATION -> loadAutomationRows();
            case MODULE_REMINDER_RULES -> loadReminderRuleRows();
            default -> {
                rows.clear();
                adapter.notifyDataSetChanged();
                emptyTv.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handlePrimaryAction() {
        if (currentGroupId == null || currentGroupId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona un piso primero", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (selectedModuleId) {
            case MODULE_CONTRACT -> openContractDialog(null);
            case MODULE_RENT -> openRentDialog(null);
            case MODULE_MAINTENANCE -> openMaintenanceDialog(null);
            case MODULE_DOCUMENTS -> openDocumentDialog(null);
            case MODULE_AUDIT -> loadAuditRows();
            case MODULE_AUTOMATION -> showAutomationActionsDialog();
            case MODULE_REMINDER_RULES -> openReminderRuleDialog(null);
        }
    }

    private void openRowDetails(ManagementRow row) {
        if (MODULE_CONTRACT.equals(row.moduleId)) {
            openContractDialog(row.snapshot);
        } else if (MODULE_RENT.equals(row.moduleId)) {
            openRentDialog(row.snapshot);
        } else if (MODULE_MAINTENANCE.equals(row.moduleId)) {
            openMaintenanceDialog(row.snapshot);
        } else if (MODULE_DOCUMENTS.equals(row.moduleId)) {
            openDocumentDialog(row.snapshot);
        } else if (MODULE_AUTOMATION.equals(row.moduleId)) {
            openAutomationDialog(row.snapshot);
        } else if (MODULE_REMINDER_RULES.equals(row.moduleId)) {
            openReminderRuleDialog(row.snapshot);
        } else {
            showReadOnlyDetails(row);
        }
    }

    private void showReadOnlyDetails(ManagementRow row) {
        View content = DialogUtils.createMessageView(requireContext(), row.detail == null ? "Sin detalle." : row.detail);
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

    private void loadContractRows() {
        contractsService.loadContractsByGroup(
                currentGroupId,
                docs -> {
                    rows.clear();
                    docs.sort((a, b) -> compareByTimestampDesc(a, b, "updatedAt", "createdAt"));
                    for (DocumentSnapshot doc : docs) {
                        String start = safe(doc.getString("startDate"));
                        String end = safe(doc.getString("endDate"));
                        double deposit = safeDouble(doc.getDouble("depositAmount"));
                        long extMonths = safeLong(doc.getLong("extensionMonths"));
                        String signersSummary = buildContractSignersSummary(doc);
                        rows.add(new ManagementRow(
                                MODULE_CONTRACT,
                                doc.getId(),
                                "Contrato " + (start.isEmpty() ? "-" : start) + " → " + (end.isEmpty() ? "-" : end),
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
                        String subtitle = "Mes " + (monthKey.isEmpty() ? "-" : monthKey)
                                + " · " + (room.isEmpty() ? "Sin habitación" : room)
                                + " · " + (tenant.isEmpty() ? "Sin inquilino" : tenant);
                        String amount = moneyFormat.format(amountPaid) + " / " + moneyFormat.format(total) + " EUR";
                        String detail = "Estado: " + (status.isEmpty() ? "pendiente" : status)
                                + "\nVence: " + safe(doc.getString("dueDateText"))
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
                        String subtitle = (room.isEmpty() ? "Sin habitación" : room)
                                + " · Responsable: " + (responsible.isEmpty() ? "Sin asignar" : responsible);
                        String detail = "Estado: " + (status.isEmpty() ? "abierta" : status)
                                + " · Coste: " + moneyFormat.format(cost) + " EUR";
                        rows.add(new ManagementRow(MODULE_MAINTENANCE, doc.getId(), title.isEmpty() ? "Incidencia" : title, subtitle, detail, doc, "Incidencia"));
                    }
                    onRowsReady("No hay incidencias registradas.");
                })
                .addOnFailureListener(e -> onLoadError("No se pudieron cargar incidencias"));
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
                        String date = safe(doc.getString("documentDate"));
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
                        String start = safe(doc.getString("startDate"));
                        String end = safe(doc.getString("endDate"));
                        String subtitle = (room.isEmpty() ? "Sin habitación" : room)
                                + " · " + (tenant.isEmpty() ? "Sin inquilino" : tenant);
                        String detail = "Cobro día " + day + " · " + moneyFormat.format(amount) + " EUR/mes";
                        rows.add(new ManagementRow(MODULE_AUTOMATION, doc.getId(), start + " → " + (end.isEmpty() ? "sin fin" : end), subtitle, detail, doc, "Regla de automatización"));
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
        EditText startEt = addLabeledEditText(form, "Fecha inicio (YYYY-MM-DD)", "2026-01-01", InputType.TYPE_CLASS_TEXT);
        EditText endEt = addLabeledEditText(form, "Fecha fin (YYYY-MM-DD)", "2026-12-31", InputType.TYPE_CLASS_TEXT);
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
            startEt.setText(safe(existingDoc.getString("startDate")));
            endEt.setText(safe(existingDoc.getString("endDate")));
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
            String start = startEt.getText().toString().trim();
            String end = endEt.getText().toString().trim();
            if (!isValidDate(start) || !isValidDate(end)) {
                Toast.makeText(requireContext(), "Revisa formato de fechas (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
                return;
            }
            double deposit = parseDouble(depositEt.getText().toString().trim(), -1);
            long extensions = parseLong(extensionEt.getText().toString().trim(), -1);
            if (deposit < 0 || extensions < 0) {
                Toast.makeText(requireContext(), "Fianza o prórrogas inválidas", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Propietario e inquilino deben tener email válido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!coSigner.isEmpty() && !isLikelyEmail(coSigner)) {
                Toast.makeText(requireContext(), "El cotitular no tiene email válido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!guarantor.isEmpty() && !isLikelyEmail(guarantor)) {
                Toast.makeText(requireContext(), "El avalista no tiene email válido", Toast.LENGTH_SHORT).show();
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
                writeAudit("Contrato", existingDoc == null ? "crear" : "editar", "Contrato guardado: " + start + " → " + end, existingDoc == null ? "" : existingDoc.getId());
                applyEventRules(EVENT_CONTRACT_ENDING, parseDate(end), null, "Fin de contrato cercano");
                dialog.dismiss();
                loadContractRows();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar contrato", Toast.LENGTH_SHORT).show());
        });
    }

    private void openRentDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        EditText monthEt = addLabeledEditText(form, "Mes (YYYY-MM)", "2026-05", InputType.TYPE_CLASS_TEXT);
        EditText roomEt = addLabeledEditText(form, "Habitación", "Habitación 1", InputType.TYPE_CLASS_TEXT);
        Spinner tenantSpinner = addLabeledSpinner(form, "Inquilino", groupMemberEmails.isEmpty() ? new String[]{"Sin miembros"} : groupMemberEmails.toArray(new String[0]));
        EditText amountEt = addLabeledEditText(form, "Base mensual (EUR)", "450", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText paidEt = addLabeledEditText(form, "Pagado hasta ahora (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText surchargeEt = addLabeledEditText(form, "Recargo (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner statusSpinner = addLabeledSpinner(form, "Estado", new String[]{"pendiente", "parcial", "pagado", "atrasado"});
        EditText dueDateEt = addLabeledEditText(form, "Vencimiento (YYYY-MM-DD)", "2026-05-05", InputType.TYPE_CLASS_TEXT);
        setupDatePicker(dueDateEt);

        if (existingDoc != null) {
            monthEt.setText(safe(existingDoc.getString("monthKey")));
            roomEt.setText(safe(existingDoc.getString("roomName")));
            selectSpinnerValue(tenantSpinner, safe(existingDoc.getString("tenantEmail")));
            amountEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("amountBase"))));
            paidEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("amountPaid"))));
            surchargeEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("surcharge"))));
            selectSpinnerValue(statusSpinner, safe(existingDoc.getString("status")));
            dueDateEt.setText(safe(existingDoc.getString("dueDateText")));
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
                Toast.makeText(requireContext(), "Mes inválido. Usa YYYY-MM", Toast.LENGTH_SHORT).show();
                return;
            }
            String dueDate = dueDateEt.getText().toString().trim();
            if (!isValidDate(dueDate)) {
                Toast.makeText(requireContext(), "Fecha de vencimiento inválida", Toast.LENGTH_SHORT).show();
                return;
            }
            double baseAmount = parseDouble(amountEt.getText().toString().trim(), -1);
            double paidAmount = parseDouble(paidEt.getText().toString().trim(), -1);
            double surcharge = parseDouble(surchargeEt.getText().toString().trim(), -1);
            if (baseAmount < 0 || paidAmount < 0 || surcharge < 0) {
                Toast.makeText(requireContext(), "Importes inválidos", Toast.LENGTH_SHORT).show();
                return;
            }
            String tenant = selectedSpinnerValue(tenantSpinner);
            String status = selectedSpinnerValue(statusSpinner);
            String roomName = roomEt.getText().toString().trim();
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
    }

    private void openMaintenanceDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        EditText titleEt = addLabeledEditText(form, "Incidencia", "Ejemplo: Fuga en baño", InputType.TYPE_CLASS_TEXT);
        EditText roomEt = addLabeledEditText(form, "Habitación/Zona", "Ejemplo: Cocina", InputType.TYPE_CLASS_TEXT);
        Spinner responsibleSpinner = addLabeledSpinner(form, "Responsable", groupMemberEmails.isEmpty() ? new String[]{"Sin miembros"} : groupMemberEmails.toArray(new String[0]));
        Spinner statusSpinner = addLabeledSpinner(form, "Estado", new String[]{"abierta", "en_progreso", "resuelta", "cancelada"});
        EditText estimatedEt = addLabeledEditText(form, "Coste estimado (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText finalEt = addLabeledEditText(form, "Coste final (EUR)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText detailEt = addLabeledEditText(form, "Descripción", "Describe la avería y el trabajo realizado", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        if (existingDoc != null) {
            titleEt.setText(safe(existingDoc.getString("title")));
            roomEt.setText(safe(existingDoc.getString("roomName")));
            selectSpinnerValue(responsibleSpinner, safe(existingDoc.getString("responsibleEmail")));
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
                Toast.makeText(requireContext(), "Indica un título para la incidencia", Toast.LENGTH_SHORT).show();
                return;
            }
            String status = selectedSpinnerValue(statusSpinner);
            String responsible = selectedSpinnerValue(responsibleSpinner);
            double estimatedCost = parseDouble(estimatedEt.getText().toString().trim(), -1);
            double finalCost = parseDouble(finalEt.getText().toString().trim(), -1);
            if (estimatedCost < 0 || finalCost < 0) {
                Toast.makeText(requireContext(), "Los costes no pueden ser negativos", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Map<String, Object>> history = existingDoc == null
                    ? new ArrayList<>()
                    : castMapList(existingDoc.get("history"));
            Map<String, Object> event = new HashMap<>();
            event.put("at", FieldValue.serverTimestamp());
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

    private void openDocumentDialog(@Nullable DocumentSnapshot existingDoc) {
        LinearLayout form = buildVerticalForm();
        Spinner typeSpinner = addLabeledSpinner(form, "Tipo", new String[]{"contrato", "factura", "inventario", "foto", "acta"});
        EditText titleEt = addLabeledEditText(form, "Título", "Ejemplo: Contrato alquiler 2026", InputType.TYPE_CLASS_TEXT);
        EditText dateEt = addLabeledEditText(form, "Fecha (YYYY-MM-DD)", "2026-05-13", InputType.TYPE_CLASS_TEXT);
        EditText refEt = addLabeledEditText(form, "Referencia/URI", "https://... o ruta", InputType.TYPE_CLASS_TEXT);
        EditText notesEt = addLabeledEditText(form, "Notas", "Detalle opcional", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        setupDatePicker(dateEt);

        if (existingDoc != null) {
            selectSpinnerValue(typeSpinner, safe(existingDoc.getString("type")));
            titleEt.setText(safe(existingDoc.getString("title")));
            dateEt.setText(safe(existingDoc.getString("documentDate")));
            refEt.setText(safe(existingDoc.getString("referenceUri")));
            notesEt.setText(safe(existingDoc.getString("notes")));
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                existingDoc == null ? "Nuevo documento" : "Editar documento",
                "Guarda referencia de contrato, factura, inventario, foto o acta.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String date = dateEt.getText().toString().trim();
            if (!date.isEmpty() && !isValidDate(date)) {
                Toast.makeText(requireContext(), "Fecha inválida. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = titleEt.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("type", selectedSpinnerValue(typeSpinner));
            data.put("title", title);
            data.put("documentDate", date);
            data.put("referenceUri", refEt.getText().toString().trim());
            data.put("notes", notesEt.getText().toString().trim());
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedByUid", authUid());
            data.put("updatedByEmail", authEmail());
            if (existingDoc == null) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("createdByUid", authUid());
                data.put("createdByEmail", authEmail());
            }

            Task<?> saveTask = existingDoc == null
                    ? db.collection("group_documents").add(data)
                    : db.collection("group_documents").document(existingDoc.getId()).update(data);
            saveTask.addOnSuccessListener(done -> {
                writeAudit("Documentos", existingDoc == null ? "crear" : "editar", title, existingDoc == null ? "" : existingDoc.getId());
                dialog.dismiss();
                loadDocumentRows();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo guardar documento", Toast.LENGTH_SHORT).show());
        });
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
        Spinner tenantSpinner = addLabeledSpinner(form, "Inquilino", groupMemberEmails.isEmpty() ? new String[]{"Sin miembros"} : groupMemberEmails.toArray(new String[0]));
        EditText rentEt = addLabeledEditText(form, "Renta mensual (EUR)", "450", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText billingDayEt = addLabeledEditText(form, "Día de cobro (1-28)", "5", InputType.TYPE_CLASS_NUMBER);
        EditText startEt = addLabeledEditText(form, "Inicio ocupación (YYYY-MM-DD)", "2026-05-01", InputType.TYPE_CLASS_TEXT);
        EditText endEt = addLabeledEditText(form, "Fin ocupación (opcional)", "", InputType.TYPE_CLASS_TEXT);
        setupDatePicker(startEt);
        setupDatePicker(endEt);

        if (existingDoc != null) {
            roomEt.setText(safe(existingDoc.getString("roomName")));
            selectSpinnerValue(tenantSpinner, safe(existingDoc.getString("tenantEmail")));
            rentEt.setText(String.valueOf(safeDouble(existingDoc.getDouble("monthlyRent"))));
            billingDayEt.setText(String.valueOf(safeLong(existingDoc.getLong("billingDay"))));
            startEt.setText(safe(existingDoc.getString("startDate")));
            endEt.setText(safe(existingDoc.getString("endDate")));
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
            String startDate = startEt.getText().toString().trim();
            String endDate = endEt.getText().toString().trim();
            if (!isValidDate(startDate) || (!endDate.isEmpty() && !isValidDate(endDate))) {
                Toast.makeText(requireContext(), "Revisa fechas de ocupación", Toast.LENGTH_SHORT).show();
                return;
            }
            long billingDay = parseLong(billingDayEt.getText().toString().trim(), -1);
            if (billingDay < 1 || billingDay > 28) {
                Toast.makeText(requireContext(), "El día de cobro debe estar entre 1 y 28", Toast.LENGTH_SHORT).show();
                return;
            }
            double rent = parseDouble(rentEt.getText().toString().trim(), -1);
            if (rent <= 0) {
                Toast.makeText(requireContext(), "La renta mensual debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("roomName", roomEt.getText().toString().trim());
            data.put("tenantEmail", selectedSpinnerValue(tenantSpinner));
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
                        Toast.makeText(requireContext(), "No hay reglas de automatización", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(requireContext(), "No hay nuevos cobros por generar", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                int createdFinal = created;
                                Tasks.whenAllComplete(tasks).addOnSuccessListener(done -> {
                                    writeAudit("Automatización", "ejecutar", "Cobros generados: " + createdFinal, "");
                                    Toast.makeText(requireContext(), "Cobros generados: " + createdFinal, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Offset inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = titleEt.getText().toString().trim();
            String body = bodyEt.getText().toString().trim();
            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(requireContext(), "Título y mensaje son obligatorios", Toast.LENGTH_SHORT).show();
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
                android.R.layout.simple_spinner_item,
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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        parent.addView(spinner);
        return spinner;
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
                    (view, year, month, day) -> target.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });
    }

    private boolean isValidDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) return false;
        try {
            dateFormat.setLenient(false);
            dateFormat.parse(isoDate.trim());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isValidMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) return false;
        String[] p = month.split("-");
        int mm = Integer.parseInt(p[1]);
        return mm >= 1 && mm <= 12;
    }

    @Nullable
    private Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            dateFormat.setLenient(false);
            return dateFormat.parse(value.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    @Nullable
    private Timestamp toTimestamp(String date) {
        Date parsed = parseDate(date);
        return parsed == null ? null : new Timestamp(parsed);
    }

    private String timestampToDateText(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "-";
        return dateFormat.format(timestamp.toDate());
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
            return Double.parseDouble(value.trim());
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
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
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
        if (type == null) return "-";
        switch (type) {
            case "contrato":
                return "Contrato";
            case "factura":
                return "Factura";
            case "inventario":
                return "Inventario";
            case "foto":
                return "Foto";
            case "acta":
                return "Acta";
            default:
                return type;
        }
    }

    private String eventTypeLabel(String eventType) {
        if (eventType == null) return "-";
        return switch (eventType) {
            case EVENT_RENT_DUE -> "Vencimiento de renta";
            case EVENT_RENT_OVERDUE -> "Impago";
            case EVENT_CONTRACT_ENDING -> "Fin de contrato";
            case EVENT_INCIDENT_PENDING -> "Incidencia abierta";
            default -> eventType;
        };
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
            TextView amountTv = view.findViewById(R.id.rowAmountTv);
            titleTv.setText(row.title);
            subtitleTv.setText(row.subtitle);
            amountTv.setText(row.amount == null ? "" : row.amount);
            int color = Color.parseColor("#0F172A");
            amountTv.setTextColor(color);
            return view;
        }
    }
}
