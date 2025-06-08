package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.NotificationManager;
import android.app.KeyguardManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * 监听屏幕状态变化的广播接收器
 * 用于在完全勿扰模式下，根据屏幕锁定/解锁状态动态调整勿扰规则
 */
public class ScreenStateReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenStateReceiver";
    private static BroadcastReceiver screenReceiver = null;
    private static boolean isRegistered = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("com.example.myapplication.ACTION_SCREEN_STATE_CHANGE".equals(action)) {
            // 注册屏幕状态变化监听器
            registerScreenStateReceiver(context);
            return;
        } else if ("com.example.myapplication.ACTION_SCREEN_STATE_UNREGISTER".equals(action)) {
            // 取消注册屏幕状态变化监听器
            unregisterScreenStateReceiver(context);
            return;
        }
        
        // 处理屏幕状态变化
        SharedPreferences prefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
        int dndMode = prefs.getInt("dnd_mode_type", 0); // 0=优先，1=完全勿扰
        
        // 只有在完全勿扰模式下才需要处理
        if (dndMode != 1) return;
        
        // 检查勿扰模式是否已启用
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_NONE || 
                notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_ALL) {
                // 勿扰模式未启用，不需处理
                return;
            }
            
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // 屏幕关闭，设置完全勿扰
                Log.d(TAG, "屏幕关闭，设置完全勿扰模式");
                updateDndPolicy(context, true);
            } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                // 屏幕打开或解锁，检查锁屏状态
                KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                boolean isLocked = keyguardManager.isKeyguardLocked();
                
                if (!isLocked || Intent.ACTION_USER_PRESENT.equals(action)) {
                    // 已解锁，设置为允许声音提醒
                    Log.d(TAG, "屏幕解锁，设置勿扰模式但允许声音");
                    updateDndPolicy(context, false);
                } else {
                    // 屏幕亮但仍锁定，维持完全勿扰
                    Log.d(TAG, "屏幕亮但仍锁定，维持完全勿扰");
                    updateDndPolicy(context, true);
                }
            }
        }
    }
    
    /**
     * 注册屏幕状态变化监听器
     */
    private void registerScreenStateReceiver(Context context) {
        if (isRegistered) return;
        
        Log.d(TAG, "注册屏幕状态变化监听器");
        
        screenReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        
        context.getApplicationContext().registerReceiver(screenReceiver, filter);
        isRegistered = true;
    }
    
    /**
     * 取消注册屏幕状态变化监听器
     */
    private void unregisterScreenStateReceiver(Context context) {
        if (!isRegistered || screenReceiver == null) return;
        
        Log.d(TAG, "取消注册屏幕状态变化监听器");
        
        try {
            context.getApplicationContext().unregisterReceiver(screenReceiver);
            isRegistered = false;
        } catch (Exception e) {
            Log.e(TAG, "取消注册屏幕监听器失败", e);
        }
    }
    
    /**
     * 更新勿扰策略
     * @param context 上下文
     * @param isLocked 是否锁屏
     */
    private void updateDndPolicy(Context context, boolean isLocked) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return;
            
            // 获取是否启用重复来电功能
            SharedPreferences prefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
            boolean allowRepeatedCalls = prefs.getBoolean("allow_repeated_calls", true);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int suppressedVisualEffects = isLocked ? 
                    NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT | 
                    NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK |
                    NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR |
                    NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE |
                    NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT |
                    NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS : 
                    NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
                
                // 设置允许的通知类型
                int allowedCategories = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
                
                // 如果启用了重复来电功能，添加重复来电权限
                if (allowRepeatedCalls) {
                    allowedCategories |= NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
                    Log.d(TAG, "在更新勿扰策略时启用重复来电");
                }
                
                NotificationManager.Policy policy = new NotificationManager.Policy(
                    allowedCategories,
                    0, // 替换 PRIORITY_SENDERS_NONE 为 0
                    suppressedVisualEffects
                );
                
                try {
                    // 先设置策略
                    notificationManager.setNotificationPolicy(policy);
                    Log.d(TAG, "更新勿扰策略完成，锁屏状态：" + (isLocked ? "锁屏" : "解锁") + 
                        (allowRepeatedCalls ? "，允许重复来电" : ""));
                } catch (Exception e) {
                    Log.e(TAG, "设置勿扰策略失败", e);
                }
            }
        }
    }
} 