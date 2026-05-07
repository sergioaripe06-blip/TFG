package com.sergio.flatshare.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.ui.widget.PieChartView;
import com.sergio.flatshare.util.SessionStore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PersonalBalanceFragment extends Fragment {
    private static final String[] TYPES = {"agua", "electricidad", "internet", "alquiler", "comida", "otros"};
    private static final int[] COLORS = {
            Color.parseColor("#44D4FF"),
            Color.parseColor("#FFD95A"),
            Color.parseColor("#8EA8FF"),
            Color.parseColor("#FF9E66"),
            Color.parseColor("#78E08F"),
            Color.parseColor("#C3C3C3")
    };
    private static final String MODE_PERSONAL = "Balance personal";
    private static final String MODE_GROUP = "Balance grupal";
    private static final String GROUP_ALL = "Todos los pisos";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<GroupOption> groupOptions = new ArrayList<>();

    private PieChartView chart;
    private LinearLayout legend;
    private Button personalModeBtn;
    private Button groupModeBtn;
    private Spinner groupSpinner;
    private TextView titleTv;
    private boolean groupModeEnabled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_balance, container, false);
        chart = view.findViewById(R.id.personalPieChart);
        legend = view.findViewById(R.id.personalLegendContainer);
        personalModeBtn = view.findViewById(R.id.personalModeBtn);
        groupModeBtn = view.findViewById(R.id.groupModeBtn);
        groupSpinner = view.findViewById(R.id.groupSelectorSpinner);
        titleTv = view.findViewById(R.id.balanceTitleTv);

        setupModeToggle();
        loadUserGroupsAndSetupSelector();
        return view;
    }

    private void setupModeToggle() {
        applyModeUi();
        personalModeBtn.setOnClickListener(v -> {
            groupModeEnabled = false;
            applyModeUi();
            refreshChart();
        });
        groupModeBtn.setOnClickListener(v -> {
            groupModeEnabled = true;
            applyModeUi();
            refreshChart();
        });
    }

    private void applyModeUi() {
        titleTv.setText(groupModeEnabled ? MODE_GROUP : MODE_PERSONAL);
        groupSpinner.setVisibility(groupModeEnabled ? View.VISIBLE : View.GONE);
        personalModeBtn.setBackgroundResource(groupModeEnabled ? R.drawable.bg_tab_default : R.drawable.bg_tab_selected);
        groupModeBtn.setBackgroundResource(groupModeEnabled ? R.drawable.bg_tab_selected : R.drawable.bg_tab_default);
        personalModeBtn.setTextColor(requireContext().getColor(groupModeEnabled ? R.color.text_light : R.color.on_primary_green));
        groupModeBtn.setTextColor(requireContext().getColor(groupModeEnabled ? R.color.on_primary_green : R.color.text_light));
    }

    private void loadUserGroupsAndSetupSelector() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("groups").whereArrayContains("members", uid).get().addOnSuccessListener(result -> {
            groupOptions.clear();
            groupOptions.add(new GroupOption(GROUP_ALL, null));
            String currentGroup = SessionStore.getCurrentGroup(requireContext());
            int currentIdx = 0;
            int idx = 1;
            for (var doc : result.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                groupOptions.add(new GroupOption(name == null ? "Piso" : name, id));
                if (currentGroup != null && currentGroup.equals(id)) currentIdx = idx;
                idx++;
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
        boolean groupMode = groupModeEnabled;
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);
        Map<String, Double> totals = createZeroTotals();

        if (groupOptions.isEmpty()) {
            render(totals);
            return;
        }

        if (!groupMode) {
            GroupOption selected = resolveSelectedGroupOption();
            String groupId = selected.groupId;
            if (groupId == null) {
                String current = SessionStore.getCurrentGroup(requireContext());
                if (current == null) {
                    render(totals);
                    return;
                }
                groupId = current;
            }
            accumulateForGroup(groupId, true, myEmail, totals, () -> render(totals));
            return;
        }

        GroupOption selected = resolveSelectedGroupOption();
        if (selected.groupId == null) {
            List<String> allIds = new ArrayList<>();
            for (GroupOption opt : groupOptions) {
                if (opt.groupId != null) allIds.add(opt.groupId);
            }
            accumulateForManyGroups(allIds, false, myEmail, totals, () -> render(totals));
        } else {
            accumulateForGroup(selected.groupId, false, myEmail, totals, () -> render(totals));
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
        var expensesQuery = db.collection("expenses").whereEqualTo("groupId", groupId);
        if (personal) expensesQuery = expensesQuery.whereEqualTo("payerEmail", myEmail);
        expensesQuery.get().addOnSuccessListener(expenses -> {
            expenses.forEach(doc -> {
                String c = normalizeType(doc.getString("category"));
                double a = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                totals.put(c, totals.getOrDefault(c, 0.0) + a);
            });

            var paymentsQuery = db.collection("payments").whereEqualTo("groupId", groupId);
            if (personal) paymentsQuery = paymentsQuery.whereEqualTo("fromEmail", myEmail);
            paymentsQuery.get().addOnSuccessListener(payments -> {
                payments.forEach(doc -> {
                    String c = normalizeType(doc.getString("category"));
                    double a = doc.getDouble("amount") == null ? 0.0 : doc.getDouble("amount");
                    totals.put(c, totals.getOrDefault(c, 0.0) + a);
                });
                done.run();
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

            typeTv.setText("● " + capitalize(type));
            typeTv.setTextColor(COLORS[i]);
            valueTv.setText(df.format(percentage) + "%  •  " + df.format(value) + " EUR");
            legend.addView(item);
        }
        chart.setSlices(slices);
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
