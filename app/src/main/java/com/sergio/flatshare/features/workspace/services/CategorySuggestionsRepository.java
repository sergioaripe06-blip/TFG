package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategorySuggestionsRepository {
    public interface Callback {
        void onLoaded(List<String> categories);
    }

    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final int SUGGESTION_QUERY_LIMIT = 120;

    private final FirebaseFirestore db;
    private final Map<String, CacheEntry> cacheByGroupId = new HashMap<>();

    public CategorySuggestionsRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void getSuggestions(@Nullable String groupId, boolean forceRefresh, @NonNull Callback callback) {
        String safeGroupId = groupId == null ? "" : groupId.trim();
        if (safeGroupId.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        if (!forceRefresh) {
            CacheEntry cached = cacheByGroupId.get(safeGroupId);
            if (cached != null && !cached.isExpired()) {
                callback.onLoaded(new ArrayList<>(cached.categories));
                return;
            }
        }

        db.collection("expenses")
                .whereEqualTo("groupId", safeGroupId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(SUGGESTION_QUERY_LIMIT)
                .get()
                .addOnSuccessListener(snapshot -> {
                    LinkedHashMap<String, String> unique = new LinkedHashMap<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String normalized = normalizeCategoryKey(doc.getString("category"));
                        if (normalized.isEmpty() || "otros".equals(normalized)) continue;
                        if (!unique.containsKey(normalized)) {
                            unique.put(normalized, normalized);
                        }
                    }
                    List<String> out = new ArrayList<>(unique.values());
                    out.sort(String::compareToIgnoreCase);
                    cacheByGroupId.put(safeGroupId, new CacheEntry(out));
                    callback.onLoaded(out);
                })
                .addOnFailureListener(e -> {
                    CacheEntry cached = cacheByGroupId.get(safeGroupId);
                    if (cached != null) {
                        callback.onLoaded(new ArrayList<>(cached.categories));
                        return;
                    }
                    callback.onLoaded(new ArrayList<>());
                });
    }

    @NonNull
    public String normalizeCategoryKey(@Nullable String rawCategory) {
        if (rawCategory == null) return "";
        String normalized = rawCategory.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return "";
        return normalized.replaceAll("\\s+", " ");
    }

    public void clearGroupCache(@Nullable String groupId) {
        if (groupId == null) return;
        cacheByGroupId.remove(groupId.trim());
    }

    private static class CacheEntry {
        final List<String> categories;
        final long loadedAtMs;

        CacheEntry(@NonNull List<String> categories) {
            this.categories = Collections.unmodifiableList(new ArrayList<>(categories));
            this.loadedAtMs = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - loadedAtMs > CACHE_TTL_MS;
        }
    }
}
