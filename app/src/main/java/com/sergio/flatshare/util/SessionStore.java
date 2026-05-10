package com.sergio.flatshare.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionStore {
    private static final String PREF = "flatshare_pref";
    private static final String KEY_GROUP = "current_group";
    private static final String KEY_ROOM_ID = "current_room_id";
    private static final String KEY_ROOM_NAME = "current_room_name";

    public static void setCurrentGroup(Context context, String groupId) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_GROUP, groupId).apply();
    }

    public static String getCurrentGroup(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getString(KEY_GROUP, null);
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
}

