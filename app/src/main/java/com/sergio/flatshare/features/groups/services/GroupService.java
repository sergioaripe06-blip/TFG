package com.sergio.flatshare.features.groups.services;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupService {
    public interface LoadGroupsCallback {
        void onLoaded(List<GroupSummary> groups, int membersTotal);
    }

    public interface ErrorCallback {
        void onError(@NonNull String message);
    }

    public interface JoinCallback {
        void onJoined(@NonNull String groupId);
    }

    public interface CreateCallback {
        void onCreated(@NonNull String groupId);
    }

    public static class GroupSummary {
        public final String id;
        public final String name;
        public final int members;
        public final String description;
        public final boolean isOwner;

        public GroupSummary(
                @NonNull String id,
                @NonNull String name,
                int members,
                @NonNull String description,
                boolean isOwner
        ) {
            this.id = id;
            this.name = name;
            this.members = members;
            this.description = description;
            this.isOwner = isOwner;
        }
    }

    private final FirebaseFirestore db;

    public GroupService(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void loadGroupsForUser(
            @NonNull String uid,
            @NonNull LoadGroupsCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("groups").whereArrayContains("members", uid).get().addOnSuccessListener(res -> {
            List<GroupSummary> out = new ArrayList<>();
            int membersTotal = 0;
            for (DocumentSnapshot doc : res.getDocuments()) {
                String name = doc.getString("name");
                String description = doc.getString("description");
                String ownerId = doc.getString("ownerId");
                Object membersField = doc.get("members");
                List<?> members = membersField instanceof List ? (List<?>) membersField : null;
                int count = members == null ? 0 : members.size();
                membersTotal += count;
                out.add(new GroupSummary(
                        doc.getId(),
                        name == null ? "Piso" : name,
                        count,
                        description == null || description.trim().isEmpty() ? "Sin direccion cargada" : description,
                        uid.equals(ownerId)
                ));
            }
            onSuccess.onLoaded(out, membersTotal);
        }).addOnFailureListener(e -> onError.onError(e == null ? "Error desconocido cargando pisos" : e.getMessage()));
    }

    public void createGroup(
            @NonNull Map<String, Object> groupData,
            @NonNull String ownerUid,
            @NonNull String fallbackName,
            @NonNull CreateCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("groups").add(groupData).addOnSuccessListener(doc -> {
            String shareCode = doc.getId().toUpperCase(Locale.ROOT);
            Map<String, Object> codeData = new HashMap<>();
            codeData.put("groupId", doc.getId());
            codeData.put("ownerId", ownerUid);
            codeData.put("name", fallbackName);
            doc.update("shareCode", shareCode);
            db.collection("group_codes").document(shareCode).set(codeData)
                    .addOnSuccessListener(v -> onSuccess.onCreated(doc.getId()))
                    .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo guardar el codigo del piso" : e.getMessage()));
        }).addOnFailureListener(e -> onError.onError(e == null ? "No se pudo crear el piso" : e.getMessage()));
    }

    public void joinGroupByCode(
            @NonNull String code,
            @NonNull String uid,
            @NonNull String email,
            @NonNull JoinCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        String uidLc = uid.trim().toLowerCase(Locale.ROOT);
        String emailLc = email.trim().toLowerCase(Locale.ROOT);

        db.collection("group_codes")
                .document(code)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.exists()) {
                        onError.onError("Codigo no valido");
                        return;
                    }
                    String groupId = result.getString("groupId");
                    if (groupId == null || groupId.trim().isEmpty()) {
                        onError.onError("Codigo no valido");
                        return;
                    }

                    db.collection("groups").document(groupId).get()
                            .addOnSuccessListener(groupDoc -> {
                                if (!groupDoc.exists()) {
                                    onError.onError("El piso ya no existe");
                                    return;
                                }
                                List<String> members = castStringList(groupDoc.get("members"));
                                List<String> memberEmails = castStringList(groupDoc.get("memberEmails"));
                                boolean alreadyMember = members.contains(uidLc) || memberEmails.contains(emailLc);
                                if (alreadyMember) {
                                    onError.onError("Acceso denegado: ya estas en este piso");
                                    return;
                                }

                                addUserToGroup(groupId, uid, emailLc, onSuccess, onError);
                            })
                            .addOnFailureListener(e -> {
                                // Un no-miembro puede no tener permiso de lectura del grupo.
                                // En ese caso intentamos directamente el alta con arrayUnion.
                                if (isPermissionDenied(e)) {
                                    addUserToGroup(groupId, uid, emailLc, onSuccess, onError);
                                    return;
                                }
                                onError.onError(e == null ? "No se pudo verificar el piso" : e.getMessage());
                            });
                })
                .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo verificar el codigo" : e.getMessage()));
    }

    private boolean isPermissionDenied(Throwable error) {
        if (!(error instanceof FirebaseFirestoreException ex)) return false;
        return ex.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
    }

    @NonNull
    private List<String> castStringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (!(raw instanceof List<?>)) return out;
        for (Object value : (List<?>) raw) {
            if (value == null) continue;
            String item = value.toString().trim().toLowerCase(Locale.ROOT);
            if (!item.isEmpty()) out.add(item);
        }
        return out;
    }

    private void addUserToGroup(
            @NonNull String groupId,
            @NonNull String uid,
            @NonNull String email,
            @NonNull JoinCallback onSuccess,
            @NonNull ErrorCallback onError
    ) {
        db.collection("groups").document(groupId).update(
                "members", FieldValue.arrayUnion(uid),
                "memberEmails", FieldValue.arrayUnion(email),
                "roles." + uid, "member"
        ).addOnSuccessListener(v -> onSuccess.onJoined(groupId))
                .addOnFailureListener(e -> onError.onError(e == null ? "No se pudo unir al piso" : e.getMessage()));
    }
}
