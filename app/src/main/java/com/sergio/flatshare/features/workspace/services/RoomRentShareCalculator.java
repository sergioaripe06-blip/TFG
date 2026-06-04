package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RoomRentShareCalculator {
    private RoomRentShareCalculator() {
    }

    @NonNull
    public static Map<String, Double> resolveResidentPercentages(
            @NonNull List<String> residents,
            @Nullable String splitMode,
            @NonNull Map<String, Double> splitPercentages
    ) {
        Map<String, Double> out = new LinkedHashMap<>();
        List<String> uniqueResidents = deduplicateResidents(residents);
        if (uniqueResidents.isEmpty()) return out;
        if (uniqueResidents.size() == 1) {
            out.put(uniqueResidents.get(0), 100.0);
            return out;
        }
        boolean percentageMode = "percentage".equalsIgnoreCase(splitMode == null ? "" : splitMode.trim());
        if (!percentageMode) {
            double equal = 100.0 / uniqueResidents.size();
            for (String resident : uniqueResidents) {
                out.put(resident, equal);
            }
            return out;
        }

        double providedSum = 0.0;
        for (String resident : uniqueResidents) {
            double value = splitPercentages.getOrDefault(resident, 0.0);
            if (value > 0.0) providedSum += value;
        }
        if (providedSum <= 0.0) {
            double equal = 100.0 / uniqueResidents.size();
            for (String resident : uniqueResidents) {
                out.put(resident, equal);
            }
            return out;
        }

        for (String resident : uniqueResidents) {
            double value = splitPercentages.getOrDefault(resident, 0.0);
            if (value < 0.0) value = 0.0;
            out.put(resident, (value * 100.0) / providedSum);
        }
        return out;
    }

    @NonNull
    public static Map<String, Double> calculateResidentAmounts(
            double totalAmount,
            @NonNull List<String> residents,
            @Nullable String splitMode,
            @NonNull Map<String, Double> splitPercentages
    ) {
        Map<String, Double> percentages = resolveResidentPercentages(residents, splitMode, splitPercentages);
        Map<String, Double> amounts = new LinkedHashMap<>();
        if (percentages.isEmpty() || totalAmount <= 0.0) return amounts;

        long totalCents = Math.round(totalAmount * 100.0d);
        List<Map.Entry<String, Double>> entries = new ArrayList<>(percentages.entrySet());
        Map<String, Long> baseCents = new LinkedHashMap<>();
        Map<String, Double> fractionalParts = new LinkedHashMap<>();
        long assignedCents = 0L;

        for (Map.Entry<String, Double> entry : entries) {
            double rawCents = totalCents * (Math.max(0.0d, entry.getValue()) / 100.0d);
            long centsFloor = (long) Math.floor(rawCents);
            baseCents.put(entry.getKey(), centsFloor);
            fractionalParts.put(entry.getKey(), rawCents - centsFloor);
            assignedCents += centsFloor;
        }

        long remainder = totalCents - assignedCents;
        while (remainder > 0L) {
            int bestIndex = 0;
            double bestFraction = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < entries.size(); i++) {
                String email = entries.get(i).getKey();
                double fraction = fractionalParts.getOrDefault(email, 0.0d);
                if (fraction > bestFraction) {
                    bestFraction = fraction;
                    bestIndex = i;
                }
            }
            String winner = entries.get(bestIndex).getKey();
            baseCents.put(winner, baseCents.getOrDefault(winner, 0L) + 1L);
            fractionalParts.put(winner, 0.0d);
            remainder--;
        }

        for (Map.Entry<String, Long> entry : baseCents.entrySet()) {
            amounts.put(entry.getKey(), entry.getValue() / 100.0d);
        }
        return amounts;
    }

    @NonNull
    private static List<String> deduplicateResidents(@NonNull List<String> residents) {
        List<String> out = new ArrayList<>();
        for (String resident : residents) {
            if (resident == null) continue;
            String normalized = resident.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || out.contains(normalized)) continue;
            out.add(normalized);
        }
        return out;
    }
}
