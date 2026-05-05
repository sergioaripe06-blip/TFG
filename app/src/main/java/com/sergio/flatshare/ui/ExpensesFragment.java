package com.sergio.flatshare.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.DialogUtils;
import com.sergio.flatshare.util.ReminderScheduler;
import com.sergio.flatshare.util.SessionStore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpensesFragment extends Fragment {
    private static final String TAB_EXPENSES = "expenses";
    private static final String TAB_SALDOS = "saldos";
    private static final String TAB_BALANCES = "balances";
    private static final String TAB_PHOTOS = "photos";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<WorkspaceRow> expenseRows = new ArrayList<>();
    private final List<WorkspaceRow> saldoRows = new ArrayList<>();
    private final List<WorkspaceRow> balanceRows = new ArrayList<>();

    private WorkspaceAdapter expensesAdapter;
    private WorkspaceAdapter saldosAdapter;
    private WorkspaceAdapter balancesAdapter;

    private TextView workspaceTitleTv;
    private TextView workspaceMetaTv;
    private ListView expensesLv;
    private ListView saldosLv;
    private ListView balancesLv;
    private View photosEmptyState;
    private View workspaceCtaLayout;
    private TextView addMainLabelTv;
    private Button gastosTabBtn;
    private Button saldosTabBtn;
    private Button balancesTabBtn;
    private Button fotosTabBtn;

    private String currentTab = TAB_EXPENSES;
    private String currentGroupId;
    private String currentGroupName = "Piso";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);
        workspaceTitleTv = view.findViewById(R.id.workspaceTitleTv);
        workspaceMetaTv = view.findViewById(R.id.workspaceMetaTv);
        expensesLv = view.findViewById(R.id.expensesLv);
        saldosLv = view.findViewById(R.id.saldosLv);
        balancesLv = view.findViewById(R.id.balancesLv);
        photosEmptyState = view.findViewById(R.id.photosEmptyState);
        workspaceCtaLayout = view.findViewById(R.id.workspaceCtaLayout);
        addMainLabelTv = view.findViewById(R.id.addMainLabelTv);
        gastosTabBtn = view.findViewById(R.id.gastosTabBtn);
        saldosTabBtn = view.findViewById(R.id.saldosTabBtn);
        balancesTabBtn = view.findViewById(R.id.balancesTabBtn);
        fotosTabBtn = view.findViewById(R.id.fotosTabBtn);
        FloatingActionButton mainFab = view.findViewById(R.id.addExpenseCenterFab);

        expensesAdapter = new WorkspaceAdapter(expenseRows);
        saldosAdapter = new WorkspaceAdapter(saldoRows);
        balancesAdapter = new WorkspaceAdapter(balanceRows);
        expensesLv.setAdapter(expensesAdapter);
        saldosLv.setAdapter(saldosAdapter);
        balancesLv.setAdapter(balancesAdapter);

        gastosTabBtn.setOnClickListener(v -> switchTab(TAB_EXPENSES));
        saldosTabBtn.setOnClickListener(v -> switchTab(TAB_SALDOS));
        balancesTabBtn.setOnClickListener(v -> switchTab(TAB_BALANCES));
        fotosTabBtn.setOnClickListener(v -> switchTab(TAB_PHOTOS));
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
            workspaceMetaTv.setText("Entra desde la pestana de pisos para ver gastos, saldos, balances y fotos.");
            expenseRows.clear();
            saldoRows.clear();
            balanceRows.clear();
            expensesAdapter.notifyDataSetChanged();
            saldosAdapter.notifyDataSetChanged();
            balancesAdapter.notifyDataSetChanged();
            workspaceCtaLayout.setVisibility(View.GONE);
            switchTab(currentTab);
            return;
        }

        workspaceCtaLayout.setVisibility(View.VISIBLE);
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            currentGroupName = doc.getString("name") == null ? "Piso actual" : doc.getString("name");
            Object membersField = doc.get("memberEmails");
            int memberCount = membersField instanceof List ? ((List<?>) membersField).size() : 0;
            String shareCode = doc.getString("shareCode");
            String meta = memberCount + " participantes";
            if (shareCode != null && !shareCode.trim().isEmpty()) {
                meta = meta + " - Codigo " + shareCode;
            }
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
        boolean showBalances = TAB_BALANCES.equals(tab);
        boolean showPhotos = TAB_PHOTOS.equals(tab);

        expensesLv.setVisibility(showExpenses ? View.VISIBLE : View.GONE);
        saldosLv.setVisibility(showSaldos ? View.VISIBLE : View.GONE);
        balancesLv.setVisibility(showBalances ? View.VISIBLE : View.GONE);
        photosEmptyState.setVisibility(showPhotos ? View.VISIBLE : View.GONE);

        updateTabStyle(gastosTabBtn, showExpenses);
        updateTabStyle(saldosTabBtn, showSaldos);
        updateTabStyle(balancesTabBtn, showBalances);
        updateTabStyle(fotosTabBtn, showPhotos);

        if (showExpenses) {
            addMainLabelTv.setText("Nuevo gasto");
            workspaceCtaLayout.setVisibility(currentGroupId == null ? View.GONE : View.VISIBLE);
        } else if (showSaldos || showBalances) {
            addMainLabelTv.setText("Nuevo pago");
            workspaceCtaLayout.setVisibility(currentGroupId == null ? View.GONE : View.VISIBLE);
        } else {
            addMainLabelTv.setText("Subir foto");
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
        } else if (TAB_SALDOS.equals(currentTab) || TAB_BALANCES.equals(currentTab)) {
            createPaymentDialog();
        } else {
            Toast.makeText(requireContext(), "La subida de fotos la conectamos en el siguiente paso", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExpenseActionDialog() {
        ViewGroup content = DialogUtils.createVerticalActions(requireContext());
        Button expenseBtn = DialogUtils.createActionButton(requireContext(), "Nuevo gasto", true);
        Button paymentBtn = DialogUtils.createActionButton(requireContext(), "Registrar pago", false);
        Button reminderBtn = DialogUtils.createActionButton(requireContext(), "Recordatorio", false);
        content.addView(expenseBtn);
        content.addView(paymentBtn);
        content.addView(reminderBtn);

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
    }

    private void createExpenseDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
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
            if (saveExpense(form, null)) {
                dialog.dismiss();
            }
        });
    }

    private boolean saveExpense(View form, @Nullable String documentId) {
        String concept = ((EditText) form.findViewById(R.id.conceptEt)).getText().toString().trim();
        String amountStr = ((EditText) form.findViewById(R.id.amountEt)).getText().toString().trim();
        String customSplit = ((EditText) form.findViewById(R.id.customSplitEt)).getText().toString().trim().toLowerCase(Locale.ROOT);
        if (concept.isEmpty() || amountStr.isEmpty() || currentGroupId == null) return false;

        double amount = Double.parseDouble(amountStr);
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", currentGroupId);
        data.put("concept", concept);
        data.put("amount", amount);
        data.put("payerId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        data.put("payerEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT));
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("customSplit", customSplit);

        if (documentId == null) {
            db.collection("expenses").add(data).addOnSuccessListener(task -> {
                loadExpenses();
                loadFinancialViews();
            });
        } else {
            db.collection("expenses").document(documentId).update(data).addOnSuccessListener(task -> {
                loadExpenses();
                loadFinancialViews();
            });
        }
        return true;
    }

    private void createPaymentDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null, false);
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
            if (amountStr.isEmpty() || toEmail.isEmpty()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("groupId", currentGroupId);
            data.put("amount", Double.parseDouble(amountStr));
            data.put("fromEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT));
            data.put("toEmail", toEmail);
            data.put("createdAt", FieldValue.serverTimestamp());
            db.collection("payments").add(data).addOnSuccessListener(task -> {
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
                String subtitle = payerEmail == null ? "Gasto compartido" : "Pagado por " + payerEmail;
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
                    String subtitle = toEmail == null ? "Pago registrado" : "A " + toEmail;
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
                balanceRows.clear();
                saldosAdapter.notifyDataSetChanged();
                balancesAdapter.notifyDataSetChanged();
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
                double amount = payment.getDouble("amount") == null ? 0.0 : payment.getDouble("amount");
                String from = payment.getString("fromEmail");
                String to = payment.getString("toEmail");
                if (from != null) net.put(from, net.getOrDefault(from, 0.0) + amount);
                if (to != null) net.put(to, net.getOrDefault(to, 0.0) - amount);
            }
            renderSaldos(net);
            renderBalances(net);
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

    private void renderBalances(Map<String, Double> net) {
        DecimalFormat df = new DecimalFormat("0.00");
        balanceRows.clear();

        List<BalanceNode> creditors = new ArrayList<>();
        List<BalanceNode> debtors = new ArrayList<>();
        for (Map.Entry<String, Double> entry : net.entrySet()) {
            double value = entry.getValue();
            if (value > 0.009) {
                creditors.add(new BalanceNode(entry.getKey(), value));
            } else if (value < -0.009) {
                debtors.add(new BalanceNode(entry.getKey(), -value));
            }
        }

        int creditorIndex = 0;
        int debtorIndex = 0;
        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            BalanceNode creditor = creditors.get(creditorIndex);
            BalanceNode debtor = debtors.get(debtorIndex);
            double payment = Math.min(creditor.remaining, debtor.remaining);
            balanceRows.add(new WorkspaceRow(
                    debtor.email + "_" + creditor.email,
                    "balance",
                    debtor.email,
                    "Debe pagar a " + creditor.email,
                    df.format(payment) + " EUR",
                    null
            ));
            creditor.remaining -= payment;
            debtor.remaining -= payment;
            if (creditor.remaining <= 0.009) creditorIndex++;
            if (debtor.remaining <= 0.009) debtorIndex++;
        }

        if (balanceRows.isEmpty()) {
            balanceRows.add(new WorkspaceRow(
                    "balanced",
                    "balance",
                    "Todo cuadrado",
                    "No hay pagos pendientes entre miembros",
                    "0.00 EUR",
                    null
            ));
        }
        balancesAdapter.notifyDataSetChanged();
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

    private void showRowDetail(WorkspaceRow row) {
        if ("payment".equals(row.type)) {
            View content = DialogUtils.createMessageView(requireContext(), row.subtitle + "\n" + row.amount);
            DialogUtils.Shell shell = DialogUtils.buildShell(
                    requireContext(),
                    row.title,
                    "Detalle del movimiento",
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
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null, false);
        ((EditText) form.findViewById(R.id.conceptEt)).setText(row.snapshot.getString("concept"));
        Double amountValue = row.snapshot.getDouble("amount");
        ((EditText) form.findViewById(R.id.amountEt)).setText(amountValue == null ? "" : String.valueOf(amountValue));
        ((EditText) form.findViewById(R.id.customSplitEt)).setText(row.snapshot.getString("customSplit"));

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
            if (saveExpense(form, row.id)) {
                dialog.dismiss();
            }
        });
    }

    private void deleteExpense(WorkspaceRow row) {
        db.collection("expenses").document(row.id).delete().addOnSuccessListener(v -> {
            loadExpenses();
            loadFinancialViews();
        });
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
        double remaining;

        BalanceNode(String email, double remaining) {
            this.email = email;
            this.remaining = remaining;
        }
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
            return view;
        }
    }
}
