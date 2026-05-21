package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentService {
    public static class ValidationResult {
        public final boolean valid;
        public final String message;
        public final double amount;

        private ValidationResult(boolean valid, @Nullable String message, double amount) {
            this.valid = valid;
            this.message = message == null ? "" : message;
            this.amount = amount;
        }

        public static ValidationResult error(@NonNull String message) {
            return new ValidationResult(false, message, 0.0);
        }

        public static ValidationResult ok(double amount) {
            return new ValidationResult(true, "", amount);
        }
    }

    public static class PaymentRoom {
        public final String id;
        public final String name;
        public final List<String> memberEmails;

        public PaymentRoom(@NonNull String id, @NonNull String name, @NonNull List<String> memberEmails) {
            this.id = id;
            this.name = name;
            this.memberEmails = memberEmails;
        }
    }

    public static class PaymentTarget {
        public final String toEmail;
        @Nullable public final String roomId;
        @Nullable public final String roomName;

        public PaymentTarget(@NonNull String toEmail, @Nullable String roomId, @Nullable String roomName) {
            this.toEmail = toEmail;
            this.roomId = roomId;
            this.roomName = roomName;
        }
    }

    public static class PaymentWrite {
        public final Map<String, Object> data;
        public final String toEmail;
        public final double splitAmount;

        public PaymentWrite(@NonNull Map<String, Object> data, @NonNull String toEmail, double splitAmount) {
            this.data = data;
            this.toEmail = toEmail;
            this.splitAmount = splitAmount;
        }
    }

    public ValidationResult validateAmountAndRequiredFields(
            @NonNull String amountStr,
            @NonNull String dueDateText,
            @Nullable Date dueDate
    ) {
        if (amountStr.trim().isEmpty() || dueDateText.trim().isEmpty()) {
            return ValidationResult.error("Debes indicar importe y fecha límite");
        }
        if (dueDate == null) {
            return ValidationResult.error("Fecha inválida. Usa YYYY-MM-DD");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
        } catch (NumberFormatException e) {
            return ValidationResult.error("Importe no válido");
        }
        if (amount <= 0.0) {
            return ValidationResult.error("El importe debe ser mayor que 0");
        }
        return ValidationResult.ok(amount);
    }

    @NonNull
    public String normalizePriority(@Nullable String priority) {
        if (priority == null || priority.trim().isEmpty()) return "media";
        return priority.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    public String normalizeCategory(boolean fixedBilling, @Nullable String category, @NonNull String rentCategoryKey) {
        if (fixedBilling) return rentCategoryKey;
        if (category == null || category.trim().isEmpty()) return "otros";
        return category.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    public String resolveConcept(@NonNull String concept, @Nullable String suggestedConcept) {
        String clean = concept.trim();
        if (!clean.isEmpty()) return clean;
        String suggested = suggestedConcept == null ? "" : suggestedConcept.trim();
        return suggested.isEmpty() ? "Pago directo" : suggested;
    }

    @NonNull
    public List<PaymentTarget> resolveTargets(
            @NonNull String targetType,
            @NonNull List<String> members,
            @NonNull List<PaymentRoom> rooms,
            @NonNull List<String> selectedMemberEmails,
            int roomIndex,
            @NonNull String fromEmail
    ) {
        List<PaymentTarget> targets = new ArrayList<>();
        if ("Miembro".equals(targetType)) {
            List<String> unique = deduplicate(selectedMemberEmails);
            for (String selected : unique) {
                String toEmail = selected.toLowerCase(Locale.ROOT);
                if (!toEmail.equals(fromEmail)) {
                    targets.add(new PaymentTarget(toEmail, null, null));
                }
            }
            return targets;
        }

        if ("Habitación".equals(targetType)) {
            if (roomIndex >= 0 && roomIndex < rooms.size()) {
                PaymentRoom room = rooms.get(roomIndex);
                for (String resident : room.memberEmails) {
                    String email = resident.toLowerCase(Locale.ROOT);
                    if (!email.equals(fromEmail)) {
                        targets.add(new PaymentTarget(email, room.id, room.name));
                    }
                }
            }
            return targets;
        }

        for (String member : members) {
            String email = member.toLowerCase(Locale.ROOT);
            if (!email.equals(fromEmail)) {
                targets.add(new PaymentTarget(email, null, null));
            }
        }
        return targets;
    }

    @NonNull
    public List<PaymentWrite> buildWrites(
            @NonNull String groupId,
            double amount,
            @NonNull String fromEmail,
            @NonNull String category,
            @NonNull String priority,
            @NonNull Date dueDate,
            @NonNull String dueDateText,
            @NonNull String concept,
            @NonNull String targetType,
            @NonNull List<PaymentTarget> targets
    ) {
        List<PaymentWrite> writes = new ArrayList<>();
        if (targets.isEmpty()) return writes;
        double splitAmount = amount / targets.size();
        for (PaymentTarget target : targets) {
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", groupId);
            data.put("amount", splitAmount);
            data.put("fromEmail", fromEmail);
            data.put("toEmail", target.toEmail);
            data.put("category", category);
            data.put("priority", priority);
            data.put("status", "pending");
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("dueAt", dueDate);
            data.put("dueDateText", dueDateText);
            data.put("concept", concept);
            data.put("targetType", targetType.toLowerCase(Locale.ROOT));
            data.put("roomId", target.roomId == null ? "" : target.roomId);
            data.put("roomName", target.roomName == null ? "" : target.roomName);
            writes.add(new PaymentWrite(data, target.toEmail, splitAmount));
        }
        return writes;
    }

    @NonNull
    private List<String> deduplicate(@NonNull List<String> values) {
        List<String> unique = new ArrayList<>();
        for (String value : values) {
            if (!unique.contains(value)) unique.add(value);
        }
        return unique;
    }
}

