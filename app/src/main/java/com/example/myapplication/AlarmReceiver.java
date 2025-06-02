package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"com.example.myapplication.SHIFT_ALARM".equals(intent.getAction())) {
            return;
        }

        String team = intent.getStringExtra("team");
        boolean isDayShift = intent.getBooleanExtra("isDayShift", false);
        
        Log.d(TAG, String.format("闹钟触发：%s班，班组：%s", isDayShift ? "白" : "夜", team));

        // 播放系统默认闹钟铃声
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            MediaPlayer mediaPlayer = MediaPlayer.create(context, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "播放闹钟铃声失败", e);
        }

        // 震动提醒
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000, 1000}, 0));
                } else {
                    vibrator.vibrate(new long[]{0, 1000, 1000}, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "震动提醒失败", e);
        }
    }
} 