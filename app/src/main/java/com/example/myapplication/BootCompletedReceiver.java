package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed received, checking if DND alarms should be set.");

            // 检查用户是否已经设置过勿扰模式
            SharedPreferences sp = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
            boolean dndSettingsExist = sp.contains("current_dnd_start_hour"); // 检查是否存在勿扰开启时间设置

            if (dndSettingsExist) {
                Log.d(TAG, "DND settings found, setting DND alarms.");
                // 设备启动完成，重新设置勿扰定时任务
                MainActivity.setDndAlarms(context);
            } else {
                Log.d(TAG, "No DND settings found, skipping setting DND alarms.");
            }
        }
    }
} 