package com.sergio.flatshare.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.util.ReminderScheduler;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<CalendarRow> allRows = new ArrayList<>();
    private final List<CalendarRow> filteredRows = new ArrayList<>();
    private CalendarAdapter adapter;
    private TextView selectedDateTv;
    private long selectedDateMs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        CalendarView calendarView = view.findViewById(R.id.paymentsCalendarView);
        ListView eventsLv = view.findViewById(R.id.calendarEventsLv);
        selectedDateTv = view.findViewById(R.id.calendarSelectedDateTv);

        adapter = new CalendarAdapter(filteredRows);
        eventsLv.setAdapter(adapter);
        selectedDateMs = startOfDay(System.currentTimeMillis());
        updateSelectedDateLabel();

        calendarView.setOnDateChangeListener((v, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            selectedDateMs = c.getTimeInMillis();
            updateSelectedDateLabel();
            applyDateFilter();
        });

        loadDeadlines();
        return view;
    }

    private void updateSelectedDateLabel() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        selectedDateTv.setText("Fecha seleccionada: " + format.format(new Date(selectedDateMs)));
    }

    private void loadDeadlines() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        allRows.clear();

        db.collection("payment_deadlines")
                .whereEqualTo("debtorEmail", myEmail)
                .get()
                .addOnSuccessListener(result -> {
                    DecimalFormat df = new DecimalFormat("0.00");
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        Date due = doc.getDate("dueAt");
                        if (due == null) continue;
                        String concept = doc.getString("concept");
                        String creditor = doc.getString("creditorEmail");
                        String groupName = doc.getString("groupName");
                        String priority = doc.getString("priority");
                        Double amount = doc.getDouble("amount");
                        String title = concept == null || concept.isEmpty() ? "Pago pendiente" : concept;
                        String subtitle = (groupName == null ? "Piso" : groupName) + " - Pagar a " + (creditor == null ? "miembro" : creditor);
                        String amountText = (amount == null ? "0.00" : df.format(amount)) + " EUR";
                        allRows.add(new CalendarRow(title, subtitle, amountText, due.getTime(), doc.getId(), normalizePriority(priority)));
                        scheduleDeadlineNotifications(title, amountText, due.getTime(), doc.getId());
                    }
                    loadRegisteredPayments(myEmail);
                });
    }

    private void loadRegisteredPayments(String myEmail) {
        Set<String> seen = new HashSet<>();
        DecimalFormat df = new DecimalFormat("0.00");

        db.collection("payments")
                .whereEqualTo("fromEmail", myEmail)
                .get()
                .addOnSuccessListener(sent -> {
                    appendPaymentRows(sent.getDocuments(), myEmail, df, seen);
                    db.collection("payments")
                            .whereEqualTo("toEmail", myEmail)
                            .get()
                            .addOnSuccessListener(received -> {
                                appendPaymentRows(received.getDocuments(), myEmail, df, seen);
                                applyDateFilter();
                            })
                            .addOnFailureListener(e -> applyDateFilter());
                })
                .addOnFailureListener(e -> applyDateFilter());
    }

    private void appendPaymentRows(List<DocumentSnapshot> docs, String myEmail, DecimalFormat df, Set<String> seen) {
        for (DocumentSnapshot doc : docs) {
            String id = doc.getId();
            if (seen.contains(id)) continue;
            seen.add(id);

            Date createdAt = doc.getDate("createdAt");
            if (createdAt == null) continue;
            Double amount = doc.getDouble("amount");
            String from = doc.getString("fromEmail");
            String to = doc.getString("toEmail");
            String status = doc.getString("status");
            String priority = doc.getString("priority");
            String title = "Pago registrado";
            String direction = myEmail.equalsIgnoreCase(from == null ? "" : from)
                    ? "Enviado a " + (to == null ? "miembro" : to)
                    : "Recibido de " + (from == null ? "miembro" : from);
            Date dueAt = doc.getDate("dueAt");
            boolean confirmed = "confirmed".equals(status);
            boolean overdue = !confirmed && dueAt != null && dueAt.getTime() < System.currentTimeMillis();
            String state = confirmed ? "Confirmado" : (overdue ? "Vencido" : "Pendiente");
            String subtitle = direction + " - " + state;
            String amountText = (amount == null ? "0.00" : df.format(amount)) + " EUR";
            allRows.add(new CalendarRow(title, subtitle, amountText, createdAt.getTime(), "payment_" + id, normalizePriority(priority)));
        }
    }

    private void applyDateFilter() {
        filteredRows.clear();
        long dayStart = startOfDay(selectedDateMs);
        long dayEnd = dayStart + 24L * 60L * 60L * 1000L - 1L;
        for (CalendarRow row : allRows) {
            if (row.dueAtMs >= dayStart && row.dueAtMs <= dayEnd) {
                filteredRows.add(row);
            }
        }
        if (filteredRows.isEmpty()) {
            filteredRows.add(new CalendarRow("Sin pagos para este dia", "No hay vencimientos registrados", "0.00 EUR", dayStart, "empty", ""));
        }
        adapter.notifyDataSetChanged();
    }

    private String normalizePriority(@Nullable String priority) {
        if (priority == null) return "baja";
        String p = priority.trim().toLowerCase(Locale.ROOT);
        if ("alta".equals(p) || "media".equals(p) || "baja".equals(p)) return p;
        return "baja";
    }

    private long startOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void scheduleDeadlineNotifications(String title, String amount, long dueAtMs, String docId) {
        long now = System.currentTimeMillis();
        long oneDayBefore = dueAtMs - 24L * 60L * 60L * 1000L;
        int reminderA = Math.abs((docId + "_a").hashCode());
        int reminderB = Math.abs((docId + "_b").hashCode());

        if (oneDayBefore > now) {
            ReminderScheduler.scheduleOneTime(requireContext(), reminderA, "Pago vence manana", title + " - " + amount, oneDayBefore);
        }
        if (dueAtMs > now) {
            ReminderScheduler.scheduleOneTime(requireContext(), reminderB, "Pago vence hoy", title + " - " + amount, dueAtMs);
        }
    }

    private static class CalendarRow {
        final String title;
        final String subtitle;
        final String amount;
        final long dueAtMs;
        final String id;
        final String priority;

        CalendarRow(String title, String subtitle, String amount, long dueAtMs, String id, String priority) {
            this.title = title;
            this.subtitle = subtitle;
            this.amount = amount;
            this.dueAtMs = dueAtMs;
            this.id = id;
            this.priority = priority;
        }
    }

    private class CalendarAdapter extends BaseAdapter {
        private final List<CalendarRow> rows;

        CalendarAdapter(List<CalendarRow> rows) {
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
            CalendarRow row = rows.get(position);
            TextView titleTv = view.findViewById(R.id.rowTitleTv);
            TextView subtitleTv = view.findViewById(R.id.rowSubtitleTv);
            TextView amountTv = view.findViewById(R.id.rowAmountTv);
            titleTv.setText(row.title);
            subtitleTv.setText(row.subtitle);
            amountTv.setText(row.amount);

            if ("alta".equals(row.priority)) {
                amountTv.setTextColor(requireContext().getColor(R.color.status_danger));
                subtitleTv.setTextColor(requireContext().getColor(R.color.status_danger));
            } else if ("media".equals(row.priority)) {
                amountTv.setTextColor(requireContext().getColor(R.color.status_warning));
                subtitleTv.setTextColor(requireContext().getColor(R.color.status_warning));
            } else if ("baja".equals(row.priority)) {
                amountTv.setTextColor(requireContext().getColor(R.color.status_success));
                subtitleTv.setTextColor(requireContext().getColor(R.color.status_success));
            } else {
                amountTv.setTextColor(requireContext().getColor(R.color.text_light));
                subtitleTv.setTextColor(requireContext().getColor(R.color.text_muted));
            }
            return view;
        }
    }
}
