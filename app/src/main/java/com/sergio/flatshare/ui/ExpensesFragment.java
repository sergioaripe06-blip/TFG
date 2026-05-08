package com.sergio.flatshare.ui;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.sergio.flatshare.util.DialogUtils;
import com.sergio.flatshare.util.ReminderScheduler;
import com.sergio.flatshare.util.SessionStore;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpensesFragment extends Fragment {
    private static final String TAB_EXPENSES = "expenses";
    private static final String TAB_SALDOS = "saldos";
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
    private static final SimpleDateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<WorkspaceRow> expenseRows = new ArrayList<>();
    private final List<WorkspaceRow> saldoRows = new ArrayList<>();

    private WorkspaceAdapter expensesAdapter;
    private WorkspaceAdapter saldosAdapter;

    private TextView workspaceTitleTv;
    private TextView workspaceMetaTv;
    private ListView expensesLv;
    private ListView saldosLv;
    private View workspaceCtaLayout;
    private TextView addMainLabelTv;
    private Button gastosTabBtn;
    private Button saldosTabBtn;

    private String currentTab = TAB_EXPENSES;
    private String currentGroupId;
    private String currentGroupName = "Piso";
    private String currentUserRole = "member";
    private String filterPersonEmail;
    private String filterCategory;
    private Long filterFromMs;
    private Long filterToMs;
    private String pendingTicketUri;
    private EditText pendingTicketAmountEt;
    private TextView pendingTicketStatusTv;

    private final ActivityResultLauncher<String> ticketPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleTicketSelected);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);
        workspaceTitleTv = view.findViewById(R.id.workspaceTitleTv);
        workspaceMetaTv = view.findViewById(R.id.workspaceMetaTv);
        expensesLv = view.findViewById(R.id.expensesLv);
        saldosLv = view.findViewById(R.id.saldosLv);
        workspaceCtaLayout = view.findViewById(R.id.workspaceCtaLayout);
        addMainLabelTv = view.findViewById(R.id.addMainLabelTv);
        gastosTabBtn = view.findViewById(R.id.gastosTabBtn);
        saldosTabBtn = view.findViewById(R.id.saldosTabBtn);
        FloatingActionButton mainFab = view.findViewById(R.id.addExpenseCenterFab);

        expensesAdapter = new WorkspaceAdapter(expenseRows);
        saldosAdapter = new WorkspaceAdapter(saldoRows);
        expensesLv.setAdapter(expensesAdapter);
        saldosLv.setAdapter(saldosAdapter);

        gastosTabBtn.setOnClickListener(v -> switchTab(TAB_EXPENSES));
        saldosTabBtn.setOnClickListener(v -> switchTab(TAB_SALDOS));
        mainFab.setOnClickListener(v -> handleMainAction());
        expensesLv.setOnItemClickListener((parent, v, position, id) -> showRowDetail(expenseRows.get(position)));

        switchTab(TAB_EXPENSES);
        refreshWorkspace();
        return view;
    }

    private void refreshWorkspace() {
        currentGroupId = SessionStore.getCurrentGroup(requireContext());
        if (currentGroupId == null) {
            currentGroupName = "Selecciona un piso";
            workspaceTitleTv.setText(currentGroupName);
            workspaceMetaTv.setText("Entra desde la pestaña de pisos para ver gastos y saldos.");
            expenseRows.clear();
            saldoRows.clear();
            expensesAdapter.notifyDataSetChanged();
            saldosAdapter.notifyDataSetChanged();
            workspaceCtaLayout.setVisibility(View.GONE);
            switchTab(currentTab);
            return;
        }

        workspaceCtaLayout.setVisibility(View.VISIBLE);
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            currentGroupName = doc.getString("name") == null ? "Piso actual" : doc.getString("name");
            currentUserRole = resolveCurrentUserRole(doc);
            String description = doc.getString("description");
            if (description == null || description.trim().isEmpty()) {
                description = "Sin descripción";
            }
            List<String> memberEmails = castEmails(doc.get("memberEmails"));
            List<String> memberIds = castStrings(doc.get("members"));
            String ownerEmail = resolveOwnerEmail(doc, memberIds, memberEmails);
            String membersLabel = memberEmails.isEmpty() ? "Sin datos" : String.join(", ", memberEmails);
            String meta = "Piso: " + description
                    + "\nPropietario: " + (ownerEmail.isEmpty() ? "Sin datos" : ownerEmail)
                    + "\nMiembros (" + memberEmails.size() + "): " + membersLabel;
            workspaceTitleTv.setText(currentGroupName);
            workspaceMetaTv.setText(meta);
        });
        loadExpenses();
        loadFinancialViews();
    }

    private void switchTab(String tab) {
        currentTab = tab;
        boolean showExpenses = TAB_EXPENSES.equals(tab);
        boolean showSaldos = TAB_SALDOS.equals(tab);

        expensesLv.setVisibility(showExpenses ? View.VISIBLE : View.GONE);
        saldosLv.setVisibility(showSaldos ? View.VISIBLE : View.GONE);

        updateTabStyle(gastosTabBtn, showExpenses);
        updateTabStyle(saldosTabBtn, showSaldos);

        if (showExpenses) {
            addMainLabelTv.setText("Nuevo gasto");
            workspaceCtaLayout.setVisibility(currentGroupId == null ? View.GONE : View.VISIBLE);
        } else {
            addMainLabelTv.setText("Nuevo pago");
            workspaceCtaLayout.setVisibility(currentGroupId == null ? View.GONE : View.VISIBLE);
        }
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
        } else if (TAB_SALDOS.equals(currentTab)) {
            createPaymentDialog();
        }
    }

    private void showExpenseActionDialog() {
        ViewGroup content = DialogUtils.createVerticalActions(requireContext());
        Button expenseBtn = DialogUtils.createActionButton(requireContext(), "Nuevo gasto", true);
        Button paymentBtn = DialogUtils.createActionButton(requireContext(), "Registrar pago", false);
        Button reminderBtn = DialogUtils.createActionButton(requireContext(), "Recordatorio", false);
        Button filterBtn = DialogUtils.createActionButton(requireContext(), "Filtros", false);
        Button pdfBtn = DialogUtils.createActionButton(requireContext(), "Exportar PDF", false);
        Button qrBtn = DialogUtils.createActionButton(requireContext(), "Mostrar QR", false);
        Button historyBtn = DialogUtils.createActionButton(requireContext(), "Historial", false);
        content.addView(expenseBtn);
        content.addView(paymentBtn);
        content.addView(reminderBtn);
        content.addView(filterBtn);
        content.addView(pdfBtn);
        content.addView(qrBtn);
        content.addView(historyBtn);

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nueva accion",
                "Crea un gasto, registra un pago o programa un aviso.",
                content,
                "Cerrar",
                null
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        expenseBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createExpenseDialog();
        });
        paymentBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createPaymentDialog();
        });
        reminderBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createReminderDialog();
        });
        filterBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showFiltersDialog();
        });
        pdfBtn.setOnClickListener(v -> {
            dialog.dismiss();
            exportMonthlySummaryPdf();
        });
        qrBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showCurrentGroupQrDialog();
        });
        historyBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showActivityHistoryDialog();
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

        TextView codeTv = DialogUtils.createMessageView(requireContext(), "Codigo: " + code);
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
        loadCurrentGroupMembers(members -> {
            if (members.isEmpty()) {
                Toast.makeText(requireContext(), "No hay miembros para repartir", Toast.LENGTH_SHORT).show();
                return;
            }
            View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
            pendingTicketUri = null;
            setupTypeSpinner((Spinner) form.findViewById(R.id.categorySpinner), null);
            setupPrioritySpinner((Spinner) form.findViewById(R.id.expensePrioritySpinner), null);
            setupDateField(form.findViewById(R.id.dueDateEt));
            setupSplitUi(form, members, null);
            setupTicketControls(form);

            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    "Nuevo gasto",
                    "Anade un gasto al piso actual.",
                    form,
                    "Cancelar",
                    "Guardar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
            shell.confirmBtn.setOnClickListener(v -> {
                if (saveExpense(form, null, members)) {
                    dialog.dismiss();
                }
            });
        });
    }

    private boolean saveExpense(View form, @Nullable String documentId, List<String> members) {
        String concept = ((EditText) form.findViewById(R.id.conceptEt)).getText().toString().trim();
        String amountStr = ((EditText) form.findViewById(R.id.amountEt)).getText().toString().trim();
        String category = ((Spinner) form.findViewById(R.id.categorySpinner)).getSelectedItem().toString();
        String priority = ((Spinner) form.findViewById(R.id.expensePrioritySpinner)).getSelectedItem().toString();
        String dueDateText = ((EditText) form.findViewById(R.id.dueDateEt)).getText().toString().trim();
        if (concept.isEmpty() || amountStr.isEmpty() || currentGroupId == null) return false;
        if (dueDateText.isEmpty()) {
            Toast.makeText(requireContext(), "Debes indicar fecha limite", Toast.LENGTH_SHORT).show();
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Importe no valido", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Fecha invalida. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
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
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null, false);
        setupTypeSpinner((Spinner) form.findViewById(R.id.paymentCategorySpinner), null);
        setupPrioritySpinner((Spinner) form.findViewById(R.id.paymentPrioritySpinner), "media");
        setupDateField(form.findViewById(R.id.paymentDueDateEt));
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Registrar pago",
                "Guarda un pago entre miembros del piso.",
                form,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            if (currentGroupId == null) return;
            String amountStr = ((EditText) form.findViewById(R.id.paymentAmountEt)).getText().toString().trim();
            String toEmail = ((EditText) form.findViewById(R.id.paymentToEt)).getText().toString().trim().toLowerCase(Locale.ROOT);
            String category = ((Spinner) form.findViewById(R.id.paymentCategorySpinner)).getSelectedItem().toString();
            String priority = ((Spinner) form.findViewById(R.id.paymentPrioritySpinner)).getSelectedItem().toString();
            String dueDateText = ((EditText) form.findViewById(R.id.paymentDueDateEt)).getText().toString().trim();
            if (amountStr.isEmpty() || toEmail.isEmpty() || dueDateText.isEmpty()) return;
            Date dueDate = parseDueDateOrNull(dueDateText);
            if (dueDate == null) {
                Toast.makeText(requireContext(), "Fecha invalida. Usa YYYY-MM-DD", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Importe no valido", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("amount", amount);
            String fromEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
            data.put("fromEmail", fromEmail);
            data.put("toEmail", toEmail);
            data.put("category", category);
            data.put("priority", priority.isEmpty() ? "media" : priority.toLowerCase(Locale.ROOT));
            data.put("status", "pending");
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("dueAt", dueDate);
            data.put("dueDateText", dueDateText);
            db.collection("payments").add(data).addOnSuccessListener(task -> {
                logActivity("payment_created", "Pago a " + toEmail, amount, "payment");
                syncPaymentDeadline(task.getId(), "Pago directo", amount, fromEmail, toEmail, dueDate, priority);
                loadExpenses();
                loadFinancialViews();
                dialog.dismiss();
            });
        });
    }

    private void createReminderDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reminder, null, false);
        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Nuevo recordatorio",
                "Programa un aviso recurrente para este piso.",
                form,
                "Cancelar",
                "Programar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String title = ((EditText) form.findViewById(R.id.reminderTitleEt)).getText().toString().trim();
            String interval = ((EditText) form.findViewById(R.id.reminderIntervalEt)).getText().toString().trim().toLowerCase(Locale.ROOT);
            if (title.isEmpty() || interval.isEmpty()) return;
            long intervalMs = interval.equals("diario") ? 24L * 60 * 60 * 1000 : interval.equals("mensual") ? 30L * 24 * 60 * 60 * 1000 : 7L * 24 * 60 * 60 * 1000;
            int reminderId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            long firstTrigger = System.currentTimeMillis() + 10_000;
            ReminderScheduler.schedule(requireContext(), reminderId, "FlatShare: " + title, "Revisa tus pagos compartidos", firstTrigger, intervalMs);

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("interval", interval);
            data.put("ownerUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            data.put("groupId", currentGroupId);
            data.put("createdAt", FieldValue.serverTimestamp());
            db.collection("reminders").add(data);

            Toast.makeText(requireContext(), "Recordatorio programado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void loadExpenses() {
        if (currentGroupId == null) return;
        DecimalFormat df = new DecimalFormat("0.00");
        expenseRows.clear();
        db.collection("expenses").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(result -> {
            for (DocumentSnapshot doc : result.getDocuments()) {
                Double amountValue = doc.getDouble("amount");
                String concept = doc.getString("concept");
                String payerEmail = doc.getString("payerEmail");
                String category = doc.getString("category");
                String dueDateText = doc.getString("dueDateText");
                String subtitle = (payerEmail == null ? "Gasto compartido" : "Pagado por " + payerEmail)
                        + (category == null || category.isEmpty() ? "" : " - " + category)
                        + (dueDateText == null || dueDateText.isEmpty() ? "" : " - vence " + dueDateText);
                if (!passesFiltersExpense(doc, payerEmail, category)) continue;
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
                    Double amountValue = doc.getDouble("amount");
                    String toEmail = doc.getString("toEmail");
                    String status = doc.getString("status");
                    Date dueAt = doc.getDate("dueAt");
                    String dueDateText = doc.getString("dueDateText");
                    boolean confirmed = "confirmed".equals(status);
                    boolean overdue = !confirmed && dueAt != null && dueAt.getTime() < System.currentTimeMillis();
                    String statusLabel = confirmed ? "Confirmado" : (overdue ? "Vencido" : "Pendiente");
                    String subtitle = (toEmail == null ? "Pago registrado" : "A " + toEmail)
                            + " - " + statusLabel
                            + (dueDateText == null || dueDateText.isEmpty() ? "" : " - vence " + dueDateText);
                    if (!passesFiltersPayment(doc)) continue;
                    expenseRows.add(new WorkspaceRow(
                            doc.getId(),
                            "payment",
                            "Pago enviado",
                            subtitle,
                            amountValue == null ? "0.00 EUR" : df.format(amountValue) + " EUR",
                            doc
                    ));
                }
                expensesAdapter.notifyDataSetChanged();
            });
        });
    }

    private void loadFinancialViews() {
        if (currentGroupId == null) return;
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(groupDoc -> {
            List<String> members = castEmails(groupDoc.get("memberEmails"));
            if (members.isEmpty()) {
                saldoRows.clear();
                saldosAdapter.notifyDataSetChanged();
                return;
            }

            Map<String, Double> net = new HashMap<>();
            for (String member : members) {
                net.put(member, 0.0);
            }

            db.collection("expenses").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(expenses -> {
                for (DocumentSnapshot doc : expenses.getDocuments()) {
                    double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                    String payerEmail = doc.getString("payerEmail");
                    String custom = doc.getString("customSplit");

                    if (custom == null || custom.isEmpty()) {
                        double part = amount / members.size();
                        for (String member : members) {
                            net.put(member, net.getOrDefault(member, 0.0) - part);
                        }
                    } else {
                        String[] parts = custom.split(",");
                        for (String partEntry : parts) {
                            String[] kv = partEntry.trim().split(":");
                            if (kv.length != 2) continue;
                            String email = kv[0].trim().toLowerCase(Locale.ROOT);
                            double percent = Double.parseDouble(kv[1].trim());
                            net.put(email, net.getOrDefault(email, 0.0) - amount * (percent / 100.0));
                        }
                    }

                    if (payerEmail != null) {
                        String normalized = payerEmail.toLowerCase(Locale.ROOT);
                        net.put(normalized, net.getOrDefault(normalized, 0.0) + amount);
                    }
                }
                applyPaymentsAndRenderFinancials(net);
            });
        });
    }

    private void applyPaymentsAndRenderFinancials(Map<String, Double> net) {
        db.collection("payments").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(payments -> {
            for (DocumentSnapshot payment : payments.getDocuments()) {
                String status = payment.getString("status");
                if (!"confirmed".equals(status)) continue;
                double amount = payment.getDouble("amount") == null ? 0.0 : payment.getDouble("amount");
                String from = payment.getString("fromEmail");
                String to = payment.getString("toEmail");
                if (from != null) net.put(from, net.getOrDefault(from, 0.0) + amount);
                if (to != null) net.put(to, net.getOrDefault(to, 0.0) - amount);
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
            saldoRows.add(new WorkspaceRow(
                    entry.getKey(),
                    "saldo",
                    entry.getKey(),
                    value >= 0 ? "Saldo a favor" : "Saldo pendiente",
                    df.format(value) + " EUR",
                    null
            ));
        }
        saldosAdapter.notifyDataSetChanged();
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
        if ("payment".equals(row.type)) {
            String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
            String toEmail = row.snapshot == null ? null : row.snapshot.getString("toEmail");
            String status = row.snapshot == null ? null : row.snapshot.getString("status");
            boolean canConfirm = row.snapshot != null && "pending".equals(status) && myEmail.equals(toEmail);
            View content = DialogUtils.createMessageView(requireContext(), row.subtitle + "\n" + row.amount);
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    row.title,
                    "Detalle del movimiento",
                    content,
                    canConfirm ? "Rechazar" : null,
                    canConfirm ? "Confirmar" : "Cerrar"
            );
            AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
            if (canConfirm) {
                shell.cancelBtn.setOnClickListener(v -> {
                    updatePaymentStatus(row.id, "rejected");
                    dialog.dismiss();
                });
                shell.confirmBtn.setOnClickListener(v -> {
                    updatePaymentStatus(row.id, "confirmed");
                    dialog.dismiss();
                });
            } else {
                shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
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
            deleteExpense(row);
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
            setupSplitUi(form, members, row.snapshot.getString("customSplit"));
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
                if (saveExpense(form, row.id, members)) {
                    dialog.dismiss();
                }
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
                    Toast.makeText(requireContext(), "Hay importes de reparto no validos", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Anade al menos una linea de reparto", Toast.LENGTH_SHORT).show();
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

    private void showFiltersDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filters, null, false);
        EditText categoryEt = form.findViewById(R.id.filterCategoryEt);
        EditText personEt = form.findViewById(R.id.filterPersonEt);
        EditText fromDateEt = form.findViewById(R.id.filterFromDateEt);
        EditText toDateEt = form.findViewById(R.id.filterToDateEt);
        categoryEt.setText(filterCategory == null ? "" : filterCategory);
        personEt.setText(filterPersonEmail == null ? "" : filterPersonEmail);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        if (filterFromMs != null) fromDateEt.setText(fmt.format(new Date(filterFromMs)));
        if (filterToMs != null) toDateEt.setText(fmt.format(new Date(filterToMs)));

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Filtros",
                "Filtra por categoria, persona y rango de fechas.",
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
            loadExpenses();
            dialog.dismiss();
        });
        shell.confirmBtn.setOnClickListener(v -> {
            String category = categoryEt.getText().toString().trim().toLowerCase(Locale.ROOT);
            String person = personEt.getText().toString().trim().toLowerCase(Locale.ROOT);
            String from = fromDateEt.getText().toString().trim();
            String to = toDateEt.getText().toString().trim();
            filterCategory = category.isEmpty() ? null : category;
            filterPersonEmail = person.isEmpty() ? null : person;
            try {
                filterFromMs = from.isEmpty() ? null : fmt.parse(from).getTime();
                if (to.isEmpty()) {
                    filterToMs = null;
                } else {
                    Calendar endDay = Calendar.getInstance();
                    endDay.setTime(fmt.parse(to));
                    endDay.set(Calendar.HOUR_OF_DAY, 23);
                    endDay.set(Calendar.MINUTE, 59);
                    endDay.set(Calendar.SECOND, 59);
                    endDay.set(Calendar.MILLISECOND, 999);
                    filterToMs = endDay.getTimeInMillis();
                }
            } catch (ParseException e) {
                Toast.makeText(requireContext(), "Formato de fecha no válido (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
                return;
            }
            loadExpenses();
            dialog.dismiss();
        });
    }

    private boolean passesFiltersExpense(DocumentSnapshot doc, @Nullable String payerEmail, @Nullable String category) {
        if (filterPersonEmail != null && (payerEmail == null || !payerEmail.equalsIgnoreCase(filterPersonEmail))) {
            return false;
        }
        if (filterCategory != null && (category == null || !category.toLowerCase(Locale.ROOT).contains(filterCategory))) {
            return false;
        }
        return passesDateFilter(doc);
    }

    private boolean passesFiltersPayment(DocumentSnapshot doc) {
        if (filterPersonEmail != null) {
            String from = doc.getString("fromEmail");
            String to = doc.getString("toEmail");
            boolean match = (from != null && from.equalsIgnoreCase(filterPersonEmail))
                    || (to != null && to.equalsIgnoreCase(filterPersonEmail));
            if (!match) return false;
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
            ReminderScheduler.scheduleOneTime(requireContext(), reminderA, "Pago vence manana", title + " - " + amountLabel, oneDayBefore);
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
                        message.append("- ").append(action == null ? "accion" : action)
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

    private interface MembersCallback {
        void onLoaded(List<String> members);
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
