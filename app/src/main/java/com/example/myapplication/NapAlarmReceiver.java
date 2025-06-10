package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.app.KeyguardManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class NapAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "NapAlarmReceiver";
    public static final String ACTION_NAP_ALARM_START = "com.example.myapplication.NAP_ALARM_START";
    public static final String ACTION_NAP_ALARM_END = "com.example.myapplication.NAP_ALARM_END";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "午睡闹钟接收器触发，action: " + (action == null ? "null" : action));
        
        boolean isStart = ACTION_NAP_ALARM_START.equals(action);
        boolean isEnd = ACTION_NAP_ALARM_END.equals(action);
        
        if (!isStart && !isEnd) {
            // 处理没有action的情况，兼容旧版本
            isStart = intent.getBooleanExtra("START_NAP_DND", false);
            isEnd = !isStart;
            Log.d(TAG, "使用Extra判断操作类型: " + (isStart ? "开始午睡" : "结束午睡"));
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager 为空");
                return;
            }
            
            // 获取当前勿扰模式状态
            int currentInterruptionFilter = notificationManager.getCurrentInterruptionFilter();
            Log.d(TAG, "当前勿扰模式状态: " + getInterruptionFilterName(currentInterruptionFilter));
            
            if (isStart) {
                // 如果已经处于勿扰模式，不要重复设置
                if (currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL && 
                    currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    Log.d(TAG, "已处于勿扰模式，不重复设置");
                    return;
                }
                
                // 开启完全勿扰模式
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
                    
                    // 允许重复来电选项
                    allowedCalls |= NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
                    Log.d(TAG, "已启用重复来电功能");
                    
                    NotificationManager.Policy policy = new NotificationManager.Policy(
                        allowedCalls,
                        0, // PRIORITY_SENDERS_NONE替换为0
                        suppressedVisualEffects
                    );
                    
                    try {
                        notificationManager.setNotificationPolicy(policy);
                        Log.d(TAG, "成功设置勿扰策略");
                    } catch (Exception e) {
                        Log.e(TAG, "设置勿扰策略失败", e);
                    }
                    
                    // 设置勿扰模式为完全阻止
                    try {
                        // 先设置策略，再用延迟设置过滤器
                        new Thread(() -> {
                            try {
                                Thread.sleep(100); // 延迟100毫秒再设置过滤器
                                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                Log.d(TAG, "午睡勿扰模式已启用（完全）, 允许闹钟和重复来电");
                            } catch (Exception e) {
                                Log.e(TAG, "设置午睡勿扰模式过滤器失败", e);
                            }
                        }).start();
                    } catch (Exception e) {
                        Log.e(TAG, "启动延迟设置勿扰模式线程失败", e);
                    }
                    
                    // 注册锁屏状态变化监听器
                    registerScreenStateReceiver(context);
                    
                    String notificationTitle = "午睡勿扰已开启";
                    String notificationText = "系统完全勿扰模式已开启，允许重复来电和闹钟，解锁时不静音";
                    AlarmHelper.createDndSettingNotification(context, notificationTitle, notificationText);
                }
            } else if (isEnd) {
                Log.d(TAG, "准备关闭午睡勿扰模式");
                // 关闭勿扰模式
                try {
                    // 先检查当前是否处于勿扰模式
                    if (currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL ||
                        currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                        Log.d(TAG, "当前未处于勿扰模式，无需关闭");
                    } else {
                        // 关闭勿扰模式
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                        Log.d(TAG, "午睡勿扰模式已关闭");
                    }
                    
                    // 取消注册锁屏状态变化监听器
                    unregisterScreenStateReceiver(context);
                    
                    // 勿扰模式关闭时的通知和Toast
                    String notificationTitle = "午睡勿扰已结束";
                    String notificationText = "系统勿扰模式已关闭";
                    AlarmHelper.createDndSettingNotification(context, notificationTitle, notificationText);
                    Toast.makeText(context, notificationTitle, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "关闭午睡勿扰模式失败", e);
                }
            }
        }
    }
    
    /**
     * 获取勿扰模式状态名称
     */
    private String getInterruptionFilterName(int filter) {
        switch (filter) {
            case NotificationManager.INTERRUPTION_FILTER_ALL:
                return "全部允许(关闭勿扰)";
            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                return "仅允许优先通知";
            case NotificationManager.INTERRUPTION_FILTER_NONE:
                return "全部阻止";
            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                return "仅允许闹钟";
            case NotificationManager.INTERRUPTION_FILTER_UNKNOWN:
                return "未知状态";
            default:
                return "状态码:" + filter;
        }
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
} 