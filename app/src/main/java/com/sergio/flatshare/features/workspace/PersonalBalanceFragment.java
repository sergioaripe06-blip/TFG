package com.sergio.flatshare.features.workspace;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.widgets.MonthlyBarChartView;
import com.sergio.flatshare.shared.widgets.PieChartView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PersonalBalanceFragment extends Fragment {
    private static final String BILLING_FIXED = "fixed";
    private static final String CATEGORY_RENT = "alquiler";
    private static final String[] TYPES = {"agua", "electricidad", "internet", "alquiler", "comida", "otros"};
    private static final int[] COLORS = {
            Color.parseColor("#44D4FF"),
            Color.parseColor("#FFD95A"),
            Color.parseColor("#8EA8FF"),
            Color.parseColor("#FF9E66"),
            Color.parseColor("#78E08F"),
            Color.parseColor("#C3C3C3")
    };
    private static final String GROUP_ALL = "Todas las habitaciones";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<GroupOption> groupOptions = new ArrayList<>();
    private static final int MONTHLY_BAR_COUNT = 6;

    private PieChartView chart;
    private MonthlyBarChartView monthlyChart;
    private LinearLayout legend;
    private Spinner groupSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_balance, container, false);
        chart = view.findViewById(R.id.personalPieChart);
        monthlyChart = view.findViewById(R.id.monthlyBarChart);
        legend = view.findViewById(R.id.personalLegendContainer);
        groupSpinner = view.findViewById(R.id.groupSelectorSpinner);
        loadUserGroupsAndSetupSelector();
        return view;
    }

    private void loadUserGroupsAndSetupSelector() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("groups").whereArrayContains("members", uid).get().addOnSuccessListener(result -> {
            groupOptions.clear();
            groupOptions.add(new GroupOption(GROUP_ALL, null));
            int currentIdx = 0;
            for (var doc : result.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                groupOptions.add(new GroupOption(name == null ? "Piso" : name, id));
            }
            List<String> labels = new ArrayList<>();
            for (GroupOption opt : groupOptions) labels.add(opt.label);
            groupSpinner.setAdapter(buildLightSpinnerAdapter(labels.toArray(new String[0])));
            groupSpinner.setSelection(currentIdx);
            groupSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
                @Override
                public void onChanged(int position) {
                    refreshChart();
                }
            });
            refreshChart();
        }).addOnFailureListener(e -> refreshChart());
    }

    private void refreshChart() {
        if (!isAdded()) return;
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        Map<String, Double> totals = createZeroTotals();

        if (groupOptions.isEmpty()) {
            render(totals);
            return;
        }

        GroupOption selected = resolveSelectedGroupOption();
        if (selected.groupId == null) {
            List<String> allIds = new ArrayList<>();
            for (GroupOption opt : groupOptions) {
                if (opt.groupId != null) allIds.add(opt.groupId);
            }
            accumulateForManyGroups(allIds, false, myEmail, totals, () -> render(totals));
            refreshMonthlyPaidBars(allIds, myEmail);
        } else {
            accumulateForGroup(selected.groupId, false, myEmail, totals, () -> render(totals));
            refreshMonthlyPaidBars(java.util.Collections.singletonList(selected.groupId), myEmail);
        }
    }

    private void accumulateForManyGroups(List<String> groupIds, boolean personal, String myEmail, Map<String, Double> totals, Runnable done) {
        if (groupIds.isEmpty()) {
            done.run();
            return;
        }
        accumulateForManyRecursive(groupIds, 0, personal, myEmail, totals, done);
    }

    private void accumulateForManyRecursive(List<String> groupIds, int index, boolean personal, String myEmail, Map<String, Double> totals, Runnable done) {
        if (index >= groupIds.size()) {
            done.run();
            return;
        }
        accumulateForGroup(groupIds.get(index), personal, myEmail, totals,
                () -> accumulateForManyRecursive(groupIds, index + 1, personal, myEmail, totals, done));
    }

    private void accumulateForGroup(String groupId, boolean personal, String myEmail, Map<String, Double> totals, Runnable done) {
        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            boolean fixedBilling = BILLING_FIXED.equalsIgnoreCase(groupDoc.getString("billingModel"));
            final var paymentsQuery = personal
                    ? db.collection("payments").whereEqualTo("groupId", groupId).whereEqualTo("fromEmail", myEmail)
                    : db.collection("payments").whereEqualTo("groupId", groupId);
            if (fixedBilling) {
                paymentsQuery.get().addOnSuccessListener(payments -> {
                    payments.forEach(doc -> {
                        String rawCategory = doc.getString("category");
                        if (rawCategory == null || !CATEGORY_RENT.equalsIgnoreCase(rawCategory.trim())) return;
                        String c = normalizeType(rawCategory);
                        double a = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                        totals.put(c, totals.getOrDefault(c, 0.0) + a);
                    });
                    done.run();
                }).addOnFailureListener(e -> done.run());
                return;
            }

            final var expensesQuery = personal
                    ? db.collection("expenses").whereEqualTo("groupId", groupId).whereEqualTo("payerEmail", myEmail)
                    : db.collection("expenses").whereEqualTo("groupId", groupId);
            expensesQuery.get().addOnSuccessListener(expenses -> {
                expenses.forEach(doc -> {
                    String c = normalizeType(doc.getString("category"));
                    double a = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                    totals.put(c, totals.getOrDefault(c, 0.0) + a);
                });

                paymentsQuery.get().addOnSuccessListener(payments -> {
                    payments.forEach(doc -> {
                        String c = normalizeType(doc.getString("category"));
                        double a = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                        totals.put(c, totals.getOrDefault(c, 0.0) + a);
                    });
                    done.run();
                }).addOnFailureListener(e -> done.run());
            }).addOnFailureListener(e -> done.run());
        }).addOnFailureListener(e -> done.run());
    }

    private GroupOption resolveSelectedGroupOption() {
        int idx = groupSpinner.getSelectedItemPosition();
        if (idx < 0 || idx >= groupOptions.size()) return groupOptions.get(0);
        return groupOptions.get(idx);
    }

    private Map<String, Double> createZeroTotals() {
        Map<String, Double> totals = new HashMap<>();
        for (String type : TYPES) totals.put(type, 0.0);
        return totals;
    }

    private void render(Map<String, Double> totals) {
        List<PieChartView.Slice> slices = new ArrayList<>();
        legend.removeAllViews();
        DecimalFormat df = new DecimalFormat("0.00");
        double total = 0.0;
        for (String t : TYPES) total += totals.getOrDefault(t, 0.0);

        for (int i = 0; i < TYPES.length; i++) {
            String type = TYPES[i];
            float value = totals.getOrDefault(type, 0.0).floatValue();
            slices.add(new PieChartView.Slice(COLORS[i], value));

            double percentage = total <= 0.0 ? 0.0 : (value * 100.0 / total);
            View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_balance_legend, legend, false);
            View dot = item.findViewById(R.id.legendColorDot);
            TextView typeTv = item.findViewById(R.id.legendTypeTv);
            TextView valueTv = item.findViewById(R.id.legendValueTv);

            GradientDrawable dotShape = new GradientDrawable();
            dotShape.setShape(GradientDrawable.OVAL);
            dotShape.setColor(COLORS[i]);
            dot.setBackground(dotShape);

            typeTv.setText(capitalize(type));
            typeTv.setTextColor(COLORS[i]);
            valueTv.setText(df.format(percentage) + "%  -  " + df.format(value) + " EUR");
            legend.addView(item);
        }
        chart.setSlices(slices);
    }

    private void refreshMonthlyPaidBars(List<String> groupIds, String myEmail) {
        LinkedHashMap<String, Double> monthlyTotals = createMonthlyBuckets(MONTHLY_BAR_COUNT);
        if (groupIds == null || groupIds.isEmpty()) {
            renderMonthlyBars(monthlyTotals);
            return;
        }
        accumulateMonthlyForManyRecursive(groupIds, 0, myEmail, monthlyTotals, () -> renderMonthlyBars(monthlyTotals));
    }

    private void accumulateMonthlyForManyRecursive(
            List<String> groupIds,
            int index,
            String myEmail,
            LinkedHashMap<String, Double> monthlyTotals,
            Runnable done
    ) {
        if (index >= groupIds.size()) {
            done.run();
            return;
        }
        String groupId = groupIds.get(index);
        accumulateMonthlyForGroup(groupId, myEmail, monthlyTotals,
                () -> accumulateMonthlyForManyRecursive(groupIds, index + 1, myEmail, monthlyTotals, done));
    }

    private void accumulateMonthlyForGroup(
            String groupId,
            String myEmail,
            LinkedHashMap<String, Double> monthlyTotals,
            Runnable done
    ) {
        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("payerEmail", myEmail)
                .get()
                .addOnSuccessListener(expenses -> {
                    expenses.forEach(doc -> {
                        Date date = resolveDocDate(doc);
                        if (date == null) return;
                        String key = monthKey(date);
                        if (!monthlyTotals.containsKey(key)) return;
                        double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                        monthlyTotals.put(key, monthlyTotals.getOrDefault(key, 0.0) + amount);
                    });

                    db.collection("payments")
                            .whereEqualTo("groupId", groupId)
                            .whereEqualTo("fromEmail", myEmail)
                            .get()
                            .addOnSuccessListener(payments -> {
                                payments.forEach(doc -> {
                                    String status = doc.getString("status");
                                    if (!"confirmed".equalsIgnoreCase(status == null ? "" : status)) return;
                                    Date date = resolveDocDate(doc);
                                    if (date == null) return;
                                    String key = monthKey(date);
                                    if (!monthlyTotals.containsKey(key)) return;
                                    double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                                    monthlyTotals.put(key, monthlyTotals.getOrDefault(key, 0.0) + amount);
                                });
                                done.run();
                            })
                            .addOnFailureListener(e -> done.run());
                })
                .addOnFailureListener(e -> done.run());
    }

    private void renderMonthlyBars(LinkedHashMap<String, Double> monthlyTotals) {
        List<MonthlyBarChartView.Bar> bars = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
            bars.add(new MonthlyBarChartView.Bar(monthLabel(entry.getKey()), entry.getValue().floatValue()));
        }
        monthlyChart.setBars(bars);
    }

    private LinkedHashMap<String, Double> createMonthlyBuckets(int count) {
        LinkedHashMap<String, Double> buckets = new LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, -(count - 1));
        for (int i = 0; i < count; i++) {
            buckets.put(monthKey(calendar.getTime()), 0.0);
            calendar.add(Calendar.MONTH, 1);
        }
        return buckets;
    }

    private Date resolveDocDate(com.google.firebase.firestore.DocumentSnapshot doc) {
        Date created = doc.getDate("createdAt");
        if (created != null) return created;
        Date due = doc.getDate("dueAt");
        if (due != null) return due;
        return null;
    }

    private String monthKey(Date date) {
        return new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(date);
    }

    private String monthLabel(String key) {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM", Locale.ROOT);
        try {
            Date date = parser.parse(key);
            if (date == null) return key;
            String month = new SimpleDateFormat("MMM", new Locale("es", "ES")).format(date);
            month = month.replace(".", "");
            return capitalize(month);
        } catch (ParseException e) {
            return key;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String normalizeType(@Nullable String type) {
        if (type == null) return "otros";
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        for (String t : TYPES) {
            if (t.equals(normalized)) return t;
        }
        return "otros";
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

    private static class GroupOption {
        final String label;
        final String groupId;

        GroupOption(String label, @Nullable String groupId) {
            this.label = label;
            this.groupId = groupId;
        }
    }

    private abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onChanged(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }

        public abstract void onChanged(int position);
    }
}
