package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentService {
    private static final String ROOM_SPLIT_EQUAL = "equal";
    private static final String ROOM_SPLIT_PERCENTAGE = "percentage";

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
        public final double monthlyCost;
        public final String splitMode;
        public final Map<String, Double> splitPercentages;

        public PaymentRoom(
                @NonNull String id,
                @NonNull String name,
                @NonNull List<String> memberEmails,
                double monthlyCost,
                @NonNull String splitMode,
                @NonNull Map<String, Double> splitPercentages
        ) {
            this.id = id;
            this.name = name;
            this.memberEmails = memberEmails;
            this.monthlyCost = monthlyCost;
            this.splitMode = splitMode;
            this.splitPercentages = splitPercentages;
        }
    }

    public static class PaymentTarget {
        public final String toEmail;
        @Nullable public final String roomId;
        @Nullable public final String roomName;
        @NonNull public final List<String> roomIds;
        @NonNull public final List<String> roomNames;
        public final double shareWeight;

        public PaymentTarget(@NonNull String toEmail, @Nullable String roomId, @Nullable String roomName) {
            this(toEmail, roomId, roomName, new ArrayList<>(), new ArrayList<>(), 0.0);
        }

        public PaymentTarget(
                @NonNull String toEmail,
                @Nullable String roomId,
                @Nullable String roomName,
                @NonNull List<String> roomIds,
                @NonNull List<String> roomNames
        ) {
            this(toEmail, roomId, roomName, roomIds, roomNames, 0.0);
        }

        public PaymentTarget(
                @NonNull String toEmail,
                @Nullable String roomId,
                @Nullable String roomName,
                @NonNull List<String> roomIds,
                @NonNull List<String> roomNames,
                double shareWeight
        ) {
            this.toEmail = toEmail;
            this.roomId = roomId;
            this.roomName = roomName;
            this.roomIds = roomIds;
            this.roomNames = roomNames;
            this.shareWeight = shareWeight;
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
            return ValidationResult.error("Debes indicar importe y fecha l\u00edmite");
        }
        if (dueDate == null) {
            return ValidationResult.error("Fecha inv\u00e1lida. Usa DD/MM/AAAA");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
        } catch (NumberFormatException e) {
            return ValidationResult.error("Importe no v\u00e1lido");
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
            @NonNull List<String> selectedRoomIds,
            @NonNull String fromEmail,
            boolean splitInsideRoom
    ) {
        List<PaymentTarget> targets = new ArrayList<>();
        String normalizedTargetType = normalizeText(targetType);

        if ("miembro".equals(normalizedTargetType)) {
            List<String> unique = deduplicate(selectedMemberEmails);
            for (String selected : unique) {
                String toEmail = selected.toLowerCase(Locale.ROOT);
                if (!toEmail.equals(fromEmail)) {
                    targets.add(new PaymentTarget(toEmail, null, null));
                }
            }
            return targets;
        }

        if ("habitacion".equals(normalizedTargetType)) {
            Map<String, PaymentRoom> roomsById = new LinkedHashMap<>();
            for (PaymentRoom room : rooms) {
                roomsById.put(room.id, room);
            }

            List<String> uniqueRoomIds = deduplicate(selectedRoomIds);
            List<PaymentRoom> selectedRooms = new ArrayList<>();
            for (String roomId : uniqueRoomIds) {
                PaymentRoom room = roomsById.get(roomId);
                if (room != null) {
                    selectedRooms.add(room);
                }
            }
            if (selectedRooms.isEmpty()) {
                return targets;
            }

            List<String> selectedRoomNames = new ArrayList<>();
            for (PaymentRoom room : selectedRooms) {
                selectedRoomNames.add(room.name);
            }

            Map<String, PaymentTarget> targetsByEmail = new LinkedHashMap<>();
            Map<String, Double> weightsByEmail = new LinkedHashMap<>();
            for (PaymentRoom room : selectedRooms) {
                for (String resident : room.memberEmails) {
                    String email = resident.toLowerCase(Locale.ROOT);
                    if (email.equals(fromEmail) || email.trim().isEmpty()) continue;
                    if (targetsByEmail.containsKey(email)) continue;
                    targetsByEmail.put(
                            email,
                            new PaymentTarget(
                                    email,
                                    room.id,
                                    room.name,
                                    new ArrayList<>(uniqueRoomIds),
                                    new ArrayList<>(selectedRoomNames)
                            )
                    );
                }
            }

            if (!splitInsideRoom) {
                targets.addAll(targetsByEmail.values());
                return targets;
            }

            for (PaymentRoom room : selectedRooms) {
                List<String> eligibleResidents = new ArrayList<>();
                for (String resident : room.memberEmails) {
                    String email = resident.toLowerCase(Locale.ROOT);
                    if (email.equals(fromEmail) || email.trim().isEmpty()) continue;
                    if (!eligibleResidents.contains(email)) eligibleResidents.add(email);
                }
                if (eligibleResidents.isEmpty()) continue;

                double roomWeight = room.monthlyCost > 0.0 ? room.monthlyCost : 1.0;
                Map<String, Double> shares = resolveRoomShares(room, eligibleResidents);
                for (Map.Entry<String, Double> shareEntry : shares.entrySet()) {
                    String email = shareEntry.getKey();
                    double residentWeight = roomWeight * shareEntry.getValue();
                    weightsByEmail.put(email, weightsByEmail.getOrDefault(email, 0.0) + residentWeight);
                }
            }

            for (Map.Entry<String, PaymentTarget> entry : targetsByEmail.entrySet()) {
                String email = entry.getKey();
                PaymentTarget base = entry.getValue();
                targets.add(new PaymentTarget(
                        base.toEmail,
                        base.roomId,
                        base.roomName,
                        base.roomIds,
                        base.roomNames,
                        weightsByEmail.getOrDefault(email, 0.0)
                ));
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

        double totalWeight = 0.0;
        boolean hasWeightedTargets = false;
        for (PaymentTarget target : targets) {
            if (target.shareWeight > 0.0) {
                hasWeightedTargets = true;
                totalWeight += target.shareWeight;
            }
        }

        double equalSplitAmount = amount / targets.size();
        for (PaymentTarget target : targets) {
            double splitAmount = equalSplitAmount;
            if (hasWeightedTargets && totalWeight > 0.0) {
                splitAmount = amount * (target.shareWeight / totalWeight);
            }
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
            data.put("targetType", normalizeText(targetType));
            data.put("roomId", target.roomId == null ? "" : target.roomId);
            data.put("roomName", target.roomName == null ? "" : target.roomName);
            data.put("roomIds", target.roomIds);
            data.put("roomNames", target.roomNames);
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

    @NonNull
    private Map<String, Double> resolveRoomShares(@NonNull PaymentRoom room, @NonNull List<String> residents) {
        Map<String, Double> shares = new LinkedHashMap<>();
        if (residents.isEmpty()) return shares;

        String mode = normalizeText(room.splitMode);
        boolean percentageMode = ROOM_SPLIT_PERCENTAGE.equals(mode) && residents.size() >= 2;
        if (!percentageMode) {
            double equalShare = 1.0 / residents.size();
            for (String resident : residents) {
                shares.put(resident, equalShare);
            }
            return shares;
        }

        double providedSum = 0.0;
        for (String resident : residents) {
            double percent = room.splitPercentages.getOrDefault(resident, 0.0);
            if (percent > 0.0) {
                providedSum += percent;
            }
        }

        if (providedSum <= 0.0) {
            double equalShare = 1.0 / residents.size();
            for (String resident : residents) {
                shares.put(resident, equalShare);
            }
            return shares;
        }

        for (String resident : residents) {
            double percent = room.splitPercentages.getOrDefault(resident, 0.0);
            if (percent < 0.0) percent = 0.0;
            shares.put(resident, percent / providedSum);
        }
        return shares;
    }

    @NonNull
    private String normalizeText(@Nullable String value) {
        if (value == null) return "";
        String lower = value.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}
