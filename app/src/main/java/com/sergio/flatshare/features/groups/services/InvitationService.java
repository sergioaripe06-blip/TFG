package com.sergio.flatshare.features.groups.services;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InvitationService {
    public interface SuccessCallback {
        void onSuccess();
    }

    public interface ErrorCallback {
        void onError(@NonNull String message);
    }

    private final FirebaseFirestore db;

    public InvitationService(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void createEmailInvitation(
            @NonNull String groupId,
            @NonNull String invitedEmail,
            @NonNull String inviterUid,
            @NonNull String inviterEmail,
            @NonNull SuccessCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> {
                    String currentGroupName = groupDoc.getString("name");
                    if (currentGroupName == null || currentGroupName.trim().isEmpty()) {
                        currentGroupName = "Piso";
                    }
                    String currentShareCode = groupDoc.getString("shareCode");
                    if (currentShareCode == null || currentShareCode.trim().isEmpty()) {
                        currentShareCode = groupId.toUpperCase(Locale.ROOT);
                    }

                    Map<String, Object> inv = new HashMap<>();
                    inv.put("groupId", groupId);
                    inv.put("groupName", currentGroupName);
                    inv.put("shareCode", currentShareCode);
                    inv.put("invitedEmail", invitedEmail);
                    inv.put("inviterUid", inviterUid);
                    inv.put("inviterEmail", inviterEmail);
                    inv.put("status", "pending");
                    inv.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("invitations").add(inv)
                            .addOnSuccessListener(v -> onSuccess.onSuccess())
                            .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo crear la invitación" : e.getMessage()));
                })
                .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo leer el piso" : e.getMessage()));
    }

    public void acceptInvitation(
            @NonNull String invitationId,
            @NonNull String groupId,
            @NonNull String uid,
            @NonNull String email,
            @NonNull SuccessCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("groups").document(groupId).update(
                "members", FieldValue.arrayUnion(uid),
                "memberEmails", FieldValue.arrayUnion(email),
                "roles." + uid, "member"
        ).addOnSuccessListener(v -> db.collection("invitations").document(invitationId).update("status", "accepted")
                .addOnSuccessListener(v2 -> onSuccess.onSuccess())
                .addOnFailureListener(e2 -> onError.onError(e2 == null ? "No se pudo actualizar la invitación" : e2.getMessage()))
        ).addOnFailureListener(e -> onError.onError(e == null ? "No se pudo aceptar la invitación" : e.getMessage()));
    }

    public void rejectInvitation(
            @NonNull String invitationId,
            @NonNull SuccessCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("invitations").document(invitationId).update("status", "rejected")
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo rechazar la invitación" : e.getMessage()));
    }
}

