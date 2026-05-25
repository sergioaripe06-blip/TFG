package com.sergio.flatshare.features.workspace;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.notifications.ReminderScheduler;
import com.sergio.flatshare.shared.ui.DialogUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarFragment extends Fragment {
    private static final SimpleDateFormat REMINDER_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private static final String[] REMINDER_INTERVAL_LABELS = {"Único", "Diario", "Semanal", "Mensual", "Personalizado"};
    private static final String[] REMINDER_INTERVAL_KEYS = {"unico", "diario", "semanal", "mensual", "personalizado"};

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<CalendarRow> allRows = new ArrayList<>();
    private final List<CalendarRow> filteredRows = new ArrayList<>();
    private final Map<String, String> memberNamesByEmail = new HashMap<>();
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
        calendarView.setDateTextAppearance(R.style.TextAppearance_FlatShare_CalendarDate);
        calendarView.setWeekDayTextAppearance(R.style.TextAppearance_FlatShare_CalendarWeekday);

        calendarView.setOnDateChangeListener((v, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            selectedDateMs = c.getTimeInMillis();
            updateSelectedDateLabel();
            applyDateFilter();
        });
        eventsLv.setOnItemLongClickListener((parent, itemView, position, id) -> {
            if (position < 0 || position >= filteredRows.size()) return true;
            CalendarRow row = filteredRows.get(position);
            if (!row.isReminder || "empty".equals(row.id)) return true;
            openReminderLongPressDialog(row);
            return true;
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
                        String subtitle = (groupName == null ? "Piso" : groupName) + " - Pagar a " + displayNameWithEmail(creditor);
                        String amountText = (amount == null ? "0.00" : df.format(amount)) + " EUR";
                        allRows.add(new CalendarRow(title, subtitle, amountText, due.getTime(), doc.getId(), normalizePriority(priority), "none", 0, false));
                        scheduleDeadlineNotifications(title, amountText, due.getTime(), doc.getId());
                    }
                    loadRegisteredPayments(myEmail);
                })
                .addOnFailureListener(e -> loadRegisteredPayments(myEmail));
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
                    ? "Enviado a " + displayNameWithEmail(to)
                    : "Recibido de " + displayNameWithEmail(from);
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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            applyDateFilter();
            return;
        }
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Set<String> seenReminderIds = new HashSet<>();
        db.collection("groups")
                .whereArrayContains("members", myUid)
                .get()
                .addOnSuccessListener(groups -> {
                    List<Task<QuerySnapshot>> reminderTasks = new ArrayList<>();
                    for (DocumentSnapshot groupDoc : groups.getDocuments()) {
                        reminderTasks.add(
                                db.collection("reminders")
                                        .whereEqualTo("groupId", groupDoc.getId())
                                        .get()
                        );
                    }
                    if (reminderTasks.isEmpty()) {
                        applyDateFilter();
                        return;
                    }
                    Tasks.whenAllComplete(reminderTasks)
                            .addOnSuccessListener(done -> {
                                for (Task<QuerySnapshot> task : reminderTasks) {
                                    if (!task.isSuccessful() || task.getResult() == null) continue;
                                    appendReminderRows(task.getResult().getDocuments(), seenReminderIds, myEmail);
                                }
                                applyDateFilter();
                            })
                            .addOnFailureListener(e -> applyDateFilter());
                })
                .addOnFailureListener(e -> applyDateFilter());
    }

    private void appendReminderRows(List<DocumentSnapshot> docs, Set<String> seenReminderIds, String myEmail) {
        String normalizedMyEmail = myEmail == null ? "" : myEmail.toLowerCase(Locale.ROOT);
        for (DocumentSnapshot doc : docs) {
            if (seenReminderIds.contains(doc.getId())) continue;
            seenReminderIds.add(doc.getId());

            String ownerEmail = doc.getString("ownerEmail");
            boolean isOwner = ownerEmail != null && normalizedMyEmail.equals(ownerEmail.toLowerCase(Locale.ROOT));
            List<String> targetEmails = castStrings(doc.get("targetEmails"));
            boolean isTarget = false;
            for (String email : targetEmails) {
                if (normalizedMyEmail.equals(email == null ? "" : email.toLowerCase(Locale.ROOT))) {
                    isTarget = true;
                    break;
                }
            }
            if (!isOwner && !isTarget) continue;

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
    }

    private String buildReminderTargetLabel(@Nullable String targetType, DocumentSnapshot doc) {
        if ("miembro".equals(targetType)) {
            String email = doc.getString("targetMemberEmail");
            return "Miembro: " + displayNameWithEmail(email);
        }
        if ("x_miembro".equals(targetType)) {
            List<String> members = castStrings(doc.get("targetEmails"));
            if (members.isEmpty()) return "X miembros";
            List<String> labels = new ArrayList<>();
            for (String email : members) {
                labels.add(displayNameWithEmail(email));
            }
            return "X miembros: " + String.join(", ", labels);
        }
        if ("habitacion".equals(targetType)) {
            String room = doc.getString("roomName");
            return "Habitación: " + (room == null ? "sin nombre" : room);
        }
        if ("x_habitacion".equals(targetType)) {
            List<String> roomNames = castStrings(doc.get("roomNames"));
            return roomNames.isEmpty() ? "X habitación" : "X habitación: " + String.join(", ", roomNames);
        }
        if ("todos_inquilinos".equals(targetType)) {
            return "Todos los inquilinos";
        }
        return "Todos los miembros";
    }

    private void openReminderLongPressDialog(@NonNull CalendarRow row) {
        String reminderDocId = reminderDocIdFromRow(row.id);
        if (reminderDocId.isEmpty()) return;

        db.collection("reminders")
                .document(reminderDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "El recordatorio ya no existe", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showReminderActionsDialog(row, doc);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "No se pudo cargar el recordatorio", Toast.LENGTH_SHORT).show()
                );
    }

    private void showReminderActionsDialog(@NonNull CalendarRow row, @NonNull DocumentSnapshot reminderDoc) {
        LinearLayout content = DialogUtils.createVerticalActions(requireContext());
        Button infoBtn = DialogUtils.createActionButton(requireContext(), "Ver información", true);
        content.addView(infoBtn);

        boolean canEdit = canEditReminder(reminderDoc);
        Button editBtn = canEdit ? DialogUtils.createActionButton(requireContext(), "Editar", false) : null;
        if (editBtn != null) content.addView(editBtn);

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
            showReminderInfoDialog(row, reminderDoc);
        });
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showReminderEditDialog(reminderDoc);
            });
        }
    }

    private void showReminderInfoDialog(@NonNull CalendarRow row, @NonNull DocumentSnapshot reminderDoc) {
        String groupName = reminderDoc.getString("groupName");
        if (groupName == null || groupName.trim().isEmpty()) {
            groupName = "Piso";
        }
        String targetLabel = buildReminderTargetLabel(reminderDoc.getString("targetType"), reminderDoc);
        String fromDate = resolveReminderDateText(reminderDoc, "startDateText", "startAt");
        String toDate = resolveReminderDateText(reminderDoc, "endDateText", "endAt");
        if ("Sin fecha".equals(toDate)) toDate = "Sin fecha fin";

        String details = "Piso: " + groupName
                + "\nDirigido a: " + targetLabel
                + "\nFrecuencia: " + reminderIntervalLabel(reminderDoc)
                + "\nDesde: " + fromDate
                + "\nHasta: " + toDate;

        View content = DialogUtils.createMessageView(requireContext(), details);
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

    private void showReminderEditDialog(@NonNull DocumentSnapshot reminderDoc) {
        if (!canEditReminder(reminderDoc)) {
            Toast.makeText(requireContext(), "Solo quien lo creó puede editarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(2), dp(4), dp(2));

        EditText titleEt = buildDialogEditText("Título");
        titleEt.setText(reminderDoc.getString("title") == null ? "" : reminderDoc.getString("title"));
        form.addView(titleEt);

        Spinner intervalSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        intervalSpinner.setBackgroundResource(R.drawable.bg_select_dark_round);
        intervalSpinner.setAdapter(buildLightSpinnerAdapter(REMINDER_INTERVAL_LABELS));
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        spinnerParams.topMargin = dp(10);
        intervalSpinner.setLayoutParams(spinnerParams);
        form.addView(intervalSpinner);

        EditText customDaysEt = buildDialogEditText("Cada cuántos días");
        customDaysEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(customDaysEt);

        EditText startDateEt = buildDialogEditText("Fecha inicio (YYYY-MM-DD)");
        setupDatePickerField(startDateEt);
        form.addView(startDateEt);

        EditText endDateEt = buildDialogEditText("Fecha fin (opcional, YYYY-MM-DD)");
        setupDatePickerField(endDateEt);
        form.addView(endDateEt);

        String intervalKey = safeLower(reminderDoc.getString("interval"));
        if (intervalKey.isEmpty()) intervalKey = "semanal";
        int intervalIndex = intervalIndexForKey(intervalKey);
        intervalSpinner.setSelection(intervalIndex);
        Long days = reminderDoc.getLong("intervalDays");
        customDaysEt.setText(days == null || days <= 0 ? "" : String.valueOf(days));
        customDaysEt.setVisibility("personalizado".equals(intervalKey) ? View.VISIBLE : View.GONE);

        String startText = resolveReminderDateText(reminderDoc, "startDateText", "startAt");
        if (!"Sin fecha".equals(startText)) startDateEt.setText(startText);
        String endText = resolveReminderDateText(reminderDoc, "endDateText", "endAt");
        if (!"Sin fecha".equals(endText)) endDateEt.setText(endText);

        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedKey = REMINDER_INTERVAL_KEYS[position];
                customDaysEt.setVisibility("personalizado".equals(selectedKey) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.addView(form, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        DialogUtils.Shell shell = DialogUtils.buildShell(
                requireContext(),
                "Editar recordatorio",
                "Guarda los cambios del recordatorio",
                scroll,
                "Cancelar",
                "Guardar"
        );
        AlertDialog dialog = DialogUtils.show(requireContext(), shell.root);
        shell.cancelBtn.setOnClickListener(v -> dialog.dismiss());
        shell.confirmBtn.setOnClickListener(v -> {
            String title = titleEt.getText() == null ? "" : titleEt.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }
            String startTextValue = startDateEt.getText() == null ? "" : startDateEt.getText().toString().trim();
            Date startAt = parseReminderDateOrNull(startTextValue);
            if (startAt == null) {
                Toast.makeText(requireContext(), "Fecha de inicio no válida", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startAt.getTime() < startOfDay(System.currentTimeMillis())) {
                Toast.makeText(requireContext(), "La fecha de inicio no puede ser pasada", Toast.LENGTH_SHORT).show();
                return;
            }

            String endTextValue = endDateEt.getText() == null ? "" : endDateEt.getText().toString().trim();
            Date endAt = null;
            if (!endTextValue.isEmpty()) {
                endAt = parseReminderDateOrNull(endTextValue);
                if (endAt == null) {
                    Toast.makeText(requireContext(), "Fecha de fin no válida", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (endAt.before(startAt)) {
                    Toast.makeText(requireContext(), "La fecha fin debe ser posterior a inicio", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String newIntervalKey = REMINDER_INTERVAL_KEYS[intervalSpinner.getSelectedItemPosition()];
            int intervalDays = 0;
            if ("personalizado".equals(newIntervalKey)) {
                String daysText = customDaysEt.getText() == null ? "" : customDaysEt.getText().toString().trim();
                try {
                    intervalDays = Integer.parseInt(daysText);
                } catch (NumberFormatException e) {
                    intervalDays = 0;
                }
                if (intervalDays <= 0) {
                    Toast.makeText(requireContext(), "Indica cada cuántos días", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            final int finalIntervalDays = intervalDays;
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title);
            updates.put("interval", newIntervalKey);
            updates.put("intervalDays", intervalDays);
            updates.put("startAt", startAt);
            updates.put("startDateText", REMINDER_DATE_FORMAT.format(startAt));
            updates.put("endAt", endAt);
            updates.put("endDateText", endAt == null ? "" : REMINDER_DATE_FORMAT.format(endAt));
            updates.put("updatedAt", FieldValue.serverTimestamp());

            db.collection("reminders")
                    .document(reminderDoc.getId())
                    .update(updates)
                    .addOnSuccessListener(done -> {
                        rescheduleReminderForCurrentUser(reminderDoc, title, startAt.getTime(), newIntervalKey, finalIntervalDays);
                        Toast.makeText(requireContext(), "Recordatorio actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadDeadlines();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "No se pudo actualizar", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    private void rescheduleReminderForCurrentUser(
            @NonNull DocumentSnapshot reminderDoc,
            @NonNull String title,
            long startAtMs,
            @NonNull String intervalKey,
            int intervalDays
    ) {
        Long reminderCode = reminderDoc.getLong("reminderCode");
        if (reminderCode == null) return;
        if (!isCurrentUserTarget(reminderDoc)) return;

        int code = reminderCode.intValue();
        ReminderScheduler.cancel(requireContext(), code);
        long intervalMs = intervalMsForReminder(intervalKey, intervalDays);
        if (intervalMs <= 0L) {
            ReminderScheduler.scheduleOneTime(requireContext(), code, "FlatShare: " + title, "Recordatorio pendiente", startAtMs);
        } else {
            ReminderScheduler.schedule(requireContext(), code, "FlatShare: " + title, "Recordatorio pendiente", startAtMs, intervalMs);
        }
    }

    private boolean isCurrentUserTarget(@NonNull DocumentSnapshot reminderDoc) {
        String myEmail = safeLower(FirebaseAuth.getInstance().getCurrentUser() == null
                ? null
                : FirebaseAuth.getInstance().getCurrentUser().getEmail());
        List<String> targets = castStrings(reminderDoc.get("targetEmails"));
        for (String email : targets) {
            if (myEmail.equals(safeLower(email))) return true;
        }
        return false;
    }

    private long intervalMsForReminder(@NonNull String intervalKey, int intervalDays) {
        if ("unico".equals(intervalKey)) return 0L;
        if ("diario".equals(intervalKey)) return 24L * 60L * 60L * 1000L;
        if ("semanal".equals(intervalKey)) return 7L * 24L * 60L * 60L * 1000L;
        if ("mensual".equals(intervalKey)) return 30L * 24L * 60L * 60L * 1000L;
        if ("personalizado".equals(intervalKey) && intervalDays > 0) {
            return intervalDays * 24L * 60L * 60L * 1000L;
        }
        return 0L;
    }

    private boolean canEditReminder(@NonNull DocumentSnapshot reminderDoc) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return false;
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String myEmail = safeLower(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        String ownerUid = reminderDoc.getString("ownerUid");
        if (ownerUid != null && ownerUid.equals(myUid)) return true;
        String ownerEmail = safeLower(reminderDoc.getString("ownerEmail"));
        return !myEmail.isEmpty() && myEmail.equals(ownerEmail);
    }

    @NonNull
    private String reminderDocIdFromRow(@Nullable String rowId) {
        if (rowId == null) return "";
        if (rowId.startsWith("reminder_")) {
            return rowId.substring("reminder_".length());
        }
        return "";
    }

    private String resolveReminderDateText(@NonNull DocumentSnapshot doc, @NonNull String textField, @NonNull String dateField) {
        String text = doc.getString(textField);
        if (text != null && !text.trim().isEmpty()) return text.trim();
        Date date = doc.getDate(dateField);
        if (date == null) return "Sin fecha";
        return REMINDER_DATE_FORMAT.format(date);
    }

    private String reminderIntervalLabel(@NonNull DocumentSnapshot doc) {
        String interval = safeLower(doc.getString("interval"));
        Long intervalDays = doc.getLong("intervalDays");
        if ("unico".equals(interval)) return "Único";
        if ("diario".equals(interval)) return "Diario";
        if ("semanal".equals(interval)) return "Semanal";
        if ("mensual".equals(interval)) return "Mensual";
        if ("personalizado".equals(interval) && intervalDays != null && intervalDays > 0) {
            return "Cada " + intervalDays + " días";
        }
        return interval.isEmpty() ? "Sin definir" : interval;
    }

    private Date parseReminderDateOrNull(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            Date raw = REMINDER_DATE_FORMAT.parse(value.trim());
            if (raw == null) return null;
            Calendar c = Calendar.getInstance();
            c.setTime(raw);
            c.set(Calendar.HOUR_OF_DAY, 10);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        } catch (ParseException e) {
            return null;
        }
    }

    private void setupDatePickerField(@NonNull EditText field) {
        field.setFocusable(false);
        field.setClickable(true);
        field.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            String current = field.getText() == null ? "" : field.getText().toString().trim();
            if (!current.isEmpty()) {
                Date parsed = parseReminderDateOrNull(current);
                if (parsed != null) now.setTime(parsed);
            }
            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> field.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private EditText buildDialogEditText(@NonNull String hint) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setHintTextColor(requireContext().getColor(R.color.text_muted));
        editText.setTextColor(requireContext().getColor(R.color.text_light));
        editText.setBackgroundResource(R.drawable.bg_input_dark_round);
        editText.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(10);
        editText.setLayoutParams(params);
        return editText;
    }

    private int intervalIndexForKey(@NonNull String key) {
        for (int i = 0; i < REMINDER_INTERVAL_KEYS.length; i++) {
            if (REMINDER_INTERVAL_KEYS[i].equalsIgnoreCase(key)) return i;
        }
        return 2;
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density);
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
        if ("unico".equals(row.intervalType)) return diffDays == 0L;
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
            ReminderScheduler.scheduleOneTime(requireContext(), reminderA, "Pago vence mañana", title + " - " + amount, oneDayBefore);
        }
        if (dueAtMs > now) {
            ReminderScheduler.scheduleOneTime(requireContext(), reminderB, "Pago vence hoy", title + " - " + amount, dueAtMs);
        }
    }

    private String displayNameWithEmail(@Nullable String email) {
        String normalized = safeLower(email);
        if (normalized.isEmpty()) return "miembro";
        ensureNameCached(normalized);
        String name = memberNamesByEmail.get(normalized);
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return name + " (" + normalized + ")";
    }

    private void ensureNameCached(String email) {
        if (email == null || email.trim().isEmpty()) return;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (memberNamesByEmail.containsKey(normalized)) return;
        memberNamesByEmail.put(normalized, normalized);
        db.collection("users")
                .whereEqualTo("email", normalized)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    String resolved = normalized;
                    if (!result.isEmpty()) {
                        String name = result.getDocuments().get(0).getString("name");
                        if (name != null && !name.trim().isEmpty()) {
                            resolved = name.trim();
                        }
                    }
                    memberNamesByEmail.put(normalized, resolved);
                    if (!isAdded()) return;
                    applyDateFilter();
                })
                .addOnFailureListener(e -> memberNamesByEmail.put(normalized, normalized));
    }

    private String safeLower(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private ArrayAdapter<String> buildLightSpinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), R.layout.item_spinner_selected, values) {
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


