package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.app.KeyguardManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.util.Calendar;
import android.util.Log;
import android.widget.Toast;

/**
 * 午睡勿扰接收器
 *
 * 统一使用系统 NotificationManager.setInterruptionFilter() 控制勿扰，
 * 不调用 AudioManager，避免在 realme/ColorOS 上触发系统提示并硬性锁住媒体音量。
 */
public class NapAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "NapAlarmReceiver";
    public static final String ACTION_NAP_ALARM_START = "com.example.myapplication.NAP_ALARM_START";
    public static final String ACTION_NAP_ALARM_END = "com.example.myapplication.NAP_ALARM_END";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "午睡闹钟触发, action=" + action + ", OEM=" + Build.MANUFACTURER);

        boolean isStart = ACTION_NAP_ALARM_START.equals(action);
        boolean isEnd = ACTION_NAP_ALARM_END.equals(action);

        if (!isStart && !isEnd) {
            isStart = intent.getBooleanExtra("START_NAP_DND", false);
            isEnd = !isStart;
            Log.d(TAG, "使用Extra判断: " + (isStart ? "开始午睡" : "结束午睡"));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            Log.e(TAG, "NotificationManager 为空");
            return;
        }

        int currentFilter = nm.getCurrentInterruptionFilter();
        Log.d(TAG, "当前勿扰状态: " + getInterruptionFilterName(currentFilter));

        if (isStart) {
            // ★ 无论当前是否已处于勿扰，都必须先确保午睡结束闹钟存在。
            //   修复：旧逻辑在"已处于勿扰"（如常规勿扰进行中）时直接 return，
            //   午睡结束闹钟未设置 → 午睡结束后勿扰无法按午睡结束时间关闭。
            scheduleNapEndAlarm(context);

            // 已处于勿扰则不重复设置系统勿扰
            if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                && currentFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                Log.d(TAG, "已处于勿扰模式，不重复设置（结束闹钟已确保存在）");
                registerScreenStateReceiver(context);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                enableNapDnd(context, nm);
            }

            AlarmHelper.createDndSettingNotification(context, "午睡勿扰已开启",
                "系统完全勿扰模式已开启，允许重复来电和闹钟，解锁时不静音");

        } else if (isEnd) {
            // 取消广播结束闹钟（防止重复触发）
            Intent endIntent = new Intent(context, NapAlarmReceiver.class);
            endIntent.setAction(ACTION_NAP_ALARM_END);
            PendingIntent endPi = PendingIntent.getBroadcast(
                    context, 1002, endIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(endPi);
                endPi.cancel();
                Log.d(TAG, "已取消午睡结束广播闹钟");
            }

            try {
                if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                    && currentFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    Log.d(TAG, "午睡勿扰已关闭");
                } else {
                    Log.d(TAG, "当前未处于勿扰，无需关闭");
                }
            } catch (Exception e) {
                Log.e(TAG, "关闭午睡勿扰失败", e);
            }

            unregisterScreenStateReceiver(context);
            AlarmHelper.createDndSettingNotification(context, "午睡勿扰已结束", "系统勿扰模式已关闭");
            Toast.makeText(context, "午睡勿扰已结束", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 用广播方式设置午睡结束闹钟（不用 ForegroundService，避免 ColorOS 拦截）
     */
    private void scheduleNapEndAlarm(Context context) {
        SharedPreferences napPrefs = context.getSharedPreferences("nap_settings", Context.MODE_PRIVATE);
        long napEndTime = napPrefs.getLong("nap_end_time", -1);
        if (napEndTime > System.currentTimeMillis()) {
            Intent endIntent = new Intent(context, NapAlarmReceiver.class);
            endIntent.setAction(ACTION_NAP_ALARM_END);
            PendingIntent endPi = PendingIntent.getBroadcast(
                    context, 1002, endIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, napEndTime, endPi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, napEndTime, endPi);
                }
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(napEndTime);
                Log.d(TAG, "⏰ 已设午睡结束广播闹钟 → "
                        + cal.get(Calendar.HOUR_OF_DAY) + ":"
                        + String.format("%02d", cal.get(Calendar.MINUTE)));
            }
        } else if (napEndTime > 0) {
            Log.w(TAG, "⏰ 午睡结束时间已过，不设闹钟");
        } else {
            Log.w(TAG, "⚠ 未找到 nap_end_time，依赖 setNapDoNotDisturb 中的兜底闹钟");
        }
    }

    private void enableNapDnd(Context context, NotificationManager nm) {
        KeyguardManager kgm = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = kgm != null && kgm.isKeyguardLocked();

        int suppressedVisual = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            suppressedVisual = locked
                ? NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
                    | NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK
                    | NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR
                    | NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE
                    | NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT
                    | NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS
                : NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
        }

        int allowed = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
            | NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;

        nm.setNotificationPolicy(new NotificationManager.Policy(allowed, 0, suppressedVisual));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                Log.d(TAG, "午睡系统DND已启用");
            } catch (Exception e) {
                Log.e(TAG, "设置午睡DND过滤器失败", e);
            }
        }, 200);

        registerScreenStateReceiver(context);
    }

    private String getInterruptionFilterName(int filter) {
        switch (filter) {
            case NotificationManager.INTERRUPTION_FILTER_ALL: return "全部允许";
            case NotificationManager.INTERRUPTION_FILTER_PRIORITY: return "仅优先通知";
            case NotificationManager.INTERRUPTION_FILTER_NONE: return "全部阻止";
            case NotificationManager.INTERRUPTION_FILTER_ALARMS: return "仅闹钟";
            case NotificationManager.INTERRUPTION_FILTER_UNKNOWN: return "未知";
            default: return "状态码:" + filter;
        }
    }

    private void registerScreenStateReceiver(Context context) {
        Intent intent = new Intent(context, ScreenStateReceiver.class);
        intent.setAction("com.example.myapplication.ACTION_SCREEN_STATE_CHANGE");
        context.sendBroadcast(intent);
    }

    private void unregisterScreenStateReceiver(Context context) {
        Intent intent = new Intent(context, ScreenStateReceiver.class);
        intent.setAction("com.example.myapplication.ACTION_SCREEN_STATE_UNREGISTER");
        context.sendBroadcast(intent);
    }
}
