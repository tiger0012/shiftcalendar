package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * 班次闹钟接收器。
 *
 * 修复：铃声不再无限循环 —— 新闹钟触发时先停止上一次播放，
 * 且每次最多响 {@link #MAX_PLAY_DURATION_MS} 后自动停止释放。
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    /** 最长响铃时长（毫秒），防止闹钟无限循环 */
    private static final long MAX_PLAY_DURATION_MS = 60_000L;

    private static MediaPlayer sMediaPlayer;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static final Runnable sStopRunnable = AlarmReceiver::stopPlaying;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"com.example.myapplication.SHIFT_ALARM".equals(intent.getAction())) {
            return;
        }

        String team = intent.getStringExtra("team");
        boolean isDayShift = intent.getBooleanExtra("isDayShift", false);

        Log.d(TAG, String.format("闹钟触发：%s班，班组：%s", isDayShift ? "白" : "夜", team));

        // 停止上一次尚未结束的铃声
        stopPlaying();

        // 播放系统默认闹钟铃声（最多响 MAX_PLAY_DURATION_MS 后自动停止）
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            MediaPlayer player = MediaPlayer.create(context, alarmUri);
            if (player != null) {
                sMediaPlayer = player;
                player.setLooping(true);
                player.start();
                sHandler.postDelayed(sStopRunnable, MAX_PLAY_DURATION_MS);
            }
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

    /** 停止并释放当前正在播放的铃声 */
    public static void stopPlaying() {
        sHandler.removeCallbacks(sStopRunnable);
        MediaPlayer player = sMediaPlayer;
        sMediaPlayer = null;
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception e) {
                Log.w(TAG, "停止铃声失败", e);
            } finally {
                try {
                    player.release();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
