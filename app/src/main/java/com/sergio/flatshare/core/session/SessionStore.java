package com.sergio.flatshare.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class SessionStore {
    private static final String PREF = "flatshare_pref";
    private static final String KEY_GROUP = "current_group";
    private static final String KEY_ROOM_ID = "current_room_id";
    private static final String KEY_ROOM_NAME = "current_room_name";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_PENDING_EMAIL = "pending_email";
    private static final String KEY_PENDING_FULL_NAME = "pending_full_name";
    private static final String KEY_PENDING_PHONE = "pending_phone";
    private static final String KEY_PENDING_BIRTH_DATE = "pending_birth_date";
    private static final String KEY_PENDING_TERMS_ACCEPTED = "pending_terms_accepted";

    public static void setCurrentGroup(Context context, String groupId) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_GROUP, groupId).apply();
    }

    public static String getCurrentGroup(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getString(KEY_GROUP, null);
    }

    public static void clearCurrentGroup(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_GROUP)
                .apply();
    }

    public static void setCurrentRoom(Context context, String roomId, String roomName) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ROOM_ID, roomId)
                .putString(KEY_ROOM_NAME, roomName)
                .apply();
    }

    public static String getCurrentRoomId(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getString(KEY_ROOM_ID, null);
    }

    public static String getCurrentRoomName(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getString(KEY_ROOM_NAME, null);
    }

    public static void clearCurrentRoom(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ROOM_ID)
                .remove(KEY_ROOM_NAME)
                .apply();
    }

    public static void setRememberMeEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_REMEMBER_ME, enabled)
                .apply();
    }

    public static boolean isRememberMeEnabled(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getBoolean(KEY_REMEMBER_ME, false);
    }

    public static void savePendingRegistrationProfile(
            Context context,
            String email,
            String fullName,
            String phone,
            String birthDate,
            boolean termsAccepted
    ) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_EMAIL, email == null ? "" : email.trim().toLowerCase(Locale.ROOT))
                .putString(KEY_PENDING_FULL_NAME, fullName == null ? "" : fullName.trim())
                .putString(KEY_PENDING_PHONE, phone == null ? "" : phone.trim())
                .putString(KEY_PENDING_BIRTH_DATE, birthDate == null ? "" : birthDate.trim())
                .putBoolean(KEY_PENDING_TERMS_ACCEPTED, termsAccepted)
                .apply();
    }

    public static PendingRegistrationProfile getPendingRegistrationProfile(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        PendingRegistrationProfile profile = new PendingRegistrationProfile();
        profile.email = p.getString(KEY_PENDING_EMAIL, "");
        profile.fullName = p.getString(KEY_PENDING_FULL_NAME, "");
        profile.phone = p.getString(KEY_PENDING_PHONE, "");
        profile.birthDate = p.getString(KEY_PENDING_BIRTH_DATE, "");
        profile.termsAccepted = p.getBoolean(KEY_PENDING_TERMS_ACCEPTED, false);
        return profile;
    }

    public static void clearPendingRegistrationProfile(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_EMAIL)
                .remove(KEY_PENDING_FULL_NAME)
                .remove(KEY_PENDING_PHONE)
                .remove(KEY_PENDING_BIRTH_DATE)
                .remove(KEY_PENDING_TERMS_ACCEPTED)
                .apply();
    }

    public static class PendingRegistrationProfile {
        public String email = "";
        public String fullName = "";
        public String phone = "";
        public String birthDate = "";
        public boolean termsAccepted;
    }
}

