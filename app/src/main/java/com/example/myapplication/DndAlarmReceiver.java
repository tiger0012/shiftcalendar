package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.app.KeyguardManager;

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
            // 获取勿扰模式类型
            SharedPreferences alarmSettingsPrefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
            int dndMode = alarmSettingsPrefs.getInt("dnd_mode_type", 0); // 0=优先，1=完全勿扰
            
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (dndMode == 0) {
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
                        
                        // 设置勿扰模式为优先通知
                        try {
                            // 先设置策略，再用延迟设置过滤器
                            new Thread(() -> {
                                try {
                                    Thread.sleep(100); // 延迟100毫秒再设置过滤器
                                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                    Log.d(TAG, "勿扰模式已启用（优先）");
                                } catch (Exception e) {
                                    Log.e(TAG, "设置优先勿扰模式过滤器失败", e);
                                    if (isHuaweiDevice()) {
                                        tryHuaweiDndMethod(context, true);
                                    }
                                }
                            }).start();
                        } catch (Exception e) {
                            Log.e(TAG, "启动延迟设置勿扰模式线程失败", e);
                        }
                    } else {
                        // 完全勿扰模式 - 只允许闹钟和重复来电，解锁状态判断
                        int suppressedVisualEffects = 0;
                        
                        // 检查设备是否解锁
                        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                        boolean isDeviceLocked = keyguardManager.isKeyguardLocked();
                        Log.d(TAG, "设备锁屏状态: " + (isDeviceLocked ? "锁屏" : "解锁"));
                        
                        // 添加SUPPRESSED_EFFECT_SCREEN_ON标志，使解锁时不静音
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            suppressedVisualEffects = isDeviceLocked ? 
                                NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT | 
                                NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT |
                                NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS : 
                                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
                        }
                        
                        // 设置允许的通知类型
                        int allowedCalls = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
                        
                        // 读取用户是否启用了重复来电选项
                        boolean allowRepeatedCalls = alarmSettingsPrefs.getBoolean("allow_repeated_calls", true);
                        if (allowRepeatedCalls) {
                            allowedCalls |= NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
                            Log.d(TAG, "已启用重复来电功能");
                        }
                        
                        NotificationManager.Policy policy = new NotificationManager.Policy(
                            allowedCalls,
                            0, // PRIORITY_SENDERS_NONE替换为0
                            suppressedVisualEffects
                        );
                        notificationManager.setNotificationPolicy(policy);
                        
                        // 设置勿扰模式为完全阻止
                        try {
                            // 先设置策略，再用延迟设置过滤器
                            new Thread(() -> {
                                try {
                                    Thread.sleep(100); // 延迟100毫秒再设置过滤器
                                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                    Log.d(TAG, "勿扰模式已启用（完全）, 允许闹钟" + (allowRepeatedCalls ? "和重复来电" : ""));
                                } catch (Exception e) {
                                    Log.e(TAG, "设置完全勿扰模式过滤器失败", e);
                                    if (isHuaweiDevice()) {
                                        tryHuaweiDndMethod(context, true);
                                    }
                                }
                            }).start();
                        } catch (Exception e) {
                            Log.e(TAG, "启动延迟设置勿扰模式线程失败", e);
                        }
                        
                        // 注册锁屏状态变化监听器
                        registerScreenStateReceiver(context);
                    }
                } else {
                // 设置勿扰模式为优先通知
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                Log.d(TAG, "勿扰模式已启用（优先）");
                }

                // 勿扰模式开启时的通知和Toast
                String modeText = dndMode == 0 ? "优先" : "完全";
                boolean allowRepeatedCalls = alarmSettingsPrefs.getBoolean("allow_repeated_calls", true);
                String repeatedCallsText = (dndMode == 1 && allowRepeatedCalls) ? "，允许重复来电" : "";
                String unlockText = (dndMode == 1) ? "，解锁时不静音" : "";
                String notificationTitle = "勿扰模式已开启";
                String notificationText = "系统" + modeText + "勿扰模式已根据您的设置开启" + unlockText + repeatedCallsText + "。";
                AlarmHelper.createDndSettingNotification(context, notificationTitle, notificationText);
                Toast.makeText(context, notificationTitle, Toast.LENGTH_SHORT).show();

            } else {
                // 关闭勿扰模式
                try {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                Log.d(TAG, "勿扰模式已关闭");
                } catch (Exception e) {
                    Log.e(TAG, "关闭勿扰模式失败", e);
                    if (isHuaweiDevice()) {
                        tryHuaweiDndMethod(context, false);
                    }
                }
                
                // 取消注册锁屏状态变化监听器
                unregisterScreenStateReceiver(context);

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
    
    /**
     * 注册锁屏状态变化监听器
     */
    private void registerScreenStateReceiver(Context context) {
        Intent screenStateIntent = new Intent(context, ScreenStateReceiver.class);
        screenStateIntent.setAction("com.example.myapplication.ACTION_SCREEN_STATE_CHANGE");
        context.sendBroadcast(screenStateIntent);
    }
    
    /**
     * 取消注册锁屏状态变化监听器
     */
    private void unregisterScreenStateReceiver(Context context) {
        Intent screenStateIntent = new Intent(context, ScreenStateReceiver.class);
        screenStateIntent.setAction("com.example.myapplication.ACTION_SCREEN_STATE_UNREGISTER");
        context.sendBroadcast(screenStateIntent);
    }

    // 添加一个方法，用于检查是否是华为设备
    private boolean isHuaweiDevice() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("huawei") || manufacturer.contains("honor");
    }
    
    // 添加一个用于尝试华为特定方法设置勿扰模式的方法
    private void tryHuaweiDndMethod(Context context, boolean enable) {
        try {
            Log.d(TAG, "尝试使用华为特定方法设置勿扰模式");
            // 华为手机可能需要使用广播或其他方式设置勿扰模式
            // 这里尝试使用华为特定的广播
            Intent intent = new Intent();
            intent.setAction("com.android.settings.action.SET_DO_NOT_DISTURB");
            intent.putExtra("do_not_disturb_enabled", enable);
            context.sendBroadcast(intent);
            Log.d(TAG, "已发送华为特定勿扰模式广播: " + enable);
        } catch (Exception e) {
            Log.e(TAG, "华为特定勿扰模式设置失败", e);
        }
    }
}