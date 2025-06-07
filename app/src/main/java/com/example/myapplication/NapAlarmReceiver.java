package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class NapAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "NapAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "午睡闹钟触发");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // 关闭勿扰模式
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                Log.d(TAG, "午睡勿扰模式已关闭");

                // 显示通知和Toast
                String notificationTitle = "午睡勿扰已结束";
                String notificationText = "系统勿扰模式已关闭";
                AlarmHelper.createDndSettingNotification(context, notificationTitle, notificationText);
                Toast.makeText(context, notificationTitle, Toast.LENGTH_SHORT).show();
            }
        }
    }
} 