package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AlarmSettingsActivity";

    private TimePicker timePickerDayShift;
    private TimePicker timePickerNightShift;
    private TimePicker timePickerDndStart;
    private TimePicker timePickerDndEnd;
    private TimePicker timePickerDndNightStart;
    private TimePicker timePickerDndNightEnd;
    private Button btnSetCustomAlarm;
    private Button btnSetDnd;

    private String currentSelectedTeam;
    private List<DayShiftGroup> shiftDataInRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        // 获取从MainActivity传递过来的数据
        if (getIntent().getExtras() != null) {
            currentSelectedTeam = getIntent().getStringExtra("selectedTeam");
            shiftDataInRange = (List<DayShiftGroup>) getIntent().getSerializableExtra("shiftData");
        }

        if (currentSelectedTeam == null || shiftDataInRange == null) {
            Toast.makeText(this, "无法获取班次数据，请返回主界面重试", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 初始化控件
        timePickerDayShift = findViewById(R.id.time_picker_day_shift);
        timePickerNightShift = findViewById(R.id.time_picker_night_shift);
        timePickerDndStart = findViewById(R.id.time_picker_dnd_start);
        timePickerDndEnd = findViewById(R.id.time_picker_dnd_end);
        timePickerDndNightStart = findViewById(R.id.time_picker_dnd_night_start);
        timePickerDndNightEnd = findViewById(R.id.time_picker_dnd_night_end);
        btnSetCustomAlarm = findViewById(R.id.btn_set_custom_alarm);
        btnSetDnd = findViewById(R.id.btn_set_dnd);

        // 确保使用24小时制
        timePickerDayShift.setIs24HourView(true);
        timePickerNightShift.setIs24HourView(true);
        timePickerDndStart.setIs24HourView(true);
        timePickerDndEnd.setIs24HourView(true);
        timePickerDndNightStart.setIs24HourView(true);
        timePickerDndNightEnd.setIs24HourView(true);

        // 从 SharedPreferences 加载保存的时间
        Log.d(TAG, "尝试从 SharedPreferences 加载时间");
        SharedPreferences sp = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        int dayShiftHour = sp.getInt("day_shift_hour", 6);
        int dayShiftMinute = sp.getInt("day_shift_minute", 0);
        int nightShiftHour = sp.getInt("night_shift_hour", 18);
        int nightShiftMinute = sp.getInt("night_shift_minute", 0);
        int dndStartHour = sp.getInt("dnd_start_hour", 22);
        int dndStartMinute = sp.getInt("dnd_start_minute", 0);
        int dndEndHour = sp.getInt("dnd_end_hour", 7);
        int dndEndMinute = sp.getInt("dnd_end_minute", 0);
        int dndNightStartHour = sp.getInt("dnd_night_start_hour", 22);
        int dndNightStartMinute = sp.getInt("dnd_night_start_minute", 0);
        int dndNightEndHour = sp.getInt("dnd_night_end_hour", 7);
        int dndNightEndMinute = sp.getInt("dnd_night_end_minute", 0);

        Log.d(TAG, "加载的时间：白班 " + dayShiftHour + ":" + dayShiftMinute + ", 夜班 " + nightShiftHour + ":" + nightShiftMinute + ", 勿扰开启 " + dndStartHour + ":" + dndStartMinute + ", 勿扰关闭 " + dndEndHour + ":" + dndEndMinute + ", 夜班勿扰开启 " + dndNightStartHour + ":" + dndNightStartMinute + ", 夜班勿扰关闭 " + dndNightEndHour + ":" + dndNightEndMinute);

        timePickerDayShift.setHour(dayShiftHour);
        timePickerDayShift.setMinute(dayShiftMinute);
        timePickerNightShift.setHour(nightShiftHour);
        timePickerNightShift.setMinute(nightShiftMinute);
        timePickerDndStart.setHour(dndStartHour);
        timePickerDndStart.setMinute(dndStartMinute);
        timePickerDndEnd.setHour(dndEndHour);
        timePickerDndEnd.setMinute(dndEndMinute);
        timePickerDndNightStart.setHour(dndNightStartHour);
        timePickerDndNightStart.setMinute(dndNightStartMinute);
        timePickerDndNightEnd.setHour(dndNightEndHour);
        timePickerDndNightEnd.setMinute(dndNightEndMinute);

        // 为设置自定义闹钟按钮添加点击事件监听器
        btnSetCustomAlarm.setOnClickListener(v -> {
            setCustomAlarm();
        });

        // 为设置勿扰模式按钮添加点击事件监听器
        btnSetDnd.setOnClickListener(v -> {
            setDoNotDisturb();
        });

        // 检查并请求闹钟和通知权限
        checkAndRequestPermissions();
    }

    private void setCustomAlarm() {
        // 保存所有时间选择器的时间
        saveAllTimes();

        // 获取用户选择的白班和夜班时间
        int dayShiftHour = timePickerDayShift.getHour();
        int dayShiftMinute = timePickerDayShift.getMinute();
        int nightShiftHour = timePickerNightShift.getHour();
        int nightShiftMinute = timePickerNightShift.getMinute();

        // 检查白班时间是否在允许范围内 (4:00 - 8:00)
        if (dayShiftHour < 4 || dayShiftHour > 8) {
            Toast.makeText(this, "白班闹钟时间必须在 4:00 到 8:00 之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查夜班时间是否在允许范围内 (16:00 - 20:00)
        if (nightShiftHour < 16 || nightShiftHour > 20) {
             Toast.makeText(this, "夜班闹钟时间必须在 16:00 到 20:00 之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 查找第一个有班次的日期和班次类型
        DayShiftGroup firstShiftGroup = null;
        boolean isFirstShiftDay = false;

        for (DayShiftGroup dayData : shiftDataInRange) {
            if (dayData.dayTeams.contains(currentSelectedTeam)) {
                firstShiftGroup = dayData;
                isFirstShiftDay = true;
                break; // 找到第一个白班
            } else if (dayData.nightTeams.contains(currentSelectedTeam)) {
                firstShiftGroup = dayData;
                isFirstShiftDay = false;
                break; // 找到第一个夜班
            }
        }

        // 如果在范围内没有找到任何班次，则不设置闹钟
        if (firstShiftGroup == null) {
            Toast.makeText(this, "从今天到下一周今天没有班次", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "从今天到下一周今天没有班次，不设置闹钟");
            return;
        }

        // 根据第一个班次类型筛选出所有对应班次日期的列表
        List<DayShiftGroup> filteredShiftData = new ArrayList<>();
        for (DayShiftGroup dayData : shiftDataInRange) {
            if (isFirstShiftDay) {
                // 如果第一个班次是白班，只收集白班日期
                if (dayData.dayTeams.contains(currentSelectedTeam)) {
                    filteredShiftData.add(dayData);
                    Log.d(TAG, "根据第一个白班添加白班日期到筛选列表：" + dayData.date);
                }
            } else {
                // 如果第一个班次是夜班，只收集夜班日期
                if (dayData.nightTeams.contains(currentSelectedTeam)) {
                    filteredShiftData.add(dayData);
                    Log.d(TAG, "根据第一个夜班添加夜班日期到筛选列表：" + dayData.date);
                }
            }
        }

        // 设置闹钟
        if (!filteredShiftData.isEmpty()) {
            int alarmHour = isFirstShiftDay ? dayShiftHour : nightShiftHour;
            int alarmMinute = isFirstShiftDay ? dayShiftMinute : nightShiftMinute;

            if (isFirstShiftDay) {
                Log.d(TAG, "设置白班闹钟，班次日期：" + filteredShiftData.size() + "天");
            } else {
                Log.d(TAG, "设置夜班闹钟，班次日期：" + filteredShiftData.size() + "天");
            }
            for (DayShiftGroup day : filteredShiftData) {
                 Log.d(TAG, "闹钟日期：" + day.date);
            }
            AlarmHelper.setShiftAlarm(this, currentSelectedTeam, isFirstShiftDay, filteredShiftData, alarmHour, alarmMinute);
        } else {
             Log.d(TAG, "在指定范围内没有找到对应班次类型的日期，不设置闹钟");
             Toast.makeText(this, "在指定范围内没有找到对应班次类型的日期", Toast.LENGTH_SHORT).show();
        }
    }

    private void setDoNotDisturb() {
        // 保存所有时间选择器的时间
        saveAllTimes();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                // 权限未授予，引导用户去设置页面授权
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "请在设置中授予勿扰模式权限", Toast.LENGTH_LONG).show();
                return;
            }

            // 判断当前选择班组当天是白班还是夜班
            boolean isDayShiftToday = false;
            boolean hasShiftToday = false;

            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String todayStr = today.format(formatter);

            // 从传递过来的 shiftDataInRange 中查找今天的班次信息
            DayShiftGroup todayShift = null;
            if (shiftDataInRange != null) {
                 for (DayShiftGroup group : shiftDataInRange) {
                    if (group.date != null && group.date.equals(todayStr)) {
                        todayShift = group;
                        break;
                    }
                }
            }

            int dndStartHour, dndStartMinute, dndEndHour, dndEndMinute;

            // 根据班次类型或无班次时使用白班勿扰区间
            int currentShiftType = 0; // 0: 无班次, 1: 白班, 2: 夜班

            if (todayShift != null) {
                 if (todayShift.dayTeams.contains(currentSelectedTeam)) {
                    isDayShiftToday = true;
                    hasShiftToday = true;
                    currentShiftType = 1; // 白班
                 } else if (todayShift.nightTeams.contains(currentSelectedTeam)) {
                     isDayShiftToday = false;
                     hasShiftToday = true;
                     currentShiftType = 2; // 夜班
                 } else {
                     // 当天没有该班组的班次
                     hasShiftToday = false;
                     currentShiftType = 0; // 无班次
                 }
            } else {
                // 没有找到今天的班次数据
                 hasShiftToday = false;
                 currentShiftType = 0; // 无班次
            }

            if (currentShiftType == 1 || currentShiftType == 0) { // 白班或无班次时使用白班勿扰区间
                dndStartHour = timePickerDndStart.getHour();
                dndStartMinute = timePickerDndStart.getMinute();
                dndEndHour = timePickerDndEnd.getHour();
                dndEndMinute = timePickerDndEnd.getMinute();
                 Log.d(TAG, "使用白班勿扰区间: " + dndStartHour + ":" + dndStartMinute + " - " + dndEndHour + ":" + dndEndMinute);
            } else { // 夜班时使用夜班勿扰区间
                dndStartHour = timePickerDndNightStart.getHour();
                dndStartMinute = timePickerDndNightStart.getMinute();
                dndEndHour = timePickerDndNightEnd.getHour();
                dndEndMinute = timePickerDndNightEnd.getMinute();
                 Log.d(TAG, "使用夜班勿扰区间: " + dndStartHour + ":" + dndStartMinute + " - " + dndEndHour + ":" + dndEndMinute);
            }

            // 保存选择的勿扰时间范围以及设置时的班次类型
            SharedPreferences sp = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
             sp.edit()
                .putInt("current_dnd_start_hour", dndStartHour)
                .putInt("current_dnd_start_minute", dndStartMinute)
                .putInt("current_dnd_end_hour", dndEndHour)
                .putInt("current_dnd_end_minute", dndEndMinute)
                .putInt("current_dnd_shift_type", currentShiftType) // 保存设置时的班次类型
                .apply();
            Log.d(TAG, "已保存当前生效的勿扰时间范围和班次信息");

            // 设置勿扰模式
            try {
                // 构建提示信息
                String shiftTypeString;
                if (currentShiftType == 1) {
                    shiftTypeString = "白班";
                } else if (currentShiftType == 2) {
                    shiftTypeString = "夜班";
                } else {
                    shiftTypeString = "无班次";
                }

                String toastMessage = String.format("已设置%s勿扰模式\n时间范围：%02d:%02d - %02d:%02d", 
                    shiftTypeString, dndStartHour, dndStartMinute, dndEndHour, dndEndMinute);

                String notificationTitle = String.format("%s勿扰区间已设置", shiftTypeString);

                String notificationText = String.format("勿扰时间: %02d:%02d - %02d:%02d", 
                    dndStartHour, dndStartMinute, dndEndHour, dndEndMinute);

                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();

                // 调用 AlarmHelper 显示勿扰设置成功的通知
                AlarmHelper.createDndSettingNotification(this, notificationTitle, notificationText);

                // 设置一个标志，表示用户已经设置过勿扰模式
                SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                appPrefs.edit().putBoolean("dnd_settings_configured", true).apply();
                Log.d(TAG, "已设置 dnd_settings_configured 标志");

                // 将设置定时任务的调用移到标志设置之后
                MainActivity.setDndAlarms(this);

                Log.d(TAG, "已调用 MainActivity.setDndAlarms");

                // --- 新增逻辑：根据当前时间判断是否应立即进入勿扰模式并设置状态 ---
                Calendar now = Calendar.getInstance();
                Calendar dndStart = Calendar.getInstance();
                dndStart.set(Calendar.HOUR_OF_DAY, dndStartHour);
                dndStart.set(Calendar.MINUTE, dndStartMinute);
                dndStart.set(Calendar.SECOND, 0);
                dndStart.set(Calendar.MILLISECOND, 0);

                Calendar dndEnd = Calendar.getInstance();
                dndEnd.set(Calendar.HOUR_OF_DAY, dndEndHour);
                dndEnd.set(Calendar.MINUTE, dndEndMinute);
                dndEnd.set(Calendar.SECOND, 0);
                dndEnd.set(Calendar.MILLISECOND, 0);

                boolean shouldBeInDndNow = false;

                if (dndStartHour < dndEndHour) { // 勿扰区间不跨天
                    if (now.after(dndStart) && now.before(dndEnd)) {
                        shouldBeInDndNow = true;
                    }
                } else { // 勿扰区间跨天 (例如 22:00 - 7:00)
                     if (now.after(dndStart) || now.before(dndEnd)) {
                         shouldBeInDndNow = true;
                     }
                }

                NotificationManager notificationManagerInstant = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManagerInstant != null) {
                    if (shouldBeInDndNow) {
                         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                             NotificationManager.Policy policy = new NotificationManager.Policy(
                                 NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS |
                                 NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS |
                                 NotificationManager.Policy.PRIORITY_CATEGORY_CALLS |
                                 NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                                 NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                                 0
                             );
                             notificationManagerInstant.setNotificationPolicy(policy);
                             Log.d(TAG, "立即设置勿扰模式：已设置优先通知规则");
                         }
                        notificationManagerInstant.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                        Log.d(TAG, "立即设置勿扰模式：已启用（优先）");
                        Toast.makeText(this, "勿扰模式已立即开启", Toast.LENGTH_SHORT).show();
                    } else {
                        notificationManagerInstant.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                        Log.d(TAG, "立即设置勿扰模式：已关闭");
                        Toast.makeText(this, "勿扰模式已立即关闭", Toast.LENGTH_SHORT).show();
                    }
                }
                // --- 新增逻辑结束 ---

            } catch (Exception e) {
                Log.e(TAG, "设置勿扰模式失败", e);
                Toast.makeText(this, "设置勿扰模式失败，请重试", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "您的设备不支持此功能", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestPermissions() {
        // 请求闹钟权限
        PermissionHelper.requestAlarmPermission(this, new PermissionHelper.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                // 闹钟权限已授予，继续请求通知权限
                requestNotificationPermission();
            }

            @Override
            public void onPermissionDenied() {
                // 闹钟权限被拒绝
                Log.e(TAG, "闹钟权限被拒绝");
                Toast.makeText(AlarmSettingsActivity.this, "需要闹钟权限才能设置班次提醒", Toast.LENGTH_LONG).show();
                 PermissionHelper.openAppSettings(AlarmSettingsActivity.this);
            }
        });
    }

    private void requestNotificationPermission() {
        // 通知权限请求需要在Activity中注册ActivityResultLauncher，
        // 为了简化，这里直接调用检查并提示用户手动开启
         if (!PermissionHelper.checkNotificationPermission(this)) {
             Log.e(TAG, "没有发送通知的权限");
             Toast.makeText(this, "需要通知权限才能发送班次提醒", Toast.LENGTH_LONG).show();
             PermissionHelper.openAppSettings(this);
         }
    }

    /**
     * 保存所有时间选择器的值到 SharedPreferences
     */
    private void saveAllTimes() {
        Log.d(TAG, "尝试保存所有时间到 SharedPreferences");
        SharedPreferences sp = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putInt("day_shift_hour", timePickerDayShift.getHour());
        editor.putInt("day_shift_minute", timePickerDayShift.getMinute());
        editor.putInt("night_shift_hour", timePickerNightShift.getHour());
        editor.putInt("night_shift_minute", timePickerNightShift.getMinute());
        editor.putInt("dnd_start_hour", timePickerDndStart.getHour());
        editor.putInt("dnd_start_minute", timePickerDndStart.getMinute());
        editor.putInt("dnd_end_hour", timePickerDndEnd.getHour());
        editor.putInt("dnd_end_minute", timePickerDndEnd.getMinute());
        editor.putInt("dnd_night_start_hour", timePickerDndNightStart.getHour());
        editor.putInt("dnd_night_start_minute", timePickerDndNightStart.getMinute());
        editor.putInt("dnd_night_end_hour", timePickerDndNightEnd.getHour());
        editor.putInt("dnd_night_end_minute", timePickerDndNightEnd.getMinute());

        editor.apply();
        Log.d(TAG, "所有时间已保存");
    }
} 