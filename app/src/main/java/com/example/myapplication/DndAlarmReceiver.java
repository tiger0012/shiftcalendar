package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class DndAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "DndAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "DndAlarmReceiver onReceive 被触发");
        boolean enable = intent.getBooleanExtra("ENABLE_DND", false);
        Log.d(TAG, "ENABLE_DND: " + enable);
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager 为空");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (enable) {
                // 开启勿扰模式时，设置优先通知规则
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // 允许闹钟、提醒、来电、短信等优先通知
                    NotificationManager.Policy policy = new NotificationManager.Policy(
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS |
                        NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS |
                        NotificationManager.Policy.PRIORITY_CATEGORY_CALLS |
                        NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                        0
                    );
                    notificationManager.setNotificationPolicy(policy);
                    Log.d(TAG, "已设置优先通知规则");
                }
                // 设置勿扰模式为优先通知
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                Log.d(TAG, "勿扰模式已启用（优先）");

                // 勿扰模式开启时的通知和Toast
                String notificationTitle = "勿扰模式已开启";
                String notificationText = "系统勿扰模式已根据您的设置开启。";
                AlarmHelper.createDndSettingNotification(context, notificationTitle, notificationText);
                Toast.makeText(context, notificationTitle, Toast.LENGTH_SHORT).show();

            } else {
                // 关闭勿扰模式
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                Log.d(TAG, "勿扰模式已关闭");

                // 勿扰模式关闭时的通知和Toast
                String notificationTitle = "勿扰模式已关闭";
                String notificationText = "系统勿扰模式已根据您的设置关闭。";
                AlarmHelper.createDndSettingNotification(context, notificationTitle, notificationText);
                Toast.makeText(context, notificationTitle, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "设备版本不支持勿扰模式");
        }

        // 勿扰模式切换完成后，重新设置下一次定时任务
        Log.d(TAG, "开始重新设置下一次定时任务");
        MainActivity.setDndAlarms(context);
        Log.d(TAG, "下一次定时任务设置完成");
    }
}