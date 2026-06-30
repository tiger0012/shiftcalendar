package com.example.myapplication;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * 勿扰模式超时接收器
 *
 * 作为“被杀后仍能触发”的兜底机制：
 * 当勿扰开启时，会创建一个带 setTimeoutAfter 的普通通知，由系统 NotificationManager
 * 在超时（即勿扰结束时间）后自动移除该通知，并触发此 Receiver 的 deleteIntent。
 * 即使 App 进程已被 ColorOS/realme 杀掉，系统通知服务仍会存活，从而保证关闭勿扰
 * 的广播能够被触发。
 */
public class DndTimeoutReceiver extends BroadcastReceiver {

    private static final String TAG = "DndTimeoutReceiver";
    public static final String ACTION_DND_TIMEOUT = "com.example.myapplication.ACTION_DND_TIMEOUT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_DND_TIMEOUT.equals(intent.getAction())) {
            return;
        }

        Log.d(TAG, "收到勿扰超时广播，尝试关闭勿扰");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e(TAG, "设备版本不支持勿扰模式");
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            Log.e(TAG, "NotificationManager 为空");
            return;
        }

        if (!nm.isNotificationPolicyAccessGranted()) {
            Log.e(TAG, "没有勿扰模式控制权限");
            return;
        }

        // 再次确认当前确实需要关闭，防止误触发
        SharedPreferences prefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
        int startHour = prefs.getInt("current_dnd_start_hour", -1);
        int startMinute = prefs.getInt("current_dnd_start_minute", 0);
        int endHour = prefs.getInt("current_dnd_end_hour", -1);
        int endMinute = prefs.getInt("current_dnd_end_minute", 0);

        if (startHour < 0 || endHour < 0) {
            Log.w(TAG, "未找到勿扰时间设置，直接关闭勿扰");
        } else if (isCurrentlyInDndRange(startHour, startMinute, endHour, endMinute)) {
            Log.w(TAG, "当前仍在勿扰区间内，跳过本次超时关闭");
            // 即使跳过，也重新注册通知兜底，确保下一次不会漏掉
            AlarmHelper.scheduleDndTimeoutNotification(context);
            return;
        }

        try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            Log.d(TAG, "勿扰模式已通过超时通知关闭");
        } catch (Exception e) {
            Log.e(TAG, "关闭勿扰模式失败", e);
        }

        // 取消结束闹钟（防止 AlarmManager 稍后重复触发）
        DndForegroundService.cancelEndAlarm(context);

        // 取消超时通知（如果系统没有自动移除）
        AlarmHelper.cancelDndTimeoutNotification(context);

        // 发送关闭通知
        AlarmHelper.createDndSettingNotification(context, "勿扰模式已关闭",
            "系统勿扰模式已通过定时通知兜底机制关闭。");

        // 重新设置下一次开启/关闭闹钟
        MainActivity.setDndAlarms(context);
    }

    /**
     * 判断当前时间是否仍在勿扰区间内（处理跨夜情况）
     */
    private boolean isCurrentlyInDndRange(int startHour, int startMinute, int endHour, int endMinute) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
        int startMinutes = startHour * 60 + startMinute;
        int endMinutes = endHour * 60 + endMinute;

        if (startMinutes < endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes < endMinutes;
        } else {
            return currentMinutes >= startMinutes || currentMinutes < endMinutes;
        }
    }
}
