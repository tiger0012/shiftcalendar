package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.media.RingtoneManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";
    private static final String ALARM_ACTION = "com.example.myapplication.SHIFT_ALARM";
    private static final int ALARM_REQUEST_CODE = 123;
    private static final String CHANNEL_ID = "shift_alarm_channel";
    private static final String CHANNEL_NAME = "班次提醒";
    private static final int NOTIFICATION_ID = 1;
    private static final String MIUI_ALARM_AUTHORITY = "com.android.deskclock.MyAiActionProvider";
    private static final String MIUI_ALARM_TYPE = "urn:aiot-spec-v3:com.mi.phones:action:[com.android.deskclock/deskclock/alarm]:0:2.0";

    public static void setShiftAlarm(Context context, String team, boolean isDayShift, List<DayShiftGroup> currentWeekData, int hour, int minute) {
        // 检查是否有设置闹钟的权限
        if (!PermissionHelper.checkAlarmPermission(context)) {
            Log.e(TAG, "没有设置闹钟的权限");
            Toast.makeText(context, "需要闹钟权限才能设置班次提醒", Toast.LENGTH_LONG).show();
            return;
        }

        // 检查通知权限
        if (!PermissionHelper.checkNotificationPermission(context)) {
            Log.e(TAG, "没有发送通知的权限");
            Toast.makeText(context, "需要通知权限才能发送班次提醒", Toast.LENGTH_LONG).show();
            if (context instanceof AppCompatActivity) {
                PermissionHelper.requestNotificationPermission((AppCompatActivity) context, 
                    ((MainActivity) context).getNotificationPermissionLauncher(),
                    new PermissionHelper.PermissionCallback() {
                        @Override
                        public void onPermissionGranted() {
                            // 权限获取后继续设置闹钟
                            continueSetAlarm(context, team, isDayShift, currentWeekData, hour, minute);
                        }

                        @Override
                        public void onPermissionDenied() {
                            Log.e(TAG, "通知权限被拒绝");
                        }
                    });
            }
            return;
        }

        continueSetAlarm(context, team, isDayShift, currentWeekData, hour, minute);
    }

    private static void continueSetAlarm(Context context, String team, boolean isDayShift, List<DayShiftGroup> currentWeekData, int hour, int minute) {
        try {
            // 创建Intent
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            
            // 设置闹钟时间
            intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);

            if (isDayShift) {
                intent.putExtra(AlarmClock.EXTRA_MESSAGE, "白班-" + team);
                Log.d(TAG, "设置白班闹钟，时间：" + hour + ":" + minute);
            } else {
                intent.putExtra(AlarmClock.EXTRA_MESSAGE, "夜班-" + team);
                Log.d(TAG, "设置夜班闹钟，时间：" + hour + ":" + minute);
            }
            
            // 计算需要重复的日期
            ArrayList<Integer> daysToRepeat = new ArrayList<>();
            if (currentWeekData != null) {
                for (DayShiftGroup dayData : currentWeekData) {
                    if (dayData.date != null) {
                        try {
                            LocalDate date = LocalDate.parse(dayData.date);
                            DayOfWeek dayOfWeek = date.getDayOfWeek();
                            boolean hasShift = false;
                            if (isDayShift && dayData.dayTeams.contains(team)) {
                                hasShift = true;
                                Log.d(TAG, "添加白班日期：" + date + " (" + dayOfWeek + ")");
                            } else if (!isDayShift && dayData.nightTeams.contains(team)) {
                                hasShift = true;
                                Log.d(TAG, "添加夜班日期：" + date + " (" + dayOfWeek + ")");
                            }

                            if (hasShift) {
                                // 将DayOfWeek转换为Calendar常量
                                int calendarDay;
                                switch (dayOfWeek) {
                                    case SUNDAY:
                                        calendarDay = Calendar.SUNDAY;
                                        break;
                                    case MONDAY:
                                        calendarDay = Calendar.MONDAY;
                                        break;
                                    case TUESDAY:
                                        calendarDay = Calendar.TUESDAY;
                                        break;
                                    case WEDNESDAY:
                                        calendarDay = Calendar.WEDNESDAY;
                                        break;
                                    case THURSDAY:
                                        calendarDay = Calendar.THURSDAY;
                                        break;
                                    case FRIDAY:
                                        calendarDay = Calendar.FRIDAY;
                                        break;
                                    case SATURDAY:
                                        calendarDay = Calendar.SATURDAY;
                                        break;
                                    default:
                                        continue;
                                }
                                daysToRepeat.add(calendarDay);
                                Log.d(TAG, "添加重复日期：" + calendarDay);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析日期或计算重复日期失败", e);
                        }
                    }
                }
            }

            // 设置重复日期
            if (!daysToRepeat.isEmpty()) {
                intent.putExtra(AlarmClock.EXTRA_DAYS, daysToRepeat);
                Log.d(TAG, "设置重复日期：" + daysToRepeat);
            }

            // 设置其他参数
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
            intent.putExtra(AlarmClock.EXTRA_VIBRATE, true);
            intent.putExtra(AlarmClock.EXTRA_RINGTONE, "content://settings/system/alarm_alert");

            // 启动系统闹钟设置界面
            context.startActivity(intent);
            Log.d(TAG, "已打开系统闹钟界面，请手动设置闹钟");

            // 显示设置成功的通知
            createNotification(context, team, isDayShift, hour, minute, daysToRepeat);

        } catch (Exception e) {
            Log.e(TAG, "设置闹钟失败", e);
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "班次提醒",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("班次提醒通知");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 1000, 1000});

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void cancelShiftAlarm(Context context) {
        // 尝试使用小米闹钟API取消闹钟
        try {
            Intent intent = new Intent();
            intent.setAction(MIUI_ALARM_TYPE);
            intent.setPackage("com.android.deskclock");
            intent.putExtra("alarmAction", 2); // 删除闹钟
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            PackageManager packageManager = context.getPackageManager();
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent);
                Log.d(TAG, "已使用小米闹钟API取消闹钟");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "使用小米闹钟API取消闹钟失败", e);
        }

        // 取消自定义闹钟 (AlarmManager)
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "取消闹钟：无法获取AlarmManager服务");
            // 在取消时，如果AlarmManager不可用，不尝试打开系统闹钟界面
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ALARM_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            alarmManager.cancel(pendingIntent);
             Log.d(TAG, "已取消自定义闹钟");
        } catch (SecurityException e) {
            Log.e(TAG, "取消自定义闹钟失败，可能是权限问题或PendingIntent不匹配", e);
        } finally {
            // 取消通知，无论是否成功取消闹钟
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
                 Log.d(TAG, "已取消通知");
            }
        }
    }

    private static String formatRepeatDays(List<Integer> repeatDays) {
        StringBuilder sb = new StringBuilder();
        String[] dayNames = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"}; // Calendar常量1-7对应周日-周六
        for (int day : repeatDays) {
            if (day >= Calendar.SUNDAY && day <= Calendar.SATURDAY) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(dayNames[day]);
            }
        }
        return sb.toString();
    }

    private static void createNotification(Context context, String teamName, boolean isDayShift, int hour, int minute, List<Integer> repeatDays) {
        String shiftType = isDayShift ? "白班" : "夜班";
        String timeStr = String.format("%02d:%02d", hour, minute);
        String daysStr = formatRepeatDays(repeatDays);
        
        // 创建通知渠道
        createNotificationChannel(context);

        // 创建点击通知时返回主界面的Intent
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("请手动确认")
            .setContentText(String.format("%s-%s 闹钟已设置\n时间：%s\n重复：%s", 
                shiftType, teamName, timeStr, daysStr))
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(String.format("%s-%s 闹钟已设置\n时间：%s\n重复：%s", 
                    shiftType, teamName, timeStr, daysStr)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // 显示通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            
            // 添加桌面弹出提示
            String toastMessage = String.format("请手动确认\n%s-%s 闹钟已设置\n时间：%s\n重复：%s", 
                shiftType, teamName, timeStr, daysStr);
            Toast toast = Toast.makeText(context, toastMessage, Toast.LENGTH_LONG);
            // 设置Toast显示位置在屏幕中间
            toast.setGravity(android.view.Gravity.CENTER, 0, 0);
            toast.show();
        } catch (SecurityException e) {
            Log.e(TAG, "显示通知失败: " + e.getMessage());
        }

        // 如果是Activity，则关闭当前Activity
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }
    }

    // 清除指定组的所有闹钟
    public static void clearTeamAlarms(Context context, String teamName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // 清除白班闹钟
        Intent dayIntent = new Intent(context, AlarmReceiver.class);
        dayIntent.setAction("com.example.myapplication.ALARM_DAY_" + teamName);
        PendingIntent dayPendingIntent = PendingIntent.getBroadcast(
            context,
            teamName.hashCode(),
            dayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(dayPendingIntent);

        // 清除夜班闹钟
        Intent nightIntent = new Intent(context, AlarmReceiver.class);
        nightIntent.setAction("com.example.myapplication.ALARM_NIGHT_" + teamName);
        PendingIntent nightPendingIntent = PendingIntent.getBroadcast(
            context,
            teamName.hashCode() + 1,
            nightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(nightPendingIntent);

        Log.d(TAG, "已清除" + teamName + "组的所有闹钟");
    }
} 