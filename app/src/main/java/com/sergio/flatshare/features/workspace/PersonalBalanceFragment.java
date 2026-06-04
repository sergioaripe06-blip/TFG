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
import com.sergio.flatshare.features.workspace.services.RoomRentShareCalculator;
import com.sergio.flatshare.shared.ui.DateInputUtils;
import com.sergio.flatshare.shared.widgets.MonthlyBarChartView;
import com.sergio.flatshare.shared.widgets.PieChartView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PersonalBalanceFragment extends Fragment {
    private static final String BILLING_FIXED = "fixed";
    private static final String CATEGORY_RENT = "alquiler";
    private static final String CATEGORY_ROOM_EXPENSE = "gasto habitación";
    private static final int[] CATEGORY_COLOR_PALETTE = {
            Color.parseColor("#44D4FF"),
            Color.parseColor("#FFD95A"),
            Color.parseColor("#8EA8FF"),
            Color.parseColor("#FF9E66"),
            Color.parseColor("#78E08F"),
            Color.parseColor("#C3C3C3"),
            Color.parseColor("#E489FF"),
            Color.parseColor("#66D1C2"),
            Color.parseColor("#FF7A9C"),
            Color.parseColor("#9ED36A"),
            Color.parseColor("#F7A95B"),
            Color.parseColor("#9AA5B1")
    };
    private static final String GROUP_ALL = "Todas las habitaciones";
    private static final int BASE_PIE_CHART_HEIGHT_DP = 270;
    private static final int PIE_CHART_STEP_HEIGHT_DP = 18;
    private static final int PIE_CHART_MAX_HEIGHT_DP = 360;

    private enum TimePeriod {
        QUARTER("Trimestre", 3),
        FOUR_MONTHS("Cuatrimestre", 4),
        HALF_YEAR("Semestre", 6),
        YEAR("Año completo", 12),
        TWO_YEARS("2 años", 24);

        final String label;
        final int months;

        TimePeriod(String label, int months) {
            this.label = label;
            this.months = months;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<GroupOption> groupOptions = new ArrayList<>();
    private final Map<String, String> ownerMemberNamesByEmail = new HashMap<>();

    private PieChartView chart;
    private MonthlyBarChartView monthlyChart;
    private LinearLayout legend;
    private Spinner groupSpinner;
    private Spinner timePeriodSpinner;
    private TextView monthlyChartTitleTv;
    private TextView monthlyChartSubtitleTv;
    private boolean viewActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewActive = true;
        View view = inflater.inflate(R.layout.fragment_personal_balance, container, false);
        chart = view.findViewById(R.id.personalPieChart);
        monthlyChart = view.findViewById(R.id.monthlyBarChart);
        legend = view.findViewById(R.id.personalLegendContainer);
        groupSpinner = view.findViewById(R.id.groupSelectorSpinner);
        timePeriodSpinner = view.findViewById(R.id.timePeriodSpinner);
        monthlyChartTitleTv = view.findViewById(R.id.monthlyChartTitleTv);
        monthlyChartSubtitleTv = view.findViewById(R.id.monthlyChartSubtitleTv);

        setupTimePeriodSelector();
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

    private void setupTimePeriodSelector() {
        if (!canUseUi() || timePeriodSpinner == null) return;
        List<String> labels = new ArrayList<>();
        for (TimePeriod period : TimePeriod.values()) {
            labels.add(period.label);
        }
        Context context = getContext();
        if (context == null) return;
        timePeriodSpinner.setAdapter(buildLightSpinnerAdapter(context, labels.toArray(new String[0])));
        timePeriodSpinner.setSelection(3);
        timePeriodSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onChanged(int position) {
                refreshChart();
            }
        });
    }

    private void loadUserGroupsAndSetupSelector() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("groups").whereArrayContains("members", uid).get().addOnSuccessListener(result -> {
            if (!canUseUi() || groupSpinner == null) return;
            groupOptions.clear();
            groupOptions.add(new GroupOption(GROUP_ALL, null, false));

            for (DocumentSnapshot doc : result.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                String ownerId = doc.getString("ownerId");
                boolean isOwner = uid.equals(ownerId);
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
            });
        } else {
            accumulateForGroup(selected.groupId, false, myEmail, totals, () -> {
                render(totals);
                refreshPersonalPaidSection(selected, myEmail);
            });
        }
    }

    private void refreshPersonalPaidSection(GroupOption selected, String myEmail) {
        if (!canUseUi() || monthlyChart == null) return;
        TimePeriod period = resolveTimePeriod();
        if (monthlyChartTitleTv != null) {
            monthlyChartTitleTv.setText("Balance por tiempo");
        }
        if (monthlyChartSubtitleTv != null) {
            String scope = selected.groupId == null
                    ? "todas las habitaciones"
                    : "el piso seleccionado";
            monthlyChartSubtitleTv.setText("Tus gastos y pagos confirmados de " + scope + " en " + period.label.toLowerCase(Locale.ROOT) + " (EUR)");
        }
        List<String> scopeGroupIds = selected.groupId == null
                ? getAllGroupIds()
                : java.util.Collections.singletonList(selected.groupId);
        refreshTimeBalanceBars(scopeGroupIds, myEmail, period);
    }

    private TimePeriod resolveTimePeriod() {
        int index = timePeriodSpinner == null ? 3 : timePeriodSpinner.getSelectedItemPosition();
        TimePeriod[] values = TimePeriod.values();
        if (index < 0 || index >= values.length) return TimePeriod.YEAR;
        return values[index];
    }

    private void refreshTimeBalanceBars(List<String> groupIds, String myEmail, TimePeriod period) {
        LinkedHashMap<String, Double> monthlyTotals = createMonthlyBuckets(period.months);
        if (groupIds == null || groupIds.isEmpty()) {
            renderMonthlyBars(monthlyTotals);
            return;
        }
        accumulateMonthlyForManyRecursive(groupIds, 0, myEmail, monthlyTotals, () -> renderMonthlyBars(monthlyTotals));
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
                    accumulateVariableRoomCharges(groupId, myEmail, personal, totals, done);
                }).addOnFailureListener(e -> done.run());
            }).addOnFailureListener(e -> done.run());
        }).addOnFailureListener(e -> done.run());
    }

    private void accumulateVariableRoomCharges(String groupId, String myEmail, boolean personal, Map<String, Double> totals, Runnable done) {
        db.collection("rooms_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(rooms -> {
                    double totalRoomAmount = 0.0;
                    for (DocumentSnapshot room : rooms.getDocuments()) {
                        double roomCost = room.getDouble("monthlyCost") == null ? 0.0 : room.getDouble("monthlyCost");
                        if (roomCost <= 0.0) continue;
                        if (!personal) {
                            totalRoomAmount += roomCost;
                            continue;
                        }
                        List<String> residents = toLowerList(room.get("memberEmails"));
                        Map<String, Double> residentAmounts = RoomRentShareCalculator.calculateResidentAmounts(
                                roomCost,
                                residents,
                                room.getString("rentSplitMode"),
                                castPercentages(room.get("rentSplitPercentages"))
                        );
                        totalRoomAmount += residentAmounts.getOrDefault(myEmail, 0.0);
                    }
                    if (totalRoomAmount > 0.0) {
                        String category = normalizeType(CATEGORY_ROOM_EXPENSE);
                        totals.put(category, totals.getOrDefault(category, 0.0) + totalRoomAmount);
                    }
                    done.run();
                })
                .addOnFailureListener(e -> done.run());
    }

    private GroupOption resolveSelectedGroupOption() {
        if (groupOptions.isEmpty()) return new GroupOption(GROUP_ALL, null, false);
        int idx = groupSpinner == null ? 0 : groupSpinner.getSelectedItemPosition();
        if (idx < 0 || idx >= groupOptions.size()) return groupOptions.get(0);
        return groupOptions.get(idx);
    }

    private Map<String, Double> createZeroTotals() {
        return new HashMap<>();
    }

    private void render(Map<String, Double> totals) {
        if (!canUseUi() || legend == null || chart == null) return;
        Context context = getContext();
        if (context == null) return;

        LinkedHashMap<String, Double> compactTotals = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            String category = normalizeType(entry.getKey());
            double amount = entry.getValue() == null ? 0.0 : entry.getValue();
            if (amount <= 0.0) continue;
            compactTotals.put(category, compactTotals.getOrDefault(category, 0.0) + amount);
        }

        List<Map.Entry<String, Double>> rows = new ArrayList<>(compactTotals.entrySet());
        rows.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<PieChartView.Slice> slices = new ArrayList<>();
        legend.removeAllViews();
        DecimalFormat df = new DecimalFormat("0.00");
        double total = 0.0;
        for (Map.Entry<String, Double> row : rows) {
            total += row.getValue();
        }

        for (Map.Entry<String, Double> row : rows) {
            String type = row.getKey();
            float value = row.getValue().floatValue();
            int color = resolveCategoryColor(type);
            slices.add(new PieChartView.Slice(color, value));

            double percentage = total <= 0.0 ? 0.0 : (value * 100.0 / total);
            View item = LayoutInflater.from(context).inflate(R.layout.item_balance_legend, legend, false);
            View dot = item.findViewById(R.id.legendColorDot);
            TextView typeTv = item.findViewById(R.id.legendTypeTv);
            TextView valueTv = item.findViewById(R.id.legendValueTv);
            TextView percentTv = item.findViewById(R.id.legendPercentTv);

            GradientDrawable dotShape = new GradientDrawable();
            dotShape.setShape(GradientDrawable.OVAL);
            dotShape.setColor(color);
            dot.setBackground(dotShape);

            typeTv.setText(capitalize(type));
            valueTv.setText(df.format(value) + " EUR");
            percentTv.setText(df.format(percentage) + "%");
            legend.addView(item);
        }
        applyDynamicPieChartHeight(rows.size());
        chart.setSlices(slices);
    }

    private void applyDynamicPieChartHeight(int categoryCount) {
        if (!canUseUi() || chart == null) return;
        ViewGroup.LayoutParams params = chart.getLayoutParams();
        if (params == null) return;

        int extraCategories = Math.max(0, categoryCount - 6);
        int targetDp = BASE_PIE_CHART_HEIGHT_DP + (extraCategories * PIE_CHART_STEP_HEIGHT_DP);
        targetDp = Math.min(targetDp, PIE_CHART_MAX_HEIGHT_DP);
        int targetPx = dpToPx(targetDp);

        if (params.height != targetPx) {
            params.height = targetPx;
            chart.setLayoutParams(params);
        }
    }

    private int dpToPx(int dp) {
        Context context = getContext();
        if (context == null) return dp;
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void refreshMonthlyPaidBars(List<String> groupIds, String myEmail) {
        LinkedHashMap<String, Double> monthlyTotals = createMonthlyBuckets(TimePeriod.YEAR.months);
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
                                db.collection("rent_collections")
                                        .whereEqualTo("groupId", groupId)
                                        .whereEqualTo("tenantEmail", myEmail)
                                        .whereEqualTo("sourceType", "room_charge")
                                        .get()
                                        .addOnSuccessListener(roomCharges -> {
                                            roomCharges.forEach(doc -> {
                                                java.util.Date date = resolveDocDate(doc);
                                                if (date == null) return;
                                                String key = monthKey(date);
                                                if (!monthlyTotals.containsKey(key)) return;
                                                double amountBase = doc.getDouble("amountBase") == null ? 0.0 : doc.getDouble("amountBase");
                                                double surcharge = doc.getDouble("surcharge") == null ? 0.0 : doc.getDouble("surcharge");
                                                monthlyTotals.put(key, monthlyTotals.getOrDefault(key, 0.0) + amountBase + surcharge);
                                            });
                                            done.run();
                                        })
                                        .addOnFailureListener(e -> done.run());
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
        return DateInputUtils.normalizeMonthKeyToDisplay(key);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String normalizeType(@Nullable String type) {
        if (type == null) return "otros";
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return "otros";
        return normalized;
    }

    private int resolveCategoryColor(@Nullable String category) {
        String normalized = normalizeType(category);
        if ("agua".equals(normalized)) return Color.parseColor("#44D4FF");
        if ("electricidad".equals(normalized)) return Color.parseColor("#FFD95A");
        if ("internet".equals(normalized)) return Color.parseColor("#8EA8FF");
        if ("alquiler".equals(normalized)) return Color.parseColor("#FF9E66");
        if (normalizeType(CATEGORY_ROOM_EXPENSE).equals(normalized)) return Color.parseColor("#A77BFF");
        if ("comida".equals(normalized)) return Color.parseColor("#78E08F");
        if ("otros".equals(normalized)) return Color.parseColor("#C3C3C3");
        int index = (normalized.hashCode() & 0x7fffffff) % CATEGORY_COLOR_PALETTE.length;
        return CATEGORY_COLOR_PALETTE[index];
    }

    private Map<String, Double> castPercentages(@Nullable Object raw) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> map)) return out;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String email = safeLower(String.valueOf(entry.getKey()));
            if (email.isEmpty()) continue;
            Object value = entry.getValue();
            if (value instanceof Number number) {
                out.put(email, number.doubleValue());
            } else {
                try {
                    out.put(email, Double.parseDouble(String.valueOf(value).trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    private List<String> toLowerList(@Nullable Object raw) {
        if (!(raw instanceof List<?> rawList)) return java.util.Collections.emptyList();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_spinner_selected, values) {
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
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
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

