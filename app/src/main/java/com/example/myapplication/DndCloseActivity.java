package com.example.myapplication;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 透明 Activity，用于在应用被杀后通过 AlarmManager 的 activity operation 重新拉起进程并关闭勿扰。
 *
 * 真我/ColorOS 的 OplusAppStartupManager 会拦截由 AlarmManager 触发的 foreground service
 * 启动（日志：isAllowStartFromStartService: prevent start），但 activity 启动的校验路径不同，
 * 有可能被允许。此 Activity 保持透明、迅速关闭勿扰后立即 finish，对用户几乎无感知。
 */
public class DndCloseActivity extends AppCompatActivity {
    private static final String TAG = "DndCloseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        Log.d(TAG, "DndCloseActivity 被启动，尝试关闭勿扰");

        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                Log.d(TAG, "勿扰模式已关闭");
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭勿扰失败", e);
        }

        // 取消通知超时兜底，避免重复触发
        AlarmHelper.cancelDndTimeoutNotification(this);

        // 重新调度下一次闹钟
        MainActivity.setDndAlarms(this);

        finish();
        overridePendingTransition(0, 0);
    }
}
