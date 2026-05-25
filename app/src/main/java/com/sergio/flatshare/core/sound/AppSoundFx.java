package com.sergio.flatshare.core.sound;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;

import androidx.annotation.NonNull;

import com.sergio.flatshare.core.settings.SettingsStore;

public final class AppSoundFx {
    private AppSoundFx() {}

    public static final String FX_GROUP_CREATED = "sfx_group_created";
    public static final String FX_EXPENSE_ACCEPTED = "sfx_expense_accepted";

    public static void playByName(@NonNull Context context, @NonNull String rawName) {
        int volumePercent = SettingsStore.getMusicVolume(context);
        if (volumePercent <= 0) return;

        int rawResId = context.getResources().getIdentifier(rawName, "raw", context.getPackageName());
        if (rawResId == 0) {
            playFallback(volumePercent);
            return;
        }

        MediaPlayer player = MediaPlayer.create(context, rawResId);
        if (player == null) {
            playFallback(volumePercent);
            return;
        }

        float normalized = Math.max(0f, Math.min(1f, volumePercent / 100f));
        float curved = normalized * normalized;
        player.setVolume(curved, curved);
        player.setOnCompletionListener(MediaPlayer::release);
        player.setOnErrorListener((mp, what, extra) -> {
            mp.release();
            return true;
        });
        player.start();
    }

    private static void playFallback(int volumePercent) {
        int toneVolume = Math.max(15, Math.min(100, volumePercent));
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, toneVolume);
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(tone::release, 180L);
    }
}
