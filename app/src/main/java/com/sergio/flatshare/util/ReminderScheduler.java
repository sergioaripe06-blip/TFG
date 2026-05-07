package com.sergio.flatshare.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.sergio.flatshare.ui.ReminderReceiver;

public class ReminderScheduler {
    public static void schedule(Context context, int reminderId, String title, String body, long triggerAtMillis, long intervalMillis) {
        if (!SettingsStore.areNotificationsEnabled(context)) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        PendingIntent pi = PendingIntent.getBroadcast(context, reminderId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, pi);
        }
    }

    public static void scheduleOneTime(Context context, int reminderId, String title, String body, long triggerAtMillis) {
        if (!SettingsStore.areNotificationsEnabled(context)) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        PendingIntent pi = PendingIntent.getBroadcast(context, reminderId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }
}

