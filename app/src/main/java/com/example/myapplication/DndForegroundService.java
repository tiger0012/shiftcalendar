package com.example.myapplication;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

/**
 * 勿扰模式「按需」服务 —— 只在需要时短暂运行！
 *
 * 开启勿扰 → 启动前台服务 → 设一个精确的 AlarmManager 闹钟到【结束时间】→ 进程可安全杀掉
 * 结束时间到 → AlarmManager 拉起本 Service → 关闭勿扰 → stopSelf → 进程消失
 *
 * 不常驻、不轮询、不耗电。
 */
public class DndForegroundService extends Service {
    private static final String TAG = "DndForegroundService";
    /** AlarmManager 触发关闭勿扰的 action */
    public static final String ACTION_DND_OFF = "com.example.myapplication.ACTION_DND_OFF";
    private static final String CHANNEL_ID = "dnd_foreground_channel";
    private static final int NOTIFICATION_ID = 9999;
    /** ForegroundService 闸钟的 request code */
    private static final int ALARM_REQUEST_CODE = 5000;
    /** 广播闸钟（兜底）的 request code，和 ALARM_REQUEST_CODE 区分 */
    private static final int BROADCAST_ALARM_REQUEST_CODE = 5001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "Service onCreate");
    }

    /**
     * 唯一入口：处理两种情况
     *   1. action=null  → 勿扰刚开启，设结束闸钟后保持存活
     *   2. ACTION_DND_OFF → 结束时间到了，关闭勿扰并退出
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "onStartCommand action=" + action);

        // ── 结束时间到了：关勿扰、停服务 ──
        if (ACTION_DND_OFF.equals(action)) {
            Log.d(TAG, "\u23f0 \u7ed3\u675f\u65f6\u95f4\u5230\u4e86\uff01AlarmManager \u89e6\u53d1\u81ea\u52a8\u5173\u95ed\u52ff\u6270");
            disableDndAndStop();
            return START_NOT_STICKY;
        }

        // ── action=null\uff1a\u521a\u542f\u52a8\uff0c\u8bbe\u95f9\u949f\u4fdd\u6d3b ──
        Notification notification = buildRunningNotification();
        startForeground(NOTIFICATION_ID, notification);

        checkAndCloseIfNeeded();
        scheduleCheckAlarm();

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        cancelCheckAlarm();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved: \u6700\u8fd1\u4efb\u52a1\u88ab\u79fb\u9664\uff0c\u5c1d\u91cd\u542f\u524d\u53f0\u670d\u52a1");

        Intent restartIntent = new Intent(this, DndForegroundService.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startForegroundService(restartIntent);
    }

    // ============================== \u6838\u5fc3\u52a8\u4f5c ==============================

    private void checkAndCloseIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        int endHour = prefs.getInt("current_dnd_end_hour", -1);
        int endMinute = prefs.getInt("current_dnd_end_minute", 0);

        if (endHour < 0) {
            Log.w(TAG, "\u672a\u627e\u5230\u52ff\u6270\u7ed3\u675f\u8bbe\u7f6e");
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        endTime.set(Calendar.HOUR_OF_DAY, endHour);
        endTime.set(Calendar.MINUTE, endMinute);
        endTime.set(Calendar.SECOND, 0);
        endTime.set(Calendar.MILLISECOND, 0);

        long diffMs = now.getTimeInMillis() - endTime.getTimeInMillis();
        boolean shouldClose = diffMs >= 0 && diffMs <= 60_000L;

        Log.d(TAG, "\u7acb\u5373\u68c0\u67e5: \u5f53\u524d="
                + now.get(Calendar.HOUR_OF_DAY) + ":"
                + String.format("%02d", now.get(Calendar.MINUTE))
                + " \u533a\u95f4=?:?-" + endHour + ":" + String.format("%02d", endMinute)
                + " \u5e94\u5728\u52ff\u6270=" + !shouldClose);

        if (shouldClose) {
            Log.d(TAG, "\u7acb\u5373\u68c0\u67e5\u53d1\u73b0\u5e94\u5173\u95ed\u52ff\u6270\uff0c\u9a6c\u4e0a\u6267\u88c2\u5173\u95ed\uff01");
            disableDndAndStop();
        }
    }

    /**
     * \u8bbe\u7f6e\u4e00\u4e2a\u7cbe\u786e\u7684 AlarmManager \u95f9\u949f\uff0c\u5730\u3010\u7ed3\u675f\u65f6\u95f4\u3011\u89e6\u53d1 ACTION_DND_OFF\u3002
     */
    private void scheduleCheckAlarm() {
        SharedPreferences prefs = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        int endHour = prefs.getInt("current_dnd_end_hour", -1);
        int endMinute = prefs.getInt("current_dnd_end_minute", 0);

        if (endHour < 0) {
            Log.w(TAG, "\u65e0\u6cd5\u8bbe\u68c0\u67e5\u95f9\u949f\uff1a\u672a\u627e\u5230\u7ed3\u675f\u65f6\u95f4");
            return;
        }

        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, endHour);
        target.set(Calendar.MINUTE, endMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        if (target.before(now) || target.equals(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(this, DndForegroundService.class);
        intent.setAction(ACTION_DND_OFF);
        PendingIntent pi = PendingIntent.getForegroundService(
                this, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
        }

        Calendar logCal = Calendar.getInstance();
        logCal.setTimeInMillis(target.getTimeInMillis());
        Log.d(TAG, "\ud83d\udcc5 \u5df2\u8bbe\u7f6e\u7cbe\u786e\u5173\u95ed\u95f9\u949f: "
                + (logCal.get(Calendar.MONTH) + 1) + "/"
                + logCal.get(Calendar.DAY_OF_MONTH) + " "
                + endHour + ":" + String.format("%02d", endMinute));
    }

    private void cancelCheckAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(this, DndForegroundService.class);
        intent.setAction(ACTION_DND_OFF);
        PendingIntent pi = PendingIntent.getForegroundService(
                this, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(pi);
        pi.cancel();
    }

    /**
     * \u5173\u95ed\u52ff\u6270\u6a21\u5f0f + \u6e05\u7406\u8d44\u6e90 + \u505c\u6b62\u670d\u52a1\u3002
     */
    private void disableDndAndStop() {
        try {
            Notification notification = buildRunningNotification();
            startForeground(NOTIFICATION_ID, notification);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                Log.d(TAG, "\u2705 \u5df2\u901a\u8fc7\u524d\u53f0\u670d\u52a1\u5173\u95ed\u52ff\u6270\u6a21\u5f0f");
            }
        } catch (Exception e) {
            Log.e(TAG, "\u5173\u95ed\u52ff\u6270\u5931\u8d25", e);
        } finally {
            cancelCheckAlarm();
            cancelBroadcastEndAlarm(this);  // \u53d6\u6d88\u5e7f\u64ad\u5156\u949f

            try { MainActivity.setDndAlarms(this); } catch (Exception ignored) {}
            AlarmHelper.cancelDndTimeoutNotification(this);

            stopForeground(true);
            stopSelf();
            Log.d(TAG, "stopService called");
        }
    }

    // ============================== \u5916\u90e8\u9759\u6001 API ==============================

    /**
     * \u542f\u52a8\u524d\u53f0\u670d\u52a1\u4fdd\u6d3b\u3002
     */
    public static void start(Context context, String title, String content, boolean isNapMode) {
        Intent intent = new Intent(context, DndForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        Log.d(TAG, "startForegroundService called");
    }

    /**
     * \u8bbe\u7f6e\u5e38\u89c4\u52ff\u6270\u7ed3\u675f\u95f9\u949f - \u53cc\u901a\u9053\u3002
     * \u901a\u9051\u2460 ForegroundService (\u8fdb\u7a0b\u5b58\u6d3b\u65f6)
     * \u901a\u9051\u2461 Broadcast \u5156\u5e95 (ColorOS\u62e6\u622a Service\u65f6)
     */
    public static void scheduleEndAlarm(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
        int endHour = prefs.getInt("current_dnd_end_hour", -1);
        int endMinute = prefs.getInt("current_dnd_end_minute", 0);

        if (endHour < 0) {
            Log.w(TAG, "scheduleEndAlarm: \u672a\u627e\u5230\u5e38\u89c4\u52ff\u6270\u7ed3\u675f\u65f6\u95f4");
            return;
        }

        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, endHour);
        target.set(Calendar.MINUTE, endMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        if (target.before(now) || target.equals(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        long targetMillis = target.getTimeInMillis();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // \u2500\u2500 \u901a\u9051\u2460\uff1aForegroundService \u8def\u5f84 \u2500\u2500
        Intent fsIntent = new Intent(context, DndForegroundService.class);
        fsIntent.setAction(ACTION_DND_OFF);
        PendingIntent fsPi = PendingIntent.getForegroundService(
                context, ALARM_REQUEST_CODE, fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // \u2500\u2500 \u901a\u9051\u2461\uff1aBroadcast \u5156\u5e95 \u2500\u2500
        Intent bcIntent = new Intent(context, DndAlarmReceiver.class);
        bcIntent.setAction(DndAlarmReceiver.ACTION_DND_ALARM_END);
        PendingIntent bcPi = PendingIntent.getBroadcast(
                context, BROADCAST_ALARM_REQUEST_CODE, bcIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, fsPi);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, bcPi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, targetMillis, fsPi);
            am.setExact(AlarmManager.RTC_WAKEUP, targetMillis, bcPi);
        }

        Calendar logCal = Calendar.getInstance();
        logCal.setTimeInMillis(targetMillis);
        Log.d(TAG, "\ud83d\udcc5 [\u5916\u90e8] \u5df2\u8bbe\u5b9a\u7ed3\u675f\u95f9\u949f \u2192 "
                + (logCal.get(Calendar.MONTH) + 1) + "/"
                + logCal.get(Calendar.DAY_OF_MONTH) + " "
                + endHour + ":" + String.format("%02d", endMinute)
                + " [\u53cc\u901a\u9051:Service+\u5e7f\u64ad]");
    }

    /**
     * \u53d6\u6d88\u4e4b\u524d\u8bbe\u7f6e\u7684\u7ed3\u675f\u95f9\u949f\uff08\u53cc\u901a\u9051\u90fd\u53d6\u6d88\uff09\u3002
     */
    public static void cancelEndAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // \u53d6\u6d88 ForegroundService \u95f9\u949f
        Intent fsIntent = new Intent(context, DndForegroundService.class);
        fsIntent.setAction(ACTION_DND_OFF);
        PendingIntent fsPi = PendingIntent.getForegroundService(
                context, ALARM_REQUEST_CODE, fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(fsPi);
        fsPi.cancel();

        // \u53d6\u6d88 Broadcast \u5156\u5e95\u95f9\u949f
        cancelBroadcastEndAlarmInternal(am, context);

        Log.d(TAG, "\u5df2\u53d6\u6d88\u7ed3\u675f\u95f9\u949f(\u53cc\u901a\u9051)");
    }

    /** \u53d6\u6d88\u5e7f\u64ad\u5156\u5e95\u95f9\u949f\uff08\u5355\u72ec\u65b9\u6cd5\uff0c\u4f9b disableDndAndStop \u8c03\u7528\uff09 */
    private static void cancelBroadcastEndAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) cancelBroadcastEndAlarmInternal(am, context);
    }

    private static void cancelBroadcastEndAlarmInternal(AlarmManager am, Context context) {
        Intent bcIntent = new Intent(context, DndAlarmReceiver.class);
        bcIntent.setAction(DndAlarmReceiver.ACTION_DND_ALARM_END);
        PendingIntent bcPi = PendingIntent.getBroadcast(
                context, BROADCAST_ALARM_REQUEST_CODE, bcIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(bcPi);
        bcPi.cancel();
    }

    // ============================== \u901a\u77e5\u6784\u5efa ==============================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "\u52ff\u6270\u6a21\u5f0f", NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("\u52ff\u6270\u6a21\u5f0f\u81ea\u52a8\u7ba1\u7406\u901a\u77e5");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildRunningNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("\ud83c\udf19 \u52ff\u6270\u6a21\u5f0f\u8fd0\u884c\u4e2d")
                .setContentText("\u5c06\u5728\u8bbe\u5b9a\u65f6\u95f4\u81ea\u52a8\u5173\u95ed")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("\u52ff\u6270\u6a21\u5f0f\u8fd0\u884c\u4e2d\n\u5c06\u5728\u8bbe\u5b9a\u65f6\u95f4\u81ea\u52a8\u5173\u95ed\n\n\u26a0\ufe0f \u8bf7\u52ff\u4ece\u6700\u8fd1\u4efb\u52a1\u5212\u6389\u6b64\u5e94\u7528"))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
