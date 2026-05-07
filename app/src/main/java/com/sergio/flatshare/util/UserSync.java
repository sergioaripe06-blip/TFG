package com.sergio.flatshare.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserSync {
    public static void ensureCurrentUserDocument() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail().trim().toLowerCase());
        data.put("uid", user.getUid());
        data.put("displayName", user.getDisplayName() == null ? user.getEmail() : user.getDisplayName());
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data, SetOptions.merge());
    }

    public static void saveCurrentUserProfile(String fullName, String username, String phone, String birthDate) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("email", user.getEmail().trim().toLowerCase());
        data.put("displayName", fullName == null || fullName.trim().isEmpty() ? user.getEmail() : fullName.trim());
        data.put("fullName", fullName == null ? "" : fullName.trim());
        data.put("username", normalizedUsername);
        data.put("phone", phone == null ? "" : phone.trim());
        data.put("birthDate", birthDate == null ? "" : birthDate.trim());
        data.put("profileCompleted", true);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data, SetOptions.merge());

        if (!normalizedUsername.isEmpty()) {
            Map<String, Object> publicData = new HashMap<>();
            publicData.put("uid", user.getUid());
            publicData.put("email", user.getEmail().trim().toLowerCase());
            publicData.put("displayName", data.get("displayName"));
            FirebaseFirestore.getInstance().collection("usernames").document(normalizedUsername).set(publicData, SetOptions.merge());
        }
    }
}

