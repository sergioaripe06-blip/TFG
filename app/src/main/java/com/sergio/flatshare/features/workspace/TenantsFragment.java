package com.sergio.flatshare.features.workspace;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.ui.DialogUtils;
import com.sergio.flatshare.core.session.SessionStore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantsFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<TenantRow> tenants = new ArrayList<>();
    private TenantsAdapter adapter;
    private TextView emptyTenantsTv;
    private String currentGroupId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tenants, container, false);
        ListView tenantsLv = view.findViewById(R.id.tenantsLv);
        emptyTenantsTv = view.findViewById(R.id.emptyTenantsTv);
        adapter = new TenantsAdapter();
        tenantsLv.setAdapter(adapter);
        tenantsLv.setOnItemLongClickListener((parent, itemView, position, id) -> {
            showTenantDetails(tenants.get(position));
            return true;
        });
        loadTenants();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTenants();
    }

    public void refreshTenants() {
        loadTenants();
    }

    private void loadTenants() {
        if (!isAdded()) return;
        currentGroupId = SessionStore.getCurrentGroup(requireContext());
        if (currentGroupId == null || currentGroupId.trim().isEmpty()) {
            tenants.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            if (emptyTenantsTv != null) emptyTenantsTv.setVisibility(View.VISIBLE);
            return;
        }
        db.collection("groups").document(currentGroupId).get().addOnSuccessListener(groupDoc -> {
            List<String> memberIds = castStrings(groupDoc.get("members"));
            List<String> memberEmails = castEmails(groupDoc.get("memberEmails"));
            String ownerId = safe(groupDoc.getString("ownerId"));
            String ownerEmail = safeLower(groupDoc.getString("ownerEmail"));
            if (!ownerId.isEmpty()) {
                memberIds.removeIf(uid -> ownerId.equals(uid));
            }
            if (!ownerEmail.isEmpty()) {
                memberEmails.removeIf(email -> ownerEmail.equals(safeLower(email)));
                resolveTenantRows(memberIds, memberEmails);
                return;
            }
            if (!ownerId.isEmpty()) {
                db.collection("users").document(ownerId).get().addOnSuccessListener(ownerDoc -> {
                    String resolvedOwnerEmail = safeLower(ownerDoc.getString("email"));
                    if (!resolvedOwnerEmail.isEmpty()) {
                        memberEmails.removeIf(email -> resolvedOwnerEmail.equals(safeLower(email)));
                    }
                    resolveTenantRows(memberIds, memberEmails);
                }).addOnFailureListener(e -> resolveTenantRows(memberIds, memberEmails));
                return;
            }
            resolveTenantRows(memberIds, memberEmails);
        });
    }

    private void resolveTenantRows(List<String> memberIds, List<String> memberEmails) {
        tenants.clear();
        if (memberEmails.isEmpty()) {
            adapter.notifyDataSetChanged();
            emptyTenantsTv.setVisibility(View.VISIBLE);
            return;
        }
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : memberIds) {
            if (uid != null && !uid.trim().isEmpty()) {
                tasks.add(db.collection("users").document(uid).get());
            }
        }
        if (tasks.isEmpty()) {
            resolveTenantRowsByEmail(memberEmails);
            return;
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(done -> {
            Map<String, TenantRow> byEmail = new HashMap<>();
            for (Task<DocumentSnapshot> task : tasks) {
                if (!task.isSuccessful() || task.getResult() == null) continue;
                DocumentSnapshot userDoc = task.getResult();
                String email = safeLower(userDoc.getString("email"));
                if (email.isEmpty()) continue;
                String displayName = userDoc.getString("name");
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = userDoc.getString("displayName");
                }
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = userDoc.getString("username");
                }
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = email;
                }
                byEmail.put(email, new TenantRow(
                        email,
                        displayName.trim(),
                        safe(userDoc.getString("username")),
                        safe(userDoc.getString("phone")),
                        safe(userDoc.getString("birthDate"))
                ));
            }
            for (String email : memberEmails) {
                String normalized = safeLower(email);
                TenantRow row = byEmail.get(normalized);
                tenants.add(row == null ? new TenantRow(normalized, normalized, "", "", "") : row);
            }
            adapter.notifyDataSetChanged();
            emptyTenantsTv.setVisibility(tenants.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            tenants.clear();
            adapter.notifyDataSetChanged();
            emptyTenantsTv.setVisibility(View.VISIBLE);
        });
    }

    private void resolveTenantRowsByEmail(List<String> memberEmails) {
        List<Task<?>> tasks = new ArrayList<>();
        Map<String, TenantRow> byEmail = new HashMap<>();
        for (String rawEmail : memberEmails) {
            String email = safeLower(rawEmail);
            if (email.isEmpty()) continue;
            Task<?> task = db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(result -> {
                        if (result.isEmpty()) {
                            byEmail.put(email, new TenantRow(email, email, "", "", ""));
                            return;
                        }
                        DocumentSnapshot userDoc = result.getDocuments().get(0);
                        String displayName = safe(userDoc.getString("name"));
                        if (displayName.isEmpty()) displayName = safe(userDoc.getString("displayName"));
                        if (displayName.isEmpty()) displayName = safe(userDoc.getString("username"));
                        if (displayName.isEmpty()) displayName = email;
                        byEmail.put(email, new TenantRow(
                                email,
                                displayName,
                                safe(userDoc.getString("username")),
                                safe(userDoc.getString("phone")),
                                safe(userDoc.getString("birthDate"))
                        ));
                    })
                    .addOnFailureListener(e -> byEmail.put(email, new TenantRow(email, email, "", "", "")));
            tasks.add(task);
        }
        if (tasks.isEmpty()) {
            for (String email : memberEmails) {
                String normalized = safeLower(email);
                if (!normalized.isEmpty()) tenants.add(new TenantRow(normalized, normalized, "", "", ""));
            }
            adapter.notifyDataSetChanged();
            emptyTenantsTv.setVisibility(tenants.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            tenants.clear();
            for (String email : memberEmails) {
                String normalized = safeLower(email);
                if (normalized.isEmpty()) continue;
                TenantRow row = byEmail.get(normalized);
                tenants.add(row == null ? new TenantRow(normalized, normalized, "", "", "") : row);
            }
            adapter.notifyDataSetChanged();
            emptyTenantsTv.setVisibility(tenants.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showTenantDetails(TenantRow tenant) {
        if (currentGroupId == null) return;
        db.collection("expenses").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(expenses -> {
            db.collection("payments").whereEqualTo("groupId", currentGroupId).get().addOnSuccessListener(payments -> {
                int paidCount = 0;
                double paidAmount = 0.0;
                int paymentCount = 0;
                double paymentAmount = 0.0;
                for (DocumentSnapshot expense : expenses.getDocuments()) {
                    String payerEmail = safeLower(expense.getString("payerEmail"));
                    if (!payerEmail.equals(tenant.email)) continue;
                    paidCount++;
                    Double amount = expense.getDouble("amount");
                    paidAmount += amount == null ? 0.0 : amount;
                }
                for (DocumentSnapshot payment : payments.getDocuments()) {
                    String from = safeLower(payment.getString("fromEmail"));
                    String to = safeLower(payment.getString("toEmail"));
                    if (!from.equals(tenant.email) && !to.equals(tenant.email)) continue;
                    paymentCount++;
                    Double amount = payment.getDouble("amount");
                    paymentAmount += amount == null ? 0.0 : amount;
                }
                DecimalFormat df = new DecimalFormat("0.00");
                String detail = "Nombre: " + tenant.displayName
                        + "\nCorreo: " + tenant.email
                        + "\nUsuario: " + (tenant.username.isEmpty() ? "-" : tenant.username)
                        + "\nTeléfono: " + (tenant.phone.isEmpty() ? "-" : tenant.phone)
                        + "\nNacimiento: " + (tenant.birthDate.isEmpty() ? "-" : tenant.birthDate)
                        + "\n\nGastos pagados: " + paidCount + " (" + df.format(paidAmount) + " EUR)"
                        + "\nPagos asociados: " + paymentCount + " (" + df.format(paymentAmount) + " EUR)";
                View content = DialogUtils.createMessageView(requireContext(), detail);
                DialogUtils.Shell shell = DialogUtils.buildShell(
                        requireContext(),
                        tenant.displayName,
                        "Información del inquilino",
                        content,
                        null,
                        "Cerrar"
                );
                AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
                shell.confirmBtn.setOnClickListener(v -> dialog.dismiss());
            });
        });
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

    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private String safeLower(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static class TenantRow {
        final String email;
        final String displayName;
        final String username;
        final String phone;
        final String birthDate;

        TenantRow(String email, String displayName, String username, String phone, String birthDate) {
            this.email = email;
            this.displayName = displayName;
            this.username = username;
            this.phone = phone;
            this.birthDate = birthDate;
        }
    }

    private class TenantsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return tenants.size();
        }

        @Override
        public Object getItem(int position) {
            return tenants.get(position);
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
            TenantRow row = tenants.get(position);
            TextView titleTv = view.findViewById(R.id.rowTitleTv);
            TextView subtitleTv = view.findViewById(R.id.rowSubtitleTv);
            TextView amountTv = view.findViewById(R.id.rowAmountTv);
            titleTv.setText(row.displayName);
            subtitleTv.setText("Correo: " + row.email + "\nMantén pulsado para ver datos y gastos");
            amountTv.setText("");
            return view;
        }
    }
}
