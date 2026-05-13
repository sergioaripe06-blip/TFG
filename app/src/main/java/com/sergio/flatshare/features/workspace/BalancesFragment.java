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
import com.google.firebase.firestore.FirebaseFirestore;
import com.sergio.flatshare.R;
import com.sergio.flatshare.core.session.SessionStore;

import java.text.DecimalFormat;
import java.util.HashMap;
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
            tv.setText("Selecciona un grupo para ver balances.");
            return;
        }
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().toLowerCase(Locale.ROOT);

        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            List<String> members = (List<String>) groupDoc.get("memberEmails");
            if (members == null || members.isEmpty()) {
                tv.setText("Grupo sin miembros válidos.");
                return;
            }

            Map<String, Double> net = new HashMap<>();
            for (String m : members) net.put(m, 0.0);

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
            String msg = myNet >= 0 ? "Te deben " + df.format(myNet) + " €" : "Debes " + df.format(Math.abs(myNet)) + " €";
            StringBuilder all = new StringBuilder(msg).append("\n\nBalance del grupo:\n");
            for (Map.Entry<String, Double> e : net.entrySet()) {
                all.append(e.getKey()).append(": ").append(df.format(e.getValue())).append(" €\n");
            }
            tv.setText(all.toString());
        });
    }
}


