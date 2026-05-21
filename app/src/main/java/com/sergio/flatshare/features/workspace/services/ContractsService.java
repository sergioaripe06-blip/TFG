package com.sergio.flatshare.features.workspace.services;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ContractsService {
    public interface SuccessCallback {
        void onLoaded(List<DocumentSnapshot> docs);
    }

    public interface ErrorCallback {
        void onError(@NonNull String message);
    }

    private final FirebaseFirestore db;

    public ContractsService(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void loadContractsByGroup(
            @NonNull String groupId,
            @NonNull SuccessCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("rental_contracts")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(snapshot -> onSuccess.onLoaded(new ArrayList<>(snapshot.getDocuments())))
                .addOnFailureListener(e -> onError.onError(e == null ? "No se pudieron cargar contratos" : e.getMessage()));
    }
}

