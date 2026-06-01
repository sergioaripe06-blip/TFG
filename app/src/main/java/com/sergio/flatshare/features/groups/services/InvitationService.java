package com.sergio.flatshare.features.groups.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InvitationService {
    public interface SuccessCallback {
        void onSuccess();
    }

    public interface ErrorCallback {
        void onError(@NonNull String message);
    }

    public interface CompletionCallback {
        void onDone();
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
        final String invitedEmailLc = invitedEmail.trim().toLowerCase(Locale.ROOT);
        final String inviterEmailLc = inviterEmail.trim().toLowerCase(Locale.ROOT);
        if (invitedEmailLc.isEmpty() || !invitedEmailLc.contains("@")) {
            onError.onError("Email inválido");
            return;
        }
        if (!inviterEmailLc.isEmpty() && invitedEmailLc.equals(inviterEmailLc)) {
            onError.onError("No puedes invitarte a ti mismo");
            return;
        }

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> {
                    if (!groupDoc.exists()) {
                        onError.onError("El piso ya no existe");
                        return;
                    }

                    List<String> memberEmails = toNormalizedEmails(groupDoc.get("memberEmails"));
                    if (memberEmails.contains(invitedEmailLc)) {
                        onError.onError("Ese correo ya es miembro del piso");
                        return;
                    }

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
                    inv.put("invitedEmail", invitedEmailLc);
                    inv.put("inviterUid", inviterUid);
                    inv.put("inviterEmail", inviterEmailLc);
                    inv.put("status", "pending");
                    inv.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("invitations")
                            .whereEqualTo("groupId", groupId)
                            .whereEqualTo("invitedEmail", invitedEmailLc)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(existing -> {
                                if (existing.isEmpty()) {
                                    db.collection("invitations").add(inv)
                                            .addOnSuccessListener(v -> onSuccess.onSuccess())
                                            .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo crear la invitación" : e.getMessage()));
                                    return;
                                }
                                // Reutiliza la primera invitación pendiente para evitar bucles por duplicados.
                                existing.getDocuments().get(0).getReference()
                                        .update(inv)
                                        .addOnSuccessListener(v -> onSuccess.onSuccess())
                                        .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo actualizar la invitación" : e.getMessage()));
                            })
                            .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo validar invitaciones previas" : e.getMessage()));
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
        final String emailLc = email.trim().toLowerCase(Locale.ROOT);
        db.collection("groups").document(groupId).update(
                "members", FieldValue.arrayUnion(uid),
                "memberEmails", FieldValue.arrayUnion(emailLc),
                "roles." + uid, "member"
        ).addOnSuccessListener(v -> db.collection("invitations").document(invitationId).update("status", "accepted")
                .addOnSuccessListener(v2 -> resolvePendingInvitationsForGroup(
                        groupId,
                        emailLc,
                        "accepted",
                        onSuccess::onSuccess
                ))
                .addOnFailureListener(e2 -> resolvePendingInvitationsForGroup(
                        groupId,
                        emailLc,
                        "accepted",
                        onSuccess::onSuccess
                ))
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

    public void resolvePendingInvitationsForGroup(
            @NonNull String groupId,
            @NonNull String invitedEmail,
            @NonNull String finalStatus,
            @Nullable CompletionCallback onDone
    ) {
        String emailLc = invitedEmail.trim().toLowerCase(Locale.ROOT);
        if (groupId.trim().isEmpty() || emailLc.isEmpty()) {
            if (onDone != null) onDone.onDone();
            return;
        }
        db.collection("invitations")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("invitedEmail", emailLc)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> updateInvitationsStatus(snapshot, finalStatus, onDone))
                .addOnFailureListener(e -> {
                    if (onDone != null) onDone.onDone();
                });
    }

    private void updateInvitationsStatus(
            @NonNull QuerySnapshot snapshot,
            @NonNull String finalStatus,
            @Nullable CompletionCallback onDone
    ) {
        if (snapshot.isEmpty()) {
            if (onDone != null) onDone.onDone();
            return;
        }
        List<com.google.android.gms.tasks.Task<Void>> updates = new ArrayList<>();
        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
            updates.add(doc.getReference().update("status", finalStatus));
        }
        com.google.android.gms.tasks.Tasks.whenAllComplete(updates)
                .addOnCompleteListener(task -> {
                    if (onDone != null) onDone.onDone();
                });
    }

    private List<String> toNormalizedEmails(@Nullable Object raw) {
        List<String> out = new ArrayList<>();
        if (!(raw instanceof List<?>)) return out;
        for (Object item : (List<?>) raw) {
            if (item == null) continue;
            String normalized = item.toString().trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) out.add(normalized);
        }
        return out;
    }
}

