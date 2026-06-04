package com.sergio.flatshare.features.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;
import com.sergio.flatshare.features.workspace.services.RoomRentShareCalculator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BalancesFragment extends Fragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balances, container, false);
        TextView tv = view.findViewById(R.id.balanceTv);
        calculate(tv);
        return view;
    }

    private void calculate(TextView tv) {
        String groupId = SessionStore.getCurrentGroup(requireContext());
        if (groupId == null) {
            tv.setText(getString(R.string.balance_select_group));
            return;
        }
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);

        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            List<String> members = (List<String>) groupDoc.get("memberEmails");
            if (members == null || members.isEmpty()) {
                tv.setText(getString(R.string.balance_invalid_group_members));
                return;
            }

            Map<String, Double> net = new HashMap<>();
            for (String m : members) net.put(m, 0.0);
            String ownerEmail = resolveOwnerEmail(groupDoc, castStrings(groupDoc.get("members")), members);
            db.collection("rooms_groups").whereEqualTo("groupId", groupId).get().addOnSuccessListener(rooms -> {
                applyRoomChargeImpacts(rooms.getDocuments(), ownerEmail, net);

                db.collection("expenses").whereEqualTo("groupId", groupId).get().addOnSuccessListener(expenses -> {
                    for (var d : expenses.getDocuments()) {
                        double amount = d.getDouble("amount") == null ? 0 : d.getDouble("amount");
                        String payerEmail = d.getString("payerEmail");
                        String custom = d.getString("customSplit");

                        if (custom == null || custom.isEmpty()) {
                            double part = amount / members.size();
                            for (String m : members) net.put(m, net.getOrDefault(m, 0.0) - part);
                        } else {
                            String[] parts = custom.split(",");
                            for (String p : parts) {
                                String[] kv = p.trim().split(":");
                                if (kv.length == 2) {
                                    String email = kv[0].trim().toLowerCase(Locale.ROOT);
                                    double percent = Double.parseDouble(kv[1].trim());
                                    double part = amount * (percent / 100.0);
                                    net.put(email, net.getOrDefault(email, 0.0) - part);
                                }
                            }
                        }

                        if (payerEmail != null) {
                            String normalized = payerEmail.toLowerCase(Locale.ROOT);
                            net.put(normalized, net.getOrDefault(normalized, 0.0) + amount);
                        }
                    }
                    renderWithPayments(groupId, myEmail, net, tv);
                });
            });
        });
    }

    private void applyRoomChargeImpacts(List<DocumentSnapshot> roomDocs, String ownerEmail, Map<String, Double> net) {
        for (DocumentSnapshot roomDoc : roomDocs) {
            double roomCost = roomDoc.getDouble("monthlyCost") == null ? 0.0 : roomDoc.getDouble("monthlyCost");
            if (roomCost <= 0.0) continue;
            List<String> residents = castEmails(roomDoc.get("memberEmails"));
            if (residents.isEmpty()) continue;
            Map<String, Double> residentAmounts = RoomRentShareCalculator.calculateResidentAmounts(
                    roomCost,
                    residents,
                    roomDoc.getString("rentSplitMode"),
                    castPercentages(roomDoc.get("rentSplitPercentages"))
            );
            for (Map.Entry<String, Double> shareEntry : residentAmounts.entrySet()) {
                String resident = shareEntry.getKey();
                double residentShare = shareEntry.getValue() == null ? 0.0 : shareEntry.getValue();
                if (net.containsKey(resident)) {
                    net.put(resident, net.getOrDefault(resident, 0.0) - residentShare);
                }
            }
            if (ownerEmail != null && !ownerEmail.trim().isEmpty() && net.containsKey(ownerEmail)) {
                net.put(ownerEmail, net.getOrDefault(ownerEmail, 0.0) + roomCost);
            }
        }
    }

    private void renderWithPayments(String groupId, String myEmail, Map<String, Double> net, TextView tv) {
        db.collection("payments").whereEqualTo("groupId", groupId).get().addOnSuccessListener(payments -> {
            for (var p : payments.getDocuments()) {
                double amount = p.getDouble("amount") == null ? 0 : p.getDouble("amount");
                String from = p.getString("fromEmail");
                String to = p.getString("toEmail");
                if (from != null) net.put(from, net.getOrDefault(from, 0.0) + amount);
                if (to != null) net.put(to, net.getOrDefault(to, 0.0) - amount);
            }
            double myNet = net.getOrDefault(myEmail, 0.0);
            DecimalFormat df = new DecimalFormat("0.00");
            String msg = myNet >= 0
                    ? getString(R.string.balance_they_owe_you, df.format(myNet))
                    : getString(R.string.balance_you_owe, df.format(Math.abs(myNet)));
            StringBuilder all = new StringBuilder(msg).append("\n\n")
                    .append(getString(R.string.balance_group_title)).append("\n");
            for (Map.Entry<String, Double> e : net.entrySet()) {
                all.append(e.getKey())
                        .append(": ")
                        .append(df.format(e.getValue()))
                        .append(" EUR\n");
            }
            tv.setText(all.toString());
        });
    }

    private List<String> castStrings(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object value : list) {
                if (value != null) out.add(String.valueOf(value));
            }
        }
        return out;
    }

    private List<String> castEmails(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object value : list) {
                if (value == null) continue;
                String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) out.add(normalized);
            }
        }
        return out;
    }

    private Map<String, Double> castPercentages(Object raw) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String email = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT);
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
        }
        return out;
    }

    private String resolveOwnerEmail(DocumentSnapshot groupDoc, List<String> memberIds, List<String> memberEmails) {
        String ownerId = groupDoc.getString("ownerId");
        if (ownerId == null || ownerId.trim().isEmpty()) return "";
        int size = Math.min(memberIds.size(), memberEmails.size());
        for (int i = 0; i < size; i++) {
            if (ownerId.equals(memberIds.get(i))) return memberEmails.get(i);
        }
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (ownerId.equals(myUid)) {
            String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            return myEmail == null ? "" : myEmail.toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
