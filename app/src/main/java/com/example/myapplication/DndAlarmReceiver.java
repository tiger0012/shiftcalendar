package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * 常规勿扰闹钟接收器
 *
 * 开启时：设 AlarmManager 精确关闭闹钟（通过 DndForegroundService 按需服务）
 * 关闭时：取消结束闹钟 + 直接关勿扰
 */
public class DndAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "DndAlarmReceiver";
    public static final String ACTION_DND_ALARM_START = "com.example.myapplication.DND_ALARM_START";
    public static final String ACTION_DND_ALARM_END = "com.example.myapplication.DND_ALARM_END";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        String action = intent.getAction();
        boolean isStart = ACTION_DND_ALARM_START.equals(action);
        boolean isEnd = ACTION_DND_ALARM_END.equals(action);

        // ★ 兼容 AlarmSettingsActivity 发的带 extra 的广播（无 action）
        if (!isStart && !isEnd) {
            boolean enableDnd = intent.getBooleanExtra("ENABLE_DND", false);
            if (enableDnd) {
                isStart = true;
            } else if (intent.hasExtra("ENABLE_DND")) {
                isEnd = true;
            }
            Log.d(TAG, "通过 ENABLE_DND extra 判断: " + (isStart ? "开启" : "关闭"));
        }

        Log.d(TAG, "收到广播: " + action + " isStart=" + isStart + " isEnd=" + isEnd);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (isStart) {
            // ★ 无论当前是否已处于勿扰，都必须确保结束闹钟 + 超时兜底通知存在。
            //   旧逻辑在"已处于勿扰"时直接 return，导致结束闹钟/兜底缺失，
            //   进程被杀后勿扰无法自动关闭（用户反馈的"勿扰不对劲"主因）。
            int currentFilter = nm.getCurrentInterruptionFilter();
            boolean alreadyOn = currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                && currentFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN;

            if (!alreadyOn) {
                // 开启勿扰
                try {
                    // 读取"允许重复来电"设置，决定策略
                    SharedPreferences prefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
                    boolean allowRepeated = prefs.getBoolean("allow_repeated_calls", true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        int allowed = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
                        if (allowRepeated) {
                            allowed |= NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
                        }
                        nm.setNotificationPolicy(new NotificationManager.Policy(allowed, 0, 0));
                    }
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    Log.d(TAG, "已开启勿扰模式，允许重复来电=" + allowRepeated);
                } catch (Exception e) {
                    Log.e(TAG, "开启勿扰失败", e);
                }
            } else {
                Log.d(TAG, "已处于勿扰模式，仅补设结束闹钟与超时兜底");
            }

            // ★ 设精确的 AlarmManager 关闭闹钟到结束时间
            DndForegroundService.scheduleEndAlarm(context);

            // ★ 超时兜底通知：即使 App 进程被杀，系统通知服务仍会在结束时间触发
            //   DndTimeoutReceiver 关闭勿扰（修复：此前只有开机时才设，勿扰开启路径从未设过）
            AlarmHelper.scheduleDndTimeoutNotification(context);

            // ★ 注册屏幕状态监听：完全勿扰模式下，解锁时自动允许声音提醒
            //   （修复：此前仅午睡勿扰注册，常规勿扰从未注册，UI 承诺的"解锁时自动允许声音"不生效）
            ScreenStateReceiver.register(context);

            // 启动前台服务保活（同时负责立即检查和 onTaskRemoved 重启）
            DndForegroundService.start(context, "勿扰模式运行中",
                "系统勿扰模式已开启", false);

        } else if (isEnd) {
            // \u5156\u5e95\u8def\u5f84\uff1a\u5e7f\u64ad\u63a5\u6536\u5668\u76f4\u63a5\u5173\u95ed\u52ff\u6270
            Log.d(TAG, "\u3010\u5156\u5e95\u3011\u6536\u5230\u7ed3\u675f\u5e7f\u64ad\uff0c\u5173\u95ed\u52ff\u6270");
            DndForegroundService.cancelEndAlarm(context);

            try {
                int currentFilter = nm.getCurrentInterruptionFilter();
                if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                    && currentFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    Log.d(TAG, "\u2705 \u5df2\u901a\u8fc7\u5156\u5e95\u5e7f\u64ad\u5173\u95ed\u52ff\u6270\u6a21\u5f0f");
                } else {
                    Log.d(TAG, "\u5f53\u524d\u672a\u5904\u4e8e\u52ff\u6270\uff0c\u65e0\u9700\u5173\u95ed");
                }
            } catch (Exception e) {
                Log.e(TAG, "\u5156\u5e95\u5173\u95ed\u52ff\u6270\u5931\u8d25", e);
            }

            // \u53d6\u6d88\u8d85\u65f6\u901a\u77e5
            AlarmHelper.cancelDndTimeoutNotification(context);

            // ★ 注销屏幕状态监听（勿扰已关闭）
            ScreenStateReceiver.unregister(context);

            // \u8c03\u5ea6\u660e\u5929\u7684\u52ff\u6270\u95f9\u949f
            try { MainActivity.setDndAlarms(context); } catch (Exception ignored) {}
        }
    }
}
