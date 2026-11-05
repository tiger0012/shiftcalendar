package com.example.myapplication;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * 透明 Activity，用于在应用被杀后通过 AlarmManager 的 activity operation 重新拉起进程并关闭勿扰。
 *
 * 真我/ColorOS 的 OplusAppStartupManager 会拦截由 AlarmManager 触发的 foreground service
 * 启动（日志：isAllowStartFromStartService: prevent start），但 activity 启动的校验路径不同，
 * 有可能被允许。此 Activity 保持透明、迅速关闭勿扰后立即 finish，对用户几乎无感知。
 *
 * 关闭失败（无勿扰控制权限/异常）时会发一条常驻高优先级通知提醒手动处理，
 * 避免用户发现不了勿扰没关。
 */
public class DndCloseActivity extends AppCompatActivity {
    private static final String TAG = "DndCloseActivity";
    private static final String FAILED_CHANNEL_ID = "dnd_close_failed";
    private static final int FAILED_NOTIFICATION_ID = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        Log.d(TAG, "DndCloseActivity 被启动，尝试关闭勿扰");

        boolean closed = false;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!nm.isNotificationPolicyAccessGranted()) {
                    Log.e(TAG, "无勿扰模式控制权限，无法自动关闭");
                } else {
                    int current = nm.getCurrentInterruptionFilter();
                    if (current == NotificationManager.INTERRUPTION_FILTER_ALL
                            || current == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                        Log.d(TAG, "勿扰当前未开启，无需关闭");
                    } else {
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                        Log.d(TAG, "勿扰模式已关闭");
                    }
                    closed = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭勿扰失败", e);
        }

        // ★ 关闭失败（未获勿扰控制权限/异常）→ 发常驻通知提醒手动处理
        if (!closed) {
            showCloseFailedNotification();
        }

        // 取消通知超时兜底，避免重复触发
        AlarmHelper.cancelDndTimeoutNotification(this);

        // 重新调度下一次闹钟
        MainActivity.setDndAlarms(this);

        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * 勿扰自动关闭失败时的高优先级通知：
     * 通知留在面板里，点进应用可手动处理，避免用户发现不了勿扰没关。
     */
    private void showCloseFailedNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                FAILED_CHANNEL_ID, "勿扰自动关闭失败", android.app.NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);

            android.app.PendingIntent contentIntent = android.app.PendingIntent.getActivity(
                this, FAILED_NOTIFICATION_ID, new Intent(this, MainActivity.class),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FAILED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⚠️ 勿扰模式未自动关闭")
                .setContentText("自动关闭失败（可能缺少「勿扰模式」权限）。点进来手动关闭。")
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
            NotificationManagerCompat.from(this).notify(FAILED_NOTIFICATION_ID, builder.build());
            Log.d(TAG, "已发送关闭失败提醒通知");
        } catch (Exception e) {
            Log.e(TAG, "发送失败通知失败", e);
        }
    }
}
