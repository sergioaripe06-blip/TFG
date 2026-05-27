package com.sergio.flatshare.core.sync;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserSync {
    public static void ensureCurrentUserDocument() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String fullName = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        if (fullName.isEmpty()) fullName = user.getEmail();
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail().trim().toLowerCase());
        data.put("uid", user.getUid());
        data.put("displayName", fullName);
        data.put("fullName", fullName);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data, SetOptions.merge());
    }

    public static void saveCurrentUserProfile(String fullName, String phone, String birthDate) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String safeFullName = fullName == null ? "" : fullName.trim();
        if (safeFullName.isEmpty()) safeFullName = user.getEmail();
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("email", user.getEmail().trim().toLowerCase());
        data.put("displayName", safeFullName);
        data.put("fullName", safeFullName);
        data.put("phone", phone == null ? "" : phone.trim());
        data.put("birthDate", birthDate == null ? "" : birthDate.trim());
        data.put("profileCompleted", true);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data, SetOptions.merge());
    }

    public static void saveTermsAcceptance() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Map<String, Object> termsData = new HashMap<>();
        termsData.put("termsAccepted", true);
        termsData.put("termsAcceptedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(termsData, SetOptions.merge());
    }
}

