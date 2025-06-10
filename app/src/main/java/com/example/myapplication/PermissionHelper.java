package com.example.myapplication;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    private static final String TAG = "PermissionHelper";
    private static final int REQUEST_CODE_ALARM_PERMISSION = 1001;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    public static boolean checkAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void requestAlarmPermission(AppCompatActivity activity, PermissionCallback callback) {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(activity, "需要精确闹钟权限才能设置班次提醒", Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                activity.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "无法打开精确闹钟权限设置", e);
                    openAppSettings(activity);
                }
                callback.onPermissionDenied();
                return;
            }
        }
        callback.onPermissionGranted();
    }

    public static void requestNotificationPermission(AppCompatActivity activity, 
            ActivityResultLauncher<String> launcher, PermissionCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "需要通知权限才能发送班次提醒", Toast.LENGTH_LONG).show();
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        callback.onPermissionGranted();
    }

    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }
} 