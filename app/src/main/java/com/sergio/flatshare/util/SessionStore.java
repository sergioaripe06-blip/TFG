package com.sergio.flatshare.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionStore {
    private static final String PREF = "flatshare_pref";
    private static final String KEY_GROUP = "current_group";

    public static void setCurrentGroup(Context context, String groupId) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_GROUP, groupId).apply();
    }

    public static String getCurrentGroup(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getString(KEY_GROUP, null);
    }
}

