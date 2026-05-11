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
                        allRows.add(new CalendarRow(title, subtitle, amountText, due.getTime(), doc.getId(), normalizePriority(priority), "none", 0, false));
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
                                loadManualReminders(myEmail);
                            })
                            .addOnFailureListener(e -> loadManualReminders(myEmail));
                })
                .addOnFailureListener(e -> loadManualReminders(myEmail));
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
            allRows.add(new CalendarRow(title, subtitle, amountText, createdAt.getTime(), "payment_" + id, normalizePriority(priority), "none", 0, false));
        }
    }

    private void loadManualReminders(String myEmail) {
        db.collection("reminders")
                .whereArrayContains("targetEmails", myEmail)
                .get()
                .addOnSuccessListener(result -> {
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        Date startAt = doc.getDate("startAt");
                        if (startAt == null) continue;
                        String title = doc.getString("title");
                        String groupName = doc.getString("groupName");
                        String targetType = doc.getString("targetType");
                        String interval = doc.getString("interval");
                        Long intervalDays = doc.getLong("intervalDays");
                        String subtitle = (groupName == null || groupName.trim().isEmpty() ? "Piso" : groupName)
                                + " - "
                                + buildReminderTargetLabel(targetType, doc);
                        allRows.add(new CalendarRow(
                                title == null || title.trim().isEmpty() ? "Recordatorio" : title,
                                subtitle,
                                "Recordatorio",
                                startAt.getTime(),
                                "reminder_" + doc.getId(),
                                "media",
                                interval == null ? "semanal" : interval.toLowerCase(Locale.ROOT),
                                intervalDays == null ? 0 : intervalDays.intValue(),
                                true
                        ));
                    }
                    applyDateFilter();
                })
                .addOnFailureListener(e -> applyDateFilter());
    }

    private String buildReminderTargetLabel(@Nullable String targetType, DocumentSnapshot doc) {
        if ("miembro".equals(targetType)) {
            String email = doc.getString("targetMemberEmail");
            return "Miembro: " + (email == null ? "miembro" : email);
        }
        if ("habitacion".equals(targetType)) {
            String room = doc.getString("roomName");
            return "Habitación: " + (room == null ? "sin nombre" : room);
        }
        if ("x_habitacion".equals(targetType)) {
            List<String> roomNames = castStrings(doc.get("roomNames"));
            return roomNames.isEmpty() ? "X habitación" : "X habitación: " + String.join(", ", roomNames);
        }
        return "Todos los miembros";
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

    private void applyDateFilter() {
        filteredRows.clear();
        long dayStart = startOfDay(selectedDateMs);
        long dayEnd = dayStart + 24L * 60L * 60L * 1000L - 1L;
        for (CalendarRow row : allRows) {
            if (row.isReminder) {
                if (reminderOccursOnDay(row, dayStart)) {
                    filteredRows.add(row);
                }
            } else if (row.dueAtMs >= dayStart && row.dueAtMs <= dayEnd) {
                filteredRows.add(row);
            }
        }
        if (filteredRows.isEmpty()) {
            filteredRows.add(new CalendarRow("Sin eventos para este día", "No hay pagos ni recordatorios", "-", dayStart, "empty", "", "none", 0, false));
        }
        adapter.notifyDataSetChanged();
    }

    private boolean reminderOccursOnDay(CalendarRow row, long dayStart) {
        long startDay = startOfDay(row.dueAtMs);
        if (dayStart < startDay) return false;
        long diffDays = (dayStart - startDay) / (24L * 60L * 60L * 1000L);
        if ("diario".equals(row.intervalType)) return true;
        if ("semanal".equals(row.intervalType)) return diffDays % 7L == 0L;
        if ("mensual".equals(row.intervalType)) {
            Calendar start = Calendar.getInstance();
            start.setTimeInMillis(startDay);
            Calendar selected = Calendar.getInstance();
            selected.setTimeInMillis(dayStart);
            return selected.get(Calendar.DAY_OF_MONTH) == start.get(Calendar.DAY_OF_MONTH);
        }
        int everyDays = row.intervalDays <= 0 ? 1 : row.intervalDays;
        return diffDays % everyDays == 0L;
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
        final String intervalType;
        final int intervalDays;
        final boolean isReminder;

        CalendarRow(String title, String subtitle, String amount, long dueAtMs, String id, String priority, String intervalType, int intervalDays, boolean isReminder) {
            this.title = title;
            this.subtitle = subtitle;
            this.amount = amount;
            this.dueAtMs = dueAtMs;
            this.id = id;
            this.priority = priority;
            this.intervalType = intervalType;
            this.intervalDays = intervalDays;
            this.isReminder = isReminder;
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
