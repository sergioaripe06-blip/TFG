package com.sergio.flatshare.features.workspace;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.core.notifications.ReminderScheduler;
import com.sergio.flatshare.core.session.SessionStore;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Hashtable;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpensesFragment extends Fragment {
    private static final String TAB_EXPENSES = "expenses";
    private static final String TAB_SALDOS = "saldos";
    private static final String TAB_REMINDERS = "reminders";
    private static final String TAB_MANAGEMENT = "management";
    private static final String BILLING_FIXED = "fixed";
    private static final String BILLING_VARIABLE = "variable";
    private static final String VARIABLE_SPLIT_EQUAL = "equal";
    private static final String VARIABLE_SPLIT_PERCENTAGE = "percentage";
    private static final String ROOM_ALL_LABEL = "Todas las habitaciones";
    private static final String FILTER_MODE_CATEGORY = "category";
    private static final String FILTER_MODE_PERSON = "person";
    private static final String FILTER_MODE_DATE = "date";
    private static final String CATEGORY_RENT = "alquiler";
    private static final String[] SPENDING_TYPES = {"agua", "electricidad", "internet", "alquiler", "comida", "otros"};
    private static final int[] SPENDING_TYPE_COLORS = {
            Color.parseColor("#44D4FF"),
            Color.parseColor("#FFD95A"),
            Color.parseColor("#8EA8FF"),
            Color.parseColor("#FF9E66"),
            Color.parseColor("#78E08F"),
            Color.parseColor("#C3C3C3")
    };
    private static final String[] PRIORITY_TYPES = {"baja", "media", "alta"};
    private static final String[] PAYMENT_TARGET_TYPES = {"Habitación", "Miembro", "Todos"};
    private static final String[] REMINDER_INTERVAL_TYPES = {"Único", "Diario", "Semanal", "Mensual", "Personalizado"};
    private static final String[] REMINDER_TARGET_TYPES = {"Todos", "Miembro", "Habitación", "X habitación", "X miembros"};
    private static final SimpleDateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<WorkspaceRow> expenseRows = new ArrayList<>();
    private final List<WorkspaceRow> saldoRows = new ArrayList<>();
    private final List<WorkspaceRow> reminderRows = new ArrayList<>();
    private final List<RoomOption> roomFilterOptions = new ArrayList<>();

    private WorkspaceAdapter expensesAdapter;
    private WorkspaceAdapter saldosAdapter;
    private WorkspaceAdapter remindersAdapter;

    private TextView workspaceTitleTv;
    private TextView workspaceMetaTv;
    private ListView expensesLv;
    private ListView saldosLv;
    private ListView remindersLv;
    private View tenantsContainer;
    private View quickBalancesCard;
    private LinearLayout quickBalancesContainer;
    private View workspaceCtaLayout;
    private TextView addMainLabelTv;
    private Button gastosTabBtn;
    private Button saldosTabBtn;
    private Button remindersTabBtn;
    private Button managementTabBtn;
    private Spinner roomFilterSpinner;
    private Button addRoomQuickBtn;
    private View expensesToolsRow;
    private EditText expensesSearchEt;
    private Button openFiltersBtn;

    private String currentTab = TAB_EXPENSES;
    private String currentGroupId;
    private String currentGroupName = "Piso";
    private String currentUserRole = "member";
    private String currentBillingModel = BILLING_VARIABLE;
    private String currentVariableSplitMode = VARIABLE_SPLIT_EQUAL;
    private String currentRoomId;
    private String currentRoomName;
    private List<String> currentRoomMembers = new ArrayList<>();
    private List<String> currentGroupMemberEmails = new ArrayList<>();
    private String filterPersonEmail;
    private String filterCategory;
    private String filterSearchQuery;
    private Long filterFromMs;
    private Long filterToMs;
    private String filterDateIso;
    private String pendingTicketUri;
    private EditText pendingTicketAmountEt;
    private TextView pendingTicketStatusTv;
    private String pendingReminderAttachmentUri;
    private TextView pendingReminderAttachmentStatusTv;
    private boolean isRebindingRoomSelectors = false;
    private boolean isUpdatingRoomFilterSpinner = false;
    private final Map<String, String> memberDisplayNamesByEmail = new HashMap<>();
    private String workspaceMetaCache = "";
    private String workspaceLocationQueryCache = "";
    private final List<BalanceMovement> lastBalanceMovements = new ArrayList<>();
    private long lastTabSwitchAtMs = 0L;
    private long lastMainActionAtMs = 0L;
    private boolean expenseActionDialogVisible = false;

    private final ActivityResultLauncher<String> ticketPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleTicketSelected);
    private final ActivityResultLauncher<String> reminderAttachmentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleReminderAttachmentSelected);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);
        workspaceTitleTv = view.findViewById(R.id.workspaceTitleTv);
        workspaceMetaTv = view.findViewById(R.id.workspaceMetaTv);
        workspaceMetaTv.setVisibility(View.GONE);
        expensesLv = view.findViewById(R.id.expensesLv);
        saldosLv = view.findViewById(R.id.saldosLv);
        remindersLv = view.findViewById(R.id.remindersLv);
        tenantsContainer = view.findViewById(R.id.tenantsContainer);
        quickBalancesCard = view.findViewById(R.id.quickBalancesCard);
        quickBalancesContainer = view.findViewById(R.id.quickBalancesContainer);
        workspaceCtaLayout = view.findViewById(R.id.workspaceCtaLayout);
        addMainLabelTv = view.findViewById(R.id.addMainLabelTv);
        gastosTabBtn = view.findViewById(R.id.gastosTabBtn);
        saldosTabBtn = view.findViewById(R.id.saldosTabBtn);
        remindersTabBtn = view.findViewById(R.id.remindersTabBtn);
        managementTabBtn = view.findViewById(R.id.tenantsTabBtn);
        roomFilterSpinner = view.findViewById(R.id.roomFilterSpinner);
        addRoomQuickBtn = view.findViewById(R.id.addRoomQuickBtn);
        expensesToolsRow = view.findViewById(R.id.expensesToolsRow);
        expensesSearchEt = view.findViewById(R.id.expensesSearchEt);
        openFiltersBtn = view.findViewById(R.id.openFiltersBtn);
        FloatingActionButton mainFab = view.findViewById(R.id.addExpenseCenterFab);

        expensesAdapter = new WorkspaceAdapter(expenseRows);
        saldosAdapter = new WorkspaceAdapter(saldoRows);
        remindersAdapter = new WorkspaceAdapter(reminderRows);
        expensesLv.setAdapter(expensesAdapter);
        saldosLv.setAdapter(saldosAdapter);
        remindersLv.setAdapter(remindersAdapter);

        gastosTabBtn.setOnClickListener(v -> requestTabSwitch(TAB_EXPENSES));
        if (saldosTabBtn != null) {
            saldosTabBtn.setVisibility(View.GONE);
            saldosTabBtn.setOnClickListener(v -> requestTabSwitch(TAB_EXPENSES));
        }
        remindersTabBtn.setOnClickListener(v -> requestTabSwitch(TAB_REMINDERS));
        managementTabBtn.setOnClickListener(v -> requestTabSwitch(TAB_MANAGEMENT));
        roomFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                if (isUpdatingRoomFilterSpinner || !isAdded()) return;
                onRoomFilterSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        addRoomQuickBtn.setOnClickListener(v -> showRoomQuickActionsDialog());
        openFiltersBtn.setOnClickListener(v -> showFiltersDialog());
        workspaceTitleTv.setOnLongClickListener(v -> {
            if (currentGroupId == null || workspaceMetaCache == null || workspaceMetaCache.trim().isEmpty()) {
                return false;
            }
            showWorkspaceMetaDialog();
            return true;
        });
        expensesSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                filterSearchQuery = query.isEmpty() ? null : query;
                loadExpenses();
            }
        });
        expensesSearchEt.setOnFocusChangeListener((v, hasFocus) -> updateCtaVisibilityForCurrentTab());
        expensesSearchEt.setOnEditorActionListener((v, actionId, event) -> {
            boolean done = actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (done) {
                expensesSearchEt.clearFocus();
                updateCtaVisibilityForCurrentTab();
                return true;
            }
            return false;
        });
        mainFab.setOnClickListener(v -> {
            if (isFastTap(lastMainActionAtMs, 400)) return;
            lastMainActionAtMs = SystemClock.elapsedRealtime();
            handleMainAction();
        });
        expensesLv.setOnItemClickListener((parent, v, position, id) -> showRowDetail(expenseRows.get(position)));
        remindersLv.setOnItemClickListener((parent, v, position, id) -> showRowDetail(reminderRows.get(position)));

        switchTab(TAB_EXPENSES);
        refreshWorkspace();
        return view;
    }

    private void refreshWorkspace() {
        currentGroupId = SessionStore.getCurrentGroup(requireContext());
        currentRoomId = SessionStore.getCurrentRoomId(requireContext());
        currentRoomName = SessionStore.getCurrentRoomName(requireContext());
        currentRoomMembers = new ArrayList<>();
        currentGroupMemberEmails = new ArrayList<>();
        lastBalanceMovements.clear();
        if (quickBalancesContainer != null) {
            quickBalancesContainer.removeAllViews();
        }
        updateQuickBalancesVisibility();
        if (currentGroupId == null) {
            currentGroupName = "Selecciona un piso";
            currentBillingModel = BILLING_VARIABLE;
            currentVariableSplitMode = VARIABLE_SPLIT_EQUAL;
            workspaceTitleTv.setText(currentGroupName);
            workspaceMetaCache = "Entra desde la pestaña de pisos para ver gastos, pagos, recordatorios y gestión del alquiler.";
            workspaceMetaTv.setText(workspaceMetaCache);
            workspaceMetaTv.setVisibility(View.GONE);
            workspaceLocationQueryCache = "";
            expenseRows.clear();
            saldoRows.clear();
            reminderRows.clear();
            roomFilterOptions.clear();
            expensesAdapter.notifyDataSetChanged();
            saldosAdapter.notifyDataSetChanged();
            remindersAdapter.notifyDataSetChanged();
            lastBalanceMovements.clear();
            if (quickBalancesContainer != null) {
                quickBalancesContainer.removeAllViews();
            }
            isUpdatingRoomFilterSpinner = true;
            roomFilterSpinner.setAdapter(buildLightSpinnerAdapter(new String[]{ROOM_ALL_LABEL}));
            roomFilterSpinner.setSelection(0);
            isUpdatingRoomFilterSpinner = false;
            addRoomQuickBtn.setVisibility(View.GONE);
            workspaceCtaLayout.setVisibility(View.GONE);
            switchTab(currentTab);
            updateQuickBalancesVisibility();
            return;
        }

        switchTab(currentTab);
        updateCtaVisibilityForCurrentTab();
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            currentGroupName = doc.getString("name") == null ? "Piso actual" : doc.getString("name");
            currentUserRole = resolveCurrentUserRole(doc);
            currentBillingModel = normalizeBillingModel(doc.getString("billingModel"));
            currentVariableSplitMode = normalizeVariableSplitMode(doc.getString("variableSplitMode"));

            String rawDescription = doc.getString("description");
            final String description = (rawDescription == null || rawDescription.trim().isEmpty())
                    ? "Sin descripción"
                    : rawDescription;
            workspaceLocationQueryCache = buildWorkspaceLocationQuery(doc, description);
            List<String> memberEmails = castEmails(doc.get("memberEmails"));
            List<String> memberIds = castStrings(doc.get("members"));
            currentGroupMemberEmails = new ArrayList<>(memberEmails);
            resolveMemberDisplayNames(memberIds, memberEmails, () -> {
                if (!isAdded()) return;
                String ownerEmail = resolveOwnerEmail(doc, memberIds, memberEmails);
                String ownerLabel = displayNameForEmail(ownerEmail);
                List<String> memberLabels = new ArrayList<>();
                for (String email : memberEmails) {
                    memberLabels.add(displayNameForEmail(email));
                }
                String membersLabel = memberLabels.isEmpty() ? "Sin datos" : String.join(", ", memberLabels);
                String billingLabel = BILLING_FIXED.equals(currentBillingModel)
                        ? "Alquiler fijo"
                        : ("Alquiler variable (" + (VARIABLE_SPLIT_PERCENTAGE.equals(currentVariableSplitMode) ? "porcentual" : "equitativo") + ")");
                String meta = "Piso: " + description
                        + "\nPropietario: " + (ownerLabel.isEmpty() ? "Sin datos" : ownerLabel)
                        + "\nModelo: " + billingLabel
                        + "\nMiembros (" + memberEmails.size() + "): " + membersLabel;
                workspaceTitleTv.setText(currentGroupName);
                workspaceMetaCache = meta;
                workspaceMetaTv.setText(meta);
                workspaceMetaTv.setVisibility(View.GONE);

                loadRoomContext(() -> {
                    loadExpenses();
                    loadFinancialViews();
                    loadReminders();
                    loadRoomFilterOptions();
                    ensureManagementFragment();
                    switchTab(currentTab);
                });
            });
        });
    }

    private void switchTab(String tab) {
        if (TAB_SALDOS.equals(tab)) {
            tab = TAB_EXPENSES;
        }
        currentTab = tab;
        boolean showExpenses = TAB_EXPENSES.equals(tab);
        boolean showReminders = TAB_REMINDERS.equals(tab);
        boolean showManagement = TAB_MANAGEMENT.equals(tab);

        expensesLv.setVisibility(showExpenses ? View.VISIBLE : View.GONE);
        saldosLv.setVisibility(View.GONE);
        remindersLv.setVisibility(showReminders ? View.VISIBLE : View.GONE);
        tenantsContainer.setVisibility(showManagement ? View.VISIBLE : View.GONE);
        expensesToolsRow.setVisibility(showExpenses && currentGroupId != null ? View.VISIBLE : View.GONE);

        updateTabStyle(gastosTabBtn, showExpenses);
        if (saldosTabBtn != null) {
            updateTabStyle(saldosTabBtn, false);
        }
        updateTabStyle(remindersTabBtn, showReminders);
        updateTabStyle(managementTabBtn, showManagement);

        if (showExpenses) {
            addMainLabelTv.setText(BILLING_FIXED.equals(currentBillingModel) ? "Nuevo pago" : "Nuevo gasto");
        } else if (showReminders) {
            addMainLabelTv.setText("Nuevo recordatorio");
        } else {
            addMainLabelTv.setText("Gestion");
            ensureManagementFragment();
        }
        updateQuickBalancesVisibility();
        updateCtaVisibilityForCurrentTab();
    }

    private void requestTabSwitch(String tab) {
        if (isFastTap(lastTabSwitchAtMs, 220)) return;
        lastTabSwitchAtMs = SystemClock.elapsedRealtime();
        switchTab(tab);
    }

    private boolean isFastTap(long lastTapAtMs, long minIntervalMs) {
        long now = SystemClock.elapsedRealtime();
        return now - lastTapAtMs < minIntervalMs;
    }

    private void showWorkspaceMetaDialog() {
        View content = DialogUtils.createMessageView(requireContext(), workspaceMetaCache);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                currentGroupName == null || currentGroupName.trim().isEmpty() ? "Datos del piso" : currentGroupName,
                "Información del piso",
                content,
                "Cerrar",
                "Ver ubicación"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openWorkspaceLocationInMaps();
        });
    }

    @NonNull
    private String buildWorkspaceLocationQuery(@NonNull DocumentSnapshot doc, @NonNull String fallbackDescription) {
        Object rawLocation = doc.get("location");
        if (rawLocation instanceof Map<?, ?> map) {
            List<String> parts = new ArrayList<>();
            appendLocationPart(parts, map.get("street"));
            appendLocationPart(parts, map.get("portal"));
            appendLocationPart(parts, map.get("postalCode"));
            appendLocationPart(parts, map.get("city"));
            appendLocationPart(parts, map.get("province"));
            if (!parts.isEmpty()) {
                return String.join(", ", parts);
            }
        }
        return fallbackDescription == null ? "" : fallbackDescription.trim();
    }

    private void appendLocationPart(@NonNull List<String> parts, @Nullable Object value) {
        if (value == null) return;
        String text = value.toString().trim();
        if (!text.isEmpty()) {
            parts.add(text);
        }
    }

    private void openWorkspaceLocationInMaps() {
        if (!isAdded()) return;
        String query = workspaceLocationQueryCache == null ? "" : workspaceLocationQueryCache.trim();
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "esa ubi no existe", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!locationSeemsValid(query)) {
            Toast.makeText(requireContext(), "esa ubi no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri mapsUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapsIntent = new Intent(Intent.ACTION_VIEW, mapsUri).setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapsIntent);
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, mapsUri));
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        try {
            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        Toast.makeText(requireContext(), "no funciona", Toast.LENGTH_SHORT).show();
    }

    private boolean locationSeemsValid(@NonNull String query) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        if (!Geocoder.isPresent()) return true;
        try {
            List<Address> results = geocoder.getFromLocationName(query, 1);
            return results != null && !results.isEmpty();
        } catch (IOException e) {
            // Si no hay red o el geocoder falla, no bloqueamos la apertura de Maps.
            return true;
        }
    }
    private void updateCtaVisibilityForCurrentTab() {
        if (workspaceCtaLayout == null) return;
        boolean ctaAllowedTab = TAB_EXPENSES.equals(currentTab) || TAB_REMINDERS.equals(currentTab);
        boolean hideForSearch = TAB_EXPENSES.equals(currentTab) && expensesSearchEt != null && expensesSearchEt.hasFocus();
        boolean visible = currentGroupId != null && ctaAllowedTab && !hideForSearch;
        workspaceCtaLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void ensureManagementFragment() {
        if (!isAdded() || tenantsContainer == null) return;
        if (getChildFragmentManager().isStateSaved()) return;
        Fragment existing = getChildFragmentManager().findFragmentByTag("management");
        if (existing instanceof RentalManagementFragment) {
            ((RentalManagementFragment) existing).refreshData();
            return;
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.tenantsContainer, new RentalManagementFragment(), "management")
                .commitAllowingStateLoss();
    }

    private void updateTabStyle(Button button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_default);
        button.setTextColor(requireContext().getColor(selected ? R.color.on_primary_green : R.color.text_light));
    }

    private void handleMainAction() {
        if (currentGroupId == null) {
            Toast.makeText(requireContext(), "Selecciona un piso primero", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TAB_EXPENSES.equals(currentTab)) {
            showExpenseActionDialog();
        } else if (TAB_REMINDERS.equals(currentTab)) {
            createReminderDialog();
        }
    }

    private void createRoomFromWorkspace() {
        if (!canManageRooms()) {
            Toast.makeText(requireContext(), "Solo el propietario puede crear habitaciones", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentGroupId == null) return;

        loadCurrentGroupRooms(rooms -> {
            int nextRoomNumber = 1;
            for (RoomOption room : rooms) {
                if (room.roomNumber >= nextRoomNumber) {
                    nextRoomNumber = room.roomNumber + 1;
                }
            }

            View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_room_setup, null, false);
            EditText roomNameEt = form.findViewById(R.id.roomNameEt);
            EditText roomCapacityEt = form.findViewById(R.id.roomCapacityEt);
            EditText roomCostEt = form.findViewById(R.id.roomCostEt);
            roomNameEt.setHint("Ejemplo: Habitación " + nextRoomNumber);

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Nueva habitación",
                    "Indica nombre, capacidad y coste mensual.",
                    form,
                    "Cancelar",
                    "Crear"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            int roomNumberToSave = nextRoomNumber;
            shell.confirmBtn.setOnClickListener(v -> {
                String nameValue = roomNameEt.getText().toString().trim();
                String capacityValue = roomCapacityEt.getText().toString().trim();
                String costValue = roomCostEt.getText().toString().trim();

                if (nameValue.isEmpty() || capacityValue.isEmpty() || costValue.isEmpty()) {
                    Toast.makeText(requireContext(), "Completa todos los datos de la habitación", Toast.LENGTH_SHORT).show();
                    return;
                }

                int parsedCapacity;
                double parsedCost;
                try {
                    parsedCapacity = Integer.parseInt(capacityValue);
                    parsedCost = Double.parseDouble(costValue);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Capacidad o coste no válidos", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (parsedCapacity <= 0) {
                    Toast.makeText(requireContext(), "La capacidad debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (parsedCost < 0) {
                    Toast.makeText(requireContext(), "El coste no puede ser negativo", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Map<String, Object> roomData = new HashMap<>();
                roomData.put("groupId", currentGroupId);
                roomData.put("roomNumber", roomNumberToSave);
                roomData.put("name", nameValue);
                roomData.put("capacity", parsedCapacity);
                roomData.put("monthlyCost", parsedCost);
                roomData.put("memberEmails", new ArrayList<String>());
                roomData.put("memberCount", 0);
                roomData.put("createdByUid", uid);
                roomData.put("updatedByUid", uid);
                roomData.put("createdAt", FieldValue.serverTimestamp());
                roomData.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("rooms_groups")
                        .add(roomData)
                        .addOnSuccessListener(done -> {
                            Toast.makeText(requireContext(), "Habitación creada", Toast.LENGTH_SHORT).show();
                            refreshWorkspace();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo crear la habitación", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void showRoomQuickActionsDialog() {
        if (currentGroupId == null) {
            Toast.makeText(requireContext(), "Selecciona un piso primero", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        Spinner actionSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        actionSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        actionSpinner.setPadding(dp(10), 0, dp(10), 0);
        actionSpinner.setAdapter(buildLightSpinnerAdapter(new String[]{
                "Ver información",
                "Editar habitación",
                "Eliminar habitación",
                "Añadir habitación"
        }));
        content.addView(actionSpinner);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Gestión de habitaciones",
                "Elige una acción y pulsa continuar.",
                content,
                "Cancelar",
                "Continuar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String selected = String.valueOf(actionSpinner.getSelectedItem());
            dialog.dismiss();
            if ("Ver información".equals(selected)) {
                showSelectedRoomInfoDialog();
            } else if ("Editar habitación".equals(selected)) {
                editSelectedRoomFromFilter();
            } else if ("Eliminar habitación".equals(selected)) {
                deleteSelectedRoomFromFilter();
            } else {
                createRoomFromWorkspace();
            }
        });
    }

    private void showSelectedRoomInfoDialog() {
        if (!hasRoomContext()) {
            Toast.makeText(requireContext(), "Selecciona una habitación en el desplegable", Toast.LENGTH_SHORT).show();
            return;
        }
        loadRoomRowForCurrentSelection(row -> {
            if (row == null || row.snapshot == null) return;
            String name = row.snapshot.getString("name");
            Long roomNumber = row.snapshot.getLong("roomNumber");
            Long capacity = row.snapshot.getLong("capacity");
            Double monthlyCost = row.snapshot.getDouble("monthlyCost");
            List<String> residents = castEmails(row.snapshot.get("memberEmails"));

            StringBuilder details = new StringBuilder();
            details.append("Nombre: ").append(name == null || name.trim().isEmpty() ? "Habitación" : name);
            details.append("\nNúmero: ").append(roomNumber == null ? "-" : roomNumber);
            details.append("\nCapacidad: ").append(capacity == null ? 0 : capacity);
            details.append("\nCoste mensual: ").append(monthlyCost == null ? "0.00" : new DecimalFormat("0.00").format(monthlyCost)).append(" EUR");
            details.append("\nResidentes: ").append(residents.isEmpty() ? "Sin asignar" : residents.size());

            View content = DialogUtils.createMessageView(requireContext(), details.toString());
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Información habitación",
                    "Datos de la habitación seleccionada.",
                    content,
                    null,
                    "Cerrar"
            );
            AlertDialog infoDialog = DialogUtils.show(requireContext(), shell.root);
            shell.confirmBtn.setOnClickListener(v -> infoDialog.dismiss());
        });
    }

    private void editSelectedRoomFromFilter() {
        if (!canManageRooms()) {
            Toast.makeText(requireContext(), "Solo el propietario puede editar habitaciones", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasRoomContext()) {
            Toast.makeText(requireContext(), "Selecciona una habitación en el desplegable", Toast.LENGTH_SHORT).show();
            return;
        }
        loadRoomRowForCurrentSelection(row -> {
            if (row == null) return;
            editRoomFromWorkspace(row);
        });
    }

    private void deleteSelectedRoomFromFilter() {
        if (!canManageRooms()) {
            Toast.makeText(requireContext(), "Solo el propietario puede eliminar habitaciones", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasRoomContext()) {
            Toast.makeText(requireContext(), "Selecciona una habitación en el desplegable", Toast.LENGTH_SHORT).show();
            return;
        }
        loadRoomRowForCurrentSelection(row -> {
            if (row == null) return;
            requestRoomDeletion(row);
        });
    }

    private void loadRoomRowForCurrentSelection(RoomRowCallback callback) {
        if (currentRoomId == null || currentRoomId.trim().isEmpty()) {
            callback.onLoaded(null);
            return;
        }
        db.collection("rooms_groups").document(currentRoomId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "La habitación ya no existe", Toast.LENGTH_SHORT).show();
                        callback.onLoaded(null);
                        return;
                    }
                    String roomName = doc.getString("name");
                    Long roomNumber = doc.getLong("roomNumber");
                    Long capacity = doc.getLong("capacity");
                    List<String> residents = castEmails(doc.get("memberEmails"));
                    String title = (roomNumber == null || roomNumber <= 0)
                            ? (roomName == null || roomName.trim().isEmpty() ? "Habitación" : roomName)
                            : "Hab. " + roomNumber + " - " + (roomName == null || roomName.trim().isEmpty() ? "Habitación" : roomName);
                    String subtitle = "Capacidad: " + (capacity == null ? 0 : capacity)
                            + " | Residentes: " + residents.size();
                    Double monthlyCost = doc.getDouble("monthlyCost");
                    WorkspaceRow row = new WorkspaceRow(
                            doc.getId(),
                            "room",
                            title,
                            subtitle,
                            (monthlyCost == null ? "0.00" : new DecimalFormat("0.00").format(monthlyCost)) + " EUR",
                            doc
                    );
                    callback.onLoaded(row);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "No se pudo cargar la habitación", Toast.LENGTH_SHORT).show();
                    callback.onLoaded(null);
                });
    }

    private void loadRoomFilterOptions() {
        if (currentGroupId == null || roomFilterSpinner == null) return;
        loadCurrentGroupRooms(rooms -> {
            roomFilterOptions.clear();
            roomFilterOptions.addAll(rooms);

            String[] labels = new String[roomFilterOptions.size() + 1];
            labels[0] = ROOM_ALL_LABEL;
            for (int i = 0; i < roomFilterOptions.size(); i++) {
                RoomOption option = roomFilterOptions.get(i);
                labels[i + 1] = option.name;
            }

            boolean hadRoomContext = hasRoomContext();
            if (hadRoomContext) {
                SessionStore.clearCurrentRoom(requireContext());
                currentRoomId = null;
                currentRoomName = null;
                currentRoomMembers = new ArrayList<>();
            }

            isUpdatingRoomFilterSpinner = true;
            roomFilterSpinner.setAdapter(buildLightSpinnerAdapter(labels));
            roomFilterSpinner.setSelection(0);
            isUpdatingRoomFilterSpinner = false;
            addRoomQuickBtn.setVisibility(canManageRooms() ? View.VISIBLE : View.GONE);

            if (hadRoomContext) {
                loadRoomContext(() -> {
                    loadExpenses();
                    loadFinancialViews();
                    loadReminders();
                    switchTab(currentTab);
                });
            }
        });
    }

    private void onRoomFilterSelected(int position) {
        if (currentGroupId == null) return;
        if (position <= 0) {
            SessionStore.clearCurrentRoom(requireContext());
            currentRoomId = null;
            currentRoomName = null;
            currentRoomMembers = new ArrayList<>();
        } else if (position - 1 < roomFilterOptions.size()) {
            RoomOption selected = roomFilterOptions.get(position - 1);
            SessionStore.setCurrentRoom(requireContext(), selected.id, selected.name);
            currentRoomId = selected.id;
            currentRoomName = selected.name;
        }

        loadRoomContext(() -> {
            loadExpenses();
            loadFinancialViews();
            loadReminders();
            switchTab(currentTab);
        });
    }

    private void showRoomActionsFromTab(WorkspaceRow row) {
        if ("all_rooms".equals(row.id) || row.snapshot == null) return;
        if (!canManageRooms()) {
            Toast.makeText(requireContext(), "Solo el propietario puede gestionar habitaciones", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        Spinner actionSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        actionSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        actionSpinner.setPadding(dp(10), 0, dp(10), 0);
        actionSpinner.setAdapter(buildLightSpinnerAdapter(new String[]{
                "Editar habitación",
                "Editar inquilinos",
                "Eliminar habitación"
        }));
        content.addView(actionSpinner);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Selecciona la acción para esta habitación.",
                content,
                "Cancelar",
                "Continuar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String selected = String.valueOf(actionSpinner.getSelectedItem());
            dialog.dismiss();
            if ("Editar habitación".equals(selected)) {
                editRoomFromWorkspace(row);
            } else if ("Editar inquilinos".equals(selected)) {
                editRoomResidentsFromWorkspace(row);
            } else {
                requestRoomDeletion(row);
            }
        });
    }

    private void editRoomFromWorkspace(WorkspaceRow row) {
        if (row.snapshot == null) return;
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_room_setup, null, false);
        EditText roomNameEt = form.findViewById(R.id.roomNameEt);
        EditText roomCapacityEt = form.findViewById(R.id.roomCapacityEt);
        EditText roomCostEt = form.findViewById(R.id.roomCostEt);

        String roomName = row.snapshot.getString("name");
        Long capacity = row.snapshot.getLong("capacity");
        Double monthlyCost = row.snapshot.getDouble("monthlyCost");
        roomNameEt.setText(roomName == null ? "" : roomName);
        roomCapacityEt.setText(capacity == null ? "" : String.valueOf(capacity.intValue()));
        roomCostEt.setText(monthlyCost == null ? "" : String.valueOf(monthlyCost));

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Editar habitación",
                "Actualiza nombre, capacidad y coste.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String nameValue = roomNameEt.getText().toString().trim();
            String capacityValue = roomCapacityEt.getText().toString().trim();
            String costValue = roomCostEt.getText().toString().trim();
            if (nameValue.isEmpty() || capacityValue.isEmpty() || costValue.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los datos de la habitación", Toast.LENGTH_SHORT).show();
                return;
            }

            int parsedCapacity;
            double parsedCost;
            try {
                parsedCapacity = Integer.parseInt(capacityValue);
                parsedCost = Double.parseDouble(costValue);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Capacidad o coste no válidos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (parsedCapacity <= 0) {
                Toast.makeText(requireContext(), "La capacidad debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                return;
            }
            if (parsedCost < 0) {
                Toast.makeText(requireContext(), "El coste no puede ser negativo", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", nameValue);
            updates.put("capacity", parsedCapacity);
            updates.put("monthlyCost", parsedCost);
            updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            updates.put("updatedAt", FieldValue.serverTimestamp());
            db.collection("rooms_groups").document(row.id)
                    .update(updates)
                    .addOnSuccessListener(v2 -> {
                    Toast.makeText(requireContext(), "Habitación actualizada", Toast.LENGTH_SHORT).show();
                        refreshWorkspace();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo actualizar la habitación", Toast.LENGTH_SHORT).show());
        });
    }

    private void editRoomResidentsFromWorkspace(WorkspaceRow row) {
        if (row.snapshot == null) return;
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                Toast.makeText(requireContext(), "No hay miembros para asignar", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> currentResidents = castEmails(row.snapshot.get("memberEmails"));

            ScrollView scroll = new ScrollView(requireContext());
            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(content);

            List<CheckBox> checks = new ArrayList<>();
            for (String member : members) {
                CheckBox cb = new CheckBox(requireContext());
                cb.setText(displayNameForEmail(member));
                cb.setTextColor(requireContext().getColor(R.color.text_light));
                cb.setChecked(currentResidents.contains(member));
                checks.add(cb);
                content.addView(cb);
            }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Editar inquilinos",
                    "Marca los miembros que viven en esta habitación.",
                    scroll,
                    "Cancelar",
                    "Guardar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            shell.confirmBtn.setOnClickListener(view -> {
                List<String> selected = new ArrayList<>();
                for (int i = 0; i < checks.size(); i++) {
                    if (checks.get(i).isChecked()) selected.add(members.get(i));
                }
                Map<String, Object> updates = new HashMap<>();
                updates.put("memberEmails", selected);
                updates.put("memberCount", selected.size());
                updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                updates.put("updatedAt", FieldValue.serverTimestamp());
                db.collection("rooms_groups").document(row.id)
                        .update(updates)
                        .addOnSuccessListener(done -> {
                            Toast.makeText(requireContext(), "Inquilinos actualizados", Toast.LENGTH_SHORT).show();
                            refreshWorkspace();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo actualizar", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void requestRoomDeletion(WorkspaceRow row) {
        showDeleteConfirmation(
                "Eliminar habitación",
                "Se eliminará la habitación y su asignación de inquilinos.",
                () -> {
                    db.collection("rooms_groups").document(row.id)
                            .delete()
                            .addOnSuccessListener(v -> {
                                if (row.id.equals(currentRoomId)) {
                                    SessionStore.clearCurrentRoom(requireContext());
                                    currentRoomId = null;
                                    currentRoomName = null;
                                }
                                Toast.makeText(requireContext(), "Habitación eliminada", Toast.LENGTH_SHORT).show();
                                refreshWorkspace();
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo eliminar la habitación", Toast.LENGTH_SHORT).show());
                }
        );
    }

    private void showExpenseActionDialog() {
        if (expenseActionDialogVisible) return;
        expenseActionDialogVisible = true;
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        boolean fixedBilling = BILLING_FIXED.equals(currentBillingModel);
        Spinner actionSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        actionSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        actionSpinner.setPadding(dp(10), 0, dp(10), 0);
        actionSpinner.setAdapter(buildLightSpinnerAdapter(
                fixedBilling
                        ? new String[]{"Registrar pago", "Exportar PDF", "Mostrar QR"}
                        : new String[]{"Nuevo gasto", "Registrar pago", "Exportar PDF", "Mostrar QR"}
        ));
        content.addView(actionSpinner);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nueva acción",
                fixedBilling
                        ? "Registra pagos, exporta PDF o comparte acceso al piso."
                        : "Crea un gasto, registra un pago o comparte acceso al piso.",
                content,
                "Cancelar",
                "Continuar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        dialog.setOnDismissListener(d -> expenseActionDialogVisible = false);
        shell.cancelBtn.setOnClickListener(v -> {
            shell.cancelBtn.setEnabled(false);
            shell.confirmBtn.setEnabled(false);
            dialog.dismiss();
        });
        shell.confirmBtn.setOnClickListener(v -> {
            shell.cancelBtn.setEnabled(false);
            shell.confirmBtn.setEnabled(false);
            String selected = String.valueOf(actionSpinner.getSelectedItem());
            dialog.dismiss();
            if ("Nuevo gasto".equals(selected)) {
                createExpenseDialog();
            } else if ("Registrar pago".equals(selected)) {
                createPaymentDialog();
            } else if ("Exportar PDF".equals(selected)) {
                exportMonthlySummaryPdf();
            } else if ("Mostrar QR".equals(selected)) {
                showCurrentGroupQrDialog();
            }
        });
    }

    private void showCurrentGroupQrDialog() {
        if (currentGroupId == null) {
            Toast.makeText(requireContext(), "Selecciona un piso primero", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            String code = doc.getString("shareCode");
            if (code == null || code.trim().isEmpty()) {
                code = doc.getId().toUpperCase(Locale.ROOT);
            }
            String payload = "flatshare://join?code=" + code;
            View content = buildQrContent(payload, code);
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "QR del piso",
                    "Comparte este QR para que se unan al piso.",
                    content,
                    null,
                    "Cerrar"
            );
            AlertDialog qrDialog = DialogUtils.show(requireContext(), shell.root);
            shell.confirmBtn.setOnClickListener(v -> qrDialog.dismiss());
        });
    }

    private View buildQrContent(String payload, String code) {
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

        TextView codeTv = DialogUtils.createMessageView(requireContext(), "Código: " + code);
        codeTv.setPadding(0, dp(10), 0, 0);
        layout.addView(codeTv);
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

    private void createExpenseDialog() {
        if (BILLING_FIXED.equals(currentBillingModel)) {
            Toast.makeText(requireContext(), "En alquiler fijo no se registran gastos de suministros.", Toast.LENGTH_LONG).show();
            return;
        }
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                Toast.makeText(requireContext(), "No hay miembros para repartir", Toast.LENGTH_SHORT).show();
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                if (rooms.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            "Primero crea al menos una habitación en la pestaña Habitaciones para poder añadir gastos.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
                pendingTicketUri = null;
                setupTypeSpinner((Spinner) form.findViewById(R.id.categorySpinner), null);
                setupPrioritySpinner((Spinner) form.findViewById(R.id.expensePrioritySpinner), null);
                setupDateField(form.findViewById(R.id.dueDateEt));
                setupSplitUi(form, members, null);
                List<String> defaultRoomIds = hasRoomContext() ? Collections.singletonList(currentRoomId) : null;
                setupRoomSelector(
                        form,
                        members,
                        rooms,
                        defaultRoomIds,
                        hasRoomContext() ? currentRoomId : null,
                        hasRoomContext() ? currentRoomName : null,
                        null
                );
                setupTicketControls(form);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Nuevo gasto",
                        "Añade un gasto al piso actual.",
                        form,
                        "Cancelar",
                        "Guardar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
                shell.confirmBtn.setOnClickListener(v -> {
                    if (saveExpense(form, null, members, rooms)) {
                        dialog.dismiss();
                    }
                });
            });
        });
    }

    private boolean saveExpense(View form, @Nullable String documentId, List<String> members, List<RoomOption> rooms) {
        if (BILLING_FIXED.equals(currentBillingModel)) {
            Toast.makeText(requireContext(), "Este piso usa alquiler fijo. No se pueden guardar gastos.", Toast.LENGTH_LONG).show();
            return false;
        }
        String concept = ((EditText) form.findViewById(R.id.conceptEt)).getText().toString().trim();
        String amountStr = ((EditText) form.findViewById(R.id.amountEt)).getText().toString().trim();
        String category = ((Spinner) form.findViewById(R.id.categorySpinner)).getSelectedItem().toString();
        String priority = ((Spinner) form.findViewById(R.id.expensePrioritySpinner)).getSelectedItem().toString();
        String dueDateText = ((EditText) form.findViewById(R.id.dueDateEt)).getText().toString().trim();
        if (concept.isEmpty() || amountStr.isEmpty() || currentGroupId == null) return false;
        if (dueDateText.isEmpty()) {
            Toast.makeText(requireContext(), "Debes indicar fecha límite", Toast.LENGTH_SHORT).show();
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Importe no válido", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (amount <= 0) {
            Toast.makeText(requireContext(), "El importe debe ser mayor que 0", Toast.LENGTH_SHORT).show();
            return false;
        }

        String customSplit = buildCustomSplitFromUi(form, amount, members);
        if (customSplit == null) {
            return false;
        }
        Date dueDate = parseDueDateOrNull(dueDateText);
        if (dueDate == null) {
            Toast.makeText(requireContext(), "Fecha inválida. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }
        List<RoomOption> selectedRooms = resolveSelectedRoomOptions(form, rooms);
        if (selectedRooms.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona al menos una habitación", Toast.LENGTH_SHORT).show();
            return false;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("groupId", currentGroupId);
        data.put("concept", concept);
        data.put("amount", amount);
        data.put("payerId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        data.put("payerEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT));
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("customSplit", customSplit);
        data.put("category", category.isEmpty() ? "otros" : category.toLowerCase(Locale.ROOT));
        data.put("priority", priority.isEmpty() ? "media" : priority.toLowerCase(Locale.ROOT));
        data.put("ticketUri", pendingTicketUri == null ? "" : pendingTicketUri);
        data.put("dueAt", dueDate);
        data.put("dueDateText", dueDateText);
        List<String> selectedRoomIds = new ArrayList<>();
        List<String> selectedRoomNames = new ArrayList<>();
        for (RoomOption room : selectedRooms) {
            selectedRoomIds.add(room.id);
            selectedRoomNames.add(room.name);
        }
        boolean allRoomsSelected = selectedRooms.size() == rooms.size();
        data.put("roomIds", selectedRoomIds);
        data.put("roomNames", selectedRoomNames);
        if (allRoomsSelected) {
            data.put("roomId", "all");
            data.put("roomName", ROOM_ALL_LABEL);
        } else if (selectedRooms.size() == 1) {
            data.put("roomId", selectedRooms.get(0).id);
            data.put("roomName", selectedRooms.get(0).name);
        } else {
            data.put("roomId", "multi");
            data.put("roomName", "Varias habitaciones");
        }

        if (documentId == null) {
            db.collection("expenses").add(data).addOnSuccessListener(task -> {
                logActivity("expense_created", concept, amount, data.get("category").toString());
                syncExpenseDeadlines(
                        task.getId(),
                        concept,
                        amount,
                        customSplit,
                        dueDate,
                        priority,
                        FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT)
                );
                pendingTicketUri = null;
                loadExpenses();
                loadFinancialViews();
            });
        } else {
            db.collection("expenses").document(documentId).update(data).addOnSuccessListener(task -> {
                logActivity("expense_edited", concept, amount, data.get("category").toString());
                syncExpenseDeadlines(
                        documentId,
                        concept,
                        amount,
                        customSplit,
                        dueDate,
                        priority,
                        FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT)
                );
                pendingTicketUri = null;
                loadExpenses();
                loadFinancialViews();
            });
        }
        return true;
    }

    private void createPaymentDialog() {
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                Toast.makeText(requireContext(), "No hay miembros disponibles para registrar el pago", Toast.LENGTH_SHORT).show();
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null, false);
                Spinner paymentCategorySpinner = form.findViewById(R.id.paymentCategorySpinner);
                if (BILLING_FIXED.equals(currentBillingModel)) {
                    paymentCategorySpinner.setAdapter(buildLightSpinnerAdapter(new String[]{capitalizeTypeLabel(CATEGORY_RENT)}));
                    paymentCategorySpinner.setSelection(0);
                    paymentCategorySpinner.setEnabled(false);
                } else {
                    setupTypeSpinner(paymentCategorySpinner, null);
                }
                setupPrioritySpinner((Spinner) form.findViewById(R.id.paymentPrioritySpinner), "media");
                setupDateField(form.findViewById(R.id.paymentDueDateEt));
                setupPaymentTargetSelectors(form, members, rooms, hasRoomContext() ? currentRoomId : null);
                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Registrar pago",
                        "Elige si el pago es para una habitación, un miembro o para todos.",
                        form,
                        "Cancelar",
                        "Guardar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
                shell.confirmBtn.setOnClickListener(v -> {
                    if (savePayment(form, members, rooms)) {
                        dialog.dismiss();
                    }
                });
            });
        });
    }

    private void setupPaymentTargetSelectors(View form, List<String> members, List<RoomOption> rooms, @Nullable String defaultRoomId) {
        Spinner targetTypeSpinner = form.findViewById(R.id.paymentTargetTypeSpinner);
        Spinner memberSpinner = form.findViewById(R.id.paymentMemberSpinner);
        Spinner roomSpinner = form.findViewById(R.id.paymentRoomSpinner);

        targetTypeSpinner.setAdapter(buildLightSpinnerAdapter(PAYMENT_TARGET_TYPES));
        memberSpinner.setAdapter(buildLightSpinnerAdapter(members.toArray(new String[0])));

        String[] roomLabels = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            roomLabels[i] = rooms.get(i).name;
        }
        roomSpinner.setAdapter(buildLightSpinnerAdapter(roomLabels));

        targetTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = PAYMENT_TARGET_TYPES[position];
                memberSpinner.setVisibility("Miembro".equals(type) ? View.VISIBLE : View.GONE);
                roomSpinner.setVisibility("Habitación".equals(type) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int roomIndex = findRoomIndexById(rooms, defaultRoomId);
        if (roomIndex >= 0) {
            targetTypeSpinner.setSelection(0);
            roomSpinner.setSelection(roomIndex);
        } else {
            targetTypeSpinner.setSelection(1);
        }
    }

    private boolean savePayment(View form, List<String> members, List<RoomOption> rooms) {
        if (currentGroupId == null) return false;
        String amountStr = ((EditText) form.findViewById(R.id.paymentAmountEt)).getText().toString().trim();
        String concept = ((EditText) form.findViewById(R.id.paymentConceptEt)).getText().toString().trim();
        String category = ((Spinner) form.findViewById(R.id.paymentCategorySpinner)).getSelectedItem().toString();
        String priority = ((Spinner) form.findViewById(R.id.paymentPrioritySpinner)).getSelectedItem().toString();
        String dueDateText = ((EditText) form.findViewById(R.id.paymentDueDateEt)).getText().toString().trim();
        String targetType = ((Spinner) form.findViewById(R.id.paymentTargetTypeSpinner)).getSelectedItem().toString();
        int memberIndex = ((Spinner) form.findViewById(R.id.paymentMemberSpinner)).getSelectedItemPosition();
        int roomIndex = ((Spinner) form.findViewById(R.id.paymentRoomSpinner)).getSelectedItemPosition();
        if (amountStr.isEmpty() || dueDateText.isEmpty()) {
            Toast.makeText(requireContext(), "Debes indicar importe y fecha límite", Toast.LENGTH_SHORT).show();
            return false;
        }
        Date dueDate = parseDueDateOrNull(dueDateText);
        if (dueDate == null) {
            Toast.makeText(requireContext(), "Fecha inválida. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Importe no válido", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (amount <= 0) {
            Toast.makeText(requireContext(), "El importe debe ser mayor que 0", Toast.LENGTH_SHORT).show();
            return false;
        }

        String fromEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        String normalizedCategory = BILLING_FIXED.equals(currentBillingModel)
                ? CATEGORY_RENT
                : (category == null || category.trim().isEmpty() ? "otros" : category.trim().toLowerCase(Locale.ROOT));
        List<PaymentTarget> targets = resolvePaymentTargets(targetType, members, rooms, memberIndex, roomIndex, fromEmail);
        if (targets.isEmpty()) {
            Toast.makeText(requireContext(), "No hay destinatarios válidos para este pago", Toast.LENGTH_SHORT).show();
            return false;
        }

        double splitAmount = amount / targets.size();
        String safePriority = priority == null || priority.trim().isEmpty() ? "media" : priority.toLowerCase(Locale.ROOT);
        String safeConcept = concept.isEmpty() ? "Pago directo" : concept;
        WriteBatch batch = db.batch();
        List<Map<String, Object>> createdPayments = new ArrayList<>();
        for (PaymentTarget target : targets) {
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("amount", splitAmount);
            data.put("fromEmail", fromEmail);
            data.put("toEmail", target.toEmail);
            data.put("category", normalizedCategory);
            data.put("priority", safePriority);
            data.put("status", "pending");
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("dueAt", dueDate);
            data.put("dueDateText", dueDateText);
            data.put("concept", safeConcept);
            data.put("targetType", targetType.toLowerCase(Locale.ROOT));
            data.put("roomId", target.roomId == null ? "" : target.roomId);
            data.put("roomName", target.roomName == null ? "" : target.roomName);
            var paymentRef = db.collection("payments").document();
            batch.set(paymentRef, data);
            Map<String, Object> created = new HashMap<>();
            created.put("paymentId", paymentRef.getId());
            created.put("toEmail", target.toEmail);
            createdPayments.add(created);
        }
        batch.commit().addOnSuccessListener(v -> {
            for (Map<String, Object> created : createdPayments) {
                String paymentId = String.valueOf(created.get("paymentId"));
                String toEmail = String.valueOf(created.get("toEmail"));
                logActivity("payment_created", "Pago a " + toEmail, splitAmount, "payment");
                syncPaymentDeadline(paymentId, safeConcept, splitAmount, fromEmail, toEmail, dueDate, safePriority);
            }
            loadExpenses();
            loadFinancialViews();
        });
        return true;
    }

    private List<PaymentTarget> resolvePaymentTargets(
            String targetType,
            List<String> members,
            List<RoomOption> rooms,
            int memberIndex,
            int roomIndex,
            String fromEmail
    ) {
        List<PaymentTarget> targets = new ArrayList<>();
        if ("Miembro".equals(targetType)) {
            if (memberIndex >= 0 && memberIndex < members.size()) {
                String toEmail = members.get(memberIndex).toLowerCase(Locale.ROOT);
                if (!toEmail.equals(fromEmail)) {
                    targets.add(new PaymentTarget(toEmail, null, null));
                }
            }
            return targets;
        }
        if ("Habitación".equals(targetType)) {
            if (roomIndex >= 0 && roomIndex < rooms.size()) {
                RoomOption room = rooms.get(roomIndex);
                for (String resident : room.memberEmails) {
                    String email = resident.toLowerCase(Locale.ROOT);
                    if (!email.equals(fromEmail)) {
                        targets.add(new PaymentTarget(email, room.id, room.name));
                    }
                }
            }
            return targets;
        }

        for (String member : members) {
            String email = member.toLowerCase(Locale.ROOT);
            if (!email.equals(fromEmail)) {
                targets.add(new PaymentTarget(email, null, null));
            }
        }
        return targets;
    }

    private void createReminderDialog() {
        if (currentGroupId == null) {
            Toast.makeText(requireContext(), "Selecciona un piso primero", Toast.LENGTH_SHORT).show();
            return;
        }
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                Toast.makeText(requireContext(), "No hay miembros en este piso", Toast.LENGTH_SHORT).show();
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reminder, null, false);
                pendingReminderAttachmentUri = null;
                setupDateField(form.findViewById(R.id.reminderStartDateEt));
                setupReminderFormControls(form, members, rooms, hasRoomContext() ? currentRoomId : null);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Nuevo recordatorio",
                        "Define concepto, destinatario, fecha e intervalo.",
                        form,
                        "Cancelar",
                        "Guardar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
                shell.confirmBtn.setOnClickListener(v -> {
                    String concept = ((EditText) form.findViewById(R.id.reminderTitleEt)).getText().toString().trim();
                    String dateText = ((EditText) form.findViewById(R.id.reminderStartDateEt)).getText().toString().trim();
                    if (concept.isEmpty() || dateText.isEmpty()) {
                        Toast.makeText(requireContext(), "Completa concepto y fecha", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Date startAt = parseReminderStartDateOrNull(dateText);
                    if (startAt == null) {
                        Toast.makeText(requireContext(), "Fecha inválida. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ReminderIntervalConfig intervalConfig = resolveReminderInterval(form);
                    if (intervalConfig == null) return;

                    ReminderTargetConfig targetConfig = resolveReminderTargets(form, members, rooms);
                    if (targetConfig == null || targetConfig.targetEmails.isEmpty()) {
                        Toast.makeText(requireContext(), "No hay destinatarios válidos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
                    String intervalKey = intervalConfig.intervalKey.toLowerCase(Locale.ROOT);
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", concept);
                    data.put("interval", intervalKey);
                    data.put("intervalDays", intervalConfig.intervalDays);
                    data.put("startAt", startAt);
                    data.put("startDateText", dateText);
                    data.put("targetType", targetConfig.targetType.toLowerCase(Locale.ROOT));
                    data.put("targetEmails", targetConfig.targetEmails);
                    data.put("targetMemberEmail", targetConfig.memberEmail);
                    data.put("roomId", targetConfig.primaryRoomId);
                    data.put("roomName", targetConfig.primaryRoomName);
                    data.put("roomIds", targetConfig.roomIds);
                    data.put("roomNames", targetConfig.roomNames);
                    data.put("attachmentUri", pendingReminderAttachmentUri == null ? "" : pendingReminderAttachmentUri);
                    data.put("ownerUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    data.put("ownerEmail", myEmail);
                    data.put("groupId", currentGroupId);
                    data.put("groupName", currentGroupName);
                    data.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("reminders").add(data).addOnSuccessListener(ref -> {
                        int reminderCode = Math.abs(("manual_" + ref.getId()).hashCode());
                        db.collection("reminders").document(ref.getId()).update("reminderCode", reminderCode);
                        if (targetConfig.targetEmails.contains(myEmail)) {
                            scheduleManualReminder(reminderCode, concept, startAt.getTime(), intervalConfig.intervalMs);
                        }
                        loadReminders();
                        Toast.makeText(requireContext(), "Recordatorio guardado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }).addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "No se pudo guardar el recordatorio", Toast.LENGTH_SHORT).show()
                    );
                });
            });
        });
    }

    private void setupReminderFormControls(View form, List<String> members, List<RoomOption> rooms, @Nullable String defaultRoomId) {
        Spinner intervalSpinner = form.findViewById(R.id.reminderIntervalSpinner);
        intervalSpinner.setAdapter(buildLightSpinnerAdapter(REMINDER_INTERVAL_TYPES));
        intervalSpinner.setSelection(1);
        TextView customDaysLabelTv = form.findViewById(R.id.reminderCustomDaysLabelTv);
        EditText customDaysEt = form.findViewById(R.id.reminderCustomDaysEt);
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int visibility = position == 4 ? View.VISIBLE : View.GONE;
                customDaysEt.setVisibility(visibility);
                customDaysLabelTv.setVisibility(visibility);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupReminderTargetSelectors(form, members, rooms, defaultRoomId);

        pendingReminderAttachmentStatusTv = form.findViewById(R.id.reminderAttachmentStatusTv);
        pendingReminderAttachmentStatusTv.setText("Sin adjunto");
        Button attachBtn = form.findViewById(R.id.reminderAttachBtn);
        attachBtn.setOnClickListener(v -> reminderAttachmentPickerLauncher.launch("*/*"));
    }

    private void setupReminderTargetSelectors(View form, List<String> members, List<RoomOption> rooms, @Nullable String defaultRoomId) {
        Spinner targetTypeSpinner = form.findViewById(R.id.reminderTargetTypeSpinner);
        Spinner memberSpinner = form.findViewById(R.id.reminderMemberSpinner);
        Spinner roomSpinner = form.findViewById(R.id.reminderRoomSpinner);
        LinearLayout roomsContainer = form.findViewById(R.id.reminderRoomsContainer);
        Button addLineBtn = form.findViewById(R.id.addReminderRoomLineBtn);
        Button removeLineBtn = form.findViewById(R.id.removeReminderRoomLineBtn);
        LinearLayout membersContainer = form.findViewById(R.id.reminderMembersContainer);
        Button addMemberLineBtn = form.findViewById(R.id.addReminderMemberLineBtn);
        Button removeMemberLineBtn = form.findViewById(R.id.removeReminderMemberLineBtn);

        targetTypeSpinner.setAdapter(buildLightSpinnerAdapter(REMINDER_TARGET_TYPES));

        List<String> memberLabels = new ArrayList<>();
        for (String email : members) {
            memberLabels.add(displayNameForEmail(email));
        }
        memberSpinner.setAdapter(buildLightSpinnerAdapter(memberLabels.toArray(new String[0])));

        List<String> roomLabels = new ArrayList<>();
        for (RoomOption room : rooms) {
            roomLabels.add(room.name);
        }
        roomSpinner.setAdapter(buildLightSpinnerAdapter(roomLabels.toArray(new String[0])));

        rebindReminderRoomLines(form, rooms, null);
        addLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectReminderRoomLineKeys(roomsContainer);
            currentKeys.add(firstAvailableRoomKey(rooms, currentKeys));
            rebindReminderRoomLines(form, rooms, currentKeys);
            updateReminderTargetSection(form, targetTypeSpinner.getSelectedItemPosition());
        });
        removeLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectReminderRoomLineKeys(roomsContainer);
            if (currentKeys.size() > 1) {
                currentKeys.remove(currentKeys.size() - 1);
            }
            rebindReminderRoomLines(form, rooms, currentKeys);
            updateReminderTargetSection(form, targetTypeSpinner.getSelectedItemPosition());
        });

        rebindReminderMemberLines(form, members, null);
        addMemberLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectReminderMemberLineKeys(membersContainer);
            currentKeys.add(firstAvailableMemberKey(members, currentKeys));
            rebindReminderMemberLines(form, members, currentKeys);
            updateReminderTargetSection(form, targetTypeSpinner.getSelectedItemPosition());
        });
        removeMemberLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectReminderMemberLineKeys(membersContainer);
            if (currentKeys.size() > 1) {
                currentKeys.remove(currentKeys.size() - 1);
            }
            rebindReminderMemberLines(form, members, currentKeys);
            updateReminderTargetSection(form, targetTypeSpinner.getSelectedItemPosition());
        });

        targetTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateReminderTargetSection(form, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int roomIndex = findRoomIndexById(rooms, defaultRoomId);
        if (roomIndex >= 0) {
            targetTypeSpinner.setSelection(2);
            roomSpinner.setSelection(roomIndex);
        } else {
            targetTypeSpinner.setSelection(0);
        }
    }

    private void updateReminderTargetSection(View form, int targetTypePosition) {
        Spinner memberSpinner = form.findViewById(R.id.reminderMemberSpinner);
        Spinner roomSpinner = form.findViewById(R.id.reminderRoomSpinner);
        LinearLayout roomsContainer = form.findViewById(R.id.reminderRoomsContainer);
        Button addLineBtn = form.findViewById(R.id.addReminderRoomLineBtn);
        Button removeLineBtn = form.findViewById(R.id.removeReminderRoomLineBtn);
        LinearLayout membersContainer = form.findViewById(R.id.reminderMembersContainer);
        Button addMemberLineBtn = form.findViewById(R.id.addReminderMemberLineBtn);
        Button removeMemberLineBtn = form.findViewById(R.id.removeReminderMemberLineBtn);

        boolean showMember = targetTypePosition == 1;
        boolean showRoom = targetTypePosition == 2;
        boolean showRoomLines = targetTypePosition == 3;
        boolean showMemberLines = targetTypePosition == 4;

        memberSpinner.setVisibility(showMember ? View.VISIBLE : View.GONE);
        roomSpinner.setVisibility(showRoom ? View.VISIBLE : View.GONE);
        roomsContainer.setVisibility(showRoomLines ? View.VISIBLE : View.GONE);
        addLineBtn.setVisibility(showRoomLines ? View.VISIBLE : View.GONE);
        removeLineBtn.setVisibility(showRoomLines && roomsContainer.getChildCount() > 1 ? View.VISIBLE : View.GONE);
        membersContainer.setVisibility(showMemberLines ? View.VISIBLE : View.GONE);
        addMemberLineBtn.setVisibility(showMemberLines ? View.VISIBLE : View.GONE);
        removeMemberLineBtn.setVisibility(showMemberLines && membersContainer.getChildCount() > 1 ? View.VISIBLE : View.GONE);
    }

    private void rebindReminderRoomLines(View form, List<RoomOption> rooms, @Nullable List<String> seedKeys) {
        LinearLayout container = form.findViewById(R.id.reminderRoomsContainer);
        List<String> selectedKeys = seedKeys == null ? collectReminderRoomLineKeys(container) : new ArrayList<>(seedKeys);
        if (selectedKeys.isEmpty()) {
            selectedKeys.add(firstAvailableRoomKey(rooms, Collections.emptyList()));
        }
        selectedKeys = deduplicateRoomKeys(selectedKeys);
        container.removeAllViews();

        for (int i = 0; i < selectedKeys.size(); i++) {
            Spinner spinner = buildReminderRoomLineSpinner(rooms, selectedKeys, i, selectedKeys.get(i));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    rebindReminderRoomLines(form, rooms, null);
                    Spinner targetTypeSpinner = form.findViewById(R.id.reminderTargetTypeSpinner);
                    updateReminderTargetSection(form, targetTypeSpinner.getSelectedItemPosition());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            container.addView(spinner);
        }
    }

    private Spinner buildReminderRoomLineSpinner(
            List<RoomOption> rooms,
            List<String> selectedKeys,
            int spinnerIndex,
            String selectedKey
    ) {
        Spinner spinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        spinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        spinner.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        if (spinnerIndex > 0) params.topMargin = dp(8);
        spinner.setLayoutParams(params);

        List<String> optionKeys = new ArrayList<>();
        List<String> optionLabels = new ArrayList<>();
        for (RoomOption room : rooms) {
            boolean selectedElsewhere = false;
            for (int i = 0; i < selectedKeys.size(); i++) {
                if (i == spinnerIndex) continue;
                if (room.id.equals(selectedKeys.get(i))) {
                    selectedElsewhere = true;
                    break;
                }
            }
            if (!selectedElsewhere || room.id.equals(selectedKey)) {
                optionKeys.add(room.id);
                optionLabels.add(room.name);
            }
        }
        if (optionKeys.isEmpty() && !rooms.isEmpty()) {
            optionKeys.add(rooms.get(0).id);
            optionLabels.add(rooms.get(0).name);
        }

        spinner.setTag(optionKeys);
        spinner.setAdapter(buildLightSpinnerAdapter(optionLabels.toArray(new String[0])));
        int selectedIndex = optionKeys.indexOf(selectedKey);
        spinner.setSelection(selectedIndex >= 0 ? selectedIndex : 0);
        return spinner;
    }

    private List<String> collectReminderRoomLineKeys(LinearLayout container) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof Spinner spinner)) continue;
            @SuppressWarnings("unchecked")
            List<String> optionKeys = (List<String>) spinner.getTag();
            int selected = spinner.getSelectedItemPosition();
            if (optionKeys == null || selected < 0 || selected >= optionKeys.size()) continue;
            keys.add(optionKeys.get(selected));
        }
        return keys;
    }

    private void rebindReminderMemberLines(View form, List<String> members, @Nullable List<String> seedKeys) {
        LinearLayout container = form.findViewById(R.id.reminderMembersContainer);
        List<String> selectedKeys = seedKeys == null ? collectReminderMemberLineKeys(container) : new ArrayList<>(seedKeys);
        if (selectedKeys.isEmpty()) {
            selectedKeys.add(firstAvailableMemberKey(members, Collections.emptyList()));
        }
        selectedKeys = deduplicateStringKeys(selectedKeys);
        container.removeAllViews();

        for (int i = 0; i < selectedKeys.size(); i++) {
            Spinner spinner = buildReminderMemberLineSpinner(members, selectedKeys, i, selectedKeys.get(i));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    rebindReminderMemberLines(form, members, null);
                    Spinner targetTypeSpinner = form.findViewById(R.id.reminderTargetTypeSpinner);
                    updateReminderTargetSection(form, targetTypeSpinner.getSelectedItemPosition());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            container.addView(spinner);
        }
    }

    private Spinner buildReminderMemberLineSpinner(
            List<String> members,
            List<String> selectedKeys,
            int spinnerIndex,
            String selectedKey
    ) {
        Spinner spinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        spinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        spinner.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        if (spinnerIndex > 0) params.topMargin = dp(8);
        spinner.setLayoutParams(params);

        List<String> optionKeys = new ArrayList<>();
        List<String> optionLabels = new ArrayList<>();
        for (String member : members) {
            boolean selectedElsewhere = false;
            for (int i = 0; i < selectedKeys.size(); i++) {
                if (i == spinnerIndex) continue;
                if (member.equals(selectedKeys.get(i))) {
                    selectedElsewhere = true;
                    break;
                }
            }
            if (!selectedElsewhere || member.equals(selectedKey)) {
                optionKeys.add(member);
                optionLabels.add(displayNameForEmail(member));
            }
        }
        if (optionKeys.isEmpty() && !members.isEmpty()) {
            optionKeys.add(members.get(0));
            optionLabels.add(displayNameForEmail(members.get(0)));
        }

        spinner.setTag(optionKeys);
        spinner.setAdapter(buildLightSpinnerAdapter(optionLabels.toArray(new String[0])));
        int selectedIndex = optionKeys.indexOf(selectedKey);
        spinner.setSelection(selectedIndex >= 0 ? selectedIndex : 0);
        return spinner;
    }

    private List<String> collectReminderMemberLineKeys(LinearLayout container) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof Spinner spinner)) continue;
            @SuppressWarnings("unchecked")
            List<String> optionKeys = (List<String>) spinner.getTag();
            int selected = spinner.getSelectedItemPosition();
            if (optionKeys == null || selected < 0 || selected >= optionKeys.size()) continue;
            keys.add(optionKeys.get(selected));
        }
        return keys;
    }

    @Nullable
    private ReminderIntervalConfig resolveReminderInterval(View form) {
        int intervalIndex = ((Spinner) form.findViewById(R.id.reminderIntervalSpinner)).getSelectedItemPosition();
        if (intervalIndex == 0) return new ReminderIntervalConfig("unico", 0, 0L);
        if (intervalIndex == 1) return new ReminderIntervalConfig("diario", 1, 24L * 60L * 60L * 1000L);
        if (intervalIndex == 2) return new ReminderIntervalConfig("semanal", 7, 7L * 24L * 60L * 60L * 1000L);
        if (intervalIndex == 3) return new ReminderIntervalConfig("mensual", 30, 30L * 24L * 60L * 60L * 1000L);

        String customDaysText = ((EditText) form.findViewById(R.id.reminderCustomDaysEt)).getText().toString().trim();
        if (customDaysText.isEmpty()) {
            Toast.makeText(requireContext(), "Indica cada cuántos días", Toast.LENGTH_SHORT).show();
            return null;
        }
        int customDays;
        try {
            customDays = Integer.parseInt(customDaysText);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Intervalo personalizado no válido", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (customDays <= 0) {
            Toast.makeText(requireContext(), "El intervalo personalizado debe ser mayor que 0", Toast.LENGTH_SHORT).show();
            return null;
        }
        long intervalMs = customDays * 24L * 60L * 60L * 1000L;
        return new ReminderIntervalConfig("personalizado", customDays, intervalMs);
    }

    @Nullable
    private ReminderTargetConfig resolveReminderTargets(View form, List<String> members, List<RoomOption> rooms) {
        int targetTypeIndex = ((Spinner) form.findViewById(R.id.reminderTargetTypeSpinner)).getSelectedItemPosition();
        if (targetTypeIndex == 0) {
            return new ReminderTargetConfig("todos", new ArrayList<>(members), "", "", new ArrayList<>(), new ArrayList<>(), "");
        }
        if (targetTypeIndex == 1) {
            int memberIndex = ((Spinner) form.findViewById(R.id.reminderMemberSpinner)).getSelectedItemPosition();
            if (memberIndex < 0 || memberIndex >= members.size()) return null;
            String targetEmail = members.get(memberIndex).toLowerCase(Locale.ROOT);
            return new ReminderTargetConfig("miembro", Collections.singletonList(targetEmail), "", "", new ArrayList<>(), new ArrayList<>(), targetEmail);
        }
        if (targetTypeIndex == 2) {
            int roomIndex = ((Spinner) form.findViewById(R.id.reminderRoomSpinner)).getSelectedItemPosition();
            if (roomIndex < 0 || roomIndex >= rooms.size()) return null;
            RoomOption room = rooms.get(roomIndex);
            return new ReminderTargetConfig(
                    "habitacion",
                    new ArrayList<>(room.memberEmails),
                    room.id,
                    room.name,
                    Collections.singletonList(room.id),
                    Collections.singletonList(room.name),
                    ""
            );
        }
        if (targetTypeIndex == 3) {
            LinearLayout linesContainer = form.findViewById(R.id.reminderRoomsContainer);
            List<String> selectedRoomKeys = collectReminderRoomLineKeys(linesContainer);
            if (selectedRoomKeys.isEmpty()) return null;
            Set<String> targetEmails = new HashSet<>();
            List<String> roomIds = new ArrayList<>();
            List<String> roomNames = new ArrayList<>();
            for (String roomKey : selectedRoomKeys) {
                for (RoomOption room : rooms) {
                    if (!room.id.equals(roomKey)) continue;
                    roomIds.add(room.id);
                    roomNames.add(room.name);
                    targetEmails.addAll(room.memberEmails);
                    break;
                }
            }
            return new ReminderTargetConfig(
                    "x_habitacion",
                    new ArrayList<>(targetEmails),
                    roomIds.isEmpty() ? "" : roomIds.get(0),
                    roomNames.isEmpty() ? "" : roomNames.get(0),
                    roomIds,
                    roomNames,
                    ""
            );
        }

        LinearLayout memberLinesContainer = form.findViewById(R.id.reminderMembersContainer);
        List<String> selectedMemberEmails = collectReminderMemberLineKeys(memberLinesContainer);
        if (selectedMemberEmails.isEmpty()) return null;
        List<String> normalizedMembers = new ArrayList<>();
        for (String email : deduplicateStringKeys(selectedMemberEmails)) {
            normalizedMembers.add(email.toLowerCase(Locale.ROOT));
        }
        return new ReminderTargetConfig(
                "x_miembro",
                normalizedMembers,
                "",
                "",
                new ArrayList<>(),
                new ArrayList<>(),
                ""
        );
    }

    private void scheduleManualReminder(int reminderCode, String title, long firstTrigger, long intervalMs) {
        if (intervalMs <= 0) {
            ReminderScheduler.scheduleOneTime(requireContext(), reminderCode, "FlatShare: " + title, "Recordatorio pendiente", firstTrigger);
            return;
        }
        ReminderScheduler.schedule(requireContext(), reminderCode, "FlatShare: " + title, "Recordatorio pendiente", firstTrigger, intervalMs);
    }

    @Nullable
    private Date parseReminderStartDateOrNull(String value) {
        try {
            Date parsed = DUE_DATE_FORMAT.parse(value);
            if (parsed == null) return null;
            Calendar selected = Calendar.getInstance();
            selected.setTime(parsed);
            selected.set(Calendar.HOUR_OF_DAY, 10);
            selected.set(Calendar.MINUTE, 0);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            Calendar selectedDay = Calendar.getInstance();
            selectedDay.setTime(parsed);
            selectedDay.set(Calendar.HOUR_OF_DAY, 0);
            selectedDay.set(Calendar.MINUTE, 0);
            selectedDay.set(Calendar.SECOND, 0);
            selectedDay.set(Calendar.MILLISECOND, 0);
            if (selectedDay.before(today)) return null;
            return selected.getTime();
        } catch (ParseException e) {
            return null;
        }
    }

    private void loadReminders() {
        if (currentGroupId == null) return;
        reminderRows.clear();
        db.collection("reminders")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(result.getDocuments());
                    docs.sort((a, b) -> {
                        Date da = a.getDate("startAt");
                        Date dbDate = b.getDate("startAt");
                        if (da == null && dbDate == null) return 0;
                        if (da == null) return 1;
                        if (dbDate == null) return -1;
                        return da.compareTo(dbDate);
                    });
                    for (DocumentSnapshot doc : docs) {
                        String title = doc.getString("title");
                        String subtitle = buildReminderSubtitle(doc);
                        String interval = doc.getString("interval");
                        String amountLabel = interval == null || interval.trim().isEmpty()
                                ? "Recordatorio"
                                : capitalizeTypeLabel(interval);
                        reminderRows.add(new WorkspaceRow(
                                doc.getId(),
                                "reminder",
                                title == null || title.trim().isEmpty() ? "Recordatorio" : title,
                                subtitle,
                                amountLabel,
                                doc
                        ));
                    }
                    remindersAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> remindersAdapter.notifyDataSetChanged());
    }

    private String buildReminderSubtitle(DocumentSnapshot doc) {
        String targetType = doc.getString("targetType");
        String startDateText = doc.getString("startDateText");
        String interval = doc.getString("interval");
        Long intervalDays = doc.getLong("intervalDays");
        String who;
        if ("miembro".equals(targetType)) {
            who = "Miembro: " + displayNameForEmail(doc.getString("targetMemberEmail"));
        } else if ("x_miembro".equals(targetType)) {
            List<String> memberEmails = castEmails(doc.get("targetEmails"));
            if (memberEmails.isEmpty()) {
                who = "X miembros";
            } else {
                List<String> memberNames = new ArrayList<>();
                for (String email : memberEmails) {
                    memberNames.add(displayNameForEmail(email));
                }
                who = "X miembros: " + String.join(", ", memberNames);
            }
        } else if ("habitacion".equals(targetType)) {
            String roomName = doc.getString("roomName");
            who = "Habitación: " + (roomName == null || roomName.trim().isEmpty() ? "Sin nombre" : roomName);
        } else if ("x_habitacion".equals(targetType)) {
            List<String> roomNames = castStrings(doc.get("roomNames"));
            who = roomNames.isEmpty() ? "X habitación" : "X habitación: " + String.join(", ", roomNames);
        } else {
            who = "Todos los miembros";
        }

        String intervalLabel = interval == null ? "semanal" : interval;
        if ("unico".equals(intervalLabel)) {
            intervalLabel = "único";
        }
        if ("personalizado".equals(intervalLabel) && intervalDays != null && intervalDays > 0) {
            intervalLabel = "cada " + intervalDays + " días";
        }
        if (startDateText == null || startDateText.trim().isEmpty()) {
            return who + " | " + intervalLabel;
        }
        return who + " | " + intervalLabel + " | desde " + startDateText;
    }
    private void loadExpenses() {
        if (currentGroupId == null) return;
        DecimalFormat df = new DecimalFormat("0.00");
        expenseRows.clear();

        var expenseQuery = db.collection("expenses").whereEqualTo("groupId", currentGroupId);

        expenseQuery.get().addOnSuccessListener(result -> {
            for (DocumentSnapshot doc : result.getDocuments()) {
                if (BILLING_FIXED.equals(currentBillingModel)) continue;
                if (hasRoomContext() && !matchesExpenseWithCurrentRoom(doc)) continue;
                Double amountValue = doc.getDouble("amount");
                String concept = doc.getString("concept");
                String payerEmail = doc.getString("payerEmail");
                String payerLabel = displayNameForEmail(payerEmail);
                String category = doc.getString("category");
                String roomName = doc.getString("roomName");
                String dueDateText = doc.getString("dueDateText");
                String subtitle = (payerEmail == null ? "Gasto compartido" : "Pagado por " + payerLabel)
                        + (roomName == null || roomName.trim().isEmpty() ? "" : " - hab. " + roomName)
                        + (category == null || category.isEmpty() ? "" : " - " + category)
                        + (dueDateText == null || dueDateText.isEmpty() ? "" : " - vence " + dueDateText);
                if (!passesFiltersExpense(doc, payerEmail, category, concept, roomName)) continue;
                expenseRows.add(new WorkspaceRow(
                        doc.getId(),
                        "expense",
                        concept == null ? "Gasto" : concept,
                        subtitle,
                        amountValue == null ? "0.00 EUR" : df.format(amountValue) + " EUR",
                        doc
                ));
            }

            db.collection("payments").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(payments -> {
                for (DocumentSnapshot doc : payments.getDocuments()) {
                    String fromEmail = doc.getString("fromEmail");
                    String toEmail = doc.getString("toEmail");
                    String toLabel = displayNameForEmail(toEmail);
                    if (hasRoomContext() && !isRoomPayment(doc, fromEmail, toEmail)) continue;

                    Double amountValue = doc.getDouble("amount");
                    String concept = doc.getString("concept");
                    String roomName = doc.getString("roomName");
                    String category = doc.getString("category");
                    if (BILLING_FIXED.equals(currentBillingModel)
                            && (category == null || !CATEGORY_RENT.equalsIgnoreCase(category.trim()))) {
                        continue;
                    }
                    String status = doc.getString("status");
                    Date dueAt = doc.getDate("dueAt");
                    String dueDateText = doc.getString("dueDateText");
                    boolean confirmed = "confirmed".equals(status);
                    boolean overdue = !confirmed && dueAt != null && dueAt.getTime() < System.currentTimeMillis();
                    String statusLabel = confirmed ? "Confirmado" : (overdue ? "Vencido" : "Pendiente");
                    String subtitle = (toEmail == null ? "Pago registrado" : "A " + toLabel)
                            + (roomName == null || roomName.trim().isEmpty() ? "" : " - hab. " + roomName)
                            + " - " + statusLabel
                            + (dueDateText == null || dueDateText.isEmpty() ? "" : " - vence " + dueDateText);
                    if (!passesFiltersPayment(doc, category)) continue;
                    expenseRows.add(new WorkspaceRow(
                            doc.getId(),
                            "payment",
                            concept == null || concept.trim().isEmpty() ? "Pago enviado" : concept,
                            subtitle,
                            amountValue == null ? "0.00 EUR" : df.format(amountValue) + " EUR",
                            doc
                    ));
                }
                expensesAdapter.notifyDataSetChanged();
            });
        });
    }

    private boolean matchesExpenseWithCurrentRoom(DocumentSnapshot doc) {
        if (!hasRoomContext()) return true;
        String singleRoomId = doc.getString("roomId");
        if ("all".equalsIgnoreCase(singleRoomId)) return true;
        if (currentRoomId != null && currentRoomId.equals(singleRoomId)) return true;
        Object roomIdsRaw = doc.get("roomIds");
        if (roomIdsRaw instanceof List<?> ids) {
            for (Object id : ids) {
                if (id != null && id.toString().equals(currentRoomId)) return true;
            }
        }
        return false;
    }

    private String normalizeBillingModel(@Nullable String raw) {
        if (raw == null) return BILLING_VARIABLE;
        return BILLING_FIXED.equalsIgnoreCase(raw.trim()) ? BILLING_FIXED : BILLING_VARIABLE;
    }

    private String normalizeVariableSplitMode(@Nullable String raw) {
        if (raw == null) return VARIABLE_SPLIT_EQUAL;
        return VARIABLE_SPLIT_PERCENTAGE.equalsIgnoreCase(raw.trim())
                ? VARIABLE_SPLIT_PERCENTAGE
                : VARIABLE_SPLIT_EQUAL;
    }

    private void loadFinancialViews() {
        if (currentGroupId == null) return;
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(groupDoc -> {
            List<String> members = castEmails(groupDoc.get("memberEmails"));
            if (members.isEmpty()) {
                saldoRows.clear();
                saldosAdapter.notifyDataSetChanged();
                lastBalanceMovements.clear();
                renderQuickBalances(Collections.emptyMap());
                return;
            }
            String billingModel = normalizeBillingModel(groupDoc.getString("billingModel"));
            String variableSplitMode = normalizeVariableSplitMode(groupDoc.getString("variableSplitMode"));
            currentBillingModel = billingModel;
            currentVariableSplitMode = variableSplitMode;

            Map<String, Double> net = new HashMap<>();
            for (String member : members) {
                net.put(member, 0.0);
            }

            if (BILLING_FIXED.equals(billingModel)) {
                loadFixedRentFinancials(groupDoc, members, net);
            } else {
                loadVariableRentFinancials(members, net, VARIABLE_SPLIT_PERCENTAGE.equals(variableSplitMode));
            }
        });
    }

    private void loadVariableRentFinancials(List<String> members, Map<String, Double> net, boolean percentageMode) {
        if (!percentageMode) {
            computeExpenseImpacts(members, net, null);
            return;
        }

        loadCurrentGroupRooms(rooms -> {
            Map<String, Double> memberWeights = computeMemberWeightsFromRooms(members, rooms);
            computeExpenseImpacts(members, net, memberWeights);
        });
    }

    private void computeExpenseImpacts(List<String> members, Map<String, Double> net, @Nullable Map<String, Double> memberWeights) {
        var expenseQuery = db.collection("expenses").whereEqualTo("groupId", currentGroupId);
        expenseQuery.get().addOnSuccessListener(expenses -> {
            for (DocumentSnapshot doc : expenses.getDocuments()) {
                if (hasRoomContext() && !matchesExpenseWithCurrentRoom(doc)) continue;
                double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                String payerEmail = doc.getString("payerEmail");
                String custom = doc.getString("customSplit");

                if (custom == null || custom.isEmpty()) {
                    if (memberWeights == null) {
                        double part = amount / members.size();
                        for (String member : members) {
                            net.put(member, net.getOrDefault(member, 0.0) - part);
                        }
                    } else {
                        applyWeightedSplit(net, members, memberWeights, amount);
                    }
                } else {
                    applyCustomSplit(net, custom, amount);
                }

                if (payerEmail != null) {
                    String normalized = payerEmail.toLowerCase(Locale.ROOT);
                    if (net.containsKey(normalized)) {
                        net.put(normalized, net.getOrDefault(normalized, 0.0) + amount);
                    }
                }
            }
            applyPaymentsAndRenderFinancials(net);
        });
    }

    private void loadFixedRentFinancials(DocumentSnapshot groupDoc, List<String> members, Map<String, Double> net) {
        String ownerEmail = resolveOwnerEmail(groupDoc, castStrings(groupDoc.get("members")), members);
        loadCurrentGroupRooms(rooms -> {
            for (RoomOption room : rooms) {
                if (hasRoomContext() && !room.id.equals(currentRoomId)) continue;
                if (room.memberEmails.isEmpty()) continue;

                double roomCost = room.monthlyCost;
                double perResident = roomCost / room.memberEmails.size();
                for (String resident : room.memberEmails) {
                    if (net.containsKey(resident)) {
                        net.put(resident, net.getOrDefault(resident, 0.0) - perResident);
                    }
                }
                if (ownerEmail != null && !ownerEmail.trim().isEmpty() && net.containsKey(ownerEmail)) {
                    net.put(ownerEmail, net.getOrDefault(ownerEmail, 0.0) + roomCost);
                }
            }
            applyPaymentsAndRenderFinancials(net);
        });
    }

    private void applyCustomSplit(Map<String, Double> net, String custom, double amount) {
        String[] parts = custom.split(",");
        for (String partEntry : parts) {
            String[] kv = partEntry.trim().split(":");
            if (kv.length != 2) continue;
            String email = kv[0].trim().toLowerCase(Locale.ROOT);
            try {
                double percent = Double.parseDouble(kv[1].trim());
                if (net.containsKey(email)) {
                    net.put(email, net.getOrDefault(email, 0.0) - amount * (percent / 100.0));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void applyWeightedSplit(Map<String, Double> net, List<String> members, Map<String, Double> memberWeights, double amount) {
        double totalWeight = 0.0;
        for (String member : members) {
            totalWeight += memberWeights.getOrDefault(member, 1.0);
        }
        if (totalWeight <= 0.0) {
            double part = amount / members.size();
            for (String member : members) {
                net.put(member, net.getOrDefault(member, 0.0) - part);
            }
            return;
        }

        for (String member : members) {
            double weight = memberWeights.getOrDefault(member, 1.0);
            double share = amount * (weight / totalWeight);
            net.put(member, net.getOrDefault(member, 0.0) - share);
        }
    }

    private Map<String, Double> computeMemberWeightsFromRooms(List<String> members, List<RoomOption> rooms) {
        Map<String, Double> weights = new HashMap<>();
        for (String member : members) {
            weights.put(member, 1.0);
        }

        for (RoomOption room : rooms) {
            if (hasRoomContext() && !room.id.equals(currentRoomId)) continue;
            if (room.memberEmails.isEmpty()) continue;

            double roomWeight = room.capacity > 0 ? room.capacity : 1.0;
            double residentWeight = roomWeight / room.memberEmails.size();
            for (String resident : room.memberEmails) {
                if (!weights.containsKey(resident)) continue;
                weights.put(resident, weights.getOrDefault(resident, 1.0) + residentWeight);
            }
        }
        return weights;
    }

    private void applyPaymentsAndRenderFinancials(Map<String, Double> net) {
        db.collection("payments").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(payments -> {
            for (DocumentSnapshot payment : payments.getDocuments()) {
                String status = payment.getString("status");
                if (!"confirmed".equals(status)) continue;
                double amount = payment.getDouble("amount") == null ? 0.0 : payment.getDouble("amount");
                String from = payment.getString("fromEmail");
                String to = payment.getString("toEmail");
                if (from != null && net.containsKey(from)) net.put(from, net.getOrDefault(from, 0.0) + amount);
                if (to != null && net.containsKey(to)) net.put(to, net.getOrDefault(to, 0.0) - amount);
            }
            renderSaldos(net);
        });
    }
    private void renderSaldos(Map<String, Double> net) {
        DecimalFormat df = new DecimalFormat("0.00");
        saldoRows.clear();
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        double myNet = net.getOrDefault(myEmail, 0.0);
        saldoRows.add(new WorkspaceRow(
                "summary",
                "summary",
                myNet >= 0 ? "Te deben" : "Debes",
                currentGroupName,
                df.format(Math.abs(myNet)) + " EUR",
                null
        ));
        for (Map.Entry<String, Double> entry : net.entrySet()) {
            double value = entry.getValue();
            String memberLabel = displayNameForEmail(entry.getKey());
            saldoRows.add(new WorkspaceRow(
                    entry.getKey(),
                    "saldo",
                    memberLabel,
                    value >= 0 ? "Saldo a favor" : "Saldo pendiente",
                    df.format(value) + " EUR",
                    null
            ));
        }
        saldosAdapter.notifyDataSetChanged();
        lastBalanceMovements.clear();
        lastBalanceMovements.addAll(buildBalanceMovements(net));
        renderQuickBalances(net);
    }

    private List<BalanceMovement> buildBalanceMovements(Map<String, Double> net) {
        final double epsilon = 0.01;
        List<BalanceNode> creditors = new ArrayList<>();
        List<BalanceNode> debtors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : net.entrySet()) {
            String email = entry.getKey();
            double value = entry.getValue();
            if (value > epsilon) {
                creditors.add(new BalanceNode(email, value));
            } else if (value < -epsilon) {
                debtors.add(new BalanceNode(email, Math.abs(value)));
            }
        }

        List<BalanceMovement> movements = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            BalanceNode debtor = debtors.get(i);
            BalanceNode creditor = creditors.get(j);
            double amount = Math.min(debtor.amount, creditor.amount);
            if (amount > epsilon) {
                movements.add(new BalanceMovement(debtor.email, creditor.email, amount));
            }
            debtor.amount -= amount;
            creditor.amount -= amount;
            if (debtor.amount <= epsilon) i++;
            if (creditor.amount <= epsilon) j++;
        }
        return movements;
    }

    private void renderQuickBalances(Map<String, Double> net) {
        if (!isAdded() || quickBalancesContainer == null || quickBalancesCard == null) return;
        quickBalancesContainer.removeAllViews();

        String myEmailRaw = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String myEmail = myEmailRaw == null ? "" : myEmailRaw.toLowerCase(Locale.ROOT);
        boolean isOwner = "admin".equals(currentUserRole);

        Map<String, Double> incomingByPerson = new LinkedHashMap<>();
        Map<String, Double> outgoingByPerson = new LinkedHashMap<>();
        for (BalanceMovement movement : lastBalanceMovements) {
            incomingByPerson.put(movement.toEmail, incomingByPerson.getOrDefault(movement.toEmail, 0.0) + movement.amount);
            outgoingByPerson.put(movement.fromEmail, outgoingByPerson.getOrDefault(movement.fromEmail, 0.0) + movement.amount);
        }

        if (isOwner) {
            List<String> people = new ArrayList<>(net.keySet());
            Collections.sort(people);
            for (String email : people) {
                double teDeben = incomingByPerson.getOrDefault(email, 0.0);
                double debes = outgoingByPerson.getOrDefault(email, 0.0);
                if (teDeben <= 0.0 && debes <= 0.0) continue;
                addQuickBalanceChip(email, teDeben, debes, true);
            }
        } else {
            Map<String, double[]> byCounterparty = new LinkedHashMap<>();
            for (BalanceMovement movement : lastBalanceMovements) {
                if (myEmail.equals(movement.toEmail)) {
                    double[] values = byCounterparty.computeIfAbsent(movement.fromEmail, key -> new double[2]);
                    values[0] += movement.amount; // te deben
                } else if (myEmail.equals(movement.fromEmail)) {
                    double[] values = byCounterparty.computeIfAbsent(movement.toEmail, key -> new double[2]);
                    values[1] += movement.amount; // debes
                }
            }
            for (Map.Entry<String, double[]> entry : byCounterparty.entrySet()) {
                double teDeben = entry.getValue()[0];
                double debes = entry.getValue()[1];
                if (teDeben <= 0.0 && debes <= 0.0) continue;
                addQuickBalanceChip(entry.getKey(), teDeben, debes, false);
            }
        }

        updateQuickBalancesVisibility();
    }

    private void addQuickBalanceChip(String personEmail, double teDeben, double debes, boolean ownerView) {
        Button chip = new Button(requireContext());
        chip.setAllCaps(false);
        chip.setMinWidth(0);
        chip.setBackgroundResource(R.drawable.bg_tab_default);
        chip.setTextColor(requireContext().getColor(R.color.text_light));
        chip.setTextSize(11f);
        chip.setPadding(dp(12), dp(8), dp(12), dp(8));
        chip.setSingleLine(false);
        chip.setMaxLines(3);

        String personLabel = displayNameForEmail(personEmail);
        if (ownerView) {
            chip.setText(personLabel + "\nTe deben: " + formatCurrency(teDeben) + " | Debe: " + formatCurrency(debes));
        } else {
            chip.setText(personLabel + "\nTe deben: " + formatCurrency(teDeben) + " | Debes: " + formatCurrency(debes));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(6);
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> showQuickBalanceDetail(personEmail, teDeben, debes, ownerView));
        quickBalancesContainer.addView(chip);
    }

    private void showQuickBalanceDetail(String personEmail, double teDeben, double debes, boolean ownerView) {
        String personLabel = displayNameForEmail(personEmail);
        StringBuilder detail = new StringBuilder();
        if (ownerView) {
            detail.append("Resumen global de ").append(personLabel).append("\n\n")
                    .append("Te deben: ").append(formatCurrency(teDeben)).append("\n")
                    .append("Debe: ").append(formatCurrency(debes));
        } else {
            detail.append("Detalle con ").append(personLabel).append("\n\n")
                    .append("Te deben: ").append(formatCurrency(teDeben)).append("\n")
                    .append("Debes: ").append(formatCurrency(debes));
        }

        View content = DialogUtils.createMessageView(requireContext(), detail.toString());
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Debes / Te deben",
                ownerView ? "Vista global del propietario" : "Vista personal",
                content,
                null,
                "Cerrar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
    }

    private void updateQuickBalancesVisibility() {
        if (quickBalancesCard == null || quickBalancesContainer == null) return;
        boolean visibleTab = TAB_EXPENSES.equals(currentTab) || TAB_REMINDERS.equals(currentTab) || TAB_MANAGEMENT.equals(currentTab);
        boolean hasRows = quickBalancesContainer.getChildCount() > 0;
        quickBalancesCard.setVisibility(currentGroupId != null && visibleTab && hasRows ? View.VISIBLE : View.GONE);
    }

    private String formatCurrency(double value) {
        return new DecimalFormat("0.00").format(Math.max(0.0, value)) + " EUR";
    }

    private boolean hasRoomContext() {
        return currentRoomId != null && !currentRoomId.trim().isEmpty();
    }

    private void loadRoomContext(Runnable onDone) {
        if (!hasRoomContext()) {
            currentRoomMembers = new ArrayList<>();
            onDone.run();
            return;
        }
        db.collection("rooms_groups").document(currentRoomId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                SessionStore.clearCurrentRoom(requireContext());
                currentRoomId = null;
                currentRoomName = null;
                currentRoomMembers = new ArrayList<>();
                onDone.run();
                return;
            }
            String roomGroupId = doc.getString("groupId");
            if (roomGroupId == null || !roomGroupId.equals(currentGroupId)) {
                SessionStore.clearCurrentRoom(requireContext());
                currentRoomId = null;
                currentRoomName = null;
                currentRoomMembers = new ArrayList<>();
                onDone.run();
                return;
            }
            currentRoomName = doc.getString("name");
            currentRoomMembers = castEmails(doc.get("memberEmails"));
            onDone.run();
        }).addOnFailureListener(e -> {
            currentRoomMembers = new ArrayList<>();
            onDone.run();
        });
    }

    private boolean isRoomPayment(DocumentSnapshot doc, @Nullable String fromEmail, @Nullable String toEmail) {
        if (!hasRoomContext()) return true;
        String paymentRoomId = doc.getString("roomId");
        if (paymentRoomId != null && paymentRoomId.equals(currentRoomId)) {
            return true;
        }
        String from = fromEmail == null ? "" : fromEmail.toLowerCase(Locale.ROOT);
        String to = toEmail == null ? "" : toEmail.toLowerCase(Locale.ROOT);
        return currentRoomMembers.contains(from) && currentRoomMembers.contains(to);
    }

    private List<String> castEmails(Object raw) {
        List<String> emails = new ArrayList<>();
        if (raw instanceof List) {
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
        if (raw instanceof List) {
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
        if (memberIds.isEmpty()) {
            onDone.run();
            return;
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
                .addOnSuccessListener(results -> {
                    for (Task<DocumentSnapshot> task : tasks) {
                        if (!task.isSuccessful() || task.getResult() == null) continue;
                        DocumentSnapshot userDoc = task.getResult();
                        String email = userDoc.getString("email");
                        if (email == null || email.trim().isEmpty()) continue;
                        String normalizedEmail = email.toLowerCase(Locale.ROOT);
                        String displayName = userDoc.getString("displayName");
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = userDoc.getString("username");
                        }
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = normalizedEmail;
                        }
                        memberDisplayNamesByEmail.put(normalizedEmail, displayName.trim());
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private String displayNameForEmail(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) return "";
        String normalized = email.toLowerCase(Locale.ROOT);
        String name = memberDisplayNamesByEmail.get(normalized);
        return name == null || name.trim().isEmpty() ? normalized : name;
    }

    private String resolveOwnerEmail(DocumentSnapshot groupDoc, List<String> memberIds, List<String> memberEmails) {
        String ownerId = groupDoc.getString("ownerId");
        if (ownerId == null || ownerId.trim().isEmpty()) {
            return "";
        }
        int size = Math.min(memberIds.size(), memberEmails.size());
        for (int i = 0; i < size; i++) {
            if (ownerId.equals(memberIds.get(i))) {
                return memberEmails.get(i);
            }
        }
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (ownerId.equals(myUid)) {
            String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            return myEmail == null ? "" : myEmail.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private void showRowDetail(WorkspaceRow row) {
        if ("reminder".equals(row.type)) {
            if (row.snapshot == null) return;
            String ownerUid = row.snapshot.getString("ownerUid");
            String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            boolean canDelete = ownerUid != null && ownerUid.equals(myUid);
            ViewGroup content = DialogUtils.createVerticalActions(requireContext());
            Button detailBtn = DialogUtils.createActionButton(requireContext(), "Ver detalle", true);
            content.addView(detailBtn);
            Button deleteBtn = null;
            if (canDelete) {
                deleteBtn = DialogUtils.createActionButton(requireContext(), "Eliminar recordatorio", false);
                content.addView(deleteBtn);
            }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    row.title,
                    "Acciones de recordatorio",
                    content,
                    "Cerrar",
                    null
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            detailBtn.setOnClickListener(v ->
                    Toast.makeText(requireContext(), row.subtitle, Toast.LENGTH_LONG).show()
            );
            if (deleteBtn != null) {
                deleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    requestReminderDeletion(row);
                });
            }
            return;
        }

        if ("payment".equals(row.type)) {
            if (row.snapshot == null) return;
            String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
            String toEmail = row.snapshot.getString("toEmail");
            String status = row.snapshot.getString("status");
            boolean canConfirm = "pending".equals(status) && myEmail.equalsIgnoreCase(toEmail == null ? "" : toEmail);
            boolean canDelete = canManagePayment(row.snapshot);

            ViewGroup content = DialogUtils.createVerticalActions(requireContext());
            Button detailBtn = DialogUtils.createActionButton(requireContext(), "Ver detalle", true);
            content.addView(detailBtn);
            Button confirmBtn = null;
            Button rejectBtn = null;
            if (canConfirm) {
                confirmBtn = DialogUtils.createActionButton(requireContext(), "Confirmar", false);
                rejectBtn = DialogUtils.createActionButton(requireContext(), "Rechazar", false);
                content.addView(confirmBtn);
                content.addView(rejectBtn);
            }
            Button deleteBtn = null;
            if (canDelete) {
                deleteBtn = DialogUtils.createActionButton(requireContext(), "Eliminar pago", false);
                content.addView(deleteBtn);
            }

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    row.title,
                    "Acciones de pago",
                    content,
                    "Cerrar",
                    null
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            detailBtn.setOnClickListener(v ->
                    Toast.makeText(requireContext(), row.subtitle + " - " + row.amount, Toast.LENGTH_LONG).show()
            );
            if (confirmBtn != null) {
                Button finalConfirmBtn = confirmBtn;
                finalConfirmBtn.setOnClickListener(v -> {
                    updatePaymentStatus(row.id, "confirmed");
                    dialog.dismiss();
                });
            }
            if (rejectBtn != null) {
                Button finalRejectBtn = rejectBtn;
                finalRejectBtn.setOnClickListener(v -> {
                    updatePaymentStatus(row.id, "rejected");
                    dialog.dismiss();
                });
            }
            if (deleteBtn != null) {
                Button finalDeleteBtn = deleteBtn;
                finalDeleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    requestPaymentDeletion(row);
                });
            }
            return;
        }

        if (row.snapshot != null) {
            String payerId = row.snapshot.getString("payerId");
            if (!canManageExpense(payerId)) {
                View content = DialogUtils.createMessageView(requireContext(), row.subtitle + "\n" + row.amount);
                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        row.title,
                        "Solo lectura (sin permisos para editar/borrar).",
                        content,
                        null,
                        "Cerrar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
                return;
            }
        } else {
            View content = DialogUtils.createMessageView(requireContext(), row.subtitle + "\n" + row.amount);
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
            return;
        }

        View content = DialogUtils.createMessageView(requireContext(), row.subtitle + "\n" + row.amount);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Puedes revisar o modificar este gasto.",
                content,
                "Borrar",
                "Editar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> {
            requestExpenseDeletion(row);
            dialog.dismiss();
        });
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            editExpense(row);
        });
    }

    private void editExpense(WorkspaceRow row) {
        if (row.snapshot == null) return;
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                Toast.makeText(requireContext(), "No hay miembros para repartir", Toast.LENGTH_SHORT).show();
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
                ((EditText) form.findViewById(R.id.conceptEt)).setText(row.snapshot.getString("concept"));
                Double amountValue = row.snapshot.getDouble("amount");
                ((EditText) form.findViewById(R.id.amountEt)).setText(amountValue == null ? "" : String.valueOf(amountValue));
                setupTypeSpinner((Spinner) form.findViewById(R.id.categorySpinner), row.snapshot.getString("category"));
                setupPrioritySpinner((Spinner) form.findViewById(R.id.expensePrioritySpinner), row.snapshot.getString("priority"));
                setupDateField(form.findViewById(R.id.dueDateEt));
                Date dueAt = row.snapshot.getDate("dueAt");
                if (dueAt != null) {
                    ((EditText) form.findViewById(R.id.dueDateEt)).setText(DUE_DATE_FORMAT.format(dueAt));
                }
                pendingTicketUri = row.snapshot.getString("ticketUri");
                String currentSplit = row.snapshot.getString("customSplit");
                setupSplitUi(form, members, currentSplit);
                setupRoomSelector(
                        form,
                        members,
                        rooms,
                        castStrings(row.snapshot.get("roomIds")),
                        row.snapshot.getString("roomId"),
                        row.snapshot.getString("roomName"),
                        currentSplit
                );
                setupTicketControls(form);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Editar gasto",
                        "Actualiza los datos de este gasto.",
                        form,
                        "Cancelar",
                        "Guardar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
                shell.confirmBtn.setOnClickListener(v -> {
                    if (saveExpense(form, row.id, members, rooms)) {
                        dialog.dismiss();
                    }
                });
            });
        });
    }

    private void loadCurrentGroupMembers(MembersCallback callback) {
        if (currentGroupId == null) {
            callback.onLoaded(new ArrayList<>());
            return;
        }
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            callback.onLoaded(castEmails(doc.get("memberEmails")));
        }).addOnFailureListener(e -> callback.onLoaded(new ArrayList<>()));
    }

    private void loadCurrentGroupRooms(RoomsCallback callback) {
        if (currentGroupId == null) {
            callback.onLoaded(new ArrayList<>());
            return;
        }
        db.collection("rooms_groups")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(result -> {
                    List<RoomOption> rooms = new ArrayList<>();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        String roomName = doc.getString("name");
                        Long roomNumber = doc.getLong("roomNumber");
                        Long capacity = doc.getLong("capacity");
                        Double monthlyCost = doc.getDouble("monthlyCost");
                        String normalizedName = roomName == null || roomName.trim().isEmpty() ? "Habitación" : roomName;
                        String label = (roomNumber == null || roomNumber <= 0)
                                ? normalizedName
                                : "Hab. " + roomNumber + " - " + normalizedName;
                        rooms.add(new RoomOption(
                                doc.getId(),
                                label,
                                castEmails(doc.get("memberEmails")),
                                roomNumber == null ? 0 : roomNumber.intValue(),
                                capacity == null ? 0 : capacity.intValue(),
                                monthlyCost == null ? 0.0 : monthlyCost
                        ));
                    }
                    rooms.sort((a, b) -> Integer.compare(a.roomNumber <= 0 ? Integer.MAX_VALUE : a.roomNumber, b.roomNumber <= 0 ? Integer.MAX_VALUE : b.roomNumber));
                    callback.onLoaded(rooms);
                })
                .addOnFailureListener(e -> callback.onLoaded(new ArrayList<>()));
    }

    private void setupRoomSelector(
            View form,
            List<String> allMembers,
            List<RoomOption> rooms,
            @Nullable List<String> selectedRoomIds,
            @Nullable String selectedRoomId,
            @Nullable String selectedRoomName,
            @Nullable String currentCustomSplit
    ) {
        boolean keepInitialSplit = currentCustomSplit != null && !currentCustomSplit.trim().isEmpty();
        form.setTag(R.id.roomMembersHintTv, keepInitialSplit);

        List<String> initialKeys = new ArrayList<>();
        if (selectedRoomIds != null && !selectedRoomIds.isEmpty()) {
            initialKeys.addAll(selectedRoomIds);
        } else if (selectedRoomId != null && !selectedRoomId.trim().isEmpty()) {
            if ("all".equalsIgnoreCase(selectedRoomId)) {
                initialKeys.add(ROOM_ALL_LABEL);
            } else {
                initialKeys.add(selectedRoomId);
            }
        } else if (selectedRoomName != null && selectedRoomName.equalsIgnoreCase(ROOM_ALL_LABEL)) {
            initialKeys.add(ROOM_ALL_LABEL);
        } else {
            initialKeys.add(rooms.isEmpty() ? ROOM_ALL_LABEL : rooms.get(0).id);
        }
        rebindRoomSelectors(form, allMembers, rooms, initialKeys);
    }

    private void rebindRoomSelectors(View form, List<String> allMembers, List<RoomOption> rooms, @Nullable List<String> seedKeys) {
        if (isRebindingRoomSelectors) return;
        isRebindingRoomSelectors = true;
        LinearLayout container = form.findViewById(R.id.roomSelectorsContainer);
        Button addRoomBtn = form.findViewById(R.id.addRoomSelectionBtn);
        Button removeRoomBtn = form.findViewById(R.id.removeRoomSelectionBtn);
        TextView roomMembersHintTv = form.findViewById(R.id.roomMembersHintTv);

        List<String> selectedKeys = seedKeys == null ? collectRoomSelectionKeys(container) : new ArrayList<>(seedKeys);
        if (selectedKeys.isEmpty()) selectedKeys.add(rooms.isEmpty() ? ROOM_ALL_LABEL : rooms.get(0).id);
        if (selectedKeys.contains(ROOM_ALL_LABEL)) {
            selectedKeys.clear();
            selectedKeys.add(ROOM_ALL_LABEL);
        }
        selectedKeys = deduplicateRoomKeys(selectedKeys);

        container.removeAllViews();
        for (int i = 0; i < selectedKeys.size(); i++) {
            boolean allowAllOption = i == 0;
            String selectedKey = selectedKeys.get(i);
            Spinner spinner = buildRoomSelectorSpinner(rooms, selectedKeys, i, allowAllOption, selectedKey);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    rebindRoomSelectors(form, allMembers, rooms, null);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            container.addView(spinner);
        }

        List<String> refreshedKeys = collectRoomSelectionKeys(container);
        if (refreshedKeys.contains(ROOM_ALL_LABEL)) {
            addRoomBtn.setVisibility(View.GONE);
            removeRoomBtn.setVisibility(View.GONE);
        } else {
            int remaining = rooms.size() - refreshedKeys.size();
            addRoomBtn.setVisibility(remaining > 0 ? View.VISIBLE : View.GONE);
            removeRoomBtn.setVisibility(refreshedKeys.size() > 1 ? View.VISIBLE : View.GONE);
        }

        addRoomBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectRoomSelectionKeys(container);
            if (currentKeys.contains(ROOM_ALL_LABEL)) return;
            currentKeys.add(firstAvailableRoomKey(rooms, currentKeys));
            rebindRoomSelectors(form, allMembers, rooms, currentKeys);
        });
        removeRoomBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectRoomSelectionKeys(container);
            if (currentKeys.size() > 1) {
                currentKeys.remove(currentKeys.size() - 1);
                rebindRoomSelectors(form, allMembers, rooms, currentKeys);
            }
        });

        List<RoomOption> selectedRooms = resolveSelectedRoomOptions(form, rooms);
        List<String> mergedMembers = mergeSelectedRoomMembers(selectedRooms, allMembers);
        if (selectedRooms.size() == rooms.size()) {
            roomMembersHintTv.setText("Todas las habitaciones seleccionadas.");
        } else if (mergedMembers.isEmpty()) {
            roomMembersHintTv.setText("Habitación sin residentes asignados.");
        } else {
            roomMembersHintTv.setText("Residentes: " + String.join(", ", mergedMembers));
        }

        Object keepInitialSplitTag = form.getTag(R.id.roomMembersHintTv);
        boolean keepInitialSplit = keepInitialSplitTag instanceof Boolean && (Boolean) keepInitialSplitTag;
        if (keepInitialSplit) {
            form.setTag(R.id.roomMembersHintTv, false);
        } else {
            setSplitRowsForMembers(form, allMembers, mergedMembers.isEmpty() ? null : mergedMembers, true);
        }
        isRebindingRoomSelectors = false;
    }

    private Spinner buildRoomSelectorSpinner(
            List<RoomOption> rooms,
            List<String> selectedKeys,
            int spinnerIndex,
            boolean allowAllOption,
            String selectedKey
    ) {
        Spinner spinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        spinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        spinner.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        if (spinnerIndex > 0) params.topMargin = dp(8);
        spinner.setLayoutParams(params);

        List<String> optionKeys = new ArrayList<>();
        List<String> optionLabels = new ArrayList<>();
        if (allowAllOption) {
            optionKeys.add(ROOM_ALL_LABEL);
            optionLabels.add(ROOM_ALL_LABEL);
        }

        for (RoomOption room : rooms) {
            boolean selectedElsewhere = false;
            for (int i = 0; i < selectedKeys.size(); i++) {
                if (i == spinnerIndex) continue;
                if (room.id.equals(selectedKeys.get(i))) {
                    selectedElsewhere = true;
                    break;
                }
            }
            if (!selectedElsewhere || room.id.equals(selectedKey)) {
                optionKeys.add(room.id);
                optionLabels.add(room.name);
            }
        }

        spinner.setTag(optionKeys);
        spinner.setAdapter(buildLightSpinnerAdapter(optionLabels.toArray(new String[0])));
        int selectedIndex = optionKeys.indexOf(selectedKey);
        spinner.setSelection(selectedIndex >= 0 ? selectedIndex : 0);
        return spinner;
    }

    private List<String> collectRoomSelectionKeys(LinearLayout container) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof Spinner spinner)) continue;
            @SuppressWarnings("unchecked")
            List<String> optionKeys = (List<String>) spinner.getTag();
            int selectedIndex = spinner.getSelectedItemPosition();
            if (optionKeys == null || selectedIndex < 0 || selectedIndex >= optionKeys.size()) continue;
            keys.add(optionKeys.get(selectedIndex));
        }
        return keys;
    }

    private List<String> deduplicateStringKeys(List<String> keys) {
        List<String> unique = new ArrayList<>();
        for (String key : keys) {
            if (!unique.contains(key)) unique.add(key);
        }
        return unique;
    }

    private List<String> deduplicateRoomKeys(List<String> keys) {
        return deduplicateStringKeys(keys);
    }

    private String firstAvailableRoomKey(List<RoomOption> rooms, List<String> selectedKeys) {
        for (RoomOption room : rooms) {
            if (!selectedKeys.contains(room.id)) return room.id;
        }
        return rooms.isEmpty() ? ROOM_ALL_LABEL : rooms.get(0).id;
    }

    private String firstAvailableMemberKey(List<String> members, List<String> selectedKeys) {
        for (String member : members) {
            if (!selectedKeys.contains(member)) return member;
        }
        return members.isEmpty() ? "" : members.get(0);
    }
    private int findRoomIndexById(List<RoomOption> rooms, @Nullable String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) return -1;
        for (int i = 0; i < rooms.size(); i++) {
            if (roomId.equals(rooms.get(i).id)) return i;
        }
        return -1;
    }

    private List<RoomOption> resolveSelectedRoomOptions(View form, List<RoomOption> loadedRooms) {
        LinearLayout container = form.findViewById(R.id.roomSelectorsContainer);
        List<String> keys = collectRoomSelectionKeys(container);
        if (keys.contains(ROOM_ALL_LABEL)) {
            return new ArrayList<>(loadedRooms);
        }
        List<RoomOption> selected = new ArrayList<>();
        for (String key : keys) {
            for (RoomOption room : loadedRooms) {
                if (room.id.equals(key)) {
                    selected.add(room);
                    break;
                }
            }
        }
        return selected;
    }

    private List<String> mergeSelectedRoomMembers(List<RoomOption> selectedRooms, List<String> allMembers) {
        List<String> merged = new ArrayList<>();
        for (RoomOption room : selectedRooms) {
            for (String email : room.memberEmails) {
                if (allMembers.contains(email) && !merged.contains(email)) {
                    merged.add(email);
                }
            }
        }
        return merged;
    }

    private List<String> sanitizeRoomMembers(List<String> roomMembers, List<String> allMembers) {
        List<String> sanitized = new ArrayList<>();
        for (String member : roomMembers) {
            if (allMembers.contains(member)) {
                sanitized.add(member);
            }
        }
        return sanitized;
    }

    private void setSplitRowsForMembers(View form, List<String> allMembers, @Nullable List<String> preferredMembers, boolean equitative) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        Button addSplitRowBtn = form.findViewById(R.id.addSplitRowBtn);
        Button removeSplitRowBtn = form.findViewById(R.id.removeSplitRowBtn);

        List<String> targetMembers;
        if (preferredMembers != null && !preferredMembers.isEmpty()) {
            targetMembers = preferredMembers;
        } else {
            targetMembers = new ArrayList<>();
            if (!allMembers.isEmpty()) {
                targetMembers.add(allMembers.get(0));
            }
        }
        if (targetMembers.isEmpty()) return;
        if (equitative && targetMembers.size() < 2) {
            equitative = false;
        }

        splitRowsContainer.removeAllViews();
        for (String member : targetMembers) {
            addSplitRow(splitRowsContainer, allMembers, member, null);
        }

        splitModeSpinner.setSelection(equitative ? 1 : 0);
        addSplitRowBtn.setVisibility(equitative ? View.GONE : View.VISIBLE);
        removeSplitRowBtn.setVisibility(!equitative && splitRowsContainer.getChildCount() > 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            row.findViewById(R.id.memberAmountEt).setVisibility(equitative ? View.GONE : View.VISIBLE);
        }
        updateSplitButtons(splitRowsContainer, removeSplitRowBtn);
    }

    private void setupSplitUi(View form, List<String> members, @Nullable String currentCustomSplit) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        Button addSplitRowBtn = form.findViewById(R.id.addSplitRowBtn);
        Button removeSplitRowBtn = form.findViewById(R.id.removeSplitRowBtn);

        String[] modes = new String[]{"Reparto personalizado", "Reparto equitativo"};
        ArrayAdapter<String> modeAdapter = buildLightSpinnerAdapter(modes);
        splitModeSpinner.setAdapter(modeAdapter);

        addSplitRow(splitRowsContainer, members, null, null);
        updateSplitButtons(splitRowsContainer, removeSplitRowBtn);

        addSplitRowBtn.setOnClickListener(v -> {
            addSplitRow(splitRowsContainer, members, null, null);
            updateSplitButtons(splitRowsContainer, removeSplitRowBtn);
        });
        removeSplitRowBtn.setOnClickListener(v -> {
            int count = splitRowsContainer.getChildCount();
            if (count > 1) {
                splitRowsContainer.removeViewAt(count - 1);
            }
            updateSplitButtons(splitRowsContainer, removeSplitRowBtn);
        });

        splitModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isEquitative = position == 1;
                addSplitRowBtn.setVisibility(isEquitative ? View.GONE : View.VISIBLE);
                removeSplitRowBtn.setVisibility(!isEquitative && splitRowsContainer.getChildCount() > 1 ? View.VISIBLE : View.GONE);
                for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
                    View row = splitRowsContainer.getChildAt(i);
                    row.findViewById(R.id.memberAmountEt).setVisibility(isEquitative ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (currentCustomSplit != null && !currentCustomSplit.trim().isEmpty()) {
            splitRowsContainer.removeAllViews();
            String[] entries = currentCustomSplit.split(",");
            for (String entry : entries) {
                String[] kv = entry.trim().split(":");
                if (kv.length != 2) continue;
                String email = kv[0].trim().toLowerCase(Locale.ROOT);
                String percentText = kv[1].trim();
                addSplitRow(splitRowsContainer, members, email, percentText);
            }
            if (splitRowsContainer.getChildCount() == 0) {
                addSplitRow(splitRowsContainer, members, null, null);
            }
            splitModeSpinner.setSelection(0);
            updateSplitButtons(splitRowsContainer, removeSplitRowBtn);
        } else {
            // Default UX for new expense: custom split with one editable person row.
            splitModeSpinner.setSelection(0);
            if (splitRowsContainer.getChildCount() > 1) {
                while (splitRowsContainer.getChildCount() > 1) {
                    splitRowsContainer.removeViewAt(splitRowsContainer.getChildCount() - 1);
                }
            }
            View firstRow = splitRowsContainer.getChildAt(0);
            if (firstRow != null) {
                firstRow.findViewById(R.id.memberAmountEt).setVisibility(View.VISIBLE);
            }
            addSplitRowBtn.setVisibility(View.VISIBLE);
            updateSplitButtons(splitRowsContainer, removeSplitRowBtn);
        }
    }

    private void addSplitRow(LinearLayout container, List<String> members, @Nullable String selectedMember, @Nullable String percentText) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_split_row, container, false);
        Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
        EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);

        ArrayAdapter<String> memberAdapter = buildLightSpinnerAdapter(members.toArray(new String[0]));
        memberSpinner.setAdapter(memberAdapter);

        if (selectedMember != null) {
            int idx = members.indexOf(selectedMember);
            if (idx >= 0) memberSpinner.setSelection(idx);
        }
        if (percentText != null) {
            memberAmountEt.setText(percentText);
        }
        container.addView(row);
    }

    private void updateSplitButtons(LinearLayout splitRowsContainer, Button removeSplitRowBtn) {
        removeSplitRowBtn.setVisibility(splitRowsContainer.getChildCount() > 1 ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private String buildCustomSplitFromUi(View form, double totalAmount, List<String> members) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        boolean equitative = splitModeSpinner.getSelectedItemPosition() == 1;

        Map<String, Double> splitsByEmail = new LinkedHashMap<>();
        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
            EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
            String email = memberSpinner.getSelectedItem().toString().toLowerCase(Locale.ROOT);

            if (equitative) {
                splitsByEmail.put(email, 0.0);
            } else {
                String amountText = memberAmountEt.getText().toString().trim();
                if (amountText.isEmpty()) {
                    Toast.makeText(requireContext(), "Completa importes en todas las lineas", Toast.LENGTH_SHORT).show();
                    return null;
                }
                double partAmount;
                try {
                    partAmount = Double.parseDouble(amountText);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Hay importes de reparto no válidos", Toast.LENGTH_SHORT).show();
                    return null;
                }
                if (partAmount < 0) {
                    Toast.makeText(requireContext(), "Ningun reparto puede ser negativo", Toast.LENGTH_SHORT).show();
                    return null;
                }
                splitsByEmail.put(email, splitsByEmail.getOrDefault(email, 0.0) + partAmount);
            }
        }

        if (equitative) {
            if (splitsByEmail.size() < 2) {
                Toast.makeText(requireContext(), "El reparto equitativo necesita minimo 2 personas", Toast.LENGTH_SHORT).show();
                return null;
            }
            double percent = 100.0 / splitsByEmail.size();
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (String email : splitsByEmail.keySet()) {
                if (i++ > 0) builder.append(",");
                builder.append(email).append(":").append(percent);
            }
            return builder.toString();
        }

        if (splitsByEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Añade al menos una línea de reparto", Toast.LENGTH_SHORT).show();
            return null;
        }

        double sum = 0.0;
        for (double value : splitsByEmail.values()) sum += value;
        if (sum <= 0.0) {
            Toast.makeText(requireContext(), "El reparto total debe ser mayor que 0", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (Math.abs(sum - totalAmount) > 0.01) {
            Toast.makeText(requireContext(), "La suma del reparto debe coincidir con el importe", Toast.LENGTH_SHORT).show();
            return null;
        }

        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Double> entry : splitsByEmail.entrySet()) {
            double percent = (entry.getValue() * 100.0) / totalAmount;
            if (i++ > 0) builder.append(",");
            builder.append(entry.getKey()).append(":").append(percent);
        }
        return builder.toString();
    }

    private void requestExpenseDeletion(WorkspaceRow row) {
        showDeleteConfirmation(
                "Eliminar gasto",
                "Se eliminará el gasto y sus vencimientos asociados.",
                () -> deleteExpense(row)
        );
    }

    private void requestPaymentDeletion(WorkspaceRow row) {
        showDeleteConfirmation(
                "Eliminar pago",
                "Se eliminará el pago y sus recordatorios asociados.",
                () -> deletePayment(row)
        );
    }

    private void requestReminderDeletion(WorkspaceRow row) {
        showDeleteConfirmation(
                "Eliminar recordatorio",
                "Se eliminará el recordatorio seleccionado.",
                () -> deleteReminder(row)
        );
    }

    private void deleteExpense(WorkspaceRow row) {
        if (row.snapshot != null && !canManageExpense(row.snapshot.getString("payerId"))) {
            Toast.makeText(requireContext(), "No tienes permisos para borrar este gasto", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("expenses").document(row.id).delete().addOnSuccessListener(v -> {
            deleteDeadlinesBySource("expense", row.id);
            if (row.snapshot != null) {
                double amount = row.snapshot.getDouble("amount") == null ? 0.0 : row.snapshot.getDouble("amount");
                logActivity("expense_deleted", row.snapshot.getString("concept"), amount, row.snapshot.getString("category"));
            }
            loadExpenses();
            loadFinancialViews();
        });
    }

    private void deletePayment(WorkspaceRow row) {
        if (row.snapshot == null || !canManagePayment(row.snapshot)) {
            Toast.makeText(requireContext(), "No tienes permisos para borrar este pago", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("payments").document(row.id).delete().addOnSuccessListener(v -> {
            deleteDeadlinesBySource("payment", row.id);
            double amount = row.snapshot.getDouble("amount") == null ? 0.0 : row.snapshot.getDouble("amount");
            logActivity("payment_deleted", row.snapshot.getString("concept"), amount, row.snapshot.getString("category"));
            loadExpenses();
            loadFinancialViews();
        });
    }

    private void deleteReminder(WorkspaceRow row) {
        if (row.snapshot == null) return;
        String ownerUid = row.snapshot.getString("ownerUid");
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (ownerUid == null || !ownerUid.equals(myUid)) {
            Toast.makeText(requireContext(), "Solo quien lo creó puede eliminarlo", Toast.LENGTH_SHORT).show();
            return;
        }
        Long reminderCode = row.snapshot.getLong("reminderCode");
        if (reminderCode != null) {
            ReminderScheduler.cancel(requireContext(), reminderCode.intValue());
        }
        db.collection("reminders").document(row.id).delete().addOnSuccessListener(v -> {
            logActivity("reminder_deleted", row.title, 0.0, "reminder");
            loadReminders();
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "No se pudo eliminar el recordatorio", Toast.LENGTH_SHORT).show()
        );
    }

    private void showFiltersDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filters, null, false);
        Spinner modeSpinner = form.findViewById(R.id.filterModeSpinner);
        View categoryRow = form.findViewById(R.id.filterCategoryRow);
        Spinner categorySpinner = form.findViewById(R.id.filterCategorySpinner);
        View personRow = form.findViewById(R.id.filterPersonRow);
        Spinner personSpinner = form.findViewById(R.id.filterPersonSpinner);
        View dateRow = form.findViewById(R.id.filterDateRow);
        EditText dateEt = form.findViewById(R.id.filterDateEt);

        String[] modeLabels = new String[]{"Categoría", "Persona", "Fecha"};
        modeSpinner.setAdapter(buildLightSpinnerAdapter(modeLabels));

        List<String> categoryValues = new ArrayList<>();
        List<String> categoryLabels = new ArrayList<>();
        categoryValues.add("");
        categoryLabels.add("Selecciona categoría");
        if (BILLING_FIXED.equals(currentBillingModel)) {
            categoryValues.add(CATEGORY_RENT);
            categoryLabels.add(capitalizeTypeLabel(CATEGORY_RENT));
        } else {
            for (String type : SPENDING_TYPES) {
                categoryValues.add(type);
                categoryLabels.add(capitalizeTypeLabel(type));
            }
        }
        categorySpinner.setAdapter(buildLightSpinnerAdapter(categoryLabels.toArray(new String[0])));

        List<String> personValues = new ArrayList<>();
        List<String> personLabels = new ArrayList<>();
        personValues.add("");
        personLabels.add("Selecciona persona");
        for (String memberEmail : currentGroupMemberEmails) {
            if (memberEmail == null || memberEmail.trim().isEmpty()) continue;
            String normalized = memberEmail.trim().toLowerCase(Locale.ROOT);
            String displayName = displayNameForEmail(normalized);
            if (displayName.equalsIgnoreCase(normalized)) {
                personLabels.add(normalized);
            } else {
                personLabels.add(displayName + " (" + normalized + ")");
            }
            personValues.add(normalized);
        }
        personSpinner.setAdapter(buildLightSpinnerAdapter(personLabels.toArray(new String[0])));

        setupDateField(dateEt);
        if (FILTER_MODE_DATE.equals(resolveActiveFilterMode()) && filterDateIso != null) {
            dateEt.setText(filterDateIso);
        }

        String activeMode = resolveActiveFilterMode();
        modeSpinner.setSelection(filterModeToIndex(activeMode));
        selectSpinnerValue(categorySpinner, categoryValues, filterCategory);
        selectSpinnerValue(personSpinner, personValues, filterPersonEmail);
        updateFilterModeRows(activeMode, categoryRow, personRow, dateRow);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mode = filterModeFromIndex(position);
                updateFilterModeRows(mode, categoryRow, personRow, dateRow);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Filtros",
                "Aplica un solo filtro: categoría, persona o fecha.",
                form,
                "Limpiar",
                "Aplicar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> {
            filterCategory = null;
            filterPersonEmail = null;
            filterFromMs = null;
            filterToMs = null;
            filterDateIso = null;
            loadExpenses();
            dialog.dismiss();
        });
        shell.confirmBtn.setOnClickListener(v -> {
            String selectedMode = filterModeFromIndex(modeSpinner.getSelectedItemPosition());
            filterCategory = null;
            filterPersonEmail = null;
            filterFromMs = null;
            filterToMs = null;
            filterDateIso = null;

            if (FILTER_MODE_CATEGORY.equals(selectedMode)) {
                int selectedIndex = categorySpinner.getSelectedItemPosition();
                String selectedCategory = selectedIndex >= 0 && selectedIndex < categoryValues.size()
                        ? categoryValues.get(selectedIndex)
                        : "";
                if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show();
                    return;
                }
                filterCategory = selectedCategory.trim().toLowerCase(Locale.ROOT);
            } else if (FILTER_MODE_PERSON.equals(selectedMode)) {
                int selectedIndex = personSpinner.getSelectedItemPosition();
                String selectedPerson = selectedIndex >= 0 && selectedIndex < personValues.size()
                        ? personValues.get(selectedIndex)
                        : "";
                if (selectedPerson == null || selectedPerson.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona una persona", Toast.LENGTH_SHORT).show();
                    return;
                }
                filterPersonEmail = selectedPerson.trim().toLowerCase(Locale.ROOT);
            } else {
                String dateText = dateEt.getText().toString().trim();
                if (dateText.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Date selectedDate = DUE_DATE_FORMAT.parse(dateText);
                    if (selectedDate == null) {
                        Toast.makeText(requireContext(), "Formato de fecha no válido (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Calendar startDay = Calendar.getInstance();
                    startDay.setTime(selectedDate);
                    startDay.set(Calendar.HOUR_OF_DAY, 0);
                    startDay.set(Calendar.MINUTE, 0);
                    startDay.set(Calendar.SECOND, 0);
                    startDay.set(Calendar.MILLISECOND, 0);

                    Calendar endDay = Calendar.getInstance();
                    endDay.setTime(selectedDate);
                    endDay.set(Calendar.HOUR_OF_DAY, 23);
                    endDay.set(Calendar.MINUTE, 59);
                    endDay.set(Calendar.SECOND, 59);
                    endDay.set(Calendar.MILLISECOND, 999);

                    filterFromMs = startDay.getTimeInMillis();
                    filterToMs = endDay.getTimeInMillis();
                    filterDateIso = dateText;
                } catch (ParseException e) {
                    Toast.makeText(requireContext(), "Formato de fecha no válido (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            loadExpenses();
            dialog.dismiss();
        });
    }

    private String resolveActiveFilterMode() {
        if (filterCategory != null && !filterCategory.trim().isEmpty()) return FILTER_MODE_CATEGORY;
        if (filterPersonEmail != null && !filterPersonEmail.trim().isEmpty()) return FILTER_MODE_PERSON;
        if (filterFromMs != null || filterToMs != null) return FILTER_MODE_DATE;
        return FILTER_MODE_CATEGORY;
    }

    private int filterModeToIndex(String mode) {
        if (FILTER_MODE_PERSON.equals(mode)) return 1;
        if (FILTER_MODE_DATE.equals(mode)) return 2;
        return 0;
    }

    private String filterModeFromIndex(int index) {
        if (index == 1) return FILTER_MODE_PERSON;
        if (index == 2) return FILTER_MODE_DATE;
        return FILTER_MODE_CATEGORY;
    }

    private void updateFilterModeRows(String mode, View categoryRow, View personRow, View dateRow) {
        categoryRow.setVisibility(FILTER_MODE_CATEGORY.equals(mode) ? View.VISIBLE : View.GONE);
        personRow.setVisibility(FILTER_MODE_PERSON.equals(mode) ? View.VISIBLE : View.GONE);
        dateRow.setVisibility(FILTER_MODE_DATE.equals(mode) ? View.VISIBLE : View.GONE);
    }

    private void selectSpinnerValue(Spinner spinner, List<String> values, @Nullable String selectedValue) {
        if (selectedValue == null || selectedValue.trim().isEmpty()) {
            spinner.setSelection(0);
            return;
        }
        String normalized = selectedValue.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < values.size(); i++) {
            String option = values.get(i);
            if (option != null && option.trim().equalsIgnoreCase(normalized)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private boolean passesFiltersExpense(
            DocumentSnapshot doc,
            @Nullable String payerEmail,
            @Nullable String category,
            @Nullable String concept,
            @Nullable String roomName
    ) {
        if (filterPersonEmail != null && (payerEmail == null || !payerEmail.equalsIgnoreCase(filterPersonEmail))) {
            return false;
        }
        if (filterCategory != null && (category == null || !category.toLowerCase(Locale.ROOT).contains(filterCategory))) {
            return false;
        }
        if (filterSearchQuery != null) {
            String combined = (concept == null ? "" : concept) + " "
                    + (payerEmail == null ? "" : payerEmail) + " "
                    + (category == null ? "" : category) + " "
                    + (roomName == null ? "" : roomName);
            if (!combined.toLowerCase(Locale.ROOT).contains(filterSearchQuery)) {
                return false;
            }
        }
        return passesDateFilter(doc);
    }

    private boolean passesFiltersPayment(DocumentSnapshot doc, @Nullable String category) {
        if (filterPersonEmail != null) {
            String from = doc.getString("fromEmail");
            String to = doc.getString("toEmail");
            boolean match = (from != null && from.equalsIgnoreCase(filterPersonEmail))
                    || (to != null && to.equalsIgnoreCase(filterPersonEmail));
            if (!match) return false;
        }
        if (filterCategory != null && (category == null || !category.toLowerCase(Locale.ROOT).contains(filterCategory))) {
            return false;
        }
        if (filterSearchQuery != null) {
            String concept = doc.getString("concept");
            String from = doc.getString("fromEmail");
            String to = doc.getString("toEmail");
            String roomName = doc.getString("roomName");
            String combined = (concept == null ? "" : concept) + " "
                    + (from == null ? "" : from) + " "
                    + (to == null ? "" : to) + " "
                    + (roomName == null ? "" : roomName) + " "
                    + (category == null ? "" : category);
            if (!combined.toLowerCase(Locale.ROOT).contains(filterSearchQuery)) {
                return false;
            }
        }
        return passesDateFilter(doc);
    }

    private boolean passesDateFilter(DocumentSnapshot doc) {
        Date createdAt = doc.getDate("createdAt");
        if (createdAt == null) return true;
        long time = createdAt.getTime();
        if (filterFromMs != null && time < filterFromMs) return false;
        return filterToMs == null || time <= filterToMs;
    }

    private void updatePaymentStatus(String paymentId, String status) {
        db.collection("payments").document(paymentId).update("status", status).addOnSuccessListener(v -> {
            logActivity("payment_" + status, "Pago " + status, 0.0, "payment");
            updateDeadlineStatusBySource("payment", paymentId, status);
            loadExpenses();
            loadFinancialViews();
        });
    }

    private boolean canManageExpense(@Nullable String payerId) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if ("admin".equals(currentUserRole)) return true;
        return payerId != null && payerId.equals(myUid);
    }

    private boolean canManagePayment(@NonNull DocumentSnapshot paymentDoc) {
        if ("admin".equals(currentUserRole)) return true;
        String fromEmail = paymentDoc.getString("fromEmail");
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        return fromEmail != null && myEmail != null && fromEmail.equalsIgnoreCase(myEmail);
    }

    private boolean canManageRooms() {
        return "admin".equals(currentUserRole);
    }

    private void showDeleteConfirmation(String title, String detail, Runnable onConfirmed) {
        View content = DialogUtils.createMessageView(requireContext(), detail);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                title,
                "Confirmar eliminación",
                content,
                "Cancelar",
                "Eliminar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirmed.run();
        });
    }

    private String resolveCurrentUserRole(DocumentSnapshot groupDoc) {
        if (groupDoc == null) return "member";
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String ownerId = groupDoc.getString("ownerId");
        if (myUid.equals(ownerId)) return "admin";
        Object rolesRaw = groupDoc.get("roles");
        if (rolesRaw instanceof Map<?, ?> roles) {
            Object role = roles.get(myUid);
            if (role != null) return role.toString();
        }
        return "member";
    }

    private void logActivity(String action, String concept, double amount, @Nullable String category) {
        if (currentGroupId == null) return;
        Map<String, Object> log = new HashMap<>();
        log.put("groupId", currentGroupId);
        log.put("action", action);
        log.put("concept", concept == null ? "" : concept);
        log.put("amount", amount);
        log.put("category", category == null ? "" : category);
        log.put("actorUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        log.put("actorEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT));
        log.put("createdAt", FieldValue.serverTimestamp());
        db.collection("activity_logs").add(log);
    }

    @Nullable
    private Date parseDueDateOrNull(String value) {
        try {
            Date date = DUE_DATE_FORMAT.parse(value);
            if (date == null) return null;
            Calendar selected = Calendar.getInstance();
            selected.setTime(date);
            selected.set(Calendar.HOUR_OF_DAY, 0);
            selected.set(Calendar.MINUTE, 0);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            if (selected.before(today)) return null;
            Calendar endDay = Calendar.getInstance();
            endDay.setTime(date);
            endDay.set(Calendar.HOUR_OF_DAY, 20);
            endDay.set(Calendar.MINUTE, 0);
            endDay.set(Calendar.SECOND, 0);
            endDay.set(Calendar.MILLISECOND, 0);
            return endDay.getTime();
        } catch (ParseException e) {
            return null;
        }
    }

    private Map<String, Double> parseCustomSplitPercentages(String customSplit) {
        Map<String, Double> split = new LinkedHashMap<>();
        if (customSplit == null || customSplit.trim().isEmpty()) return split;
        String[] entries = customSplit.split(",");
        for (String entry : entries) {
            String[] kv = entry.trim().split(":");
            if (kv.length != 2) continue;
            try {
                split.put(kv[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return split;
    }

    private void syncExpenseDeadlines(String expenseId, String concept, double amount, String customSplit, Date dueAt, String priority, String payerEmail) {
        if (currentGroupId == null) return;
        db.collection("payment_deadlines")
                .whereEqualTo("sourceType", "expense")
                .whereEqualTo("sourceId", expenseId)
                .get()
                .addOnSuccessListener(existing -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : existing.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    Map<String, Double> split = parseCustomSplitPercentages(customSplit);
                    for (Map.Entry<String, Double> entry : split.entrySet()) {
                        String debtor = entry.getKey();
                        if (debtor.equalsIgnoreCase(payerEmail)) continue;
                        double partAmount = amount * (entry.getValue() / 100.0);
                        Map<String, Object> data = new HashMap<>();
                        data.put("groupId", currentGroupId);
                        data.put("groupName", currentGroupName);
                        data.put("sourceType", "expense");
                        data.put("sourceId", expenseId);
                        data.put("concept", concept);
                        data.put("amount", partAmount);
                        data.put("debtorEmail", debtor);
                        data.put("creditorEmail", payerEmail);
                        data.put("dueAt", dueAt);
                        data.put("priority", priority == null ? "media" : priority.toLowerCase(Locale.ROOT));
                        data.put("status", "pending");
                        data.put("createdAt", FieldValue.serverTimestamp());
                        batch.set(db.collection("payment_deadlines").document(), data);

                        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
                        if (debtor.equalsIgnoreCase(myEmail)) {
                            scheduleDeadlineNotifications(concept, partAmount, dueAt, expenseId + "_" + debtor);
                        }
                    }
                    batch.commit();
                });
    }

    private void syncPaymentDeadline(String paymentId, String concept, double amount, String fromEmail, String toEmail, Date dueAt, String priority) {
        if (currentGroupId == null) return;
        db.collection("payment_deadlines")
                .whereEqualTo("sourceType", "payment")
                .whereEqualTo("sourceId", paymentId)
                .get()
                .addOnSuccessListener(existing -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : existing.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("groupId", currentGroupId);
                    data.put("groupName", currentGroupName);
                    data.put("sourceType", "payment");
                    data.put("sourceId", paymentId);
                    data.put("concept", concept);
                    data.put("amount", amount);
                    data.put("debtorEmail", fromEmail);
                    data.put("creditorEmail", toEmail);
                    data.put("dueAt", dueAt);
                    data.put("priority", priority == null ? "media" : priority.toLowerCase(Locale.ROOT));
                    data.put("status", "pending");
                    data.put("createdAt", FieldValue.serverTimestamp());
                    batch.set(db.collection("payment_deadlines").document(), data);
                    batch.commit();
                    String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
                    if (fromEmail.equalsIgnoreCase(myEmail)) {
                        scheduleDeadlineNotifications(concept, amount, dueAt, paymentId);
                    }
                });
    }

    private void scheduleDeadlineNotifications(String concept, double amount, Date dueAt, String suffix) {
        scheduleDeadlineNotifications(concept, new DecimalFormat("0.00").format(amount) + " EUR", dueAt.getTime(), suffix);
    }

    private void scheduleDeadlineNotifications(String concept, String amountLabel, long dueAtMs, String suffix) {
        long now = System.currentTimeMillis();
        long oneDayBefore = dueAtMs - 24L * 60L * 60L * 1000L;
        int reminderA = Math.abs(("due_a_" + suffix).hashCode());
        int reminderB = Math.abs(("due_b_" + suffix).hashCode());
        String title = concept == null || concept.isEmpty() ? "Pago pendiente" : concept;
        if (oneDayBefore > now) {
            ReminderScheduler.scheduleOneTime(requireContext(), reminderA, "Pago vence mañana", title + " - " + amountLabel, oneDayBefore);
        }
        if (dueAtMs > now) {
            ReminderScheduler.scheduleOneTime(requireContext(), reminderB, "Pago vence hoy", title + " - " + amountLabel, dueAtMs);
        }
    }

    private void deleteDeadlinesBySource(String sourceType, String sourceId) {
        db.collection("payment_deadlines")
                .whereEqualTo("sourceType", sourceType)
                .whereEqualTo("sourceId", sourceId)
                .get()
                .addOnSuccessListener(result -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit();
                });
    }

    private void updateDeadlineStatusBySource(String sourceType, String sourceId, String status) {
        db.collection("payment_deadlines")
                .whereEqualTo("sourceType", sourceType)
                .whereEqualTo("sourceId", sourceId)
                .get()
                .addOnSuccessListener(result -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        batch.update(doc.getReference(), "status", status);
                    }
                    batch.commit();
                });
    }

    private void showActivityHistoryDialog() {
        if (currentGroupId == null) return;
        db.collection("activity_logs")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(result -> {
                    StringBuilder message = new StringBuilder();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        String action = doc.getString("action");
                        String actor = doc.getString("actorEmail");
                        String concept = doc.getString("concept");
                        Double amount = doc.getDouble("amount");
                        message.append("- ").append(action == null ? "acción" : action)
                                .append(" | ").append(actor == null ? "usuario" : actor)
                                .append(" | ").append(concept == null ? "" : concept)
                                .append(" | ").append(amount == null ? "0.00" : amount).append(" EUR\n");
                    }
                    if (message.length() == 0) message.append("Sin actividad todavía.");
                    View content = DialogUtils.createMessageView(requireContext(), message.toString());
                    DialogUtils.Shell shell = DialogUtils.buildShell(
                            requireContext(),
                            "Historial de actividad",
                            "Creaciones, ediciones y borrados del piso.",
                            content,
                            null,
                            "Cerrar"
                    );
                    AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                    shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
                });
    }

    private void setupTicketControls(View form) {
        Button attachBtn = form.findViewById(R.id.attachTicketBtn);
        pendingTicketAmountEt = form.findViewById(R.id.amountEt);
        pendingTicketStatusTv = form.findViewById(R.id.ticketStatusTv);
        pendingTicketStatusTv.setText(pendingTicketUri == null || pendingTicketUri.isEmpty()
                ? "Sin ticket adjunto"
                : "Ticket adjunto");
        attachBtn.setOnClickListener(v -> ticketPickerLauncher.launch("image/*"));
    }

    private void handleTicketSelected(@Nullable Uri uri) {
        if (uri == null || !isAdded()) return;
        pendingTicketUri = uri.toString();
        if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("Ticket adjunto. Procesando OCR...");
        try {
            InputImage inputImage = InputImage.fromFilePath(requireContext(), uri);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(inputImage)
                    .addOnSuccessListener(text -> {
                        String detected = extractFirstAmount(text.getText());
                        if (detected != null && pendingTicketAmountEt != null) {
                            pendingTicketAmountEt.setText(detected);
                            if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("OCR detectó importe: " + detected);
                        } else if (pendingTicketStatusTv != null) {
                            pendingTicketStatusTv.setText("OCR listo, no se encontró importe claro.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("OCR falló.");
                    });
        } catch (Exception e) {
            if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("No se pudo leer la imagen.");
        }
    }

    private void handleReminderAttachmentSelected(@Nullable Uri uri) {
        if (uri == null || !isAdded()) return;
        pendingReminderAttachmentUri = uri.toString();
        if (pendingReminderAttachmentStatusTv != null) {
            pendingReminderAttachmentStatusTv.setText("Adjunto: " + uri.getLastPathSegment());
        }
    }

    @Nullable
    private String extractFirstAmount(String rawText) {
        Pattern pattern = Pattern.compile("(\\d+[\\.,]\\d{2})");
        Matcher matcher = pattern.matcher(rawText == null ? "" : rawText);
        if (matcher.find()) {
            return matcher.group(1).replace(",", ".");
        }
        return null;
    }

    private void exportMonthlySummaryPdf() {
        if (expenseRows.isEmpty()) {
            Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(ContextCompat.getColor(requireContext(), R.color.text_light));
            paint.setTextSize(14f);
            int y = 40;
            page.getCanvas().drawText("Resumen mensual - " + currentGroupName, 30, y, paint);
            y += 28;
            for (WorkspaceRow row : expenseRows) {
                if (y > 800) break;
                page.getCanvas().drawText(row.title + " | " + row.subtitle + " | " + row.amount, 30, y, paint);
                y += 22;
            }
            pdfDocument.finishPage(page);

            String fileName = "flatshare_resumen_" + System.currentTimeMillis() + ".pdf";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(requireContext(), "No se pudo crear el PDF", Toast.LENGTH_SHORT).show();
                return;
            }
            try (var out = requireContext().getContentResolver().openOutputStream(uri)) {
                pdfDocument.writeTo(out);
            }
            pdfDocument.close();
            Toast.makeText(requireContext(), "PDF exportado en Descargas", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error exportando PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private static class WorkspaceRow {
        final String id;
        final String type;
        final String title;
        final String subtitle;
        final String amount;
        final DocumentSnapshot snapshot;

        WorkspaceRow(String id, String type, String title, String subtitle, String amount, @Nullable DocumentSnapshot snapshot) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.amount = amount;
            this.snapshot = snapshot;
        }
    }

    private static class BalanceNode {
        final String email;
        double amount;

        BalanceNode(String email, double amount) {
            this.email = email;
            this.amount = amount;
        }
    }

    private static class BalanceMovement {
        final String fromEmail;
        final String toEmail;
        final double amount;

        BalanceMovement(String fromEmail, String toEmail, double amount) {
            this.fromEmail = fromEmail;
            this.toEmail = toEmail;
            this.amount = amount;
        }
    }

    private interface MembersCallback {
        void onLoaded(List<String> members);
    }

    private interface RoomsCallback {
        void onLoaded(List<RoomOption> rooms);
    }

    private interface RoomRowCallback {
        void onLoaded(@Nullable WorkspaceRow row);
    }

    private static class RoomOption {
        final String id;
        final String name;
        final List<String> memberEmails;
        final int roomNumber;
        final int capacity;
        final double monthlyCost;

        RoomOption(String id, String name, List<String> memberEmails, int roomNumber, int capacity, double monthlyCost) {
            this.id = id;
            this.name = name;
            this.memberEmails = memberEmails;
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.monthlyCost = monthlyCost;
        }
    }

    private static class PaymentTarget {
        final String toEmail;
        final String roomId;
        final String roomName;

        PaymentTarget(String toEmail, @Nullable String roomId, @Nullable String roomName) {
            this.toEmail = toEmail;
            this.roomId = roomId;
            this.roomName = roomName;
        }
    }

    private static class ReminderIntervalConfig {
        final String intervalKey;
        final int intervalDays;
        final long intervalMs;

        ReminderIntervalConfig(String intervalKey, int intervalDays, long intervalMs) {
            this.intervalKey = intervalKey;
            this.intervalDays = intervalDays;
            this.intervalMs = intervalMs;
        }
    }

    private static class ReminderTargetConfig {
        final String targetType;
        final List<String> targetEmails;
        final String primaryRoomId;
        final String primaryRoomName;
        final List<String> roomIds;
        final List<String> roomNames;
        final String memberEmail;

        ReminderTargetConfig(
                String targetType,
                List<String> targetEmails,
                String primaryRoomId,
                String primaryRoomName,
                List<String> roomIds,
                List<String> roomNames,
                String memberEmail
        ) {
            this.targetType = targetType;
            this.targetEmails = targetEmails;
            this.primaryRoomId = primaryRoomId;
            this.primaryRoomName = primaryRoomName;
            this.roomIds = roomIds;
            this.roomNames = roomNames;
            this.memberEmail = memberEmail;
        }
    }

    private ArrayAdapter<String> buildLightSpinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, values) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(requireContext().getColor(R.color.text_light));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(requireContext().getColor(R.color.text_light));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void setupTypeSpinner(Spinner spinner, @Nullable String selectedType) {
        ArrayAdapter<String> adapter = new TypeSpinnerAdapter(SPENDING_TYPES);
        spinner.setAdapter(adapter);
        if (selectedType == null) return;
        String normalized = selectedType.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < SPENDING_TYPES.length; i++) {
            if (SPENDING_TYPES[i].equals(normalized)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(SPENDING_TYPES.length - 1);
    }

    private String capitalizeTypeLabel(String type) {
        if (type == null || type.isEmpty()) return "";
        return Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }

    private class TypeSpinnerAdapter extends ArrayAdapter<String> {
        TypeSpinnerAdapter(String[] values) {
            super(requireContext(), R.layout.item_type_spinner, values);
            setDropDownViewResource(R.layout.item_type_spinner);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return bindView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return bindView(position, convertView, parent);
        }

        private View bindView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_type_spinner, parent, false);
            }
            View dot = view.findViewById(R.id.typeColorDot);
            TextView nameTv = view.findViewById(R.id.typeNameTv);

            String type = getItem(position);
            nameTv.setText(capitalizeTypeLabel(type));
            nameTv.setTextColor(requireContext().getColor(R.color.text_light));

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(SPENDING_TYPE_COLORS[position % SPENDING_TYPE_COLORS.length]);
            dot.setBackground(circle);
            return view;
        }
    }

    private void setupPrioritySpinner(Spinner spinner, @Nullable String selectedPriority) {
        ArrayAdapter<String> adapter = buildLightSpinnerAdapter(PRIORITY_TYPES);
        spinner.setAdapter(adapter);
        String normalized = selectedPriority == null ? "media" : selectedPriority.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < PRIORITY_TYPES.length; i++) {
            if (PRIORITY_TYPES[i].equals(normalized)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(1);
    }

    private void setupDateField(EditText dateField) {
        dateField.setOnClickListener(v -> showDueDatePicker(dateField));
        dateField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDueDatePicker(dateField);
        });
    }

    private void showDueDatePicker(EditText target) {
        Calendar current = Calendar.getInstance();
        String existing = target.getText().toString().trim();
        if (!existing.isEmpty()) {
            try {
                Date parsed = DUE_DATE_FORMAT.parse(existing);
                if (parsed != null) current.setTime(parsed);
            } catch (ParseException ignored) {
            }
        }
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    target.setText(DUE_DATE_FORMAT.format(selected.getTime()));
                },
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH)
        );
        Calendar min = Calendar.getInstance();
        min.set(Calendar.HOUR_OF_DAY, 0);
        min.set(Calendar.MINUTE, 0);
        min.set(Calendar.SECOND, 0);
        min.set(Calendar.MILLISECOND, 0);
        dialog.getDatePicker().setMinDate(min.getTimeInMillis());
        dialog.show();
    }

    private class WorkspaceAdapter extends BaseAdapter {
        private final List<WorkspaceRow> rows;

        WorkspaceAdapter(List<WorkspaceRow> rows) {
            this.rows = rows;
        }

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

            WorkspaceRow row = rows.get(position);
            TextView titleTv = view.findViewById(R.id.rowTitleTv);
            TextView subtitleTv = view.findViewById(R.id.rowSubtitleTv);
            TextView amountTv = view.findViewById(R.id.rowAmountTv);

            titleTv.setText(row.title);
            subtitleTv.setText(row.subtitle);
            amountTv.setText(row.amount);

            if ("payment".equals(row.type)) {
                if (row.subtitle.contains("Confirmado")) {
                    amountTv.setTextColor(requireContext().getColor(R.color.status_success));
                    subtitleTv.setTextColor(requireContext().getColor(R.color.status_success));
                    titleTv.setText("Pago confirmado");
                } else if (row.subtitle.contains("Vencido")) {
                    amountTv.setTextColor(requireContext().getColor(R.color.status_danger));
                    subtitleTv.setTextColor(requireContext().getColor(R.color.status_danger));
                    titleTv.setText("Pago vencido");
                } else {
                    amountTv.setTextColor(requireContext().getColor(R.color.status_warning));
                    subtitleTv.setTextColor(requireContext().getColor(R.color.status_warning));
                    titleTv.setText("Pago pendiente");
                }
            } else {
                amountTv.setTextColor(requireContext().getColor(R.color.text_light));
                subtitleTv.setTextColor(requireContext().getColor(R.color.text_muted));
            }
            return view;
        }
    }
}


