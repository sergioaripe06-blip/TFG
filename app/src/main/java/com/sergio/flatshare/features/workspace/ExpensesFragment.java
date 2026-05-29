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
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.widget.BaseAdapter;
import android.widget.Toast;
import android.widget.Button;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.EditText;
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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.shared.ui.NoticeUtils;
import com.sergio.flatshare.core.notifications.ReminderScheduler;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.core.sound.AppSoundFx;
import com.sergio.flatshare.features.workspace.services.CategorySuggestionsRepository;
import com.sergio.flatshare.features.workspace.services.ExpenseDialogs;
import com.sergio.flatshare.features.workspace.services.ExpenseService;
import com.sergio.flatshare.features.workspace.services.PaymentService;
import com.sergio.flatshare.features.workspace.services.ReminderService;

import java.text.DecimalFormat;
import java.text.Normalizer;
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
    private static final String ROOM_SPLIT_EQUAL = "equal";
    private static final String ROOM_SPLIT_PERCENTAGE = "percentage";
    private static final String ROOM_ALL_LABEL = "Todas las habitaciones";
    private static final String FILTER_MODE_CATEGORY = "category";
    private static final String FILTER_MODE_PERSON = "person";
    private static final String FILTER_MODE_DATE = "date";
    private static final String CATEGORY_RENT = "alquiler";
    private static final String ROW_TYPE_PENDING_DEBT = "pending_debt";
    private static final String STATUS_REQUESTED = "requested";
    private static final String STATUS_SUBMITTED = "submitted";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CONFIRMED = "confirmed";
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
    private static final String[] REMINDER_INTERVAL_TYPES = {"Una sola vez", "Cada día", "Cada semana", "Cada mes", "Personalizado"};
    private static final String[] REMINDER_TARGET_TYPES = {"Inquilinos seleccionados", "Habitaciones seleccionadas", "Todos los inquilinos"};
    private static final SimpleDateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CategorySuggestionsRepository categorySuggestionsRepository = new CategorySuggestionsRepository(db);
    private final PaymentService paymentService = new PaymentService();
    private final ExpenseService expenseService = new ExpenseService();
    private final ReminderService reminderService = new ReminderService();
    private final List<WorkspaceRow> expenseRows = new ArrayList<>();
    private final List<WorkspaceRow> saldoRows = new ArrayList<>();
    private final List<WorkspaceRow> reminderRows = new ArrayList<>();
    private final List<RoomOption> roomFilterOptions = new ArrayList<>();

    private WorkspaceAdapter expensesAdapter;
    private WorkspaceAdapter saldosAdapter;
    private WorkspaceAdapter remindersAdapter;

    private TextView workspaceTitleTv;
    private TextView workspaceMetaTv;
    private View workspaceHeaderCard;
    private LinearLayout titleTabsRow;
    private LinearLayout tabRow;
    private ListView expensesLv;
    private ListView saldosLv;
    private ListView remindersLv;
    private View tenantsContainer;
    private View quickBalancesCard;
    private LinearLayout quickBalancesContainer;
    private View workspaceCtaLayout;
    private TextView addMainLabelTv;
    private Button mainActionBtn;
    private Button gastosTabBtn;
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
    private Button pendingPaymentConfirmBtn;
    private boolean requirePaymentProofForCurrentDialog = false;
    @Nullable private PendingDebtRequest activePendingDebtRequest;
    private boolean isRebindingRoomSelectors = false;
    private boolean isUpdatingRoomFilterSpinner = false;
    private final Map<String, String> memberDisplayNamesByEmail = new HashMap<>();
    private String workspaceMetaCache = "";
    private String workspaceDescriptionCache = "";
    private String workspaceOwnerCache = "";
    private String workspaceBillingCache = "";
    private final List<String> workspaceMembersCache = new ArrayList<>();
    private String workspaceLocationQueryCache = "";
    private final List<BalanceMovement> lastBalanceMovements = new ArrayList<>();
    private long lastTabSwitchAtMs = 0L;
    private long lastMainActionAtMs = 0L;
    private boolean expenseActionDialogVisible = false;
    private boolean tabsUnderTitle = false;

    private final ActivityResultLauncher<String> ticketPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleTicketSelected);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);
        workspaceTitleTv = view.findViewById(R.id.workspaceTitleTv);
        workspaceMetaTv = view.findViewById(R.id.workspaceMetaTv);
        workspaceHeaderCard = view.findViewById(R.id.workspaceHeaderCard);
        titleTabsRow = view.findViewById(R.id.titleTabsRow);
        tabRow = view.findViewById(R.id.tabRow);
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
        remindersTabBtn = view.findViewById(R.id.remindersTabBtn);
        managementTabBtn = view.findViewById(R.id.tenantsTabBtn);
        roomFilterSpinner = view.findViewById(R.id.roomFilterSpinner);
        addRoomQuickBtn = view.findViewById(R.id.addRoomQuickBtn);
        expensesToolsRow = view.findViewById(R.id.expensesToolsRow);
        expensesSearchEt = view.findViewById(R.id.expensesSearchEt);
        openFiltersBtn = view.findViewById(R.id.openFiltersBtn);
        mainActionBtn = view.findViewById(R.id.addExpenseCenterFab);

        expensesAdapter = new WorkspaceAdapter(expenseRows);
        saldosAdapter = new WorkspaceAdapter(saldoRows);
        remindersAdapter = new WorkspaceAdapter(reminderRows);
        expensesLv.setAdapter(expensesAdapter);
        saldosLv.setAdapter(saldosAdapter);
        remindersLv.setAdapter(remindersAdapter);

        gastosTabBtn.setOnClickListener(v -> requestTabSwitch(TAB_EXPENSES));
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
        mainActionBtn.setOnClickListener(v -> {
            if (isFastTap(lastMainActionAtMs, 400)) return;
            lastMainActionAtMs = SystemClock.elapsedRealtime();
            handleMainAction();
        });
        expensesLv.setOnItemClickListener((parent, v, position, id) -> showRowDetail(expenseRows.get(position)));
        remindersLv.setOnItemClickListener((parent, v, position, id) -> showRowDetail(reminderRows.get(position)));
        remindersLv.setOnItemLongClickListener((parent, v, position, id) -> {
            if (position < 0 || position >= reminderRows.size()) return true;
            showReminderLongPressActions(reminderRows.get(position));
            return true;
        });
        if (workspaceCtaLayout != null) {
            workspaceCtaLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    applyBottomContentInset()
            );
        }
        if (quickBalancesCard != null) {
            quickBalancesCard.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    applyBottomContentInset()
            );
        }
        if (workspaceHeaderCard != null) {
            workspaceHeaderCard.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    updateTitleTabsPlacement()
            );
        }

        switchTab(TAB_EXPENSES);
        refreshWorkspace();
        view.post(this::applyBottomContentInset);
        view.post(this::updateTitleTabsPlacement);
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
            updateTitleTabsPlacement();
            workspaceMetaCache = "Entra desde la pestaña de pisos para ver gastos, pagos, recordatorios y gestión del alquiler.";
            workspaceMetaTv.setText(workspaceMetaCache);
            workspaceDescriptionCache = "";
            workspaceOwnerCache = "";
            workspaceBillingCache = "";
            workspaceMembersCache.clear();
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
                String ownerLabel = memberReferenceInline(ownerEmail);
                String membersLabel = formatMembersDetailed(memberEmails);
                String billingLabel = BILLING_FIXED.equals(currentBillingModel)
                        ? "Alquiler fijo"
                        : "Alquiler variable (por habitación)";
                String safeOwner = ownerLabel.isEmpty() ? "Sin datos" : ownerLabel;
                String meta = "Piso: " + description
                        + "\nPropietario: " + safeOwner
                        + "\nModelo: " + billingLabel
                        + "\nMiembros (" + memberEmails.size() + "):\n" + membersLabel;
                workspaceTitleTv.setText(currentGroupName);
                updateTitleTabsPlacement();
                workspaceMetaCache = meta;
                workspaceDescriptionCache = description;
                workspaceOwnerCache = safeOwner;
                workspaceBillingCache = billingLabel;
                workspaceMembersCache.clear();
                workspaceMembersCache.addAll(memberEmails);
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
        updateTabStyle(remindersTabBtn, showReminders);
        updateTabStyle(managementTabBtn, showManagement);

        if (showExpenses) {
            addMainLabelTv.setText("Nueva acción");
        } else if (showReminders) {
            addMainLabelTv.setText("Nuevo recordatorio");
        } else {
            addMainLabelTv.setText("Gestion");
            ensureManagementFragment();
        }
        if (mainActionBtn != null && addMainLabelTv != null) {
            mainActionBtn.setText(addMainLabelTv.getText());
        }
        updateQuickBalancesVisibility();
        updateCtaVisibilityForCurrentTab();
    }

    private void updateTitleTabsPlacement() {
        if (!isAdded() || workspaceTitleTv == null || titleTabsRow == null || tabRow == null) return;
        workspaceTitleTv.post(() -> {
            if (!isAdded() || workspaceTitleTv == null || titleTabsRow == null || tabRow == null) return;

            applyTitleTabsLayout(false);
            int rowWidth = titleTabsRow.getWidth() - titleTabsRow.getPaddingLeft() - titleTabsRow.getPaddingRight();
            int tabsWidth = measureWrapWidth(tabRow);
            boolean noSpace = rowWidth > 0 && (rowWidth - tabsWidth) < dp(150);
            boolean ellipsized = isTextEllipsized(workspaceTitleTv);
            applyTitleTabsLayout(noSpace || ellipsized);
        });
    }

    private void applyTitleTabsLayout(boolean placeTabsBelowTitle) {
        if (workspaceTitleTv == null || titleTabsRow == null || tabRow == null) return;
        if (tabsUnderTitle == placeTabsBelowTitle
                && titleTabsRow.getOrientation() == (placeTabsBelowTitle ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL)) {
            return;
        }
        tabsUnderTitle = placeTabsBelowTitle;

        LinearLayout.LayoutParams titleLp = (LinearLayout.LayoutParams) workspaceTitleTv.getLayoutParams();
        LinearLayout.LayoutParams tabsLp = (LinearLayout.LayoutParams) tabRow.getLayoutParams();

        if (placeTabsBelowTitle) {
            titleTabsRow.setOrientation(LinearLayout.VERTICAL);
            titleTabsRow.setGravity(Gravity.CENTER_HORIZONTAL);
            titleLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            titleLp.weight = 0f;
            titleLp.bottomMargin = dp(8);
            workspaceTitleTv.setMaxLines(2);
            workspaceTitleTv.setEllipsize(TextUtils.TruncateAt.END);

            tabsLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            tabsLp.gravity = Gravity.CENTER_HORIZONTAL;
            tabsLp.leftMargin = 0;
            tabsLp.topMargin = 0;
            applyTabButtonsLayout(true);
        } else {
            titleTabsRow.setOrientation(LinearLayout.HORIZONTAL);
            titleTabsRow.setGravity(Gravity.TOP);
            titleLp.width = 0;
            titleLp.weight = 1f;
            titleLp.bottomMargin = 0;
            workspaceTitleTv.setMaxLines(1);
            workspaceTitleTv.setEllipsize(TextUtils.TruncateAt.END);

            tabsLp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            tabsLp.gravity = Gravity.NO_GRAVITY;
            tabsLp.leftMargin = dp(10);
            tabsLp.topMargin = 0;
            applyTabButtonsLayout(false);
        }

        workspaceTitleTv.setLayoutParams(titleLp);
        tabRow.setLayoutParams(tabsLp);
        titleTabsRow.requestLayout();
    }

    private void applyTabButtonsLayout(boolean expanded) {
        if (gastosTabBtn == null || remindersTabBtn == null || managementTabBtn == null) return;
        Button[] buttons = new Button[]{gastosTabBtn, remindersTabBtn, managementTabBtn};
        for (int i = 0; i < buttons.length; i++) {
            Button button = buttons[i];
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) button.getLayoutParams();
            if (expanded) {
                lp.width = 0;
                lp.weight = 1f;
                lp.leftMargin = i == 0 ? 0 : dp(6);
            } else {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.weight = 0f;
                lp.leftMargin = i == 0 ? 0 : dp(6);
            }
            button.setLayoutParams(lp);
        }
    }

    private int measureWrapWidth(@NonNull View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        return view.getMeasuredWidth();
    }

    private boolean isTextEllipsized(@NonNull TextView tv) {
        Layout layout = tv.getLayout();
        if (layout == null) return false;
        for (int i = 0; i < layout.getLineCount(); i++) {
            if (layout.getEllipsisCount(i) > 0) return true;
        }
        return false;
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
    loadCurrentGroupRooms(rooms -> {
        View content = buildWorkspaceInfoDialogContent(rooms);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Datos del piso",
                "Información del piso",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
    });
}

private View buildWorkspaceInfoDialogContent(@NonNull List<RoomOption> rooms) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setFillViewport(true);

    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(4), dp(2), dp(4), dp(2));
    scrollView.addView(root, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT
    ));

    String heroTitle = currentGroupName == null || currentGroupName.trim().isEmpty() ? "Piso" : currentGroupName.trim();
    String heroSubtitle = workspaceDescriptionCache.isEmpty() ? "Sin descripción" : workspaceDescriptionCache;
    addHeroInfoCard(root, heroTitle, heroSubtitle);

    addInfoCard(root, "Propietario", workspaceOwnerCache.isEmpty() ? "Sin datos" : workspaceOwnerCache);
    addInfoCard(root, "Modelo de reparto", workspaceBillingCache.isEmpty() ? "Sin datos" : workspaceBillingCache);

    TextView roomsTitle = new TextView(requireContext());
    roomsTitle.setTextColor(requireContext().getColor(R.color.text_light));
    roomsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    roomsTitle.setText("Habitaciones (" + rooms.size() + ")");
    LinearLayout.LayoutParams roomsTitleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    roomsTitleParams.topMargin = dp(12);
    roomsTitle.setLayoutParams(roomsTitleParams);
    root.addView(roomsTitle);

    if (rooms.isEmpty()) {
        addInfoCard(root, "Habitaciones", "Sin habitaciones creadas");
    } else {
        for (RoomOption room : rooms) {
            String roomName = room.name == null || room.name.trim().isEmpty() ? "Habitación" : room.name.trim();
            String splitModeLabel = ROOM_SPLIT_PERCENTAGE.equals(normalizeRoomSplitMode(room.rentSplitMode))
                    ? "Porcentual"
                    : "Equitativo";

            LinearLayout roomCard = new LinearLayout(requireContext());
            roomCard.setOrientation(LinearLayout.VERTICAL);
            roomCard.setBackgroundResource(R.drawable.bg_input_dark_round);
            roomCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            roomCard.setClickable(true);
            roomCard.setFocusable(true);
            roomCard.setOnClickListener(v -> showRoomActionsFromWorkspaceInfo(room));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(10);
            roomCard.setLayoutParams(params);

            TextView roomTitle = new TextView(requireContext());
            roomTitle.setTextColor(requireContext().getColor(R.color.primary_green));
            roomTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            roomTitle.setText(roomName);
            roomCard.addView(roomTitle);

            TextView roomDetails = new TextView(requireContext());
            roomDetails.setTextColor(requireContext().getColor(R.color.text_light));
            roomDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            roomDetails.setPadding(0, dp(4), 0, 0);
            roomDetails.setText(
                    "Capacidad: " + room.capacity
                            + " · Residentes: " + room.memberEmails.size()
                            + " · Reparto: " + splitModeLabel
            );
            roomCard.addView(roomDetails);

            TextView roomTapHint = new TextView(requireContext());
            roomTapHint.setTextColor(requireContext().getColor(R.color.text_muted));
            roomTapHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            roomTapHint.setPadding(0, dp(3), 0, 0);
            roomTapHint.setText("Toca para ver opciones");
            roomCard.addView(roomTapHint);

            root.addView(roomCard);
        }
    }

    Button addRoomBtn = DialogUtils.createActionButton(requireContext(), "Añadir habitación", false);
    Button openLocationBtn = DialogUtils.createActionButton(requireContext(), "Ver ubicación", true);
    LinearLayout.LayoutParams addParams = (LinearLayout.LayoutParams) addRoomBtn.getLayoutParams();
    addParams.topMargin = dp(14);
    addRoomBtn.setLayoutParams(addParams);

    addRoomBtn.setOnClickListener(v -> createRoomFromWorkspace());
    openLocationBtn.setOnClickListener(v -> openWorkspaceLocationInMaps());
    root.addView(addRoomBtn);
    root.addView(openLocationBtn);

    return scrollView;
}

private void showRoomActionsFromWorkspaceInfo(@NonNull RoomOption room) {
    loadRoomRowById(room.id, row -> {
        if (row == null) return;
        LinearLayout content = DialogUtils.createVerticalActions(requireContext());
        Button infoBtn = DialogUtils.createActionButton(requireContext(), "Ver información", true);
        Button editBtn = DialogUtils.createActionButton(requireContext(), "Editar habitación", false);
        Button deleteBtn = DialogUtils.createActionButton(requireContext(), "Eliminar habitación", false);
        content.addView(infoBtn);
        if (canManageRooms()) {
            content.addView(editBtn);
            content.addView(deleteBtn);
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Selecciona la acción para esta habitación.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        infoBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showRoomInfoFromRow(row);
        });
        editBtn.setOnClickListener(v -> {
            dialog.dismiss();
            editRoomFromWorkspace(row);
        });
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            requestRoomDeletion(row);
        });
    });
}

private void showRoomInfoFromRow(@NonNull WorkspaceRow row) {
    if (row.snapshot == null) return;
    String name = row.snapshot.getString("name");
    Long roomNumber = row.snapshot.getLong("roomNumber");
    Long capacity = row.snapshot.getLong("capacity");
    Double monthlyCost = row.snapshot.getDouble("monthlyCost");
    List<String> residents = castEmails(row.snapshot.get("memberEmails"));
    View content = buildRoomInfoDialogContent(name, roomNumber, capacity, monthlyCost, residents);
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
}

private void loadRoomRowById(@Nullable String roomId, @NonNull RoomRowCallback callback) {
    if (roomId == null || roomId.trim().isEmpty()) {
        callback.onLoaded(null);
        return;
    }
    db.collection("rooms_groups").document(roomId).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    NoticeUtils.show(requireContext(), "La habitación ya no existe");
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
                NoticeUtils.show(requireContext(), "No se pudo cargar la habitación");
                callback.onLoaded(null);
            });
}

    private void addHeroInfoCard(@NonNull LinearLayout parent, @NonNull String title, @NonNull String subtitle) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_group_row);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        card.setLayoutParams(params);

        TextView titleTv = new TextView(requireContext());
        titleTv.setTextColor(requireContext().getColor(R.color.text_light));
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        titleTv.setText(title.trim().isEmpty() ? "Sin título" : title.trim());
        card.addView(titleTv);

        TextView subtitleTv = new TextView(requireContext());
        subtitleTv.setTextColor(requireContext().getColor(R.color.text_muted));
        subtitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitleTv.setPadding(0, dp(4), 0, 0);
        subtitleTv.setText(subtitle.trim().isEmpty() ? "Sin datos" : subtitle.trim());
        card.addView(subtitleTv);

        parent.addView(card);
    }

    private void addInfoCard(@NonNull LinearLayout parent, @NonNull String label, @NonNull String value) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_input_dark_round);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(10);
        card.setLayoutParams(params);

        TextView labelTv = new TextView(requireContext());
        labelTv.setTextColor(requireContext().getColor(R.color.primary_green));
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        labelTv.setText(label);
        card.addView(labelTv);

        TextView valueTv = new TextView(requireContext());
        valueTv.setTextColor(requireContext().getColor(R.color.text_light));
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        valueTv.setPadding(0, dp(4), 0, 0);
        valueTv.setText(value.trim().isEmpty() ? "Sin datos" : value.trim());
        card.addView(valueTv);

        parent.addView(card);
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
            NoticeUtils.show(requireContext(), "esa ubi no existe");
            return;
        }
        if (!locationSeemsValid(query)) {
            NoticeUtils.show(requireContext(), "esa ubi no existe");
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
        NoticeUtils.show(requireContext(), "no funciona");
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
        applyBottomContentInset();
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
            NoticeUtils.show(requireContext(), "Selecciona un piso primero");
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
            NoticeUtils.show(requireContext(), "Solo el propietario puede crear habitaciones");
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
                    NoticeUtils.show(requireContext(), "Completa todos los datos de la habitación");
                    return;
                }

                int parsedCapacity;
                double parsedCost;
                try {
                    parsedCapacity = Integer.parseInt(capacityValue);
                    parsedCost = Double.parseDouble(costValue);
                } catch (NumberFormatException e) {
                    NoticeUtils.show(requireContext(), "Capacidad o coste no válidos");
                    return;
                }
                if (parsedCapacity <= 0) {
                    NoticeUtils.show(requireContext(), "La capacidad debe ser mayor que 0");
                    return;
                }
                if (parsedCost < 0) {
                    NoticeUtils.show(requireContext(), "El coste no puede ser negativo");
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
                roomData.put("rentSplitMode", ROOM_SPLIT_EQUAL);
                roomData.put("rentSplitPercentages", new HashMap<String, Object>());
                roomData.put("rentSplitOrder", new ArrayList<String>());
                roomData.put("createdByUid", uid);
                roomData.put("updatedByUid", uid);
                roomData.put("createdAt", FieldValue.serverTimestamp());
                roomData.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("rooms_groups")
                        .add(roomData)
                        .addOnSuccessListener(done -> {
                            NoticeUtils.show(requireContext(), "Habitación creada");
                            refreshWorkspace();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo crear la habitación", Toast.LENGTH_SHORT).show());
            });
        });
    }

    private void showRoomQuickActionsDialog() {
        if (currentGroupId == null) {
            NoticeUtils.show(requireContext(), "Selecciona un piso primero");
            return;
        }

        LinearLayout content = DialogUtils.createVerticalActions(requireContext());
        Button infoBtn = DialogUtils.createActionButton(requireContext(), "Ver información", true);
        Button editBtn = DialogUtils.createActionButton(requireContext(), "Editar habitación", false);
        Button deleteBtn = DialogUtils.createActionButton(requireContext(), "Eliminar habitación", false);
        Button addBtn = DialogUtils.createActionButton(requireContext(), "Añadir habitación", false);
        content.addView(infoBtn);
        content.addView(editBtn);
        content.addView(deleteBtn);
        content.addView(addBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Gestión de habitaciones",
                "Elige una acción.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        infoBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showSelectedRoomInfoDialog();
        });
        editBtn.setOnClickListener(v -> {
            dialog.dismiss();
            editSelectedRoomFromFilter();
        });
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            deleteSelectedRoomFromFilter();
        });
        addBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createRoomFromWorkspace();
        });
    }

    private void showSelectedRoomInfoDialog() {
        if (!hasRoomContext()) {
            NoticeUtils.show(requireContext(), "Selecciona una habitación en el desplegable");
            return;
        }
        loadRoomRowForCurrentSelection(row -> {
            if (row == null || row.snapshot == null) return;
            String name = row.snapshot.getString("name");
            Long roomNumber = row.snapshot.getLong("roomNumber");
            Long capacity = row.snapshot.getLong("capacity");
            Double monthlyCost = row.snapshot.getDouble("monthlyCost");
            List<String> residents = castEmails(row.snapshot.get("memberEmails"));

            View content = buildRoomInfoDialogContent(name, roomNumber, capacity, monthlyCost, residents);
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

    private View buildRoomInfoDialogContent(
            @Nullable String roomName,
            @Nullable Long roomNumber,
            @Nullable Long capacity,
            @Nullable Double monthlyCost,
            @Nullable List<String> residents
    ) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(4), dp(2), dp(4), dp(2));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        String safeRoomName = roomName == null || roomName.trim().isEmpty() ? "Habitación" : roomName.trim();
        String monthly = monthlyCost == null ? "0.00" : new DecimalFormat("0.00").format(monthlyCost);
        List<String> safeResidents = residents == null ? Collections.emptyList() : residents;
        String roomNumberLabel = roomNumber == null ? "-" : String.valueOf(roomNumber);

        addHeroInfoCard(root, safeRoomName, "Hab. " + roomNumberLabel + "  •  " + monthly + " EUR/mes");
        addInfoCard(root, "Capacidad máxima", capacity == null ? "0" : String.valueOf(capacity));
        addInfoCard(root, "Residentes actuales", String.valueOf(safeResidents.size()));

        TextView membersTitle = new TextView(requireContext());
        membersTitle.setTextColor(requireContext().getColor(R.color.text_light));
        membersTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        membersTitle.setText("Residentes (" + safeResidents.size() + ")");
        LinearLayout.LayoutParams membersTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        membersTitleParams.topMargin = dp(12);
        membersTitle.setLayoutParams(membersTitleParams);
        root.addView(membersTitle);

        if (safeResidents.isEmpty()) {
            addInfoCard(root, "Residentes", "Sin asignar");
            return scrollView;
        }

        int index = 1;
        for (String email : safeResidents) {
            if (email == null || email.trim().isEmpty()) continue;
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            String name = displayNameForEmail(normalized);
            if (name == null || name.trim().isEmpty()) {
                name = "Sin nombre";
            }

            LinearLayout memberCard = new LinearLayout(requireContext());
            memberCard.setOrientation(LinearLayout.VERTICAL);
            memberCard.setBackgroundResource(R.drawable.bg_input_dark_round);
            memberCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(10);
            memberCard.setLayoutParams(params);

            TextView memberTitle = new TextView(requireContext());
            memberTitle.setTextColor(requireContext().getColor(R.color.primary_green));
            memberTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            memberTitle.setText("Miembro " + index);
            memberCard.addView(memberTitle);

            TextView nameTv = new TextView(requireContext());
            nameTv.setTextColor(requireContext().getColor(R.color.text_light));
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            nameTv.setPadding(0, dp(4), 0, 0);
            nameTv.setText("Nombre: " + name);
            memberCard.addView(nameTv);

            TextView emailTv = new TextView(requireContext());
            emailTv.setTextColor(requireContext().getColor(R.color.text_muted));
            emailTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            emailTv.setPadding(0, dp(2), 0, 0);
            emailTv.setText("Correo: " + normalized);
            memberCard.addView(emailTv);

            root.addView(memberCard);
            index++;
        }

        return scrollView;
    }
    private void editSelectedRoomFromFilter() {
        if (!canManageRooms()) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede editar habitaciones");
            return;
        }
        if (!hasRoomContext()) {
            NoticeUtils.show(requireContext(), "Selecciona una habitación en el desplegable");
            return;
        }
        loadRoomRowForCurrentSelection(row -> {
            if (row == null) return;
            editRoomFromWorkspace(row);
        });
    }

    private void deleteSelectedRoomFromFilter() {
        if (!canManageRooms()) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede eliminar habitaciones");
            return;
        }
        if (!hasRoomContext()) {
            NoticeUtils.show(requireContext(), "Selecciona una habitación en el desplegable");
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
                        NoticeUtils.show(requireContext(), "La habitación ya no existe");
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
                    NoticeUtils.show(requireContext(), "No se pudo cargar la habitación");
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
            NoticeUtils.show(requireContext(), "Solo el propietario puede gestionar habitaciones");
            return;
        }

        LinearLayout content = DialogUtils.createVerticalActions(requireContext());
        Button editRoomBtn = DialogUtils.createActionButton(requireContext(), "Editar habitación", true);
        Button editTenantsBtn = DialogUtils.createActionButton(requireContext(), "Editar inquilinos", false);
        Button deleteRoomBtn = DialogUtils.createActionButton(requireContext(), "Eliminar habitación", false);
        content.addView(editRoomBtn);
        content.addView(editTenantsBtn);
        content.addView(deleteRoomBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Selecciona la acción para esta habitación.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        editRoomBtn.setOnClickListener(v -> {
            dialog.dismiss();
            editRoomFromWorkspace(row);
        });
        editTenantsBtn.setOnClickListener(v -> {
            dialog.dismiss();
            editRoomResidentsFromWorkspace(row);
        });
        deleteRoomBtn.setOnClickListener(v -> {
            dialog.dismiss();
            requestRoomDeletion(row);
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
                NoticeUtils.show(requireContext(), "Completa todos los datos de la habitación");
                return;
            }

            int parsedCapacity;
            double parsedCost;
            try {
                parsedCapacity = Integer.parseInt(capacityValue);
                parsedCost = Double.parseDouble(costValue);
            } catch (NumberFormatException e) {
                NoticeUtils.show(requireContext(), "Capacidad o coste no válidos");
                return;
            }
            if (parsedCapacity <= 0) {
                NoticeUtils.show(requireContext(), "La capacidad debe ser mayor que 0");
                return;
            }
            if (parsedCost < 0) {
                NoticeUtils.show(requireContext(), "El coste no puede ser negativo");
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
                    NoticeUtils.show(requireContext(), "Habitación actualizada");
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
                NoticeUtils.show(requireContext(), "No hay miembros para asignar");
                return;
            }
            List<String> currentResidents = castEmails(row.snapshot.get("memberEmails"));
            Map<String, Double> persistedSplitPercentages = castPercentages(row.snapshot.get("rentSplitPercentages"));
            String persistedSplitMode = normalizeRoomSplitMode(row.snapshot.getString("rentSplitMode"));

            ScrollView scroll = new ScrollView(requireContext());
            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(2), 0, dp(2), 0);
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

            TextView splitModeLabel = new TextView(requireContext());
            splitModeLabel.setText("Reparto de alquiler en esta habitación");
            splitModeLabel.setTextColor(requireContext().getColor(R.color.text_light));
            splitModeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            splitModeLabel.setPadding(0, dp(12), 0, dp(4));
            content.addView(splitModeLabel);

            Spinner splitModeSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
            splitModeSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
            splitModeSpinner.setPadding(dp(12), 0, dp(12), 0);
            splitModeSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(50)
            ));
            splitModeSpinner.setAdapter(buildLightSpinnerAdapter(new String[]{"Equitativo", "Porcentual"}));
            splitModeSpinner.setSelection(ROOM_SPLIT_PERCENTAGE.equals(persistedSplitMode) ? 1 : 0);
            content.addView(splitModeSpinner);

            TextView splitHintTv = new TextView(requireContext());
            splitHintTv.setTextColor(requireContext().getColor(R.color.text_muted));
            splitHintTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            splitHintTv.setPadding(0, dp(6), 0, dp(6));
            content.addView(splitHintTv);

            TextView autoResidentLabelTv = new TextView(requireContext());
            autoResidentLabelTv.setText("Inquilino automático (porcentaje restante):");
            autoResidentLabelTv.setTextColor(requireContext().getColor(R.color.text_light));
            autoResidentLabelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            autoResidentLabelTv.setPadding(0, dp(6), 0, dp(4));
            content.addView(autoResidentLabelTv);

            Spinner autoResidentSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
            autoResidentSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
            autoResidentSpinner.setPadding(dp(12), 0, dp(12), 0);
            autoResidentSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(50)
            ));
            content.addView(autoResidentSpinner);

            LinearLayout percentageRowsContainer = new LinearLayout(requireContext());
            percentageRowsContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams percentageRowsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            percentageRowsParams.topMargin = dp(8);
            percentageRowsContainer.setLayoutParams(percentageRowsParams);
            content.addView(percentageRowsContainer);

            TextView autoPercentPreviewTv = new TextView(requireContext());
            autoPercentPreviewTv.setTextColor(requireContext().getColor(R.color.text_muted));
            autoPercentPreviewTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            autoPercentPreviewTv.setPadding(0, dp(6), 0, 0);
            content.addView(autoPercentPreviewTv);

            Map<String, Double> draftPercentages = new LinkedHashMap<>(persistedSplitPercentages);
            String[] autoResidentEmailRef = new String[]{""};
            boolean[] isRefreshingSplit = new boolean[]{false};
            boolean[] isBindingAutoSpinner = new boolean[]{false};

            Runnable refreshSplitSection = () -> {
                if (isRefreshingSplit[0]) return;
                isRefreshingSplit[0] = true;

                List<String> selectedResidents = deduplicateStringKeys(collectCheckedMembers(checks, members));
                boolean canUsePercentage = selectedResidents.size() >= 2;
                boolean percentageMode = canUsePercentage && splitModeSpinner.getSelectedItemPosition() == 1;

                if (!canUsePercentage && splitModeSpinner.getSelectedItemPosition() == 1) {
                    splitModeSpinner.setSelection(0);
                }
                splitModeSpinner.setEnabled(canUsePercentage);
                splitModeSpinner.setAlpha(canUsePercentage ? 1f : 0.55f);

                if (!canUsePercentage) {
                    splitHintTv.setText("Con menos de 2 inquilinos se asigna el 100% al único residente.");
                    autoResidentLabelTv.setVisibility(View.GONE);
                    autoResidentSpinner.setVisibility(View.GONE);
                    percentageRowsContainer.setVisibility(View.GONE);
                    autoPercentPreviewTv.setVisibility(View.GONE);
                    isRefreshingSplit[0] = false;
                    return;
                }

                splitHintTv.setText(percentageMode
                        ? "Define porcentajes manuales y un inquilino automático para cerrar el 100%."
                        : "Se divide a partes iguales entre los residentes.");

                if (!percentageMode) {
                    autoResidentLabelTv.setVisibility(View.GONE);
                    autoResidentSpinner.setVisibility(View.GONE);
                    percentageRowsContainer.setVisibility(View.GONE);
                    autoPercentPreviewTv.setVisibility(View.GONE);
                    isRefreshingSplit[0] = false;
                    return;
                }

                autoResidentLabelTv.setVisibility(View.VISIBLE);
                autoResidentSpinner.setVisibility(View.VISIBLE);
                percentageRowsContainer.setVisibility(View.VISIBLE);
                autoPercentPreviewTv.setVisibility(View.VISIBLE);

                String autoResident = autoResidentEmailRef[0];
                if (autoResident == null || autoResident.trim().isEmpty() || !selectedResidents.contains(autoResident)) {
                    autoResident = selectedResidents.get(selectedResidents.size() - 1);
                    autoResidentEmailRef[0] = autoResident;
                }

                List<String> autoOptions = new ArrayList<>(selectedResidents);
                List<String> autoLabels = new ArrayList<>();
                for (String option : autoOptions) {
                    autoLabels.add(displayNameForEmail(option));
                }

                isBindingAutoSpinner[0] = true;
                autoResidentSpinner.setTag(autoOptions);
                autoResidentSpinner.setAdapter(buildLightSpinnerAdapter(autoLabels.toArray(new String[0])));
                int autoIndex = autoOptions.indexOf(autoResident);
                autoResidentSpinner.setSelection(autoIndex >= 0 ? autoIndex : 0);
                autoResidentEmailRef[0] = autoOptions.get(autoResidentSpinner.getSelectedItemPosition());
                isBindingAutoSpinner[0] = false;

                percentageRowsContainer.removeAllViews();
                List<EditText> editableInputs = new ArrayList<>();
                List<String> editableResidents = new ArrayList<>();
                for (String resident : selectedResidents) {
                    if (resident.equals(autoResidentEmailRef[0])) continue;
                    editableResidents.add(resident);

                    TextView residentLabel = new TextView(requireContext());
                    residentLabel.setText(displayNameForEmail(resident) + " (%):");
                    residentLabel.setTextColor(requireContext().getColor(R.color.text_light));
                    residentLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    residentLabel.setPadding(0, dp(6), 0, dp(4));
                    percentageRowsContainer.addView(residentLabel);

                    EditText percentEt = new EditText(requireContext());
                    percentEt.setBackgroundResource(R.drawable.bg_input_dark_round);
                    percentEt.setTextColor(requireContext().getColor(R.color.text_light));
                    percentEt.setHintTextColor(requireContext().getColor(R.color.text_muted));
                    percentEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    percentEt.setHint("0 - 100");
                    percentEt.setTag(resident);
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
                    percentageRowsContainer.addView(percentEt);
                    editableInputs.add(percentEt);
                }

                Runnable updateAutoPreview = () -> {
                    double editableSum = 0.0;
                    for (int i = 0; i < editableInputs.size(); i++) {
                        String resident = editableResidents.get(i);
                        double value = parsePercentInput(editableInputs.get(i).getText() == null
                                ? ""
                                : editableInputs.get(i).getText().toString());
                        if (value < 0.0) value = 0.0;
                        draftPercentages.put(resident, value);
                        editableSum += value;
                    }
                    double remaining = 100.0 - editableSum;
                    autoPercentPreviewTv.setText(
                            displayNameForEmail(autoResidentEmailRef[0])
                                    + ": "
                                    + formatPercent(Math.max(0.0, remaining))
                                    + "% (automático)"
                    );
                    autoPercentPreviewTv.setTextColor(requireContext().getColor(
                            remaining < 0.0 ? R.color.status_danger : R.color.text_muted
                    ));
                };
                updateAutoPreview.run();

                for (EditText percentEt : editableInputs) {
                    percentEt.addTextChangedListener(new TextWatcher() {
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

                isRefreshingSplit[0] = false;
            };

            for (CheckBox check : checks) {
                check.setOnCheckedChangeListener((buttonView, isChecked) -> refreshSplitSection.run());
            }
            splitModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    refreshSplitSection.run();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            autoResidentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isBindingAutoSpinner[0]) return;
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) autoResidentSpinner.getTag();
                    if (options == null || position < 0 || position >= options.size()) return;
                    autoResidentEmailRef[0] = options.get(position);
                    refreshSplitSection.run();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            refreshSplitSection.run();

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Editar inquilinos",
                    "Marca quién vive en esta habitación y ajusta el reparto del alquiler.",
                    scroll,
                    "Cancelar",
                    "Guardar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            shell.confirmBtn.setOnClickListener(view -> {
                List<String> selected = deduplicateStringKeys(collectCheckedMembers(checks, members));

                String splitMode = normalizeRoomSplitMode(
                        splitModeSpinner.getSelectedItemPosition() == 1 ? ROOM_SPLIT_PERCENTAGE : ROOM_SPLIT_EQUAL
                );
                if (selected.size() < 2) {
                    splitMode = ROOM_SPLIT_EQUAL;
                }

                Map<String, Object> splitPercentagesToSave = new LinkedHashMap<>();
                List<String> splitOrderToSave = new ArrayList<>(selected);
                if (ROOM_SPLIT_PERCENTAGE.equals(splitMode)) {
                    @SuppressWarnings("unchecked")
                    List<String> autoOptions = (List<String>) autoResidentSpinner.getTag();
                    if (autoOptions == null || autoOptions.isEmpty()) {
                        NoticeUtils.show(requireContext(), "No se pudo calcular el reparto porcentual");
                        return;
                    }

                    String autoResident = autoResidentEmailRef[0];
                    if (autoResident == null || autoResident.trim().isEmpty() || !selected.contains(autoResident)) {
                        autoResident = autoOptions.get(0);
                    }

                    double editableSum = 0.0;
                    for (int i = 0; i < percentageRowsContainer.getChildCount(); i++) {
                        View child = percentageRowsContainer.getChildAt(i);
                        if (!(child instanceof EditText percentEt)) continue;
                        Object tag = percentEt.getTag();
                        if (!(tag instanceof String resident)) continue;
                        double value = parsePercentInput(percentEt.getText() == null ? "" : percentEt.getText().toString());
                        if (value < 0.0) value = 0.0;
                        editableSum += value;
                        splitPercentagesToSave.put(resident, round2(value));
                    }

                    if (editableSum > 100.0) {
                        NoticeUtils.show(requireContext(), "La suma de porcentajes no puede superar 100");
                        return;
                    }
                    double autoValue = round2(100.0 - editableSum);
                    if (autoValue < 0.0) {
                        NoticeUtils.show(requireContext(), "El porcentaje automático no es válido");
                        return;
                    }
                    splitPercentagesToSave.put(autoResident, autoValue);
                    splitOrderToSave.remove(autoResident);
                    splitOrderToSave.add(autoResident);
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("memberEmails", selected);
                updates.put("memberCount", selected.size());
                updates.put("rentSplitMode", splitMode);
                updates.put("rentSplitPercentages", splitPercentagesToSave);
                updates.put("rentSplitOrder", splitOrderToSave);
                updates.put("updatedByUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                updates.put("updatedAt", FieldValue.serverTimestamp());
                db.collection("rooms_groups").document(row.id)
                        .update(updates)
                        .addOnSuccessListener(done -> {
                            NoticeUtils.show(requireContext(), "Inquilinos actualizados");
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
                                NoticeUtils.show(requireContext(), "Habitación eliminada");
                                refreshWorkspace();
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "No se pudo eliminar la habitación", Toast.LENGTH_SHORT).show());
                }
        );
    }

    private void showExpenseActionDialog() {
        if (expenseActionDialogVisible) return;
        expenseActionDialogVisible = true;
        if (!isVariableTenantUser()) {
            showExpenseActionDialogInternal(null);
            return;
        }
        loadMyPendingDebtRequests(this::showExpenseActionDialogInternal);
    }

    private void showExpenseActionDialogInternal(@Nullable List<PendingDebtRequest> pendingDebts) {
        LinearLayout content = DialogUtils.createVerticalActions(requireContext());
        boolean fixedBilling = BILLING_FIXED.equals(currentBillingModel);
        boolean variableTenant = isVariableTenantUser();
        List<PendingDebtRequest> safePendingDebts = pendingDebts == null ? new ArrayList<>() : pendingDebts;
        boolean hasPendingDebts = !safePendingDebts.isEmpty();

        Button expenseBtn = null;
        if (!fixedBilling) {
            expenseBtn = DialogUtils.createActionButton(requireContext(), "Nuevo gasto", true);
            content.addView(expenseBtn);
        }

        String paymentLabel = variableTenant ? "Pagar pendiente" : "Registrar pago";
        Button paymentBtn = DialogUtils.createActionButton(requireContext(), paymentLabel, fixedBilling);
        if (variableTenant && !hasPendingDebts) {
            paymentBtn.setEnabled(false);
            paymentBtn.setAlpha(0.45f);
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("No tienes pagos pendientes.");
            emptyTv.setTextColor(requireContext().getColor(R.color.text_muted));
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            emptyTv.setPadding(0, dp(8), 0, dp(2));
            content.addView(emptyTv);
        }
        Button exportPdfBtn = DialogUtils.createActionButton(requireContext(), "Exportar PDF", false);
        content.addView(paymentBtn);
        content.addView(exportPdfBtn);

        String subtitle;
        if (variableTenant) {
            subtitle = hasPendingDebts
                    ? "Selecciona un pago pendiente para registrar el justificante."
                    : "No tienes deuda pendiente para registrar pago.";
        } else if (fixedBilling) {
            subtitle = "Registra un pago o exporta PDF.";
        } else {
            subtitle = "Crea un gasto, registra un pago o exporta PDF.";
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nueva acción",
                subtitle,
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        dialog.setOnDismissListener(d -> expenseActionDialogVisible = false);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        if (expenseBtn != null) {
            expenseBtn.setOnClickListener(v -> {
                dialog.dismiss();
                createExpenseDialog();
            });
        }
        paymentBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (variableTenant) {
                if (safePendingDebts.isEmpty()) {
                    NoticeUtils.show(requireContext(), "No tienes pagos pendientes.");
                    return;
                }
                showPendingDebtPickerDialog(safePendingDebts);
                return;
            }
            createPaymentDialog();
        });
        exportPdfBtn.setOnClickListener(v -> {
            dialog.dismiss();
            exportMonthlySummaryPdf();
        });
    }

    private void showCurrentGroupQrDialog() {
        if (currentGroupId == null) {
            NoticeUtils.show(requireContext(), "Selecciona un piso primero");
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
            NoticeUtils.show(requireContext(), "En alquiler fijo no se registran gastos de suministros.");
            return;
        }
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                NoticeUtils.show(requireContext(), "No hay miembros para repartir");
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                if (rooms.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Primero crea al menos una habitación en la pestaña Habitaciones para poder añadir gastos.");
                    return;
                }
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
                pendingTicketUri = null;
                setupCategoryInput((AutoCompleteTextView) form.findViewById(R.id.categoryEt), null);
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
                View scrollableForm = ExpenseDialogs.wrapFormForDialogScroll(requireContext(), form);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Nuevo gasto",
                        "Añade un gasto al piso actual.",
                        scrollableForm,
                        "Cancelar",
                        "Guardar"
                );
                ExpenseDialogs.tuneLongFormShell(shell);
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                ExpenseDialogs.adjustLongFormDialogWindow(requireContext(), dialog);
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
            NoticeUtils.show(requireContext(), "Este piso usa alquiler fijo. No se pueden guardar gastos.");
            return false;
        }
        String concept = ((EditText) form.findViewById(R.id.conceptEt)).getText().toString().trim();
        String amountStr = ((EditText) form.findViewById(R.id.amountEt)).getText().toString().trim();
        String category = normalizeCategoryKey(((EditText) form.findViewById(R.id.categoryEt)).getText().toString());
        String priority = ((Spinner) form.findViewById(R.id.expensePrioritySpinner)).getSelectedItem().toString();
        String dueDateText = ((EditText) form.findViewById(R.id.dueDateEt)).getText().toString().trim();
        if (concept.isEmpty() || amountStr.isEmpty() || currentGroupId == null) return false;
        if (dueDateText.isEmpty()) {
            NoticeUtils.show(requireContext(), "Debes indicar fecha límite");
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            NoticeUtils.show(requireContext(), "Importe no válido");
            return false;
        }
        if (amount <= 0) {
            NoticeUtils.show(requireContext(), "El importe debe ser mayor que 0");
            return false;
        }

        String customSplit = buildCustomSplitFromUi(form, amount, members);
        if (customSplit == null) {
            return false;
        }
        Date dueDate = parseDueDateOrNull(dueDateText);
        if (dueDate == null) {
            NoticeUtils.show(requireContext(), "Fecha inválida. Usa YYYY-MM-DD");
            return false;
        }
        List<RoomOption> selectedRooms;
        if (isRoomScopeSelected(form)) {
            selectedRooms = getRoomsSelectedInSplitRows(form, rooms);
        } else {
            selectedRooms = new ArrayList<>(rooms);
        }
        if (selectedRooms.isEmpty()) {
            NoticeUtils.show(requireContext(), "Selecciona al menos una habitación");
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
        data.put("category", category.isEmpty() ? "otros" : category);
        data.put("priority", expenseService.normalizePriority(priority));
        data.put("ticketUri", pendingTicketUri == null ? "" : pendingTicketUri);
        data.put("dueAt", dueDate);
        data.put("dueDateText", dueDateText);
        data.put("status", STATUS_REQUESTED);
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
        createPaymentDialog(null);
    }

    private void createPaymentDialog(@Nullable PendingDebtRequest pendingDebtRequest) {
        activePendingDebtRequest = pendingDebtRequest;
        pendingTicketUri = null;
        pendingPaymentConfirmBtn = null;
        requirePaymentProofForCurrentDialog = pendingDebtRequest != null || !isOwnerUser();
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                NoticeUtils.show(requireContext(), "No hay miembros disponibles para registrar el pago");
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null, false);
                AutoCompleteTextView paymentCategoryInputEt = form.findViewById(R.id.paymentCategoryInputEt);
                TextView paymentCategoryFixedTv = form.findViewById(R.id.paymentCategoryFixedTv);
                View paymentCategoryFixedContainer = form.findViewById(R.id.paymentCategoryFixedContainer);
                TextView paymentCategoryFixedHintTv = form.findViewById(R.id.paymentCategoryFixedHintTv);
                EditText paymentAmountEt = form.findViewById(R.id.paymentAmountEt);
                if (BILLING_FIXED.equals(currentBillingModel)) {
                    paymentCategoryInputEt.setVisibility(View.GONE);
                    paymentCategoryFixedContainer.setVisibility(View.VISIBLE);
                    paymentCategoryFixedTv.setText(capitalizeTypeLabel(CATEGORY_RENT));
                    paymentCategoryFixedHintTv.setVisibility(View.VISIBLE);
                    setupFixedPaymentConceptSuggestions(form);
                } else {
                    paymentCategoryFixedContainer.setVisibility(View.GONE);
                    paymentCategoryFixedHintTv.setVisibility(View.GONE);
                    paymentCategoryInputEt.setVisibility(View.VISIBLE);
                    setupCategoryInput(paymentCategoryInputEt, null);
                }
                setupPrioritySpinner((Spinner) form.findViewById(R.id.paymentPrioritySpinner), "media");
                setupDateField(form.findViewById(R.id.paymentDueDateEt));
                setupPaymentTargetSelectors(form, members, rooms, hasRoomContext() ? currentRoomId : null);
                setPendingDebtUiLocked(form, false);
                setupPaymentProofControls(form, paymentAmountEt);
                if (pendingDebtRequest != null) {
                    configurePaymentDialogForPendingDebt(form, members, rooms, pendingDebtRequest);
                }

                String dialogTitle = pendingDebtRequest == null ? "Registrar pago" : "Registrar pago pendiente";
                String dialogSubtitle = pendingDebtRequest == null
                        ? "Elige si el pago es para una habitación, un miembro o para todos."
                        : "Revisa los datos, adjunta justificante y env?a el pago.";

                View scrollableForm = ExpenseDialogs.wrapFormForDialogScroll(requireContext(), form);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        dialogTitle,
                        dialogSubtitle,
                        scrollableForm,
                        "Cancelar",
                        "Enviar pago"
                );
                ExpenseDialogs.tuneLongFormShell(shell);
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                ExpenseDialogs.adjustLongFormDialogWindow(requireContext(), dialog);
                pendingPaymentConfirmBtn = shell.confirmBtn;
                updatePaymentConfirmButtonState();
                shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
                shell.confirmBtn.setOnClickListener(v -> {
                    if (savePayment(form, members, rooms)) {
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(v -> {
                    pendingPaymentConfirmBtn = null;
                    requirePaymentProofForCurrentDialog = false;
                    activePendingDebtRequest = null;
                    pendingTicketAmountEt = null;
                    pendingTicketStatusTv = null;
                    pendingTicketUri = null;
                });
            });
        });
    }

    private void setupPaymentProofControls(@NonNull View form, @Nullable EditText amountInputEt) {
        Button attachBtn = form.findViewById(R.id.attachTicketBtnPayment);
        TextView proofStatusTv = form.findViewById(R.id.paymentProofStatusTv);
        if (attachBtn == null || proofStatusTv == null) return;
        pendingTicketAmountEt = amountInputEt;
        pendingTicketStatusTv = proofStatusTv;
        proofStatusTv.setText(pendingTicketUri == null || pendingTicketUri.isEmpty()
                ? "Sin justificante adjunto"
                : "Justificante adjunto");
        attachBtn.setOnClickListener(v -> ticketPickerLauncher.launch("image/*"));
    }

    private void updatePaymentConfirmButtonState() {
        if (pendingPaymentConfirmBtn == null) return;
        boolean enabled = !requirePaymentProofForCurrentDialog
                || (pendingTicketUri != null && !pendingTicketUri.trim().isEmpty());
        pendingPaymentConfirmBtn.setEnabled(enabled);
        pendingPaymentConfirmBtn.setAlpha(enabled ? 1f : 0.45f);
    }

    private boolean isVariableTenantUser() {
        return !isOwnerUser();
    }

    private void loadMyPendingDebtRequests(@NonNull PendingDebtCallback callback) {
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        db.collection("payment_deadlines")
                .whereEqualTo("groupId", currentGroupId)
                .whereEqualTo("debtorEmail", myEmail)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(result -> {
                    List<PendingDebtRequest> rows = new ArrayList<>();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        String concept = doc.getString("concept");
                        if (concept == null || concept.trim().isEmpty()) concept = "Pago pendiente";
                        Double amount = doc.getDouble("amount");
                        String creditorEmail = doc.getString("creditorEmail");
                        String dueDateText = doc.getString("dueDateText");
                        Date dueAt = doc.getDate("dueAt");
                        String priority = doc.getString("priority");
                        rows.add(new PendingDebtRequest(
                                doc.getId(),
                                concept,
                                amount == null ? 0.0 : amount,
                                creditorEmail == null ? "" : creditorEmail.toLowerCase(Locale.ROOT),
                                dueDateText == null ? "" : dueDateText,
                                dueAt,
                                priority == null ? "media" : priority.toLowerCase(Locale.ROOT),
                                doc
                        ));
                    }
                    rows.sort((a, b) -> {
                        long ta = a.dueAt == null ? Long.MAX_VALUE : a.dueAt.getTime();
                        long tb = b.dueAt == null ? Long.MAX_VALUE : b.dueAt.getTime();
                        return Long.compare(ta, tb);
                    });
                    callback.onLoaded(rows);
                })
                .addOnFailureListener(e -> callback.onLoaded(new ArrayList<>()));
    }

    private void showPendingDebtPickerDialog(@NonNull List<PendingDebtRequest> pendingDebts) {
        ViewGroup content = DialogUtils.createVerticalActions(requireContext());
        int maxItems = pendingDebts.size();
        for (int i = 0; i < maxItems; i++) {
            PendingDebtRequest debt = pendingDebts.get(i);
            String creditorLabel = debt.creditorEmail == null || debt.creditorEmail.trim().isEmpty()
                    ? "Sin acreedor"
                    : memberReferenceInline(debt.creditorEmail);
            String label = debt.concept + " - " + formatCurrency(debt.amount) + " - A " + creditorLabel;
            Button itemBtn = DialogUtils.createActionButton(requireContext(), label, i == 0);
            itemBtn.setOnClickListener(v -> createPaymentDialog(debt));
            content.addView(itemBtn);
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Tus pagos pendientes",
                "Selecciona un pendiente para registrar el pago.",
                content,
                "Cerrar",
                null
        );
        AlertDialog pickerDialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> pickerDialog.dismiss());
    }

    private void configurePaymentDialogForPendingDebt(
            @NonNull View form,
            @NonNull List<String> members,
            @NonNull List<RoomOption> rooms,
            @NonNull PendingDebtRequest debt
    ) {
        EditText amountEt = form.findViewById(R.id.paymentAmountEt);
        EditText conceptEt = form.findViewById(R.id.paymentConceptEt);
        EditText dueDateEt = form.findViewById(R.id.paymentDueDateEt);
        Spinner targetTypeSpinner = form.findViewById(R.id.paymentTargetTypeSpinner);
        Spinner prioritySpinner = form.findViewById(R.id.paymentPrioritySpinner);
        AutoCompleteTextView categoryEt = form.findViewById(R.id.paymentCategoryInputEt);
        TextView proofStatusTv = form.findViewById(R.id.paymentProofStatusTv);
        TextView memberLabelTv = form.findViewById(R.id.paymentMemberLabelTv);

        amountEt.setText(formatPercent(debt.amount));
        conceptEt.setText(debt.concept);
        if (debt.dueDateText != null && !debt.dueDateText.trim().isEmpty()) {
            dueDateEt.setText(debt.dueDateText);
        } else if (debt.dueAt != null) {
            dueDateEt.setText(DUE_DATE_FORMAT.format(debt.dueAt));
        } else {
            dueDateEt.setText(DUE_DATE_FORMAT.format(new Date()));
        }

        if (categoryEt.getText() == null || categoryEt.getText().toString().trim().isEmpty()) {
            categoryEt.setText("Otros", false);
        }
        amountEt.setEnabled(false);
        conceptEt.setEnabled(false);
        dueDateEt.setEnabled(false);
        targetTypeSpinner.setEnabled(false);
        targetTypeSpinner.setClickable(false);
        targetTypeSpinner.setAlpha(0.65f);
        if (memberLabelTv != null) {
            memberLabelTv.setText("A quién pagas:");
        }

        int priorityIndex = indexOfPriorityType(debt.priority);
        if (priorityIndex >= 0) {
            prioritySpinner.setSelection(priorityIndex);
        }

        rebindPaymentMemberLines(form, members, rooms.size(), Collections.singletonList(debt.creditorEmail));
        int memberTargetIndex = indexOfPaymentTargetType("miembro");
        if (memberTargetIndex < 0) memberTargetIndex = 1;
        targetTypeSpinner.setSelection(memberTargetIndex);
        setPendingDebtUiLocked(form, true);
        updatePaymentTargetSection(form, memberTargetIndex, members.size(), rooms.size());

        Button addMemberLineBtn = form.findViewById(R.id.addPaymentMemberLineBtn);
        Button removeMemberLineBtn = form.findViewById(R.id.removePaymentMemberLineBtn);
        addMemberLineBtn.setVisibility(View.GONE);
        removeMemberLineBtn.setVisibility(View.GONE);
        LinearLayout membersContainer = form.findViewById(R.id.paymentMembersContainer);
        for (int i = 0; i < membersContainer.getChildCount(); i++) {
            View child = membersContainer.getChildAt(i);
            EditText rowAmountEt = child.findViewById(R.id.memberAmountEt);
            if (rowAmountEt != null) {
                rowAmountEt.setText(formatPercent(debt.amount));
                rowAmountEt.setEnabled(false);
                rowAmountEt.setFocusable(false);
                rowAmountEt.setFocusableInTouchMode(false);
            }
            child.setEnabled(false);
            child.setClickable(false);
            child.setAlpha(0.8f);
        }

        if (proofStatusTv != null) {
            proofStatusTv.setText("Debes adjuntar justificante para enviar este pago.");
        }
    }

    private int indexOfPriorityType(@Nullable String value) {
        if (value == null) return -1;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < PRIORITY_TYPES.length; i++) {
            if (PRIORITY_TYPES[i].equals(normalized)) return i;
        }
        return -1;
    }

    private int indexOfPaymentTargetType(@NonNull String expectedNormalizedType) {
        String expected = expectedNormalizedType.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < PAYMENT_TARGET_TYPES.length; i++) {
            String normalized = safeLowerText(PAYMENT_TARGET_TYPES[i]);
            if (expected.equals(normalized)) return i;
        }
        return -1;
    }

    private void setupFixedPaymentConceptSuggestions(View form) {
        if (!BILLING_FIXED.equals(currentBillingModel) || currentGroupId == null) return;
        AutoCompleteTextView conceptInput = form.findViewById(R.id.paymentConceptEt);
        if (conceptInput == null) return;

        db.collection("payments")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(result.getDocuments());
                    docs.sort((a, b) -> {
                        Date da = a.getDate("createdAt");
                        Date dbDate = b.getDate("createdAt");
                        if (da == null && dbDate == null) return 0;
                        if (da == null) return 1;
                        if (dbDate == null) return -1;
                        return dbDate.compareTo(da);
                    });

                    LinkedHashMap<String, String> uniqueByLower = new LinkedHashMap<>();
                    for (DocumentSnapshot doc : docs) {
                        String category = doc.getString("category");
                        if (category == null || !CATEGORY_RENT.equalsIgnoreCase(category.trim())) continue;
                        String concept = doc.getString("concept");
                        if (concept == null) continue;
                        String clean = concept.trim();
                        if (clean.isEmpty()) continue;
                        String key = clean.toLowerCase(Locale.ROOT);
                        if (!uniqueByLower.containsKey(key)) {
                            uniqueByLower.put(key, clean);
                        }
                        if (uniqueByLower.size() >= 8) break;
                    }

                    List<String> suggestions = new ArrayList<>(uniqueByLower.values());
                    if (suggestions.isEmpty()) return;

                    conceptInput.setAdapter(new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            suggestions
                    ));
                    conceptInput.setThreshold(1);
                    conceptInput.setOnClickListener(v -> conceptInput.showDropDown());
                    conceptInput.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) conceptInput.post(conceptInput::showDropDown);
                    });

                    if (conceptInput.getText().toString().trim().isEmpty()) {
                        conceptInput.setHint("Sugerido: " + suggestions.get(0));
                    }
                });
    }

    private void setupCategoryInput(@NonNull AutoCompleteTextView input, @Nullable String selectedCategory) {
        String normalizedSelected = normalizeCategoryKey(selectedCategory);
        if (!normalizedSelected.isEmpty()) {
            input.setText(capitalizeTypeLabel(normalizedSelected), false);
        }
        input.setThreshold(1);
        input.setOnClickListener(v -> input.showDropDown());
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) input.post(input::showDropDown);
        });

        loadGroupCategorySuggestions(false, categories -> {
            if (!isAdded()) return;
            List<String> labels = new ArrayList<>();
            for (String category : categories) {
                if (category == null || category.trim().isEmpty()) continue;
                labels.add(capitalizeTypeLabel(category));
            }
            input.setAdapter(buildLightDropdownAdapter(labels.toArray(new String[0])));
        });
    }

    private void loadGroupCategorySuggestions(boolean forceRefresh, @NonNull CategorySuggestionsRepository.Callback callback) {
        categorySuggestionsRepository.getSuggestions(currentGroupId, forceRefresh, callback);
    }

    private String normalizeCategoryKey(@Nullable String rawCategory) {
        return expenseService.normalizeCategory(rawCategory);
    }

    private void setupPaymentTargetSelectors(View form, List<String> members, List<RoomOption> rooms, @Nullable String defaultRoomId) {
        Spinner targetTypeSpinner = form.findViewById(R.id.paymentTargetTypeSpinner);
        LinearLayout roomsContainer = form.findViewById(R.id.paymentRoomsContainer);
        Button addRoomLineBtn = form.findViewById(R.id.addPaymentRoomLineBtn);
        Button removeRoomLineBtn = form.findViewById(R.id.removePaymentRoomLineBtn);
        LinearLayout membersContainer = form.findViewById(R.id.paymentMembersContainer);
        Button addMemberLineBtn = form.findViewById(R.id.addPaymentMemberLineBtn);
        Button removeMemberLineBtn = form.findViewById(R.id.removePaymentMemberLineBtn);
        TextView memberLabelTv = form.findViewById(R.id.paymentMemberLabelTv);

        targetTypeSpinner.setAdapter(buildLightSpinnerAdapter(PAYMENT_TARGET_TYPES));
        if (memberLabelTv != null) {
            memberLabelTv.setText("Miembro(s):");
        }

        rebindPaymentRoomLines(form, rooms, members.size(), null);
        addRoomLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectPaymentRoomLineKeys(roomsContainer);
            currentKeys.add(firstAvailableRoomKey(rooms, currentKeys));
            rebindPaymentRoomLines(form, rooms, members.size(), currentKeys);
            updatePaymentTargetSection(form, targetTypeSpinner.getSelectedItemPosition(), members.size(), rooms.size());
        });
        removeRoomLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectPaymentRoomLineKeys(roomsContainer);
            if (currentKeys.size() > 1) {
                currentKeys.remove(currentKeys.size() - 1);
            }
            rebindPaymentRoomLines(form, rooms, members.size(), currentKeys);
            updatePaymentTargetSection(form, targetTypeSpinner.getSelectedItemPosition(), members.size(), rooms.size());
        });

        rebindPaymentMemberLines(form, members, rooms.size(), null);
        addMemberLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectPaymentMemberLineKeys(membersContainer);
            currentKeys.add(firstAvailableMemberKey(members, currentKeys));
            rebindPaymentMemberLines(form, members, rooms.size(), currentKeys);
            updatePaymentTargetSection(form, targetTypeSpinner.getSelectedItemPosition(), members.size(), rooms.size());
        });
        removeMemberLineBtn.setOnClickListener(v -> {
            List<String> currentKeys = collectPaymentMemberLineKeys(membersContainer);
            if (currentKeys.size() > 1) {
                currentKeys.remove(currentKeys.size() - 1);
            }
            rebindPaymentMemberLines(form, members, rooms.size(), currentKeys);
            updatePaymentTargetSection(form, targetTypeSpinner.getSelectedItemPosition(), members.size(), rooms.size());
        });

        targetTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePaymentTargetSection(form, position, members.size(), rooms.size());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int roomIndex = findRoomIndexById(rooms, defaultRoomId);
        if (roomIndex >= 0) {
            List<String> initialRoomKeys = new ArrayList<>();
            initialRoomKeys.add(rooms.get(roomIndex).id);
            rebindPaymentRoomLines(form, rooms, members.size(), initialRoomKeys);
            targetTypeSpinner.setSelection(0);
        } else {
            targetTypeSpinner.setSelection(1);
        }
    }

    private void updatePaymentTargetSection(View form, int targetTypePosition, int membersCount, int roomsCount) {
        TextView memberLabelTv = form.findViewById(R.id.paymentMemberLabelTv);
        LinearLayout membersContainer = form.findViewById(R.id.paymentMembersContainer);
        Button addMemberLineBtn = form.findViewById(R.id.addPaymentMemberLineBtn);
        Button removeMemberLineBtn = form.findViewById(R.id.removePaymentMemberLineBtn);
        TextView roomLabelTv = form.findViewById(R.id.paymentRoomLabelTv);
        LinearLayout roomsContainer = form.findViewById(R.id.paymentRoomsContainer);
        Button addRoomLineBtn = form.findViewById(R.id.addPaymentRoomLineBtn);
        Button removeRoomLineBtn = form.findViewById(R.id.removePaymentRoomLineBtn);

        if (isPendingDebtUiLocked(form)) {
            roomLabelTv.setVisibility(View.GONE);
            roomsContainer.setVisibility(View.GONE);
            addRoomLineBtn.setVisibility(View.GONE);
            removeRoomLineBtn.setVisibility(View.GONE);

            memberLabelTv.setVisibility(View.VISIBLE);
            membersContainer.setVisibility(View.VISIBLE);
            addMemberLineBtn.setVisibility(View.GONE);
            removeMemberLineBtn.setVisibility(View.GONE);
            return;
        }

        boolean showRoomSection = targetTypePosition == 0;
        boolean showMemberSection = targetTypePosition == 1;

        roomLabelTv.setVisibility(showRoomSection ? View.VISIBLE : View.GONE);
        roomsContainer.setVisibility(showRoomSection ? View.VISIBLE : View.GONE);
        int remainingRooms = roomsCount - roomsContainer.getChildCount();
        addRoomLineBtn.setVisibility(showRoomSection && remainingRooms > 0 ? View.VISIBLE : View.GONE);
        removeRoomLineBtn.setVisibility(showRoomSection ? View.VISIBLE : View.GONE);
        boolean canRemoveRoom = showRoomSection && roomsContainer.getChildCount() > 1;
        removeRoomLineBtn.setEnabled(canRemoveRoom);
        removeRoomLineBtn.setAlpha(canRemoveRoom ? 1f : 0.45f);

        memberLabelTv.setVisibility(showMemberSection ? View.VISIBLE : View.GONE);
        membersContainer.setVisibility(showMemberSection ? View.VISIBLE : View.GONE);

        int remainingMembers = membersCount - membersContainer.getChildCount();
        addMemberLineBtn.setVisibility(showMemberSection && remainingMembers > 0 ? View.VISIBLE : View.GONE);
        removeMemberLineBtn.setVisibility(showMemberSection ? View.VISIBLE : View.GONE);
        boolean canRemove = showMemberSection && membersContainer.getChildCount() > 1;
        removeMemberLineBtn.setEnabled(canRemove);
        removeMemberLineBtn.setAlpha(canRemove ? 1f : 0.45f);
    }

    private void setPendingDebtUiLocked(@NonNull View form, boolean locked) {
        form.setTag(R.id.paymentProofStatusTv, locked);
    }

    private boolean isPendingDebtUiLocked(@NonNull View form) {
        Object tag = form.getTag(R.id.paymentProofStatusTv);
        return tag instanceof Boolean && (Boolean) tag;
    }

    private void rebindPaymentMemberLines(View form, List<String> members, int roomsCount, @Nullable List<String> seedKeys) {
        LinearLayout container = form.findViewById(R.id.paymentMembersContainer);
        Map<String, String> existingAmounts = collectPaymentLineAmounts(container);
        List<String> selectedKeys = seedKeys == null ? collectPaymentMemberLineKeys(container) : new ArrayList<>(seedKeys);
        if (selectedKeys.isEmpty()) {
            selectedKeys.add(firstAvailableMemberKey(members, Collections.emptyList()));
        }
        selectedKeys = deduplicateStringKeys(selectedKeys);
        container.removeAllViews();

        for (int i = 0; i < selectedKeys.size(); i++) {
            String selectedKey = selectedKeys.get(i);
            View row = buildPaymentLineRow(
                    members,
                    members,
                    selectedKeys,
                    i,
                    selectedKey,
                    existingAmounts.getOrDefault(selectedKey, "")
            );
            Spinner spinner = row.findViewById(R.id.memberSpinner);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    rebindPaymentMemberLines(form, members, roomsCount, null);
                    Spinner targetTypeSpinner = form.findViewById(R.id.paymentTargetTypeSpinner);
                    updatePaymentTargetSection(form, targetTypeSpinner.getSelectedItemPosition(), members.size(), roomsCount);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            container.addView(row);
        }
    }

    private View buildPaymentLineRow(
            List<String> optionKeysSource,
            List<String> optionLabelsSource,
            List<String> selectedKeys,
            int rowIndex,
            String selectedKey,
            String amountText
    ) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_split_row, null, false);
        Spinner spinner = row.findViewById(R.id.memberSpinner);
        EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
        List<String> optionKeys = new ArrayList<>();
        List<String> optionLabels = new ArrayList<>();
        for (int idx = 0; idx < optionKeysSource.size(); idx++) {
            String key = optionKeysSource.get(idx);
            String label = optionLabelsSource.get(idx);
            boolean selectedElsewhere = false;
            for (int i = 0; i < selectedKeys.size(); i++) {
                if (i == rowIndex) continue;
                if (key.equals(selectedKeys.get(i))) {
                    selectedElsewhere = true;
                    break;
                }
            }
            if (!selectedElsewhere || key.equals(selectedKey)) {
                optionKeys.add(key);
                optionLabels.add(label);
            }
        }
        if (optionKeys.isEmpty() && !optionKeysSource.isEmpty()) {
            optionKeys.add(optionKeysSource.get(0));
            optionLabels.add(optionLabelsSource.get(0));
        }
        spinner.setTag(optionKeys);
        spinner.setAdapter(buildLightSpinnerAdapter(optionLabels.toArray(new String[0])));
        int selectedIndex = optionKeys.indexOf(selectedKey);
        spinner.setSelection(selectedIndex >= 0 ? selectedIndex : 0);
        memberAmountEt.setText(amountText == null ? "" : amountText);
        return row;
    }

    private List<String> collectPaymentMemberLineKeys(LinearLayout container) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Spinner spinner = child.findViewById(R.id.memberSpinner);
            if (spinner == null) continue;
            @SuppressWarnings("unchecked")
            List<String> optionKeys = (List<String>) spinner.getTag();
            int selected = spinner.getSelectedItemPosition();
            if (optionKeys == null || selected < 0 || selected >= optionKeys.size()) continue;
            keys.add(optionKeys.get(selected));
        }
        return keys;
    }

    private void rebindPaymentRoomLines(View form, List<RoomOption> rooms, int membersCount, @Nullable List<String> seedKeys) {
        LinearLayout container = form.findViewById(R.id.paymentRoomsContainer);
        Map<String, String> existingAmounts = collectPaymentLineAmounts(container);
        List<String> selectedKeys = seedKeys == null ? collectPaymentRoomLineKeys(container) : new ArrayList<>(seedKeys);
        if (selectedKeys.isEmpty()) {
            selectedKeys.add(firstAvailableRoomKey(rooms, Collections.emptyList()));
        }
        selectedKeys = deduplicateRoomKeys(selectedKeys);
        container.removeAllViews();
        List<String> roomKeys = new ArrayList<>();
        List<String> roomLabels = new ArrayList<>();
        for (RoomOption room : rooms) {
            roomKeys.add(room.id);
            roomLabels.add(room.name);
        }

        for (int i = 0; i < selectedKeys.size(); i++) {
            String selectedKey = selectedKeys.get(i);
            View row = buildPaymentLineRow(
                    roomKeys,
                    roomLabels,
                    selectedKeys,
                    i,
                    selectedKey,
                    existingAmounts.getOrDefault(selectedKey, "")
            );
            Spinner spinner = row.findViewById(R.id.memberSpinner);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    rebindPaymentRoomLines(form, rooms, membersCount, null);
                    Spinner targetTypeSpinner = form.findViewById(R.id.paymentTargetTypeSpinner);
                    updatePaymentTargetSection(form, targetTypeSpinner.getSelectedItemPosition(), membersCount, rooms.size());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            container.addView(row);
        }
    }

    private List<String> collectPaymentRoomLineKeys(LinearLayout container) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Spinner spinner = child.findViewById(R.id.memberSpinner);
            if (spinner == null) continue;
            @SuppressWarnings("unchecked")
            List<String> optionKeys = (List<String>) spinner.getTag();
            int selected = spinner.getSelectedItemPosition();
            if (optionKeys == null || selected < 0 || selected >= optionKeys.size()) continue;
            keys.add(optionKeys.get(selected));
        }
        return keys;
    }

    private Map<String, String> collectPaymentLineAmounts(@NonNull LinearLayout container) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Spinner spinner = child.findViewById(R.id.memberSpinner);
            EditText amountEt = child.findViewById(R.id.memberAmountEt);
            if (spinner == null || amountEt == null) continue;
            @SuppressWarnings("unchecked")
            List<String> optionKeys = (List<String>) spinner.getTag();
            int selected = spinner.getSelectedItemPosition();
            if (optionKeys == null || selected < 0 || selected >= optionKeys.size()) continue;
            String key = optionKeys.get(selected);
            String value = amountEt.getText() == null ? "" : amountEt.getText().toString().trim();
            out.put(key, value);
        }
        return out;
    }

    private List<PaymentService.PaymentTarget> resolvePaymentTargetsByMemberLines(
            @NonNull List<String> selectedMemberEmails,
            @NonNull Map<String, String> memberLineAmounts,
            @NonNull String fromEmail
    ) {
        List<PaymentService.PaymentTarget> targets = new ArrayList<>();
        for (String emailRaw : selectedMemberEmails) {
            String email = safeLowerText(emailRaw);
            if (email.isEmpty() || email.equals(safeLowerText(fromEmail))) continue;
            String amountText = memberLineAmounts.getOrDefault(emailRaw, memberLineAmounts.getOrDefault(email, ""));
            double lineAmount;
            try {
                lineAmount = amountText == null || amountText.trim().isEmpty() ? 0.0 : Double.parseDouble(amountText.trim());
            } catch (NumberFormatException e) {
                lineAmount = -1.0;
            }
            if (lineAmount <= 0.0) continue;
            targets.add(new PaymentService.PaymentTarget(
                    email,
                    null,
                    null,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    lineAmount
            ));
        }
        return targets;
    }

    private List<PaymentService.PaymentTarget> resolvePaymentTargetsByRoomLines(
            @NonNull List<String> selectedRoomIds,
            @NonNull Map<String, String> roomLineAmounts,
            @NonNull List<RoomOption> rooms,
            @NonNull String fromEmail
    ) {
        List<PaymentService.PaymentTarget> targets = new ArrayList<>();
        Map<String, PaymentService.PaymentTarget> byEmail = new LinkedHashMap<>();
        Map<String, Double> weightedByEmail = new LinkedHashMap<>();
        Map<String, RoomOption> roomById = new LinkedHashMap<>();
        for (RoomOption room : rooms) {
            roomById.put(room.id, room);
        }
        for (String roomId : selectedRoomIds) {
            RoomOption room = roomById.get(roomId);
            if (room == null) continue;
            String amountText = roomLineAmounts.getOrDefault(roomId, "");
            double roomAmount;
            try {
                roomAmount = amountText == null || amountText.trim().isEmpty() ? 0.0 : Double.parseDouble(amountText.trim());
            } catch (NumberFormatException e) {
                roomAmount = -1.0;
            }
            if (roomAmount <= 0.0) continue;

            Map<String, Double> residentPercents = resolveRoomResidentPercentagesForAllocation(room);
            if (residentPercents == null) {
                return new ArrayList<>();
            }
            if (residentPercents.isEmpty()) continue;

            for (Map.Entry<String, Double> entry : residentPercents.entrySet()) {
                String email = safeLowerText(entry.getKey());
                if (email.isEmpty() || email.equals(safeLowerText(fromEmail))) continue;
                double residentShare = (roomAmount * Math.max(0.0, entry.getValue())) / 100.0;
                if (residentShare <= 0.0) continue;
                weightedByEmail.put(email, weightedByEmail.getOrDefault(email, 0.0) + residentShare);
                byEmail.putIfAbsent(email, new PaymentService.PaymentTarget(
                        email,
                        room.id,
                        room.name,
                        Collections.singletonList(room.id),
                        Collections.singletonList(room.name),
                        0.0
                ));
            }
        }
        for (Map.Entry<String, PaymentService.PaymentTarget> entry : byEmail.entrySet()) {
            String email = entry.getKey();
            PaymentService.PaymentTarget base = entry.getValue();
            targets.add(new PaymentService.PaymentTarget(
                    base.toEmail,
                    base.roomId,
                    base.roomName,
                    base.roomIds,
                    base.roomNames,
                    weightedByEmail.getOrDefault(email, 0.0)
            ));
        }
        return targets;
    }

    private List<PaymentService.PaymentRoom> toPaymentRooms(List<RoomOption> rooms) {
        List<PaymentService.PaymentRoom> out = new ArrayList<>();
        for (RoomOption room : rooms) {
            out.add(new PaymentService.PaymentRoom(
                    room.id,
                    room.name,
                    room.memberEmails,
                    room.monthlyCost,
                    room.rentSplitMode,
                    room.rentSplitPercentages
            ));
        }
        return out;
    }

    private boolean savePayment(View form, List<String> members, List<RoomOption> rooms) {
        if (currentGroupId == null) return false;
        String amountStr = ((EditText) form.findViewById(R.id.paymentAmountEt)).getText().toString().trim();
        String concept = ((EditText) form.findViewById(R.id.paymentConceptEt)).getText().toString().trim();
        String category = "";
        if (!BILLING_FIXED.equals(currentBillingModel)) {
            category = normalizeCategoryKey(((EditText) form.findViewById(R.id.paymentCategoryInputEt)).getText().toString());
        }
        String priority = ((Spinner) form.findViewById(R.id.paymentPrioritySpinner)).getSelectedItem().toString();
        String dueDateText = ((EditText) form.findViewById(R.id.paymentDueDateEt)).getText().toString().trim();
        String targetType = ((Spinner) form.findViewById(R.id.paymentTargetTypeSpinner)).getSelectedItem().toString();
        LinearLayout memberLinesContainer = form.findViewById(R.id.paymentMembersContainer);
        LinearLayout roomLinesContainer = form.findViewById(R.id.paymentRoomsContainer);
        List<String> selectedMemberEmails = collectPaymentMemberLineKeys(memberLinesContainer);
        List<String> selectedRoomIds = collectPaymentRoomLineKeys(roomLinesContainer);
        Map<String, String> memberLineAmounts = collectPaymentLineAmounts(memberLinesContainer);
        Map<String, String> roomLineAmounts = collectPaymentLineAmounts(roomLinesContainer);
        Date dueDate = parseDueDateOrNull(dueDateText);
        PaymentService.ValidationResult validation = paymentService.validateAmountAndRequiredFields(amountStr, dueDateText, dueDate);
        if (!validation.valid) {
            NoticeUtils.show(requireContext(), validation.message);
            return false;
        }
        boolean missingProof = pendingTicketUri == null || pendingTicketUri.trim().isEmpty();
        if ((activePendingDebtRequest != null || !isOwnerUser()) && missingProof) {
            NoticeUtils.show(requireContext(), "Adjunta una foto del justificante para enviar el pago");
            return false;
        }
        double amount = validation.amount;

        String fromEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        PendingDebtRequest selectedPendingDebt = activePendingDebtRequest;
        if (selectedPendingDebt != null) {
            targetType = "Miembro";
            selectedRoomIds.clear();
            selectedMemberEmails.clear();
            if (selectedPendingDebt.creditorEmail != null && !selectedPendingDebt.creditorEmail.trim().isEmpty()) {
                selectedMemberEmails.add(selectedPendingDebt.creditorEmail.trim().toLowerCase(Locale.ROOT));
            }
        }
        String normalizedCategory = paymentService.normalizeCategory(
                BILLING_FIXED.equals(currentBillingModel),
                category,
                CATEGORY_RENT
        );
        List<PaymentService.PaymentTarget> targets;
        String normalizedTargetType = safeLowerText(targetType);
        if (selectedPendingDebt != null) {
            targets = paymentService.resolveTargets(
                    targetType,
                    members,
                    toPaymentRooms(rooms),
                    selectedMemberEmails,
                    selectedRoomIds,
                    fromEmail,
                    BILLING_FIXED.equals(currentBillingModel)
            );
        } else if ("miembro".equals(normalizedTargetType)) {
            targets = resolvePaymentTargetsByMemberLines(selectedMemberEmails, memberLineAmounts, fromEmail);
        } else if ("habitacion".equals(normalizedTargetType) || "habitación".equals(normalizedTargetType)) {
            targets = resolvePaymentTargetsByRoomLines(selectedRoomIds, roomLineAmounts, rooms, fromEmail);
        } else {
            targets = paymentService.resolveTargets(
                    targetType,
                    members,
                    toPaymentRooms(rooms),
                    selectedMemberEmails,
                    selectedRoomIds,
                    fromEmail,
                    BILLING_FIXED.equals(currentBillingModel)
            );
        }
        if (targets.isEmpty()) {
            NoticeUtils.show(requireContext(), "No hay destinatarios válidos para este pago");
            return false;
        }
        if (!"todos".equals(normalizedTargetType) && selectedPendingDebt == null) {
            double linesTotal = 0.0;
            for (PaymentService.PaymentTarget target : targets) {
                linesTotal += Math.max(0.0, target.shareWeight);
            }
            if (Math.abs(round2(linesTotal) - round2(amount)) > 0.01) {
                NoticeUtils.show(requireContext(), "La suma de líneas debe coincidir con el importe total");
                return false;
            }
        }

        String safePriority = paymentService.normalizePriority(priority);
        String suggestedConcept = "";
        if (BILLING_FIXED.equals(currentBillingModel)) {
            CharSequence hint = ((EditText) form.findViewById(R.id.paymentConceptEt)).getHint();
            if (hint != null) {
                String hintText = hint.toString().trim();
                if (hintText.startsWith("Sugerido:")) {
                    suggestedConcept = hintText.substring("Sugerido:".length()).trim();
                }
            }
        }
        String safeConcept = paymentService.resolveConcept(concept, suggestedConcept);
        String safeTargetType = targetType;
        if (("Habitación".equals(targetType) || "Habitación".equals(targetType)) && selectedRoomIds.size() > 1) {
            safeTargetType = "x_habitacion";
        }
        List<PaymentService.PaymentWrite> writes = paymentService.buildWrites(
                currentGroupId,
                amount,
                fromEmail,
                normalizedCategory,
                safePriority,
                dueDate,
                dueDateText,
                safeConcept,
                safeTargetType,
                targets
        );
        if (writes.isEmpty()) {
            NoticeUtils.show(requireContext(), "No se han podido generar pagos válidos");
            return false;
        }
        if (selectedPendingDebt != null) {
            if (writes.size() != 1) {
                NoticeUtils.show(requireContext(), "El pago pendiente debe enviarse en una sola línea");
                return false;
            }
            PaymentService.PaymentWrite write = writes.get(0);
            if (!amountsMatchToCent(write.splitAmount, selectedPendingDebt.amount)) {
                NoticeUtils.show(requireContext(), "El importe no coincide con tu pendiente exacto");
                return false;
            }
            if (!safeLowerText(write.toEmail).equals(safeLowerText(selectedPendingDebt.creditorEmail))) {
                NoticeUtils.show(requireContext(), "El destinatario no coincide con el acreedor de la deuda");
                return false;
            }
        }
        String proofUri = pendingTicketUri == null ? "" : pendingTicketUri.trim();
        WriteBatch batch = db.batch();
        List<Map<String, Object>> createdPayments = new ArrayList<>();
        for (PaymentService.PaymentWrite write : writes) {
            var paymentRef = db.collection("payments").document();
            write.data.put("ticketUri", proofUri);
            write.data.put("status", selectedPendingDebt == null ? STATUS_REQUESTED : STATUS_PENDING);
            if (selectedPendingDebt != null) {
                write.data.put("sourceDebtId", selectedPendingDebt.id);
                write.data.put("sourceDebtType", "payment_deadline");
                String sourceType = selectedPendingDebt.snapshot.getString("sourceType");
                String sourceId = selectedPendingDebt.snapshot.getString("sourceId");
                write.data.put("sourceType", sourceType == null ? "" : sourceType);
                write.data.put("sourceId", sourceId == null ? "" : sourceId);
            }
            batch.set(paymentRef, write.data);
            Map<String, Object> created = new HashMap<>();
            created.put("paymentId", paymentRef.getId());
            created.put("toEmail", write.toEmail);
            created.put("splitAmount", write.splitAmount);
            createdPayments.add(created);
        }
        batch.commit().addOnSuccessListener(v -> {
            if (selectedPendingDebt != null) {
                Map<String, Object> debtUpdate = new HashMap<>();
                debtUpdate.put("status", STATUS_SUBMITTED);
                debtUpdate.put("submittedAt", FieldValue.serverTimestamp());
                debtUpdate.put("proofUri", proofUri);
                selectedPendingDebt.snapshot.getReference().update(debtUpdate);
                updateExpenseStatusAfterDeadlineChange(selectedPendingDebt.id);
            }
            for (Map<String, Object> created : createdPayments) {
                String paymentId = String.valueOf(created.get("paymentId"));
                String toEmail = String.valueOf(created.get("toEmail"));
                double splitAmount = created.get("splitAmount") instanceof Number
                        ? ((Number) created.get("splitAmount")).doubleValue()
                        : 0.0;
                logActivity("payment_created", "Pago a " + toEmail, splitAmount, "payment");
                if (selectedPendingDebt == null) {
                    syncPaymentDeadline(paymentId, safeConcept, splitAmount, fromEmail, toEmail, dueDate, safePriority);
                }
            }
            loadExpenses();
            loadFinancialViews();
        });
        return true;
    }

    private void createReminderDialog() {
        if (currentGroupId == null) {
            NoticeUtils.show(requireContext(), "Selecciona un piso primero");
            return;
        }
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                NoticeUtils.show(requireContext(), "No hay miembros en este piso");
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reminder, null, false);
                setupDateField(form.findViewById(R.id.reminderStartDateEt));
                setupDateField(form.findViewById(R.id.reminderEndDateEt));
                setupReminderFormControls(form, members, rooms, hasRoomContext() ? currentRoomId : null);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Nuevo recordatorio",
                        "Define el concepto, la frecuencia y los destinatarios del recordatorio.",
                        form,
                        null,
                        "Guardar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.confirmBtn.setOnClickListener(v -> {
                    String concept = ((EditText) form.findViewById(R.id.reminderTitleEt)).getText().toString().trim();
                    String dateText = ((EditText) form.findViewById(R.id.reminderStartDateEt)).getText().toString().trim();
                    if (concept.isEmpty() || dateText.isEmpty()) {
                        NoticeUtils.show(requireContext(), "Completa concepto y fecha");
                        return;
                    }
                    Date startAt = parseReminderStartDateOrNull(dateText);
                    if (startAt == null) {
                        NoticeUtils.show(requireContext(), "Fecha inv\u00e1lida. Usa YYYY-MM-DD");
                        return;
                    }

                    ReminderIntervalConfig intervalConfig = resolveReminderInterval(form);
                    if (intervalConfig == null) return;
                    String endDateText = ((EditText) form.findViewById(R.id.reminderEndDateEt)).getText().toString().trim();
                    Date endAt = null;
                    if (!"unico".equals(intervalConfig.intervalKey) && !endDateText.isEmpty()) {
                        endAt = parseReminderStartDateOrNull(endDateText);
                        if (endAt == null) {
                            NoticeUtils.show(requireContext(), "Fecha de finalizaci\u00f3n inv\u00e1lida. Usa YYYY-MM-DD");
                            return;
                        }
                        if (endAt.before(startAt)) {
                            NoticeUtils.show(requireContext(), "La fecha de finalizaci\u00f3n no puede ser anterior al inicio");
                            return;
                        }
                    }

                    ReminderTargetConfig targetConfig = resolveReminderTargets(form, members, rooms);
                    if (targetConfig == null || targetConfig.targetEmails.isEmpty()) {
                        NoticeUtils.show(requireContext(), "No hay destinatarios v\u00e1lidos");
                        return;
                    }

                    String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
                    Map<String, Object> data = reminderService.buildReminderData(
                            concept,
                            intervalConfig.intervalKey,
                            intervalConfig.intervalDays,
                            startAt,
                            dateText,
                            endAt,
                            endDateText,
                            targetConfig.targetType,
                            targetConfig.targetEmails,
                            targetConfig.memberEmail,
                            targetConfig.primaryRoomId,
                            targetConfig.primaryRoomName,
                            targetConfig.roomIds,
                            targetConfig.roomNames,
                            FirebaseAuth.getInstance().getCurrentUser().getUid(),
                            myEmail,
                            currentGroupId,
                            currentGroupName
                    );
                    data.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("reminders").add(data).addOnSuccessListener(ref -> {
                        int reminderCode = Math.abs(("manual_" + ref.getId()).hashCode());
                        db.collection("reminders").document(ref.getId()).update("reminderCode", reminderCode);
                        if (targetConfig.targetEmails.contains(myEmail)) {
                            scheduleManualReminder(reminderCode, concept, startAt.getTime(), intervalConfig.intervalMs);
                        }
                        loadReminders();
                        NoticeUtils.show(requireContext(), "Recordatorio guardado");
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
        TextView customDaysLabelTv = form.findViewById(R.id.reminderCustomDaysLabelTv);
        EditText customDaysEt = form.findViewById(R.id.reminderCustomDaysEt);
        TextView startDateLabelTv = form.findViewById(R.id.reminderStartDateLabelTv);
        TextView endDateLabelTv = form.findViewById(R.id.reminderEndDateLabelTv);
        EditText endDateEt = form.findViewById(R.id.reminderEndDateEt);
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int visibility = position == 4 ? View.VISIBLE : View.GONE;
                customDaysEt.setVisibility(visibility);
                customDaysLabelTv.setVisibility(visibility);
                boolean isSingle = position == 0;
                startDateLabelTv.setText(isSingle ? "Fecha (AAAA-MM-DD):" : "Fecha de inicio (AAAA-MM-DD):");
                int endVisibility = isSingle ? View.GONE : View.VISIBLE;
                endDateLabelTv.setVisibility(endVisibility);
                endDateEt.setVisibility(endVisibility);
                if (isSingle) {
                    endDateEt.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        intervalSpinner.setSelection(1);

        setupReminderTargetSelectors(form, members, rooms, defaultRoomId);
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
        // Legacy single-select spinners stay hidden; now we always use line-based selectors.
        memberSpinner.setVisibility(View.GONE);
        roomSpinner.setVisibility(View.GONE);

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
            List<String> initialRoomKeys = new ArrayList<>();
            initialRoomKeys.add(rooms.get(roomIndex).id);
            rebindReminderRoomLines(form, rooms, initialRoomKeys);
            targetTypeSpinner.setSelection(1);
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

        boolean showMemberLines = targetTypePosition == 0;
        boolean showRoomLines = targetTypePosition == 1;

        memberSpinner.setVisibility(View.GONE);
        roomSpinner.setVisibility(View.GONE);
        roomsContainer.setVisibility(showRoomLines ? View.VISIBLE : View.GONE);
        addLineBtn.setVisibility(showRoomLines ? View.VISIBLE : View.GONE);
        removeLineBtn.setVisibility(showRoomLines ? View.VISIBLE : View.GONE);
        removeLineBtn.setEnabled(showRoomLines && roomsContainer.getChildCount() > 1);
        removeLineBtn.setAlpha(removeLineBtn.isEnabled() ? 1f : 0.45f);
        membersContainer.setVisibility(showMemberLines ? View.VISIBLE : View.GONE);
        addMemberLineBtn.setVisibility(showMemberLines ? View.VISIBLE : View.GONE);
        removeMemberLineBtn.setVisibility(showMemberLines ? View.VISIBLE : View.GONE);
        removeMemberLineBtn.setEnabled(showMemberLines && membersContainer.getChildCount() > 1);
        removeMemberLineBtn.setAlpha(removeMemberLineBtn.isEnabled() ? 1f : 0.45f);
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
            NoticeUtils.show(requireContext(), "Indica cada cu\u00e1ntos d\u00edas");
            return null;
        }
        int customDays;
        try {
            customDays = Integer.parseInt(customDaysText);
        } catch (NumberFormatException e) {
            NoticeUtils.show(requireContext(), "Intervalo personalizado no v\u00e1lido");
            return null;
        }
        if (customDays <= 0) {
            NoticeUtils.show(requireContext(), "El intervalo personalizado debe ser mayor que 0");
            return null;
        }
        long intervalMs = customDays * 24L * 60L * 60L * 1000L;
        return new ReminderIntervalConfig("personalizado", customDays, intervalMs);
    }

    @Nullable
    private ReminderTargetConfig resolveReminderTargets(View form, List<String> members, List<RoomOption> rooms) {
        int targetTypeIndex = ((Spinner) form.findViewById(R.id.reminderTargetTypeSpinner)).getSelectedItemPosition();
        if (targetTypeIndex == 0) {
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
        if (targetTypeIndex == 1) {
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
        if (targetTypeIndex == 2) {
            List<String> normalizedMembers = new ArrayList<>();
            for (String email : deduplicateStringKeys(members)) {
                normalizedMembers.add(email.toLowerCase(Locale.ROOT));
            }
            if (normalizedMembers.isEmpty()) return null;
            return new ReminderTargetConfig(
                    "todos_inquilinos",
                    normalizedMembers,
                    "",
                    "",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    ""
            );
        }
        return null;
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
        String startDateText = doc.getString("startDateText");
        String endDateText = doc.getString("endDateText");
        String interval = doc.getString("interval");
        Long intervalDays = doc.getLong("intervalDays");
        String targetLabel = buildReminderTargetLabelForDetail(doc);

        String intervalKey = interval == null ? "semanal" : interval.toLowerCase(Locale.ROOT);
        String intervalLabel = intervalKey;
        if ("unico".equals(intervalLabel)) {
            intervalLabel = "\u00fanico";
        }
        if ("personalizado".equals(intervalLabel) && intervalDays != null && intervalDays > 0) {
            intervalLabel = "cada " + intervalDays + " d\u00edas";
        }
        String prefix = "Para: " + targetLabel + " · Frecuencia: " + capitalizeTypeLabel(intervalLabel);
        if (startDateText == null || startDateText.trim().isEmpty()) return prefix;
        if ("unico".equals(intervalKey)) return prefix + " · Fecha: " + startDateText;
        if (endDateText != null && !endDateText.trim().isEmpty()) {
            return prefix + " · Desde: " + startDateText + " · Hasta: " + endDateText;
        }
        return prefix + " · Desde: " + startDateText;
    }
    private void loadExpenses() {
        if (currentGroupId == null) return;
        DecimalFormat df = new DecimalFormat("0.00");
        expenseRows.clear();

        var expenseQuery = db.collection("expenses").whereEqualTo("groupId", currentGroupId);

        expenseQuery.get().addOnSuccessListener(result -> {
            for (DocumentSnapshot doc : result.getDocuments()) {
                if (hasRoomContext() && !matchesExpenseWithCurrentRoom(doc)) continue;
                Double amountValue = doc.getDouble("amount");
                String concept = doc.getString("concept");
                String payerEmail = doc.getString("payerEmail");
                String payerLabel = memberReferenceInline(payerEmail);
                String category = doc.getString("category");
                String roomName = doc.getString("roomName");
                String dueDateText = doc.getString("dueDateText");
                String status = normalizeFlowStatus(doc.getString("status"));
                String statusLabel = statusLabel(status);
                String subtitle = (payerEmail == null ? "Gasto compartido" : "Pagado por " + payerLabel)
                        + (roomName == null || roomName.trim().isEmpty() ? "" : " - hab. " + roomName)
                        + (category == null || category.isEmpty() ? "" : " - " + category)
                        + " - " + statusLabel
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
                    String toLabel = memberReferenceInline(toEmail);
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
                    String normalizedStatus = normalizeFlowStatus(status);
                    boolean confirmed = STATUS_CONFIRMED.equals(normalizedStatus);
                    boolean overdue = (STATUS_PENDING.equals(normalizedStatus) || STATUS_REQUESTED.equals(normalizedStatus))
                            && dueAt != null
                            && dueAt.getTime() < System.currentTimeMillis();
                    String statusLabel = statusLabel(normalizedStatus);
                    if (overdue && !confirmed) statusLabel = "Pendiente (vencido)";
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
                if (!isVariableTenantUser()) {
                    expensesAdapter.notifyDataSetChanged();
                    return;
                }
                loadMyPendingDebtRequests(pendingDebts -> {
                    if (pendingDebts.isEmpty()) {
                        expenseRows.add(new WorkspaceRow(
                                "pending_debt_empty",
                                ROW_TYPE_PENDING_DEBT,
                                "No tienes pagos pendientes",
                                "Cuando tengas una deuda asignada aparecerá aquí¿",
                                "-",
                                null
                        ));
                    }
                    for (PendingDebtRequest debt : pendingDebts) {
                        if (!passesPendingDebtFilters(debt)) continue;
                        String dueLabel = debt.dueDateText == null || debt.dueDateText.trim().isEmpty()
                                ? ""
                                : " - vence " + debt.dueDateText;
                        String subtitle = "A " + memberReferenceInline(debt.creditorEmail)
                                + " - Solicitado"
                                + dueLabel;
                        expenseRows.add(new WorkspaceRow(
                                debt.id,
                                ROW_TYPE_PENDING_DEBT,
                                debt.concept,
                                subtitle,
                                df.format(debt.amount) + " EUR",
                                debt.snapshot
                        ));
                    }
                    expensesAdapter.notifyDataSetChanged();
                });
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
                renderQuickBalances();
                return;
            }
            String billingModel = normalizeBillingModel(groupDoc.getString("billingModel"));
            currentBillingModel = billingModel;

            Map<String, Double> net = new HashMap<>();
            for (String member : members) {
                net.put(member, 0.0);
            }

            if (BILLING_FIXED.equals(billingModel)) {
                loadFixedRentFinancials(groupDoc, members, net);
            } else {
                loadVariableRentFinancials(members, net);
            }
        });
    }

    private void loadVariableRentFinancials(List<String> members, Map<String, Double> net) {
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
                Map<String, Double> roomPercentages = resolveRoomResidentPercentages(room);
                for (Map.Entry<String, Double> shareEntry : roomPercentages.entrySet()) {
                    String resident = shareEntry.getKey();
                    double percent = shareEntry.getValue();
                    double residentShare = roomCost * (percent / 100.0);
                    if (net.containsKey(resident)) {
                        net.put(resident, net.getOrDefault(resident, 0.0) - residentShare);
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
            weights.put(member, 0.0);
        }

        for (RoomOption room : rooms) {
            if (hasRoomContext() && !room.id.equals(currentRoomId)) continue;
            if (room.memberEmails.isEmpty()) continue;

            double roomWeight = room.monthlyCost > 0.0 ? room.monthlyCost : 1.0;
            Map<String, Double> roomPercentages = resolveRoomResidentPercentages(room);
            for (Map.Entry<String, Double> shareEntry : roomPercentages.entrySet()) {
                String resident = shareEntry.getKey();
                if (!weights.containsKey(resident)) continue;
                double residentWeight = roomWeight * (Math.max(0.0, shareEntry.getValue()) / 100.0);
                weights.put(resident, weights.getOrDefault(resident, 0.0) + residentWeight);
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
        renderQuickBalances();
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

    private void renderQuickBalances() {
        if (!isAdded() || quickBalancesContainer == null || quickBalancesCard == null) return;
        quickBalancesContainer.removeAllViews();

        String myEmailRaw = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String myEmail = myEmailRaw == null ? "" : myEmailRaw.toLowerCase(Locale.ROOT);
        double teDebenTotal = 0.0;
        double debesTotal = 0.0;

        for (BalanceMovement movement : lastBalanceMovements) {
            if (myEmail.equals(movement.toEmail)) {
                teDebenTotal += movement.amount;
            } else if (myEmail.equals(movement.fromEmail)) {
                debesTotal += movement.amount;
            }
        }

        addQuickSummaryCard("Te deben", teDebenTotal);
        addQuickSummaryCard("Debes", debesTotal);
        updateQuickBalancesVisibility();
    }

    private void addQuickSummaryCard(String title, double amount) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_tab_default);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.weight = 1f;
        params.leftMargin = dp(6);
        params.rightMargin = dp(6);
        card.setLayoutParams(params);

        TextView titleTv = new TextView(requireContext());
        titleTv.setText(title);
        titleTv.setTextColor(requireContext().getColor(R.color.text_muted));
        titleTv.setTextSize(12f);
        titleTv.setTypeface(titleTv.getTypeface(), android.graphics.Typeface.BOLD);

        TextView amountTv = new TextView(requireContext());
        amountTv.setText(formatCurrency(amount));
        amountTv.setTextColor(requireContext().getColor(R.color.text_light));
        amountTv.setTextSize(18f);
        amountTv.setTypeface(amountTv.getTypeface(), android.graphics.Typeface.BOLD);
        amountTv.setPadding(0, dp(6), 0, 0);

        card.addView(titleTv);
        card.addView(amountTv);
        quickBalancesContainer.addView(card);
    }

    private void updateQuickBalancesVisibility() {
        if (quickBalancesCard == null || quickBalancesContainer == null) return;
        boolean visibleTab = TAB_EXPENSES.equals(currentTab) || TAB_REMINDERS.equals(currentTab) || TAB_MANAGEMENT.equals(currentTab);
        boolean hasRows = quickBalancesContainer.getChildCount() > 0;
        quickBalancesCard.setVisibility(currentGroupId != null && visibleTab && hasRows ? View.VISIBLE : View.GONE);
        applyBottomContentInset();
    }

    private void applyBottomContentInset() {
        if (!isAdded()) return;
        int baseInset = dp(12);
        int overlayInset = 0;
        if (workspaceCtaLayout != null && workspaceCtaLayout.getVisibility() == View.VISIBLE) {
            int ctaHeight = workspaceCtaLayout.getHeight();
            if (ctaHeight <= 0) {
                ctaHeight = dp(180);
            }
            overlayInset = ctaHeight + dp(16);
        }
        int finalInset = baseInset + overlayInset;
        applyListInset(expensesLv, finalInset);
        applyListInset(saldosLv, finalInset);
        applyListInset(remindersLv, finalInset);
        if (tenantsContainer != null) {
            tenantsContainer.setPadding(
                    tenantsContainer.getPaddingLeft(),
                    tenantsContainer.getPaddingTop(),
                    tenantsContainer.getPaddingRight(),
                    finalInset
            );
        }
    }

    private void applyListInset(@Nullable ListView listView, int insetBottom) {
        if (listView == null) return;
        listView.setPadding(
                listView.getPaddingLeft(),
                listView.getPaddingTop(),
                listView.getPaddingRight(),
                insetBottom
        );
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
        List<String> paymentRoomIds = castStrings(doc.get("roomIds"));
        if (paymentRoomIds.contains(currentRoomId)) {
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

    private Map<String, Double> castPercentages(Object raw) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> map)) return out;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String email = entry.getKey().toString().trim().toLowerCase(Locale.ROOT);
            if (email.isEmpty()) continue;
            double value;
            if (entry.getValue() instanceof Number number) {
                value = number.doubleValue();
            } else {
                try {
                    value = Double.parseDouble(entry.getValue().toString().trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            if (value < 0.0) value = 0.0;
            out.put(email, value);
        }
        return out;
    }

    private List<String> collectCheckedMembers(List<CheckBox> checks, List<String> members) {
        List<String> selected = new ArrayList<>();
        int size = Math.min(checks.size(), members.size());
        for (int i = 0; i < size; i++) {
            if (checks.get(i).isChecked()) {
                selected.add(members.get(i));
            }
        }
        return selected;
    }

    private String normalizeRoomSplitMode(@Nullable String raw) {
        if (raw == null) return ROOM_SPLIT_EQUAL;
        return ROOM_SPLIT_PERCENTAGE.equalsIgnoreCase(raw.trim()) ? ROOM_SPLIT_PERCENTAGE : ROOM_SPLIT_EQUAL;
    }

    private double parsePercentInput(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private String formatPercent(double value) {
        double rounded = round2(value);
        if (Math.abs(rounded - Math.rint(rounded)) < 0.0001) {
            return String.valueOf((int) Math.rint(rounded));
        }
        return new DecimalFormat("0.##").format(rounded);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Double> resolveRoomResidentPercentages(@NonNull RoomOption room) {
        Map<String, Double> out = new LinkedHashMap<>();
        List<String> residents = deduplicateStringKeys(room.memberEmails);
        if (residents.isEmpty()) return out;
        if (residents.size() == 1) {
            out.put(residents.get(0), 100.0);
            return out;
        }

        String splitMode = normalizeRoomSplitMode(room.rentSplitMode);
        if (!ROOM_SPLIT_PERCENTAGE.equals(splitMode)) {
            double equal = 100.0 / residents.size();
            for (String resident : residents) {
                out.put(resident, equal);
            }
            return out;
        }

        double providedSum = 0.0;
        for (String resident : residents) {
            double value = room.rentSplitPercentages.getOrDefault(resident, 0.0);
            if (value > 0.0) {
                providedSum += value;
            }
        }
        if (providedSum <= 0.0) {
            double equal = 100.0 / residents.size();
            for (String resident : residents) {
                out.put(resident, equal);
            }
            return out;
        }

        for (String resident : residents) {
            double value = room.rentSplitPercentages.getOrDefault(resident, 0.0);
            if (value < 0.0) value = 0.0;
            out.put(resident, (value * 100.0) / providedSum);
        }
        return out;
    }

    @Nullable
    private Map<String, Double> resolveRoomResidentPercentagesForAllocation(@NonNull RoomOption room) {
        List<String> residents = deduplicateStringKeys(room.memberEmails);
        if (residents.isEmpty()) return new LinkedHashMap<>();
        if (residents.size() == 1) {
            Map<String, Double> single = new LinkedHashMap<>();
            single.put(residents.get(0), 100.0);
            return single;
        }

        String splitMode = normalizeRoomSplitMode(room.rentSplitMode);
        if (ROOM_SPLIT_PERCENTAGE.equals(splitMode)) {
            double providedSum = 0.0;
            for (String resident : residents) {
                double value = room.rentSplitPercentages.getOrDefault(resident, 0.0);
                if (value > 0.0) providedSum += value;
            }
            if (providedSum <= 0.0) {
                NoticeUtils.show(
                        requireContext(),
                        "La habitación " + room.name + " está en reparto personalizado. Configura sus porcentajes antes de continuar."
                );
                return null;
            }
        }
        return resolveRoomResidentPercentages(room);
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
                        String displayName = userDoc.getString("name");
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = userDoc.getString("displayName");
                        }
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

    private String memberReferenceInline(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Sin datos";
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String name = displayNameForEmail(normalized);
        if (name == null || name.trim().isEmpty()) {
            return normalized;
        }
        if (name.equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return name + " (" + normalized + ")";
    }

    private String formatMembersDetailed(List<String> memberEmails) {
        if (memberEmails == null || memberEmails.isEmpty()) {
            return "Sin datos";
        }
        StringBuilder out = new StringBuilder();
        int index = 1;
        for (String email : memberEmails) {
            if (email == null) continue;
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) continue;
            String name = displayNameForEmail(normalized);
            if (name == null || name.trim().isEmpty()) {
                name = normalized;
            }
            if (out.length() > 0) out.append("\n");
            out.append("Miembro ").append(index).append(":");
            out.append("\nNombre: ").append(name);
            out.append("\nCorreo: ").append(normalized);
            index++;
        }
        return out.length() == 0 ? "Sin datos" : out.toString();
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

    private void showReminderLongPressActions(@NonNull WorkspaceRow row) {
        if (row.snapshot == null) return;
        boolean canDelete = canDeleteReminder(row.snapshot);

        ViewGroup content = DialogUtils.createVerticalActions(requireContext());
        Button infoBtn = DialogUtils.createActionButton(requireContext(), "Ver información", true);
        content.addView(infoBtn);

        Button deleteOneBtn = null;
        Button deleteAllBtn = null;
        if (canDelete) {
            deleteOneBtn = DialogUtils.createActionButton(requireContext(), "Eliminar solo hoy", false);
            deleteAllBtn = DialogUtils.createActionButton(requireContext(), "Eliminar todos los días", false);
            content.addView(deleteOneBtn);
            content.addView(deleteAllBtn);
        }

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Acciones del recordatorio",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());

        infoBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showReminderInfoDialog(row);
        });
        if (deleteOneBtn != null) {
            Button finalDeleteOneBtn = deleteOneBtn;
            finalDeleteOneBtn.setOnClickListener(v -> {
                dialog.dismiss();
                requestReminderDeleteToday(row);
            });
        }
        if (deleteAllBtn != null) {
            Button finalDeleteAllBtn = deleteAllBtn;
            finalDeleteAllBtn.setOnClickListener(v -> {
                dialog.dismiss();
                requestReminderDeletionAllDays(row);
            });
        }
    }

    private void showReminderInfoDialog(@NonNull WorkspaceRow row) {
        if (row.snapshot == null) return;
        DocumentSnapshot doc = row.snapshot;
        String groupName = currentGroupName == null || currentGroupName.trim().isEmpty() ? "Piso actual" : currentGroupName;
        String targetTitle = buildReminderTargetTitleForDetail(doc);
        List<String> targetItems = buildReminderTargetItemsForDetail(doc);
        String intervalLabel = buildReminderIntervalLabel(doc);
        String fromDate = resolveReminderDateText(doc, "startDateText", "startAt");
        String toDate = resolveReminderDateText(doc, "endDateText", "endAt");
        if ("Sin fecha".equals(toDate)) {
            toDate = "Sin fecha fin";
        }

        View content = DialogUtils.createReminderDetailView(
                requireContext(),
                groupName,
                targetTitle,
                targetItems,
                intervalLabel,
                fromDate,
                toDate
        );
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Detalle del recordatorio",
                content,
                null,
                "Cerrar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
    }

    private boolean canDeleteReminder(@NonNull DocumentSnapshot reminderDoc) {
        String ownerUid = reminderDoc.getString("ownerUid");
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return isOwnerUser() || (ownerUid != null && ownerUid.equals(myUid));
    }

    private void requestReminderDeleteToday(@NonNull WorkspaceRow row) {
        if (row.snapshot == null || !canDeleteReminder(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo quien lo creó o el propietario puede eliminarlo");
            return;
        }
        showDeleteConfirmation(
                "Eliminar solo hoy",
                "Solo se eliminará la ocurrencia de hoy. Las siguientes se mantienen.",
                () -> deleteReminderOnlyToday(row)
        );
    }

    private void requestReminderDeletionAllDays(@NonNull WorkspaceRow row) {
        if (row.snapshot == null || !canDeleteReminder(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo quien lo creó o el propietario puede eliminarlo");
            return;
        }
        showDeleteConfirmation(
                "Eliminar todos los días",
                "Se eliminará el recordatorio completo y todas sus repeticiones.",
                () -> deleteReminder(row)
        );
    }

    private void deleteReminderOnlyToday(@NonNull WorkspaceRow row) {
        if (row.snapshot == null) return;
        DocumentSnapshot doc = row.snapshot;
        if (!canDeleteReminder(doc)) {
            NoticeUtils.show(requireContext(), "Solo quien lo creó o el propietario puede eliminarlo");
            return;
        }

        long intervalMs = resolveReminderIntervalMs(doc);
        Date startAt = doc.getDate("startAt");
        Date endAt = doc.getDate("endAt");
        Long reminderCode = doc.getLong("reminderCode");

        // Si no es periódico o faltan datos base, lo tratamos como eliminación completa.
        if (intervalMs <= 0L || startAt == null) {
            deleteReminder(row);
            return;
        }

        long todayStart = startOfTodayMs();
        long todayEnd = endOfTodayMs();
        Long todayOccurrence = findFirstOccurrenceOnOrAfter(startAt.getTime(), intervalMs, todayStart);
        if (todayOccurrence == null || todayOccurrence > todayEnd) {
            NoticeUtils.show(requireContext(), "Este recordatorio no tiene ejecución hoy");
            return;
        }

        if (endAt != null && todayOccurrence > endAt.getTime()) {
            NoticeUtils.show(requireContext(), "El recordatorio ya terminó");
            return;
        }

        long nextAfterToday = todayOccurrence + intervalMs;
        if (endAt != null && nextAfterToday > endAt.getTime()) {
            deleteReminder(row);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        Date nextDate = new Date(nextAfterToday);
        updates.put("startAt", nextDate);
        updates.put("startDateText", DUE_DATE_FORMAT.format(nextDate));
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("reminders").document(row.id)
                .update(updates)
                .addOnSuccessListener(v -> {
                    if (reminderCode != null) {
                        ReminderScheduler.cancel(requireContext(), reminderCode.intValue());
                        if (shouldScheduleReminderForCurrentUser(doc)) {
                            String title = doc.getString("title");
                            if (title == null || title.trim().isEmpty()) title = row.title;
                            ReminderScheduler.schedule(
                                    requireContext(),
                                    reminderCode.intValue(),
                                    "FlatShare: " + title,
                                    "Recordatorio pendiente",
                                    nextAfterToday,
                                    intervalMs
                            );
                        }
                    }
                    NoticeUtils.show(requireContext(), "Recordatorio eliminado para hoy");
                    loadReminders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "No se pudo actualizar el recordatorio", Toast.LENGTH_SHORT).show()
                );
    }

    @Nullable
    private Long findFirstOccurrenceOnOrAfter(long firstTriggerMs, long intervalMs, long thresholdMs) {
        if (intervalMs <= 0L) {
            return firstTriggerMs >= thresholdMs ? firstTriggerMs : null;
        }
        if (firstTriggerMs >= thresholdMs) return firstTriggerMs;
        long diff = thresholdMs - firstTriggerMs;
        long jumps = diff / intervalMs;
        long candidate = firstTriggerMs + (jumps * intervalMs);
        while (candidate < thresholdMs) {
            candidate += intervalMs;
        }
        return candidate;
    }

    private long resolveReminderIntervalMs(@NonNull DocumentSnapshot doc) {
        String interval = safeLowerText(doc.getString("interval"));
        Long intervalDays = doc.getLong("intervalDays");
        if ("unico".equals(interval)) return 0L;
        if ("diario".equals(interval)) return 24L * 60L * 60L * 1000L;
        if ("semanal".equals(interval)) return 7L * 24L * 60L * 60L * 1000L;
        if ("mensual".equals(interval)) return 30L * 24L * 60L * 60L * 1000L;
        if ("personalizado".equals(interval) && intervalDays != null && intervalDays > 0) {
            return intervalDays * 24L * 60L * 60L * 1000L;
        }
        return 0L;
    }

    private boolean shouldScheduleReminderForCurrentUser(@NonNull DocumentSnapshot doc) {
        String myEmail = safeLowerText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        List<String> targetEmails = castEmails(doc.get("targetEmails"));
        for (String email : targetEmails) {
            if (myEmail.equals(safeLowerText(email))) return true;
        }
        return false;
    }

    private long startOfTodayMs() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long endOfTodayMs() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private String resolveReminderDateText(@NonNull DocumentSnapshot doc, @NonNull String textField, @NonNull String dateField) {
        String text = doc.getString(textField);
        if (text != null && !text.trim().isEmpty()) return text.trim();
        Date date = doc.getDate(dateField);
        if (date == null) return "Sin fecha";
        return DUE_DATE_FORMAT.format(date);
    }

    private String buildReminderIntervalLabel(@NonNull DocumentSnapshot doc) {
        String interval = safeLowerText(doc.getString("interval"));
        Long intervalDays = doc.getLong("intervalDays");
        if ("unico".equals(interval)) return "único";
        if ("diario".equals(interval)) return "Diario";
        if ("semanal".equals(interval)) return "Semanal";
        if ("mensual".equals(interval)) return "Mensual";
        if ("personalizado".equals(interval) && intervalDays != null && intervalDays > 0) {
            return "Cada " + intervalDays + " días";
        }
        return interval.isEmpty() ? "Sin definir" : capitalizeTypeLabel(interval);
    }

    private String buildReminderTargetLabelForDetail(@NonNull DocumentSnapshot doc) {
        String targetType = safeLowerText(doc.getString("targetType"));
        if ("miembro".equals(targetType)) {
            return "Por miembro: " + memberReferenceInline(doc.getString("targetMemberEmail"));
        }
        if ("x_miembro".equals(targetType)) {
            List<String> targetEmails = castEmails(doc.get("targetEmails"));
            if (targetEmails.isEmpty()) return "Por miembros";
            List<String> names = new ArrayList<>();
            for (String email : targetEmails) names.add(memberReferenceInline(email));
            return "Por miembros: " + String.join(", ", names);
        }
        if ("habitacion".equals(targetType)) {
            String roomName = doc.getString("roomName");
            return "Por habitación: " + (roomName == null || roomName.trim().isEmpty() ? "Sin nombre" : roomName.trim());
        }
        if ("x_habitacion".equals(targetType)) {
            List<String> roomNames = castStrings(doc.get("roomNames"));
            if (roomNames.isEmpty()) return "Por habitaciones";
            return "Por habitaciones: " + String.join(", ", roomNames);
        }
        if ("todos_inquilinos".equals(targetType)) return "Para todos los inquilinos";
        return "Para todos los miembros";
    }

    private String buildReminderTargetTitleForDetail(@NonNull DocumentSnapshot doc) {
        String targetType = safeLowerText(doc.getString("targetType"));
        if ("habitacion".equals(targetType) || "x_habitacion".equals(targetType)) return "Dirigido a habitaciones";
        if ("miembro".equals(targetType) || "x_miembro".equals(targetType)) return "Dirigido a personas";
        return "Dirigido a";
    }

    private List<String> buildReminderTargetItemsForDetail(@NonNull DocumentSnapshot doc) {
        String targetType = safeLowerText(doc.getString("targetType"));
        List<String> items = new ArrayList<>();
        if ("miembro".equals(targetType)) {
            items.add(memberReferenceInline(doc.getString("targetMemberEmail")));
            return items;
        }
        if ("x_miembro".equals(targetType)) {
            List<String> targetEmails = castEmails(doc.get("targetEmails"));
            for (String email : targetEmails) items.add(memberReferenceInline(email));
            return items;
        }
        if ("habitacion".equals(targetType)) {
            String roomName = doc.getString("roomName");
            items.add(roomName == null || roomName.trim().isEmpty() ? "Sin nombre" : roomName.trim());
            return items;
        }
        if ("x_habitacion".equals(targetType)) {
            items.addAll(castStrings(doc.get("roomNames")));
            return items;
        }
        if ("todos_inquilinos".equals(targetType)) {
            items.add("Todos los inquilinos");
            return items;
        }
        items.add("Todos los miembros");
        return items;
    }

    private void showRowDetail(WorkspaceRow row) {
        if (ROW_TYPE_PENDING_DEBT.equals(row.type)) {
            if (row.snapshot == null) {
                NoticeUtils.show(requireContext(), "No tienes pagos pendientes.");
                return;
            }
            String concept = row.snapshot.getString("concept");
            Double amount = row.snapshot.getDouble("amount");
            String creditorEmail = row.snapshot.getString("creditorEmail");
            String dueDateText = row.snapshot.getString("dueDateText");
            Date dueAt = row.snapshot.getDate("dueAt");
            String priority = row.snapshot.getString("priority");
            PendingDebtRequest debt = new PendingDebtRequest(
                    row.id,
                    concept == null || concept.trim().isEmpty() ? "Pago pendiente" : concept,
                    amount == null ? 0.0 : amount,
                    creditorEmail == null ? "" : creditorEmail.toLowerCase(Locale.ROOT),
                    dueDateText == null ? "" : dueDateText,
                    dueAt,
                    priority == null ? "media" : priority.toLowerCase(Locale.ROOT),
                    row.snapshot
            );
            createPaymentDialog(debt);
            return;
        }

        if ("reminder".equals(row.type)) {
            if (row.snapshot == null) return;
            boolean canDelete = canDeleteReminder(row.snapshot);
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
            detailBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showReminderInfoDialog(row);
            });
            if (deleteBtn != null) {
                deleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    requestReminderDeletionAllDays(row);
                });
            }
            return;
        }

        if ("payment".equals(row.type)) {
            if (row.snapshot == null) return;
            String status = normalizeFlowStatus(row.snapshot.getString("status"));
            boolean canToggle = canTogglePaymentStatus(row.snapshot);
            boolean canDelete = canDeletePayment(row.snapshot);
            String targetStatus;
            if (STATUS_REQUESTED.equals(status)) {
                targetStatus = STATUS_PENDING;
            } else if (STATUS_PENDING.equals(status)) {
                targetStatus = STATUS_CONFIRMED;
            } else {
                targetStatus = STATUS_PENDING;
            }
            showPaymentInfoDialog(row, status, canToggle, canDelete, targetStatus);
            return;
        }

        if (row.snapshot != null) {
            String payerId = row.snapshot.getString("payerId");
            if (!canManageExpense(payerId)) {
                View content = DialogUtils.createInfoRowsView(requireContext(), buildExpenseInfoRows(row));
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
            if (!canEditOrDeleteExpense(row.snapshot)) {
                View content = DialogUtils.createInfoRowsView(requireContext(), buildExpenseInfoRows(row));
                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        row.title,
                        "Solo puedes editar o eliminar en estado Solicitado (rojo).",
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

        View content = DialogUtils.createInfoRowsView(requireContext(), buildExpenseInfoRows(row));
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

    private void showPaymentInfoDialog(
            @NonNull WorkspaceRow row,
            @NonNull String status,
            boolean canToggle,
            boolean canDelete,
            @NonNull String targetStatus
    ) {
        View content = DialogUtils.createInfoRowsView(requireContext(), buildPaymentInfoRows(row));
        boolean editableStatus = STATUS_REQUESTED.equals(status) || STATUS_PENDING.equals(status);
        boolean showActions = editableStatus && (canToggle || canDelete);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                row.title,
                "Detalle del pago",
                content,
                showActions && canDelete ? "Borrar" : null,
                showActions && canToggle ? "Cambiar estado" : "Cerrar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        if (showActions && canDelete && shell.cancelBtn != null) {
            shell.cancelBtn.setOnClickListener(v -> {
                dialog.dismiss();
                requestPaymentDeletion(row);
            });
        }
        if (showActions && canToggle && shell.confirmBtn != null) {
            shell.confirmBtn.setOnClickListener(v -> {
                dialog.dismiss();
                requestPaymentStatusChange(row, targetStatus);
            });
        } else if (shell.confirmBtn != null) {
            shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
        }
    }

    private Map<String, String> buildPaymentInfoRows(@NonNull WorkspaceRow row) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Nombre del piso", currentGroupName == null || currentGroupName.trim().isEmpty() ? "Piso actual" : currentGroupName);
        if (row.snapshot == null) {
            rows.put("Detalle", row.subtitle == null ? "Sin datos" : row.subtitle);
            rows.put("Importe", row.amount == null ? "0.00 EUR" : row.amount);
            return rows;
        }

        String concept = row.snapshot.getString("concept");
        String fromEmail = row.snapshot.getString("fromEmail");
        String toEmail = row.snapshot.getString("toEmail");
        String roomName = row.snapshot.getString("roomName");
        String dueDateText = row.snapshot.getString("dueDateText");
        Date dueAt = row.snapshot.getDate("dueAt");
        if ((dueDateText == null || dueDateText.trim().isEmpty()) && dueAt != null) {
            dueDateText = DUE_DATE_FORMAT.format(dueAt);
        }
        String status = normalizeFlowStatus(row.snapshot.getString("status"));

        rows.put("Concepto", concept == null || concept.trim().isEmpty() ? "Pago" : concept.trim());
        rows.put("De", fromEmail == null || fromEmail.trim().isEmpty() ? "Sin datos" : memberReferenceInline(fromEmail));
        rows.put("Para", toEmail == null || toEmail.trim().isEmpty() ? "Sin datos" : memberReferenceInline(toEmail));
        rows.put("Habitación", roomName == null || roomName.trim().isEmpty() ? "Varias habitaciones" : roomName.trim());
        rows.put("Estado", statusLabel(status));
        rows.put("Vence", dueDateText == null || dueDateText.trim().isEmpty() ? "Sin fecha" : dueDateText.trim());
        rows.put("Importe", row.amount == null || row.amount.trim().isEmpty() ? "0.00 EUR" : row.amount.trim());
        return rows;
    }

    private Map<String, String> buildExpenseInfoRows(@NonNull WorkspaceRow row) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Nombre del piso", currentGroupName == null || currentGroupName.trim().isEmpty() ? "Piso actual" : currentGroupName);
        if (row.snapshot == null) {
            rows.put("Detalle", row.subtitle == null ? "Sin datos" : row.subtitle);
            rows.put("Importe", row.amount == null ? "0.00 EUR" : row.amount);
            return rows;
        }

        String payerEmail = row.snapshot.getString("payerEmail");
        String roomName = row.snapshot.getString("roomName");
        String category = row.snapshot.getString("category");
        String dueDateText = row.snapshot.getString("dueDateText");
        String status = normalizeFlowStatus(row.snapshot.getString("status"));
        String customSplit = row.snapshot.getString("customSplit");

        rows.put("Pagado por", payerEmail == null || payerEmail.trim().isEmpty() ? "Sin datos" : memberReferenceInline(payerEmail));
        rows.put("Para", buildExpenseTargetsLabel(customSplit, payerEmail));
        rows.put("Habitación", roomName == null || roomName.trim().isEmpty() ? "Varias habitaciones" : roomName);
        rows.put("Categoría", category == null || category.trim().isEmpty() ? "Sin categoría" : capitalizeTypeLabel(category));
        rows.put("Estado", statusLabel(status));
        rows.put("Vence", dueDateText == null || dueDateText.trim().isEmpty() ? "Sin fecha" : dueDateText);
        rows.put("Importe", row.amount == null || row.amount.trim().isEmpty() ? "0.00 EUR" : row.amount);
        return rows;
    }

    @NonNull
    private String buildExpenseTargetsLabel(@Nullable String customSplit, @Nullable String payerEmail) {
        Map<String, Double> split = parseCustomSplitPercentages(customSplit);
        if (split.isEmpty()) return "Sin datos";

        String payer = payerEmail == null ? "" : payerEmail.trim().toLowerCase(Locale.ROOT);
        List<String> targets = new ArrayList<>();
        for (String email : split.keySet()) {
            if (email == null || email.trim().isEmpty()) continue;
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (!payer.isEmpty() && payer.equals(normalized)) continue;
            if (!targets.contains(normalized)) targets.add(normalized);
        }
        if (targets.isEmpty() && !payer.isEmpty()) {
            targets.add(payer);
        }
        if (targets.isEmpty()) return "Sin datos";

        List<String> labels = new ArrayList<>();
        for (String target : targets) {
            labels.add(memberReferenceInline(target));
        }
        return String.join(", ", labels);
    }

    private void editExpense(WorkspaceRow row) {
        if (row.snapshot == null) return;
        if (!canEditOrDeleteExpense(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo puedes editar en estado Solicitado");
            return;
        }
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                NoticeUtils.show(requireContext(), "No hay miembros para repartir");
                return;
            }
            loadCurrentGroupRooms(rooms -> {
                View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
                ((EditText) form.findViewById(R.id.conceptEt)).setText(row.snapshot.getString("concept"));
                Double amountValue = row.snapshot.getDouble("amount");
                ((EditText) form.findViewById(R.id.amountEt)).setText(amountValue == null ? "" : String.valueOf(amountValue));
                setupCategoryInput((AutoCompleteTextView) form.findViewById(R.id.categoryEt), row.snapshot.getString("category"));
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
                View scrollableForm = ExpenseDialogs.wrapFormForDialogScroll(requireContext(), form);

                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        "Editar gasto",
                        "Actualiza los datos de este gasto.",
                        scrollableForm,
                        "Cancelar",
                        "Guardar"
                );
                ExpenseDialogs.tuneLongFormShell(shell);
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                ExpenseDialogs.adjustLongFormDialogWindow(requireContext(), dialog);
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
                        String rentSplitMode = normalizeRoomSplitMode(doc.getString("rentSplitMode"));
                        Map<String, Double> rentSplitPercentages = castPercentages(doc.get("rentSplitPercentages"));
                        List<String> rentSplitOrder = castEmails(doc.get("rentSplitOrder"));
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
                                monthlyCost == null ? 0.0 : monthlyCost,
                                rentSplitMode,
                                rentSplitPercentages,
                                rentSplitOrder
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
        // UX simplificada: la selección de reparto se hace solo en el bloque inferior.
        View roomSectionLabel = form.findViewById(R.id.roomSectionLabelTv);
        LinearLayout roomSelectorsContainer = form.findViewById(R.id.roomSelectorsContainer);
        Button addRoomSelectionBtn = form.findViewById(R.id.addRoomSelectionBtn);
        Button removeRoomSelectionBtn = form.findViewById(R.id.removeRoomSelectionBtn);
        TextView roomMembersHintTv = form.findViewById(R.id.roomMembersHintTv);
        if (roomSectionLabel != null) roomSectionLabel.setVisibility(View.GONE);
        if (roomSelectorsContainer != null) roomSelectorsContainer.setVisibility(View.GONE);
        if (addRoomSelectionBtn != null) addRoomSelectionBtn.setVisibility(View.GONE);
        if (removeRoomSelectionBtn != null) removeRoomSelectionBtn.setVisibility(View.GONE);
        if (roomMembersHintTv != null) roomMembersHintTv.setVisibility(View.GONE);

        setSplitCandidateMembers(form, allMembers);
        setSplitRoomCandidates(form, rooms);
        refreshSplitRowsFromScope(form, allMembers, true);
        refreshSplitControls(form);
        bindSplitRowsWatcher(form);
        refreshSplitRemainingIndicator(form);
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

        List<RoomOption> selectedRooms = new ArrayList<>(rooms);
        List<String> mergedMembers = mergeSelectedRoomMembers(selectedRooms, allMembers);
        setSplitCandidateMembers(form, mergedMembers.isEmpty() ? allMembers : mergedMembers);
        setSplitRoomCandidates(form, selectedRooms);
        if (selectedRooms.size() == rooms.size()) {
            roomMembersHintTv.setText("Todas las habitaciones seleccionadas.");
        } else if (mergedMembers.isEmpty()) {
            roomMembersHintTv.setText("Habitación sin residentes asignados.");
        } else {
            roomMembersHintTv.setText("Residentes:\n" + formatMembersDetailed(mergedMembers));
        }

        Object keepInitialSplitTag = form.getTag(R.id.roomMembersHintTv);
        boolean keepInitialSplit = keepInitialSplitTag instanceof Boolean && (Boolean) keepInitialSplitTag;
        if (keepInitialSplit) {
            form.setTag(R.id.roomMembersHintTv, false);
        } else {
            refreshSplitRowsFromScope(form, allMembers, false);
            refreshSplitControls(form);
            bindSplitRowsWatcher(form);
            refreshSplitRemainingIndicator(form);
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

    private List<RoomOption> getRoomsSelectedInSplitRows(@NonNull View form, @NonNull List<RoomOption> loadedRooms) {
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        if (splitRowsContainer == null) return new ArrayList<>();
        List<RoomOption> selected = new ArrayList<>();
        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            Spinner roomSpinner = row.findViewById(R.id.memberSpinner);
            if (roomSpinner == null || roomSpinner.getSelectedItem() == null) continue;
            String roomLabel = roomSpinner.getSelectedItem().toString();
            for (RoomOption room : loadedRooms) {
                if (roomLabel.equals(room.name) && !selected.contains(room)) {
                    selected.add(room);
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

    private void setSplitCandidateMembers(@NonNull View form, @NonNull List<String> candidates) {
        List<String> normalized = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String email = candidate.trim().toLowerCase(Locale.ROOT);
            if (email.isEmpty() || normalized.contains(email)) continue;
            normalized.add(email);
        }
        form.setTag(R.id.splitRowsContainer, normalized);
    }

    private void setSplitRoomCandidates(@NonNull View form, @NonNull List<RoomOption> rooms) {
        form.setTag(R.id.splitTargetScopeSpinner, new ArrayList<>(rooms));
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private List<RoomOption> getSplitRoomCandidates(@NonNull View form) {
        Object raw = form.getTag(R.id.splitTargetScopeSpinner);
        if (!(raw instanceof List<?> rawList)) return new ArrayList<>();
        List<RoomOption> out = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof RoomOption room) out.add(room);
        }
        return out;
    }

    @NonNull
    private List<String> getSplitCandidateMembers(@NonNull View form, @NonNull List<String> fallback) {
        Object raw = form.getTag(R.id.splitRowsContainer);
        if (!(raw instanceof List<?> rawList)) return new ArrayList<>(fallback);

        List<String> resolved = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof String emailRaw)) continue;
            String email = emailRaw.trim().toLowerCase(Locale.ROOT);
            if (email.isEmpty() || resolved.contains(email)) continue;
            resolved.add(email);
        }
        return resolved.isEmpty() ? new ArrayList<>(fallback) : resolved;
    }

    private void setSplitRowsForMembers(View form, List<String> allMembers, @Nullable List<String> preferredMembers, boolean equitative) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);

        List<String> candidateMembers = new ArrayList<>(allMembers);
        List<String> rowMembers = new ArrayList<>();
        if (preferredMembers != null && !preferredMembers.isEmpty()) {
            rowMembers.addAll(sanitizeRoomMembers(preferredMembers, allMembers));
        }
        if (rowMembers.isEmpty() && !candidateMembers.isEmpty()) {
            rowMembers.add(candidateMembers.get(0));
        }
        if (candidateMembers.isEmpty() || rowMembers.isEmpty()) return;
        setSplitCandidateMembers(form, candidateMembers);

        splitRowsContainer.removeAllViews();
        for (String member : rowMembers) {
            addSplitRow(splitRowsContainer, candidateMembers, member, null);
        }

        if (rowMembers.size() == 1) {
            View singleRow = splitRowsContainer.getChildAt(0);
            if (singleRow != null) {
                EditText amountEt = form.findViewById(R.id.amountEt);
                EditText memberAmountEt = singleRow.findViewById(R.id.memberAmountEt);
                String totalText = amountEt == null || amountEt.getText() == null ? "" : amountEt.getText().toString().trim();
                if (!totalText.isEmpty()) {
                    memberAmountEt.setText(totalText);
                }
            }
        }

        splitModeSpinner.setSelection(equitative ? 1 : 0);
        refreshSplitControls(form);
        bindSplitRowsWatcher(form);
        refreshSplitRemainingIndicator(form);
    }

    private void setupSplitUi(View form, List<String> members, @Nullable String currentCustomSplit) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        Spinner splitTargetScopeSpinner = form.findViewById(R.id.splitTargetScopeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        Button addSplitRowBtn = form.findViewById(R.id.addSplitRowBtn);
        Button removeSplitRowBtn = form.findViewById(R.id.removeSplitRowBtn);

        String[] modes = new String[]{"Reparto personalizado", "Reparto equitativo"};
        ArrayAdapter<String> modeAdapter = buildLightSpinnerAdapter(modes);
        splitModeSpinner.setAdapter(modeAdapter);
        splitTargetScopeSpinner.setAdapter(buildLightSpinnerAdapter(new String[]{"Personas", "Habitaciones"}));
        splitTargetScopeSpinner.setSelection(0);

        setSplitCandidateMembers(form, members);
        addSplitRow(splitRowsContainer, members, null, null);
        refreshSplitControls(form);

        addSplitRowBtn.setOnClickListener(v -> {
            if (isRoomScopeSelected(form)) {
                List<RoomOption> roomCandidates = getSplitRoomCandidates(form);
                List<String> existingRoomLabels = new ArrayList<>();
                for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
                    View row = splitRowsContainer.getChildAt(i);
                    Spinner sp = row.findViewById(R.id.memberSpinner);
                    if (sp != null && sp.getSelectedItem() != null) {
                        existingRoomLabels.add(sp.getSelectedItem().toString());
                    }
                }
                String nextRoom = null;
                for (RoomOption room : roomCandidates) {
                    if (!existingRoomLabels.contains(room.name)) {
                        nextRoom = room.name;
                        break;
                    }
                }
                if (nextRoom != null) {
                    List<String> roomLabels = new ArrayList<>();
                    for (RoomOption room : roomCandidates) roomLabels.add(room.name);
                    addSplitRow(splitRowsContainer, roomLabels, nextRoom, null);
                }
            } else {
                List<String> candidates = getSplitCandidateMembers(form, members);
                String nextMember = null;
                List<String> selectedMembers = new ArrayList<>();
                for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
                    View row = splitRowsContainer.getChildAt(i);
                    Spinner sp = row.findViewById(R.id.memberSpinner);
                    if (sp == null || sp.getSelectedItem() == null) continue;
                    String selected = sp.getSelectedItem().toString().trim().toLowerCase(Locale.ROOT);
                    if (!selected.isEmpty()) selectedMembers.add(selected);
                }
                for (String candidate : candidates) {
                    if (!selectedMembers.contains(candidate)) {
                        nextMember = candidate;
                        break;
                    }
                }
                addSplitRow(splitRowsContainer, candidates, nextMember, null);
            }
            refreshSplitControls(form);
            bindSplitRowsWatcher(form);
            refreshSplitRemainingIndicator(form);
        });
        removeSplitRowBtn.setOnClickListener(v -> {
            int count = splitRowsContainer.getChildCount();
            if (count > 1) {
                splitRowsContainer.removeViewAt(count - 1);
            }
            refreshSplitControls(form);
            bindSplitRowsWatcher(form);
            refreshSplitRemainingIndicator(form);
        });

        splitModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshSplitControls(form);
                refreshSplitRemainingIndicator(form);
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
            refreshSplitControls(form);
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
            refreshSplitControls(form);
        }

        EditText amountEt = form.findViewById(R.id.amountEt);
        amountEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                autoAssignSingleMemberAmount(form);
                refreshSplitRemainingIndicator(form);
            }
        });

        splitTargetScopeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshSplitRowsFromScope(form, members, false);
                refreshSplitControls(form);
                bindSplitRowsWatcher(form);
                refreshSplitRemainingIndicator(form);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        bindSplitRowsWatcher(form);
        autoAssignSingleMemberAmount(form);
        refreshSplitRemainingIndicator(form);
    }

    private boolean isRoomScopeSelected(@NonNull View form) {
        Spinner splitTargetScopeSpinner = form.findViewById(R.id.splitTargetScopeSpinner);
        return splitTargetScopeSpinner != null && splitTargetScopeSpinner.getSelectedItemPosition() == 1;
    }

    private void refreshSplitRowsFromScope(@NonNull View form, @NonNull List<String> allMembers, boolean keepCurrentValues) {
        if (isRoomScopeSelected(form)) {
            setSplitRowsForRooms(form, keepCurrentValues);
            return;
        }
        List<String> selectedMembers = new ArrayList<>();
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        if (splitRowsContainer != null) {
            for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
                View row = splitRowsContainer.getChildAt(i);
                Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
                if (memberSpinner == null || memberSpinner.getSelectedItem() == null) continue;
                String email = memberSpinner.getSelectedItem().toString().trim().toLowerCase(Locale.ROOT);
                if (email.isEmpty() || selectedMembers.contains(email)) continue;
                selectedMembers.add(email);
            }
        }
        setSplitRowsForMembers(form, allMembers, selectedMembers.isEmpty() ? null : selectedMembers, false);
    }

    private void setSplitRowsForRooms(@NonNull View form, boolean keepCurrentValues) {
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        List<RoomOption> selectedRooms = getSplitRoomCandidates(form);
        if (selectedRooms.isEmpty()) {
            splitRowsContainer.removeAllViews();
            return;
        }

        Map<String, String> existingAmountsByRoom = new LinkedHashMap<>();
        List<String> existingRoomLabels = new ArrayList<>();
        if (keepCurrentValues) {
            for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
                View row = splitRowsContainer.getChildAt(i);
                Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
                EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
                if (memberSpinner == null || memberAmountEt == null || memberSpinner.getSelectedItem() == null) continue;
                String roomLabel = memberSpinner.getSelectedItem().toString();
                existingRoomLabels.add(roomLabel);
                existingAmountsByRoom.put(roomLabel, memberAmountEt.getText() == null ? "" : memberAmountEt.getText().toString().trim());
            }
        }

        List<String> roomLabels = new ArrayList<>();
        for (RoomOption room : selectedRooms) {
            if (roomLabels.contains(room.name)) continue;
            roomLabels.add(room.name);
        }
        if (existingRoomLabels.isEmpty() && !roomLabels.isEmpty()) {
            existingRoomLabels.add(roomLabels.get(0));
        }
        splitRowsContainer.removeAllViews();
        for (String roomLabel : existingRoomLabels) {
            String existing = existingAmountsByRoom.get(roomLabel);
            addSplitRow(splitRowsContainer, roomLabels, roomLabel, existing);
        }
    }

    private void refreshSplitControls(@NonNull View form) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        Spinner splitTargetScopeSpinner = form.findViewById(R.id.splitTargetScopeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        Button addSplitRowBtn = form.findViewById(R.id.addSplitRowBtn);
        Button removeSplitRowBtn = form.findViewById(R.id.removeSplitRowBtn);
        if (splitModeSpinner == null || splitRowsContainer == null || addSplitRowBtn == null || removeSplitRowBtn == null || splitTargetScopeSpinner == null) return;

        boolean roomScope = isRoomScopeSelected(form);

        boolean singleResidentMode = !roomScope && isSingleResidentSplitMode(splitRowsContainer);
        if (singleResidentMode && splitModeSpinner.getSelectedItemPosition() != 0) {
            splitModeSpinner.setSelection(0);
        }
        boolean equitative = splitModeSpinner.getSelectedItemPosition() == 1 && !singleResidentMode && !roomScope;

        splitModeSpinner.setEnabled(!singleResidentMode && !roomScope);
        splitModeSpinner.setAlpha((singleResidentMode || roomScope) ? 0.55f : 1f);
        if (roomScope) {
            int availableRooms = getSplitRoomCandidates(form).size();
            int selectedRows = splitRowsContainer.getChildCount();
            addSplitRowBtn.setVisibility(selectedRows < availableRooms ? View.VISIBLE : View.GONE);
            removeSplitRowBtn.setVisibility(selectedRows > 1 ? View.VISIBLE : View.GONE);
        } else {
            int totalCandidates = getSplitCandidateMembers(form, new ArrayList<>()).size();
            int currentRows = splitRowsContainer.getChildCount();
            addSplitRowBtn.setVisibility((singleResidentMode || currentRows >= totalCandidates) ? View.GONE : View.VISIBLE);
            removeSplitRowBtn.setVisibility(currentRows > 1 ? View.VISIBLE : View.GONE);
        }

        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
            TextView memberFixedTv = row.findViewById(R.id.memberFixedTv);
            EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
            if (memberSpinner == null || memberFixedTv == null || memberAmountEt == null) continue;

            if (singleResidentMode) {
                String email = "";
                Object selected = memberSpinner.getSelectedItem();
                if (selected != null) {
                    email = selected.toString().trim().toLowerCase(Locale.ROOT);
                }
                memberSpinner.setVisibility(View.GONE);
                memberFixedTv.setVisibility(View.VISIBLE);
                memberFixedTv.setText("Persona: " + memberReferenceInline(email));
                memberAmountEt.setVisibility(View.VISIBLE);
                memberAmountEt.setEnabled(false);
                memberAmountEt.setFocusable(false);
                memberAmountEt.setFocusableInTouchMode(false);
                memberAmountEt.setClickable(false);
            } else {
                memberSpinner.setVisibility(View.VISIBLE);
                memberFixedTv.setVisibility(View.GONE);
                memberAmountEt.setEnabled(true);
                memberAmountEt.setFocusable(true);
                memberAmountEt.setFocusableInTouchMode(true);
                memberAmountEt.setClickable(true);
                memberAmountEt.setVisibility(equitative ? View.GONE : View.VISIBLE);
            }
        }
    }

    private boolean isSingleResidentSplitMode(@NonNull LinearLayout splitRowsContainer) {
        if (splitRowsContainer.getChildCount() != 1) return false;
        View row = splitRowsContainer.getChildAt(0);
        Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
        return memberSpinner != null
                && memberSpinner.getAdapter() != null
                && memberSpinner.getAdapter().getCount() == 1;
    }

    private void bindSplitRowsWatcher(@NonNull View form) {
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
            if (Boolean.TRUE.equals(memberAmountEt.getTag(R.id.memberAmountEt))) continue;
            memberAmountEt.setTag(R.id.memberAmountEt, true);
            memberAmountEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    refreshSplitRemainingIndicator(form);
                }
            });
        }
    }

    private void autoAssignSingleMemberAmount(@NonNull View form) {
        if (isRoomScopeSelected(form)) return;
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        if (splitModeSpinner.getSelectedItemPosition() != 0) return;
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        if (splitRowsContainer.getChildCount() != 1) return;

        View row = splitRowsContainer.getChildAt(0);
        EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
        EditText amountEt = form.findViewById(R.id.amountEt);
        String totalText = amountEt == null || amountEt.getText() == null ? "" : amountEt.getText().toString().trim();
        if (totalText.isEmpty()) return;

        if (!totalText.equals(memberAmountEt.getText() == null ? "" : memberAmountEt.getText().toString().trim())) {
            memberAmountEt.setText(totalText);
        }
    }

    private void refreshSplitRemainingIndicator(@NonNull View form) {
        TextView remainingTv = form.findViewById(R.id.splitRemainingTv);
        EditText amountEt = form.findViewById(R.id.amountEt);
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        if (remainingTv == null || amountEt == null || splitModeSpinner == null || splitRowsContainer == null) return;

        String totalText = amountEt.getText() == null ? "" : amountEt.getText().toString().trim();
        double total;
        try {
            total = totalText.isEmpty() ? 0.0 : Double.parseDouble(totalText);
        } catch (NumberFormatException e) {
            remainingTv.setVisibility(View.VISIBLE);
            remainingTv.setText("Importe total no válido.");
            remainingTv.setTextColor(requireContext().getColor(R.color.status_danger));
            return;
        }
        if (total <= 0.0) {
            remainingTv.setVisibility(View.GONE);
            return;
        }

        boolean roomScope = isRoomScopeSelected(form);
        boolean equitative = splitModeSpinner.getSelectedItemPosition() == 1 && !roomScope;
        if (roomScope) {
            equitative = false;
        }
        if (equitative) {
            remainingTv.setVisibility(View.VISIBLE);
            int participants = 0;
            for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
                View row = splitRowsContainer.getChildAt(i);
                Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
                if (memberSpinner == null || memberSpinner.getSelectedItem() == null) continue;
                String selected = memberSpinner.getSelectedItem().toString().trim();
                if (!selected.isEmpty()) participants++;
            }
            if (participants < 2) {
                remainingTv.setText("Equitativo: a?ade al menos 2 personas.");
                remainingTv.setTextColor(requireContext().getColor(R.color.status_warning));
            } else {
                DecimalFormat amountFormat = new DecimalFormat("0.00");
                double perPerson = total / participants;
                remainingTv.setText("Equitativo: " + participants + " personas x " + amountFormat.format(perPerson) + " EUR = " + amountFormat.format(total) + " EUR.");
                remainingTv.setTextColor(requireContext().getColor(R.color.status_success));
            }
            return;
        }

        double assigned = 0.0;
        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
            String value = memberAmountEt.getText() == null ? "" : memberAmountEt.getText().toString().trim();
            if (value.isEmpty()) continue;
            try {
                assigned += Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
            }
        }

        double remaining = round2(total - assigned);
        remainingTv.setVisibility(View.VISIBLE);
        DecimalFormat amountFormat = new DecimalFormat("0.00");
        String assignedVsTotal = "Asignado: " + amountFormat.format(round2(assigned)) + " / " + amountFormat.format(total) + " EUR.";
        boolean singleResidentMode = isSingleResidentSplitMode(splitRowsContainer);

        if (singleResidentMode) {
            remainingTv.setText(assignedVsTotal + " Reparto automático correcto.");
            remainingTv.setTextColor(requireContext().getColor(R.color.status_success));
            return;
        }
        if (Math.abs(remaining) <= 0.01) {
            remainingTv.setText(assignedVsTotal + " Reparto perfecto, listo para guardar.");
            remainingTv.setTextColor(requireContext().getColor(R.color.status_success));
        } else if (remaining > 0.0) {
            remainingTv.setText(assignedVsTotal + " Te faltan por añadir " + amountFormat.format(remaining) + " EUR.");
            remainingTv.setTextColor(requireContext().getColor(R.color.status_danger));
        } else {
            remainingTv.setText(assignedVsTotal + " Te has pasado " + amountFormat.format(Math.abs(remaining)) + " EUR.");
            remainingTv.setTextColor(requireContext().getColor(R.color.status_danger));
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

    @Nullable
    private String buildCustomSplitFromUi(View form, double totalAmount, List<String> members) {
        Spinner splitModeSpinner = form.findViewById(R.id.splitModeSpinner);
        LinearLayout splitRowsContainer = form.findViewById(R.id.splitRowsContainer);
        boolean roomScope = isRoomScopeSelected(form);
        boolean equitative = splitModeSpinner.getSelectedItemPosition() == 1 && !roomScope;

        Map<String, Double> splitsByEmail = new LinkedHashMap<>();
        for (int i = 0; i < splitRowsContainer.getChildCount(); i++) {
            View row = splitRowsContainer.getChildAt(i);
            Spinner memberSpinner = row.findViewById(R.id.memberSpinner);
            EditText memberAmountEt = row.findViewById(R.id.memberAmountEt);
            String selected = memberSpinner.getSelectedItem().toString();

            if (equitative) {
                String email = selected.toLowerCase(Locale.ROOT);
                splitsByEmail.put(email, 0.0);
            } else {
                String amountText = memberAmountEt.getText().toString().trim();
                if (amountText.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Completa importes en todas las lineas");
                    return null;
                }
                double partAmount;
                try {
                    partAmount = Double.parseDouble(amountText);
                } catch (NumberFormatException e) {
                    NoticeUtils.show(requireContext(), "Hay importes de reparto no válidos");
                    return null;
                }
                if (partAmount < 0) {
                    NoticeUtils.show(requireContext(), "Ningun reparto puede ser negativo");
                    return null;
                }
                if (roomScope) {
                    RoomOption room = null;
                    for (RoomOption candidate : getSplitRoomCandidates(form)) {
                        if (candidate.name.equals(selected)) {
                            room = candidate;
                            break;
                        }
                    }
                    if (room == null) {
                        NoticeUtils.show(requireContext(), "Hay una habitación de reparto no válida");
                        return null;
                    }
                    Map<String, Double> residentsSplit = resolveRoomResidentPercentagesForAllocation(room);
                    if (residentsSplit == null) {
                        return null;
                    }
                    if (residentsSplit.isEmpty()) {
                        NoticeUtils.show(requireContext(), "Una habitación seleccionada no tiene inquilinos");
                        return null;
                    }
                    for (Map.Entry<String, Double> splitEntry : residentsSplit.entrySet()) {
                        String resident = splitEntry.getKey();
                        double percent = splitEntry.getValue();
                        double amountForResident = (partAmount * Math.max(0.0, percent)) / 100.0;
                        splitsByEmail.put(resident, splitsByEmail.getOrDefault(resident, 0.0) + amountForResident);
                    }
                } else {
                    String email = selected.toLowerCase(Locale.ROOT);
                    splitsByEmail.put(email, splitsByEmail.getOrDefault(email, 0.0) + partAmount);
                }
            }
        }

        if (equitative) {
            if (splitsByEmail.size() < 2) {
                NoticeUtils.show(requireContext(), "El reparto equitativo necesita minimo 2 personas");
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
            NoticeUtils.show(requireContext(), "Añade al menos una línea de reparto");
            return null;
        }

        double sum = 0.0;
        for (double value : splitsByEmail.values()) sum += value;
        if (sum <= 0.0) {
            NoticeUtils.show(requireContext(), "El reparto total debe ser mayor que 0");
            return null;
        }
        if (Math.abs(sum - totalAmount) > 0.01) {
            NoticeUtils.show(requireContext(), "La suma del reparto debe coincidir con el importe");
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
        if (row.snapshot == null || !canEditOrDeleteExpense(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo puedes eliminar en estado Solicitado");
            return;
        }
        showDeleteConfirmation(
                "Eliminar gasto",
                "Se eliminará el gasto y sus vencimientos asociados.",
                () -> deleteExpense(row)
        );
    }

    private void requestPaymentDeletion(WorkspaceRow row) {
        if (row.snapshot == null || !canDeletePayment(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo se puede eliminar un pago en estado Solicitado");
            return;
        }
        showDeleteConfirmation(
                "Eliminar pago",
                "Se eliminará el pago y sus recordatorios asociados.",
                () -> deletePayment(row)
        );
    }

    private void requestReminderDeletion(WorkspaceRow row) {
        requestReminderDeletionAllDays(row);
    }

    private void deleteExpense(WorkspaceRow row) {
        if (row.snapshot == null || !canEditOrDeleteExpense(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo puedes editar o eliminar gastos en estado Solicitado");
            return;
        }
        if (!canManageExpense(row.snapshot.getString("payerId"))) {
            NoticeUtils.show(requireContext(), "No tienes permisos para borrar este gasto");
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
        if (row.snapshot == null || !canDeletePayment(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo se puede eliminar un pago en estado Solicitado");
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
        if (!canDeleteReminder(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo quien lo creó o el propietario puede eliminarlo");
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
            loadGroupCategorySuggestions(false, categories -> {
                if (!isAdded()) return;
                categoryValues.clear();
                categoryLabels.clear();
                categoryValues.add("");
                categoryLabels.add("Selecciona categoría");
                for (String category : categories) {
                    if (category == null || category.trim().isEmpty()) continue;
                    categoryValues.add(category);
                    categoryLabels.add(capitalizeTypeLabel(category));
                }
                categorySpinner.setAdapter(buildLightSpinnerAdapter(categoryLabels.toArray(new String[0])));
                selectSpinnerValue(categorySpinner, categoryValues, filterCategory);
            });
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
                    NoticeUtils.show(requireContext(), "Selecciona una categoría");
                    return;
                }
                filterCategory = selectedCategory.trim().toLowerCase(Locale.ROOT);
            } else if (FILTER_MODE_PERSON.equals(selectedMode)) {
                int selectedIndex = personSpinner.getSelectedItemPosition();
                String selectedPerson = selectedIndex >= 0 && selectedIndex < personValues.size()
                        ? personValues.get(selectedIndex)
                        : "";
                if (selectedPerson == null || selectedPerson.trim().isEmpty()) {
                    NoticeUtils.show(requireContext(), "Selecciona una persona");
                    return;
                }
                filterPersonEmail = selectedPerson.trim().toLowerCase(Locale.ROOT);
            } else {
                String dateText = dateEt.getText().toString().trim();
                if (dateText.isEmpty()) {
                    NoticeUtils.show(requireContext(), "Selecciona una fecha");
                    return;
                }
                try {
                    Date selectedDate = DUE_DATE_FORMAT.parse(dateText);
                    if (selectedDate == null) {
                        NoticeUtils.show(requireContext(), "Formato de fecha no válido (YYYY-MM-DD)");
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
                    NoticeUtils.show(requireContext(), "Formato de fecha no válido (YYYY-MM-DD)");
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

    private boolean passesPendingDebtFilters(@NonNull PendingDebtRequest debt) {
        if (filterPersonEmail != null) {
            boolean match = debt.creditorEmail.equalsIgnoreCase(filterPersonEmail);
            if (!match) return false;
        }
        if (filterCategory != null) {
            return false;
        }
        if (filterSearchQuery != null) {
            String combined = debt.concept + " " + debt.creditorEmail + " " + debt.dueDateText;
            if (!combined.toLowerCase(Locale.ROOT).contains(filterSearchQuery)) {
                return false;
            }
        }
        return passesDateFilter(debt.snapshot);
    }

    private String normalizeFlowStatus(@Nullable String rawStatus) {
        String normalized = safeLowerText(rawStatus);
        if (STATUS_CONFIRMED.equals(normalized) || "paid".equals(normalized) || "pagado".equals(normalized)) {
            return STATUS_CONFIRMED;
        }
        if (STATUS_SUBMITTED.equals(normalized)) {
            return STATUS_SUBMITTED;
        }
        if (STATUS_PENDING.equals(normalized)) {
            return STATUS_PENDING;
        }
        return STATUS_REQUESTED;
    }

    private String statusLabel(@Nullable String rawStatus) {
        String normalized = normalizeFlowStatus(rawStatus);
        if (STATUS_CONFIRMED.equals(normalized)) return "Pagado";
        if (STATUS_SUBMITTED.equals(normalized)) return "En revisión";
        if (STATUS_PENDING.equals(normalized)) return "Pendiente";
        return "Solicitado";
    }

    private boolean passesDateFilter(DocumentSnapshot doc) {
        Date createdAt = doc.getDate("createdAt");
        if (createdAt == null) return true;
        long time = createdAt.getTime();
        if (filterFromMs != null && time < filterFromMs) return false;
        return filterToMs == null || time <= filterToMs;
    }

    private void updatePaymentStatus(String paymentId, String status) {
        db.collection("payments").document(paymentId).get().addOnSuccessListener(paymentDoc -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            db.collection("payments").document(paymentId).update(updates).addOnSuccessListener(v -> {
                if ("confirmed".equalsIgnoreCase(status) && isAdded()) {
                    AppSoundFx.playByName(requireContext(), AppSoundFx.FX_EXPENSE_ACCEPTED);
                }
                logActivity("payment_" + status, "Pago " + status, 0.0, "payment");
                updateDeadlineStatusBySource("payment", paymentId, status);

                String sourceDebtId = paymentDoc.getString("sourceDebtId");
                if (sourceDebtId != null && !sourceDebtId.trim().isEmpty()) {
                    String debtStatus = STATUS_CONFIRMED.equalsIgnoreCase(status) ? STATUS_CONFIRMED : STATUS_SUBMITTED;
                    db.collection("payment_deadlines").document(sourceDebtId).update(
                            "status", debtStatus,
                            "updatedAt", FieldValue.serverTimestamp()
                    ).addOnSuccessListener(done -> updateExpenseStatusAfterDeadlineChange(sourceDebtId));
                }

                loadExpenses();
                loadFinancialViews();
            });
        });
    }

    private boolean canManageExpense(@Nullable String payerId) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if ("admin".equals(currentUserRole)) return true;
        return payerId != null && payerId.equals(myUid);
    }

    private boolean canEditOrDeleteExpense(@NonNull DocumentSnapshot expenseDoc) {
        if (!canManageExpense(expenseDoc.getString("payerId"))) return false;
        String status = normalizeFlowStatus(expenseDoc.getString("status"));
        return STATUS_REQUESTED.equals(status);
    }

    private boolean isOwnerUser() {
        return "admin".equals(currentUserRole);
    }

    private boolean canTogglePaymentStatus(@NonNull DocumentSnapshot paymentDoc) {
        if (!isOwnerUser()) return false;
        String status = normalizeFlowStatus(paymentDoc.getString("status"));
        return STATUS_REQUESTED.equals(status);
    }

    private boolean canDeletePayment(@NonNull DocumentSnapshot paymentDoc) {
        if (!isOwnerUser()) return false;
        String status = normalizeFlowStatus(paymentDoc.getString("status"));
        return STATUS_REQUESTED.equals(status);
    }

    private void requestPaymentStatusChange(@NonNull WorkspaceRow row, @NonNull String targetStatus) {
        if (row.snapshot == null) return;
        if (!isOwnerUser()) {
            NoticeUtils.show(requireContext(), "Solo el propietario puede cambiar el estado del pago");
            return;
        }
        if (!canTogglePaymentStatus(row.snapshot)) {
            NoticeUtils.show(requireContext(), "Solo puedes cambiar estado en pagos Solicitados");
            return;
        }
        String action = "confirmed".equalsIgnoreCase(targetStatus) ? "marcar como pagado" : "marcar como pendiente";
        View content = DialogUtils.createMessageView(
                requireContext(),
                "Vas a " + action + " este pago.\n\\n¿Deseas continuar?"
        );
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Confirmar cambio de estado",
                "Esta acción actualizará el estado del pago.",
                content,
                "Cancelar",
                "Aceptar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            updatePaymentStatus(row.id, targetStatus.toLowerCase(Locale.ROOT));
        });
    }

    private boolean amountsMatchToCent(double left, double right) {
        long leftCents = Math.round(left * 100.0d);
        long rightCents = Math.round(right * 100.0d);
        return leftCents == rightCents;
    }

    private String safeLowerText(@Nullable String value) {
        if (value == null) return "";
        String lower = value.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
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

    private void updateExpenseStatusAfterDeadlineChange(@NonNull String deadlineId) {
        db.collection("payment_deadlines").document(deadlineId).get().addOnSuccessListener(deadlineDoc -> {
            if (!deadlineDoc.exists()) return;
            String sourceType = safeLowerText(deadlineDoc.getString("sourceType"));
            if (!"expense".equals(sourceType)) return;
            String expenseId = deadlineDoc.getString("sourceId");
            if (expenseId == null || expenseId.trim().isEmpty()) return;
            refreshExpenseFlowStatus(expenseId);
        });
    }

    private void refreshExpenseFlowStatus(@NonNull String expenseId) {
        db.collection("payment_deadlines")
                .whereEqualTo("sourceType", "expense")
                .whereEqualTo("sourceId", expenseId)
                .get()
                .addOnSuccessListener(result -> {
                    String targetStatus = STATUS_CONFIRMED;
                    if (result.isEmpty()) {
                        targetStatus = STATUS_CONFIRMED;
                    } else {
                        boolean hasPending = false;
                        boolean hasSubmitted = false;
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            String deadlineStatus = safeLowerText(doc.getString("status"));
                            if (STATUS_PENDING.equals(deadlineStatus)) {
                                hasPending = true;
                            } else if (STATUS_SUBMITTED.equals(deadlineStatus)) {
                                hasSubmitted = true;
                            }
                        }
                        if (hasPending) {
                            targetStatus = STATUS_REQUESTED;
                        } else if (hasSubmitted) {
                            targetStatus = STATUS_PENDING;
                        } else {
                            targetStatus = STATUS_CONFIRMED;
                        }
                    }
                    db.collection("expenses").document(expenseId).update(
                            "status", targetStatus,
                            "updatedAt", FieldValue.serverTimestamp()
                    );
                });
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
                        data.put("dueDateText", dueAt == null ? "" : DUE_DATE_FORMAT.format(dueAt));
                        data.put("priority", priority == null ? "media" : priority.toLowerCase(Locale.ROOT));
                        data.put("status", STATUS_PENDING);
                        data.put("createdAt", FieldValue.serverTimestamp());
                        batch.set(db.collection("payment_deadlines").document(), data);

                        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
                        if (debtor.equalsIgnoreCase(myEmail)) {
                            scheduleDeadlineNotifications(concept, partAmount, dueAt, expenseId + "_" + debtor);
                        }
                    }
                    batch.commit().addOnSuccessListener(done -> refreshExpenseFlowStatus(expenseId));
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
                    data.put("status", STATUS_PENDING);
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
        updatePaymentConfirmButtonState();
        if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("Ticket adjunto. Procesando OCR...");
        try {
            InputImage inputImage = InputImage.fromFilePath(requireContext(), uri);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(inputImage)
                    .addOnSuccessListener(text -> {
                        String detected = extractFirstAmount(text.getText());
                        if (detected != null && pendingTicketAmountEt != null) {
                            if (activePendingDebtRequest != null) {
                                if (pendingTicketStatusTv != null) {
                                    pendingTicketStatusTv.setText("OCR detectó " + detected + ", pero se mantiene tu importe pendiente bloqueado.");
                                }
                            } else {
                                pendingTicketAmountEt.setText(detected);
                                if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("OCR detectó importe: " + detected);
                            }
                        } else if (pendingTicketStatusTv != null) {
                            pendingTicketStatusTv.setText("OCR listo, no se encontró importe claro.");
                        }
                        updatePaymentConfirmButtonState();
                    })
                    .addOnFailureListener(e -> {
                        if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("OCR falló.");
                        updatePaymentConfirmButtonState();
                    });
        } catch (Exception e) {
            if (pendingTicketStatusTv != null) pendingTicketStatusTv.setText("No se pudo leer la imagen.");
            updatePaymentConfirmButtonState();
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
            NoticeUtils.show(requireContext(), "No hay datos para exportar");
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
                NoticeUtils.show(requireContext(), "No se pudo crear el PDF");
                return;
            }
            try (var out = requireContext().getContentResolver().openOutputStream(uri)) {
                pdfDocument.writeTo(out);
            }
            pdfDocument.close();
            NoticeUtils.show(requireContext(), "PDF exportado en Descargas");
        } catch (Exception e) {
            NoticeUtils.show(requireContext(), "Error exportando PDF");
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

    private static class PendingDebtRequest {
        final String id;
        final String concept;
        final double amount;
        final String creditorEmail;
        final String dueDateText;
        @Nullable final Date dueAt;
        final String priority;
        @NonNull final DocumentSnapshot snapshot;

        PendingDebtRequest(
                @NonNull String id,
                @NonNull String concept,
                double amount,
                @NonNull String creditorEmail,
                @NonNull String dueDateText,
                @Nullable Date dueAt,
                @NonNull String priority,
                @NonNull DocumentSnapshot snapshot
        ) {
            this.id = id;
            this.concept = concept;
            this.amount = amount;
            this.creditorEmail = creditorEmail;
            this.dueDateText = dueDateText;
            this.dueAt = dueAt;
            this.priority = priority;
            this.snapshot = snapshot;
        }
    }

    private interface MembersCallback {
        void onLoaded(List<String> members);
    }

    private interface PendingDebtCallback {
        void onLoaded(List<PendingDebtRequest> debts);
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
        final String rentSplitMode;
        final Map<String, Double> rentSplitPercentages;
        final List<String> rentSplitOrder;

        RoomOption(
                String id,
                String name,
                List<String> memberEmails,
                int roomNumber,
                int capacity,
                double monthlyCost,
                String rentSplitMode,
                Map<String, Double> rentSplitPercentages,
                List<String> rentSplitOrder
        ) {
            this.id = id;
            this.name = name;
            this.memberEmails = memberEmails;
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.monthlyCost = monthlyCost;
            this.rentSplitMode = rentSplitMode;
            this.rentSplitPercentages = rentSplitPercentages;
            this.rentSplitOrder = rentSplitOrder;
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, values) {
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
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }

    private ArrayAdapter<String> buildLightDropdownAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, values) {
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

            String flowStatus = STATUS_REQUESTED;
            if (ROW_TYPE_PENDING_DEBT.equals(row.type)) {
                flowStatus = STATUS_REQUESTED;
            } else if (row.snapshot != null) {
                flowStatus = normalizeFlowStatus(row.snapshot.getString("status"));
            }

            if ("reminder".equals(row.type)) {
                amountTv.setTextColor(requireContext().getColor(R.color.text_light));
                subtitleTv.setTextColor(requireContext().getColor(R.color.text_light));
            } else if (STATUS_CONFIRMED.equals(flowStatus)) {
                amountTv.setTextColor(requireContext().getColor(R.color.status_success));
                subtitleTv.setTextColor(requireContext().getColor(R.color.status_success));
            } else if (STATUS_PENDING.equals(flowStatus)) {
                amountTv.setTextColor(requireContext().getColor(R.color.status_warning));
                subtitleTv.setTextColor(requireContext().getColor(R.color.status_warning));
            } else {
                amountTv.setTextColor(requireContext().getColor(R.color.status_danger));
                subtitleTv.setTextColor(requireContext().getColor(R.color.status_danger));
            }

            if ("payment".equals(row.type)) {
                if (STATUS_CONFIRMED.equals(flowStatus)) {
                    titleTv.setText("Pago pagado");
                } else if (STATUS_PENDING.equals(flowStatus)) {
                    titleTv.setText("Pago pendiente");
                } else {
                    titleTv.setText("Pago solicitado");
                }
            } else if ("expense".equals(row.type)) {
                if (STATUS_CONFIRMED.equals(flowStatus)) {
                    titleTv.setText("Gasto pagado");
                } else if (STATUS_PENDING.equals(flowStatus)) {
                    titleTv.setText("Gasto pendiente");
                } else {
                    titleTv.setText("Gasto solicitado");
                }
            } else if (ROW_TYPE_PENDING_DEBT.equals(row.type)) {
                titleTv.setText("Pago solicitado");
            }
            return view;
        }
    }
}












