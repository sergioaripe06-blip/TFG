package com.sergio.flatshare.features.workspace;

import android.content.Context;
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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.shared.widgets.MonthlyBarChartView;
import com.sergio.flatshare.shared.widgets.PieChartView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;

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
    private static final int MONTHLY_BAR_COUNT = 6;

    private enum OwnerPeriod {
        MONTH("Mes actual", 1),
        QUARTER("Trimestre", 3),
        FOUR_MONTHS("Cuatrimestre", 4),
        HALF_YEAR("Semestre", 6),
        YEAR("Año", 12);

        final String label;
        final int months;

        OwnerPeriod(String label, int months) {
            this.label = label;
            this.months = months;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<GroupOption> groupOptions = new ArrayList<>();
    private final LinkedHashSet<String> ownedGroupIds = new LinkedHashSet<>();
    private final Map<String, String> ownerMemberNamesByEmail = new HashMap<>();

    private PieChartView chart;
    private MonthlyBarChartView monthlyChart;
    private MonthlyBarChartView ownerPaidChart;
    private LinearLayout legend;
    private LinearLayout ownerStatsSection;
    private Spinner groupSpinner;
    private Spinner ownerPeriodSpinner;
    private TextView monthlyChartTitleTv;
    private TextView monthlyChartSubtitleTv;
    private TextView ownerChartSubtitleTv;
    private TextView ownerMemberDetailTitleTv;
    private LinearLayout ownerMemberDetailContainer;
    private boolean viewActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewActive = true;
        View view = inflater.inflate(R.layout.fragment_personal_balance, container, false);
        chart = view.findViewById(R.id.personalPieChart);
        monthlyChart = view.findViewById(R.id.monthlyBarChart);
        ownerPaidChart = view.findViewById(R.id.ownerPaidChart);
        legend = view.findViewById(R.id.personalLegendContainer);
        groupSpinner = view.findViewById(R.id.groupSelectorSpinner);
        ownerPeriodSpinner = view.findViewById(R.id.ownerPeriodSpinner);
        ownerStatsSection = view.findViewById(R.id.ownerStatsSection);
        monthlyChartTitleTv = view.findViewById(R.id.monthlyChartTitleTv);
        monthlyChartSubtitleTv = view.findViewById(R.id.monthlyChartSubtitleTv);
        ownerChartSubtitleTv = view.findViewById(R.id.ownerChartSubtitleTv);
        ownerMemberDetailTitleTv = view.findViewById(R.id.ownerMemberDetailTitleTv);
        ownerMemberDetailContainer = view.findViewById(R.id.ownerMemberDetailContainer);

        setupOwnerPeriodSelector();
        loadUserGroupsAndSetupSelector();
        return view;
    }

    @Override
    public void onDestroyView() {
        viewActive = false;
        super.onDestroyView();
    }

    private boolean canUseUi() {
        return viewActive && isAdded() && getContext() != null;
    }

    private void setupOwnerPeriodSelector() {
        if (!canUseUi() || ownerPeriodSpinner == null) return;
        List<String> labels = new ArrayList<>();
        for (OwnerPeriod period : OwnerPeriod.values()) {
            labels.add(period.label);
        }
        Context context = getContext();
        if (context == null) return;
        ownerPeriodSpinner.setAdapter(buildLightSpinnerAdapter(context, labels.toArray(new String[0])));
        ownerPeriodSpinner.setSelection(0);
        ownerPeriodSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onChanged(int position) {
                refreshOwnerPaidSection();
            }
        });
    }

    private void loadUserGroupsAndSetupSelector() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("groups").whereArrayContains("members", uid).get().addOnSuccessListener(result -> {
            if (!canUseUi() || groupSpinner == null) return;
            groupOptions.clear();
            ownedGroupIds.clear();
            groupOptions.add(new GroupOption(GROUP_ALL, null, false));

            for (DocumentSnapshot doc : result.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String ownerId = doc.getString("ownerId");
                boolean isOwner = uid.equals(ownerId);
                if (isOwner) ownedGroupIds.add(id);
                groupOptions.add(new GroupOption(name == null ? "Piso" : name, id, isOwner));
            }

            List<String> labels = new ArrayList<>();
            for (GroupOption opt : groupOptions) labels.add(opt.label);
            Context context = getContext();
            if (context == null) return;
            groupSpinner.setAdapter(buildLightSpinnerAdapter(context, labels.toArray(new String[0])));
            groupSpinner.setSelection(0);
            groupSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
                @Override
                public void onChanged(int position) {
                    refreshChart();
                }
            });
            refreshChart();
        }).addOnFailureListener(e -> {
            if (!canUseUi()) return;
            refreshChart();
        });
    }

    private void refreshChart() {
        if (!canUseUi()) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String myEmail = safeLower(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        Map<String, Double> totals = createZeroTotals();

        if (groupOptions.isEmpty()) {
            render(totals);
            return;
        }

        GroupOption selected = resolveSelectedGroupOption();
        if (selected.groupId == null) {
            List<String> allIds = getAllGroupIds();
            accumulateForManyGroups(allIds, false, myEmail, totals, () -> {
                render(totals);
                refreshPersonalPaidSection(selected, myEmail);
                refreshOwnerPaidSection();
            });
        } else {
            accumulateForGroup(selected.groupId, false, myEmail, totals, () -> {
                render(totals);
                refreshPersonalPaidSection(selected, myEmail);
                refreshOwnerPaidSection();
            });
        }
    }

    private void refreshPersonalPaidSection(GroupOption selected, String myEmail) {
        if (!canUseUi() || monthlyChart == null) return;
        if (selected.groupId == null) {
            if (monthlyChartTitleTv != null) {
                monthlyChartTitleTv.setText("Pagado por mes (todas las habitaciones)");
            }
            if (monthlyChartSubtitleTv != null) {
                monthlyChartSubtitleTv.setText("Tus gastos y pagos confirmados (EUR)");
            }
            refreshMonthlyPaidBars(getAllGroupIds(), myEmail);
            return;
        }

        if (monthlyChartTitleTv != null) {
            monthlyChartTitleTv.setText("Pagado este mes por inquilino");
        }
        if (monthlyChartSubtitleTv != null) {
            monthlyChartSubtitleTv.setText("Importe registrado en el piso seleccionado (EUR)");
        }
        refreshTenantMonthlyBars(selected.groupId);
    }

    private void refreshTenantMonthlyBars(String groupId) {
        LinkedHashMap<String, Double> totalsByTenant = new LinkedHashMap<>();
        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            List<String> memberEmails = toLowerList(groupDoc.get("memberEmails"));
            if (memberEmails != null) {
                for (String email : memberEmails) {
                    totalsByTenant.put(email, 0.0);
                }
            }

            db.collection("expenses")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .addOnSuccessListener(expenses -> {
                        for (DocumentSnapshot doc : expenses.getDocuments()) {
                            DateRange monthlyRange = currentMonthRange();
                            DateRange guard = monthlyRange;
                            java.util.Date date = resolveDocDate(doc);
                            if (date == null || !guard.contains(date)) continue;
                            String payerEmail = safeLower(doc.getString("payerEmail"));
                            if (payerEmail.isEmpty()) continue;
                            double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                            totalsByTenant.put(payerEmail, totalsByTenant.getOrDefault(payerEmail, 0.0) + amount);
                        }

                        db.collection("payments")
                                .whereEqualTo("groupId", groupId)
                                .whereEqualTo("status", "confirmed")
                                .get()
                                .addOnSuccessListener(payments -> {
                                    for (DocumentSnapshot doc : payments.getDocuments()) {
                                        DateRange monthlyRange = currentMonthRange();
                                        DateRange guard = monthlyRange;
                                        java.util.Date date = resolveDocDate(doc);
                                        if (date == null || !guard.contains(date)) continue;
                                        String fromEmail = safeLower(doc.getString("fromEmail"));
                                        if (fromEmail.isEmpty()) continue;
                                        double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                                        totalsByTenant.put(fromEmail, totalsByTenant.getOrDefault(fromEmail, 0.0) + amount);
                                    }
                                    renderTenantBarsWithResolvedNames(totalsByTenant);
                                })
                                .addOnFailureListener(e -> renderTenantBarsWithResolvedNames(totalsByTenant));
                    })
                    .addOnFailureListener(e -> renderTenantBarsWithResolvedNames(totalsByTenant));
        }).addOnFailureListener(e -> renderTenantBarsWithResolvedNames(totalsByTenant));
    }

    private void renderTenantBarsWithResolvedNames(LinkedHashMap<String, Double> totalsByTenant) {
        resolveNamesForEmails(new ArrayList<>(totalsByTenant.keySet()), () -> renderTenantBars(totalsByTenant));
    }

    private void renderTenantBars(LinkedHashMap<String, Double> totalsByTenant) {
        if (!canUseUi() || monthlyChart == null) return;

        List<MonthlyBarChartView.Bar> bars = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, Double> entry : totalsByTenant.entrySet()) {
            String email = entry.getKey();
            String displayName = memberNameOnly(email);
            String label = "Sin nombre".equalsIgnoreCase(displayName)
                    ? "Miembro " + index
                    : shortLabel(displayName);
            if (label.isEmpty()) {
                label = "M" + index;
            }
            bars.add(new MonthlyBarChartView.Bar(label, entry.getValue().floatValue()));
            index++;
        }
        monthlyChart.setBars(bars);
    }

    private void refreshOwnerPaidSection() {
        if (!canUseUi() || ownerStatsSection == null || ownerPaidChart == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        GroupOption selected = resolveSelectedGroupOption();
        List<String> scopeGroupIds = resolveOwnerScopeGroupIds(selected);
        if (scopeGroupIds.isEmpty()) {
            ownerStatsSection.setVisibility(View.GONE);
            ownerPaidChart.setBars(Collections.emptyList());
            renderOwnerMemberDetails(new LinkedHashMap<>());
            return;
        }

        ownerStatsSection.setVisibility(View.VISIBLE);
        OwnerPeriod period = resolveOwnerPeriod();
        if (ownerChartSubtitleTv != null) {
            ownerChartSubtitleTv.setText("Pagos confirmados recibidos en " + period.label.toLowerCase(Locale.ROOT) + " (EUR)");
        }

        LinkedHashMap<String, Double> monthlyTotals = createMonthlyBuckets(period.months);
        LinkedHashMap<String, LinkedHashMap<String, Double>> detailByMonthMember = createMonthlyMemberBuckets(monthlyTotals);
        String myEmail = safeLower(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        accumulateOwnerMonthlyForManyRecursive(
                scopeGroupIds,
                0,
                myEmail,
                monthlyTotals,
                detailByMonthMember,
                () -> {
                    renderOwnerBars(monthlyTotals);
                    resolveOwnerMemberNames(detailByMonthMember, () -> renderOwnerMemberDetails(detailByMonthMember));
                }
        );
    }

    private List<String> resolveOwnerScopeGroupIds(GroupOption selected) {
        if (selected.groupId == null) {
            return new ArrayList<>(ownedGroupIds);
        }
        if (selected.isOwner) {
            return Collections.singletonList(selected.groupId);
        }
        return Collections.emptyList();
    }

    private OwnerPeriod resolveOwnerPeriod() {
        int index = ownerPeriodSpinner == null ? 0 : ownerPeriodSpinner.getSelectedItemPosition();
        OwnerPeriod[] values = OwnerPeriod.values();
        if (index < 0 || index >= values.length) return OwnerPeriod.MONTH;
        return values[index];
    }

    private void accumulateOwnerMonthlyForManyRecursive(
            List<String> groupIds,
            int index,
            String myEmail,
            LinkedHashMap<String, Double> monthlyTotals,
            LinkedHashMap<String, LinkedHashMap<String, Double>> detailByMonthMember,
            Runnable done
    ) {
        if (index >= groupIds.size()) {
            done.run();
            return;
        }
        String groupId = groupIds.get(index);
        accumulateOwnerMonthlyForGroup(groupId, myEmail, monthlyTotals, detailByMonthMember,
                () -> accumulateOwnerMonthlyForManyRecursive(groupIds, index + 1, myEmail, monthlyTotals, detailByMonthMember, done));
    }

    private void accumulateOwnerMonthlyForGroup(
            String groupId,
            String myEmail,
            LinkedHashMap<String, Double> monthlyTotals,
            LinkedHashMap<String, LinkedHashMap<String, Double>> detailByMonthMember,
            Runnable done
    ) {
        db.collection("payments")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(payments -> {
                    for (DocumentSnapshot doc : payments.getDocuments()) {
                        java.util.Date date = resolveDocDate(doc);
                        if (date == null) continue;

                        String monthKey = monthKey(date);
                        if (!monthlyTotals.containsKey(monthKey)) continue;

                        String toEmail = safeLower(doc.getString("toEmail"));
                        String fromEmail = safeLower(doc.getString("fromEmail"));

                        if (!toEmail.isEmpty() && !toEmail.equals(myEmail)) continue;
                        if (fromEmail.equals(myEmail)) continue;

                        double amount = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                        monthlyTotals.put(monthKey, monthlyTotals.getOrDefault(monthKey, 0.0) + amount);
                        LinkedHashMap<String, Double> memberTotals = detailByMonthMember.get(monthKey);
                        if (memberTotals != null && !fromEmail.isEmpty()) {
                            memberTotals.put(fromEmail, memberTotals.getOrDefault(fromEmail, 0.0) + amount);
                        }
                    }
                    done.run();
                })
                .addOnFailureListener(e -> done.run());
    }

    private void renderOwnerBars(LinkedHashMap<String, Double> monthlyTotals) {
        if (!canUseUi() || ownerPaidChart == null) return;
        List<MonthlyBarChartView.Bar> bars = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
            bars.add(new MonthlyBarChartView.Bar(monthLabel(entry.getKey()), entry.getValue().floatValue()));
        }
        ownerPaidChart.setBars(bars);
    }

    private void resolveOwnerMemberNames(
            LinkedHashMap<String, LinkedHashMap<String, Double>> detailByMonthMember,
            Runnable done
    ) {
        List<String> allEmails = new ArrayList<>();
        for (LinkedHashMap<String, Double> totalsByEmail : detailByMonthMember.values()) {
            for (String email : totalsByEmail.keySet()) {
                String normalized = safeLower(email);
                if (!normalized.isEmpty() && !allEmails.contains(normalized)) {
                    allEmails.add(normalized);
                }
            }
        }
        if (allEmails.isEmpty()) {
            done.run();
            return;
        }

        List<Task<?>> tasks = new ArrayList<>();
        for (String email : allEmails) {
            if (ownerMemberNamesByEmail.containsKey(email)) continue;
            Task<?> task = db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(result -> {
                        String resolvedName = "Sin nombre";
                        if (!result.isEmpty()) {
                            DocumentSnapshot userDoc = result.getDocuments().get(0);
                            resolvedName = resolveNameFromUserDoc(userDoc);
                        }
                        ownerMemberNamesByEmail.put(email, resolvedName);
                    })
                    .addOnFailureListener(e -> ownerMemberNamesByEmail.put(email, "Sin nombre"));
            tasks.add(task);
        }

        if (tasks.isEmpty()) {
            done.run();
            return;
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> done.run());
    }

    private void renderOwnerMemberDetails(LinkedHashMap<String, LinkedHashMap<String, Double>> detailByMonthMember) {
        if (!canUseUi() || ownerMemberDetailContainer == null || ownerMemberDetailTitleTv == null) return;
        Context context = getContext();
        if (context == null) return;

        ownerMemberDetailContainer.removeAllViews();
        boolean hasRows = false;

        List<String> monthKeys = new ArrayList<>(detailByMonthMember.keySet());
        Collections.reverse(monthKeys);
        for (String monthKey : monthKeys) {
            LinkedHashMap<String, Double> memberTotals = detailByMonthMember.get(monthKey);
            if (memberTotals == null || memberTotals.isEmpty()) continue;

            List<Map.Entry<String, Double>> rows = new ArrayList<>(memberTotals.entrySet());
            rows.sort(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed());

            for (Map.Entry<String, Double> row : rows) {
                double amount = row.getValue() == null ? 0.0 : row.getValue();
                if (amount <= 0.0) continue;

                TextView line = new TextView(context);
                line.setTextColor(context.getColor(R.color.text_light));
                line.setTextSize(14f);
                String month = monthLabel(monthKey);
                String memberInfo = memberNameOnly(row.getKey());
                line.setText(String.format(Locale.ROOT, "%s - %.2f EUR - %s", month, amount, memberInfo));
                ownerMemberDetailContainer.addView(line);
                hasRows = true;
            }
        }

        if (!hasRows) {
            TextView empty = new TextView(context);
            empty.setTextColor(context.getColor(R.color.text_muted));
            empty.setTextSize(13f);
            empty.setText("Sin cobros confirmados por miembro en este periodo.");
            ownerMemberDetailContainer.addView(empty);
        }
        ownerMemberDetailTitleTv.setVisibility(View.VISIBLE);
    }

    private List<String> getAllGroupIds() {
        List<String> ids = new ArrayList<>();
        for (GroupOption option : groupOptions) {
            if (option.groupId != null && !option.groupId.trim().isEmpty()) ids.add(option.groupId);
        }
        return ids;
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
        if (groupOptions.isEmpty()) return new GroupOption(GROUP_ALL, null, false);
        int idx = groupSpinner == null ? 0 : groupSpinner.getSelectedItemPosition();
        if (idx < 0 || idx >= groupOptions.size()) return groupOptions.get(0);
        return groupOptions.get(idx);
    }

    private Map<String, Double> createZeroTotals() {
        Map<String, Double> totals = new HashMap<>();
        for (String type : TYPES) totals.put(type, 0.0);
        return totals;
    }

    private void render(Map<String, Double> totals) {
        if (!canUseUi() || legend == null || chart == null) return;
        Context context = getContext();
        if (context == null) return;
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
            View item = LayoutInflater.from(context).inflate(R.layout.item_balance_legend, legend, false);
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
                        java.util.Date date = resolveDocDate(doc);
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
                                    java.util.Date date = resolveDocDate(doc);
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
        if (!canUseUi() || monthlyChart == null) return;
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

    private LinkedHashMap<String, LinkedHashMap<String, Double>> createMonthlyMemberBuckets(LinkedHashMap<String, Double> monthlyTotals) {
        LinkedHashMap<String, LinkedHashMap<String, Double>> buckets = new LinkedHashMap<>();
        for (String monthKey : monthlyTotals.keySet()) {
            buckets.put(monthKey, new LinkedHashMap<>());
        }
        return buckets;
    }

    private java.util.Date resolveDocDate(DocumentSnapshot doc) {
        java.util.Date created = doc.getDate("createdAt");
        if (created != null) return created;
        java.util.Date due = doc.getDate("dueAt");
        if (due != null) return due;
        return null;
    }

    private String monthKey(java.util.Date date) {
        return new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(date);
    }

    private String monthLabel(String key) {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM", Locale.ROOT);
        try {
            java.util.Date date = parser.parse(key);
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

    private List<String> toLowerList(@Nullable Object raw) {
        if (!(raw instanceof List<?> rawList)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Object value : rawList) {
            if (value == null) continue;
            String normalized = safeLower(String.valueOf(value));
            if (!normalized.isEmpty()) out.add(normalized);
        }
        return out;
    }

    private DateRange currentMonthRange() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        return new DateRange(start.getTimeInMillis(), end.getTimeInMillis());
    }

    private String shortLabelFromEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "";
        String local = email.trim();
        int at = local.indexOf('@');
        if (at > 0) local = local.substring(0, at);
        if (local.length() > 8) local = local.substring(0, 8);
        return local;
    }

    private String shortLabel(String value) {
        if (value == null) return "";
        String label = value.trim();
        if (label.isEmpty()) return "";
        if (label.length() > 10) label = label.substring(0, 10);
        return label;
    }

    private String displayNameFromEmail(@Nullable String email) {
        String normalized = safeLower(email);
        if (normalized.isEmpty()) return "Sin nombre";
        String realName = ownerMemberNamesByEmail.get(normalized);
        if (realName != null && !realName.trim().isEmpty()) {
            return realName.trim();
        }
        return normalized;
    }

    private String memberNameOnly(@Nullable String email) {
        String normalized = safeLower(email);
        if (normalized.isEmpty()) return "Sin nombre";
        String name = displayNameFromEmail(normalized);
        if (name.equalsIgnoreCase(normalized) || name.contains("@")) {
            return "Sin nombre";
        }
        return name;
    }

    private void resolveNamesForEmails(List<String> emails, Runnable done) {
        List<Task<?>> tasks = new ArrayList<>();
        for (String email : emails) {
            String normalized = safeLower(email);
            if (normalized.isEmpty() || ownerMemberNamesByEmail.containsKey(normalized)) continue;
            Task<?> task = db.collection("users")
                    .whereEqualTo("email", normalized)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(result -> {
                        String resolvedName = "Sin nombre";
                        if (!result.isEmpty()) {
                            DocumentSnapshot userDoc = result.getDocuments().get(0);
                            resolvedName = resolveNameFromUserDoc(userDoc);
                        }
                        ownerMemberNamesByEmail.put(normalized, resolvedName);
                    })
                    .addOnFailureListener(e -> ownerMemberNamesByEmail.put(normalized, "Sin nombre"));
            tasks.add(task);
        }
        if (tasks.isEmpty()) {
            done.run();
            return;
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> done.run());
    }

    private String safeLower(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveNameFromUserDoc(@NonNull DocumentSnapshot userDoc) {
        String[] keys = {"name", "displayName", "fullName", "username"};
        for (String key : keys) {
            String value = userDoc.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "Sin nombre";
    }

    private ArrayAdapter<String> buildLightSpinnerAdapter(Context context, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, values) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(context.getColor(R.color.text_light));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(context.getColor(R.color.text_light));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static class GroupOption {
        final String label;
        final String groupId;
        final boolean isOwner;

        GroupOption(String label, @Nullable String groupId, boolean isOwner) {
            this.label = label;
            this.groupId = groupId;
            this.isOwner = isOwner;
        }
    }

    private static class DateRange {
        final long startInclusive;
        final long endExclusive;

        DateRange(long startInclusive, long endExclusive) {
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }

        boolean contains(java.util.Date date) {
            if (date == null) return false;
            long time = date.getTime();
            return time >= startInclusive && time < endExclusive;
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
