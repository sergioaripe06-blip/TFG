package com.sergio.flatshare.features.groups.services;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitialRoomsSetupFlow {
    public static class RoomDraftInput {
        public final int roomNumber;
        public final String name;
        public final int capacity;
        public final double monthlyCost;

        public RoomDraftInput(int roomNumber, @NonNull String name, int capacity, double monthlyCost) {
            this.roomNumber = roomNumber;
            this.name = name;
            this.capacity = capacity;
            this.monthlyCost = monthlyCost;
        }
    }

    public interface SuccessCallback {
        void onSuccess();
    }

    public interface ErrorCallback {
        void onError(@NonNull String message);
    }

    private final FirebaseFirestore db;

    public InitialRoomsSetupFlow(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void saveInitialRooms(
            @NonNull String groupId,
            @NonNull String createdByUid,
            @NonNull List<RoomDraftInput> drafts,
            @NonNull SuccessCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        if (drafts.isEmpty()) {
            onSuccess.onSuccess();
            return;
        }
        WriteBatch batch = db.batch();
        for (RoomDraftInput draft : drafts) {
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("groupId", groupId);
            roomData.put("roomNumber", draft.roomNumber);
            roomData.put("name", draft.name);
            roomData.put("capacity", draft.capacity);
            roomData.put("monthlyCost", draft.monthlyCost);
            roomData.put("memberEmails", new ArrayList<String>());
            roomData.put("memberCount", 0);
            roomData.put("createdByUid", createdByUid);
            roomData.put("updatedByUid", createdByUid);
            roomData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            roomData.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            DocumentReference roomRef = db.collection("rooms_groups").document();
            batch.set(roomRef, roomData);
        }
        batch.commit()
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onError.onError(e == null ? "No se pudieron guardar habitaciones" : e.getMessage()));
    }

    public Task<Void> deleteDocumentsInChunks(@NonNull List<DocumentReference> refs) {
        if (refs.isEmpty()) {
            return Tasks.forResult(null);
        }
        final int chunkSize = 450;
        List<Task<Void>> commits = new ArrayList<>();
        WriteBatch batch = db.batch();
        int count = 0;
        for (DocumentReference ref : refs) {
            batch.delete(ref);
            count++;
            if (count == chunkSize) {
                commits.add(batch.commit());
                batch = db.batch();
                count = 0;
            }
        }
        if (count > 0) {
            commits.add(batch.commit());
        }
        return commits.isEmpty() ? Tasks.forResult(null) : Tasks.whenAll(commits);
    }
}

