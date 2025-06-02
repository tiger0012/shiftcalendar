package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
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
import java.util.List;

public class AlarmSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AlarmSettingsActivity";

    private TimePicker timePickerDayShift;
    private TimePicker timePickerNightShift;
    private TimePicker timePickerDndStart;
    private TimePicker timePickerDndEnd;
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
        btnSetCustomAlarm = findViewById(R.id.btn_set_custom_alarm);
        btnSetDnd = findViewById(R.id.btn_set_dnd);

        // 设置默认时间
        timePickerDayShift.setHour(6);
        timePickerDayShift.setMinute(0);
        timePickerNightShift.setHour(18);
        timePickerNightShift.setMinute(0);
        timePickerDndStart.setHour(22);
        timePickerDndStart.setMinute(0);
        timePickerDndEnd.setHour(6);
        timePickerDndEnd.setMinute(0);

        // 确保使用24小时制
        timePickerDayShift.setIs24HourView(true);
        timePickerNightShift.setIs24HourView(true);
        timePickerDndStart.setIs24HourView(true);
        timePickerDndEnd.setIs24HourView(true);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                // 权限未授予，引导用户去设置页面授权
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "请在设置中授予勿扰模式权限", Toast.LENGTH_LONG).show();
                return;
            }

            // 获取用户设置的时间范围
            int startHour = timePickerDndStart.getHour();
            int startMinute = timePickerDndStart.getMinute();
            int endHour = timePickerDndEnd.getHour();
            int endMinute = timePickerDndEnd.getMinute();

            // 设置勿扰模式
            try {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                Toast.makeText(this, 
                    String.format("已设置勿扰模式\n时间范围：%02d:%02d - %02d:%02d", 
                        startHour, startMinute, endHour, endMinute), 
                    Toast.LENGTH_LONG).show();
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
} 