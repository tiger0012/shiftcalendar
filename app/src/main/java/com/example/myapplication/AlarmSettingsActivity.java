package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.app.TimePickerDialog;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AlarmSettingsActivity";

    private TextView tvDayShiftTime;
    private TextView tvNightShiftTime;
    private TextView tvDndStartTime;
    private TextView tvDndEndTime;
    private TextView tvDndNightStartTime;
    private TextView tvDndNightEndTime;
    private TextView tvNapDuration;
    private Button btnSetCustomAlarm;
    private Button btnSetDnd;
    private Button btnSetNapDnd;
    private TextView tvDndDescription;
    private TextView tvAlarmDescription;
    private TextView tvNapDescription;

    private String currentSelectedTeam;
    private List<DayShiftGroup> shiftDataInRange;

    private int dayShiftHour = 6;
    private int dayShiftMinute = 0;
    private int nightShiftHour = 18;
    private int nightShiftMinute = 0;
    private int dndStartHour = 22;
    private int dndStartMinute = 0;
    private int dndEndHour = 6;
    private int dndEndMinute = 0;
    private int dndNightStartHour = 9;
    private int dndNightStartMinute = 0;
    private int dndNightEndHour = 18;
    private int dndNightEndMinute = 0;
    private int napHour = 0;
    private int napMinute = 35;

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
        tvDayShiftTime = findViewById(R.id.tv_day_shift_time);
        tvNightShiftTime = findViewById(R.id.tv_night_shift_time);
        tvDndStartTime = findViewById(R.id.tv_dnd_start_time);
        tvDndEndTime = findViewById(R.id.tv_dnd_end_time);
        tvDndNightStartTime = findViewById(R.id.tv_dnd_night_start_time);
        tvDndNightEndTime = findViewById(R.id.tv_dnd_night_end_time);
        tvNapDuration = findViewById(R.id.tv_nap_duration);
        btnSetCustomAlarm = findViewById(R.id.btn_set_custom_alarm);
        btnSetDnd = findViewById(R.id.btn_set_dnd);
        btnSetNapDnd = findViewById(R.id.btn_set_nap_dnd);
        tvDndDescription = findViewById(R.id.tv_dnd_description);
        tvAlarmDescription = findViewById(R.id.tv_alarm_description);
        tvNapDescription = findViewById(R.id.tv_nap_description);
        
        // 设置勿扰模式说明文字
        String dndDescriptionText = "• 勿扰模式将设置为完全勿扰，在指定时段内禁止所有通知声音\n"
                              + "• 允许重复来电响铃(同一号码15分钟内多次呼叫)\n"
                              + "• 闹钟声音不受影响\n"
                              + "• 手机解锁时自动允许声音提醒";
        if (tvDndDescription != null) {
            tvDndDescription.setText(dndDescriptionText);
        }
        
        // 设置闹钟说明文字
        String alarmDescriptionText = "• 根据班组自动设置闹钟，工作日提醒，休息日不提醒\n"
                                + "• 白班和夜班使用不同的闹钟时间\n"
                                + "• 点击时间可以自定义闹钟时间";
        if (tvAlarmDescription != null) {
            tvAlarmDescription.setText(alarmDescriptionText);
        }
        
        // 设置午睡勿扰说明文字
        String napDescriptionText = "• 设置临时勿扰状态和闹钟\n"
                              + "• 勿扰将在设定时长后自动关闭\n"
                              + "• 适合午休或短暂休息使用";
        if (tvNapDescription != null) {
            tvNapDescription.setText(napDescriptionText);
        }

        // 从 SharedPreferences 加载保存的时间
        loadSavedTimes();

        // 设置时间显示
        updateTimeDisplay();

        // 设置点击事件
        setupTimeClickListeners();

        // 为设置自定义闹钟按钮添加点击事件监听器
        btnSetCustomAlarm.setOnClickListener(v -> {
            setCustomAlarm();
        });

        // 为设置勿扰模式按钮添加点击事件监听器
        btnSetDnd.setOnClickListener(v -> {
            setDoNotDisturb();
        });

        // 为设置午睡勿扰按钮添加点击事件监听器
        btnSetNapDnd.setOnClickListener(v -> {
            setNapDoNotDisturb();
        });

        // 检查并请求闹钟和通知权限
        checkAndRequestPermissions();
    }

    private void loadSavedTimes() {
        SharedPreferences sp = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        dayShiftHour = sp.getInt("day_shift_hour", 6);
        dayShiftMinute = sp.getInt("day_shift_minute", 0);
        nightShiftHour = sp.getInt("night_shift_hour", 18);
        nightShiftMinute = sp.getInt("night_shift_minute", 0);
        dndStartHour = sp.getInt("dnd_start_hour", 22);
        dndStartMinute = sp.getInt("dnd_start_minute", 0);
        dndEndHour = sp.getInt("dnd_end_hour", 6);
        dndEndMinute = sp.getInt("dnd_end_minute", 0);
        dndNightStartHour = sp.getInt("dnd_night_start_hour", 9);
        dndNightStartMinute = sp.getInt("dnd_night_start_minute", 0);
        dndNightEndHour = sp.getInt("dnd_night_end_hour", 18);
        dndNightEndMinute = sp.getInt("dnd_night_end_minute", 0);
        
        // 不再需要读取开关状态，默认勿扰模式为完全勿扰并允许重复来电
    }

    private void updateTimeDisplay() {
        tvDayShiftTime.setText(String.format("%02d:%02d", dayShiftHour, dayShiftMinute));
        tvNightShiftTime.setText(String.format("%02d:%02d", nightShiftHour, nightShiftMinute));
        tvDndStartTime.setText(String.format("%02d:%02d", dndStartHour, dndStartMinute));
        tvDndEndTime.setText(String.format("%02d:%02d", dndEndHour, dndEndMinute));
        tvDndNightStartTime.setText(String.format("%02d:%02d", dndNightStartHour, dndNightStartMinute));
        tvDndNightEndTime.setText(String.format("%02d:%02d", dndNightEndHour, dndNightEndMinute));
        tvNapDuration.setText(String.format("%02d:%02d", napHour, napMinute));
    }

    private void setupTimeClickListeners() {
        tvDayShiftTime.setOnClickListener(v -> showTimePickerDialog(true, false, false, false, false, false));
        tvNightShiftTime.setOnClickListener(v -> showTimePickerDialog(false, true, false, false, false, false));
        tvDndStartTime.setOnClickListener(v -> showTimePickerDialog(false, false, true, false, false, false));
        tvDndEndTime.setOnClickListener(v -> showTimePickerDialog(false, false, false, true, false, false));
        tvDndNightStartTime.setOnClickListener(v -> showTimePickerDialog(false, false, false, false, true, false));
        tvDndNightEndTime.setOnClickListener(v -> showTimePickerDialog(false, false, false, false, false, true));
        tvNapDuration.setOnClickListener(v -> showTimePickerDialog(false, false, false, false, false, false));
    }

    private void showTimePickerDialog(boolean isDayShift, boolean isNightShift, 
                                    boolean isDndStart, boolean isDndEnd,
                                    boolean isDndNightStart, boolean isDndNightEnd) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            (view, hourOfDay, minute) -> {
                if (isDayShift) {
                    dayShiftHour = hourOfDay;
                    dayShiftMinute = minute;
                } else if (isNightShift) {
                    nightShiftHour = hourOfDay;
                    nightShiftMinute = minute;
                } else if (isDndStart) {
                    dndStartHour = hourOfDay;
                    dndStartMinute = minute;
                } else if (isDndEnd) {
                    dndEndHour = hourOfDay;
                    dndEndMinute = minute;
                } else if (isDndNightStart) {
                    dndNightStartHour = hourOfDay;
                    dndNightStartMinute = minute;
                } else if (isDndNightEnd) {
                    dndNightEndHour = hourOfDay;
                    dndNightEndMinute = minute;
                } else {
                    napHour = hourOfDay;
                    napMinute = minute;
                }
                updateTimeDisplay();
                saveAllTimes();
            },
            isDayShift ? dayShiftHour :
            isNightShift ? nightShiftHour :
            isDndStart ? dndStartHour :
            isDndEnd ? dndEndHour :
            isDndNightStart ? dndNightStartHour :
            isDndNightEnd ? dndNightEndHour :
            napHour,
            isDayShift ? dayShiftMinute :
            isNightShift ? nightShiftMinute :
            isDndStart ? dndStartMinute :
            isDndEnd ? dndEndMinute :
            isDndNightStart ? dndNightStartMinute :
            isDndNightEnd ? dndNightEndMinute :
            napMinute,
            true
        );
        timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        timePickerDialog.show();
    }

    private void saveAllTimes() {
        SharedPreferences sp = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putInt("day_shift_hour", dayShiftHour);
        editor.putInt("day_shift_minute", dayShiftMinute);
        editor.putInt("night_shift_hour", nightShiftHour);
        editor.putInt("night_shift_minute", nightShiftMinute);
        editor.putInt("dnd_start_hour", dndStartHour);
        editor.putInt("dnd_start_minute", dndStartMinute);
        editor.putInt("dnd_end_hour", dndEndHour);
        editor.putInt("dnd_end_minute", dndEndMinute);
        editor.putInt("dnd_night_start_hour", dndNightStartHour);
        editor.putInt("dnd_night_start_minute", dndNightStartMinute);
        editor.putInt("dnd_night_end_hour", dndNightEndHour);
        editor.putInt("dnd_night_end_minute", dndNightEndMinute);

        editor.apply();
    }

    private void setCustomAlarm() {
        // 保存所有时间选择器的时间
        saveAllTimes();

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

        // 设置默认值：完全勿扰模式且允许重复来电
        int dndModeType = 1; // 1=完全勿扰
        boolean allowRepeatedCalls = true; // 允许重复来电
        
        SharedPreferences sp = getSharedPreferences("alarm_settings_prefs", MODE_PRIVATE);
        sp.edit()
          .putInt("dnd_mode_type", dndModeType)
          .putBoolean("allow_repeated_calls", allowRepeatedCalls)
          .apply();

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
                dndStartHour = this.dndStartHour;
                dndStartMinute = this.dndStartMinute;
                dndEndHour = this.dndEndHour;
                dndEndMinute = this.dndEndMinute;
                 Log.d(TAG, "使用白班勿扰区间: " + dndStartHour + ":" + dndStartMinute + " - " + dndEndHour + ":" + dndEndMinute);
            } else { // 夜班时使用夜班勿扰区间
                dndStartHour = this.dndNightStartHour;
                dndStartMinute = this.dndNightStartMinute;
                dndEndHour = this.dndNightEndHour;
                dndEndMinute = this.dndNightEndMinute;
                 Log.d(TAG, "使用夜班勿扰区间: " + dndStartHour + ":" + dndStartMinute + " - " + dndEndHour + ":" + dndEndMinute);
            }

            // 保存选择的勿扰时间范围以及设置时的班次类型
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
                
                String toastMessage = String.format("已设置%s完全勿扰模式\n时间范围：%02d:%02d - %02d:%02d\n已允许重复来电和闹钟", 
                    shiftTypeString, dndStartHour, dndStartMinute, dndEndHour, dndEndMinute);

                String notificationTitle = String.format("%s勿扰区间已设置", shiftTypeString);

                String notificationText = String.format("勿扰时间: %02d:%02d - %02d:%02d，允许重复来电和闹钟", 
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
                             NotificationManager.Policy policy;
                             if (dndModeType == 1) {
                                 // 完全勿扰模式 - 只允许闹钟
                                 policy = new NotificationManager.Policy(
                                     NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                                     0,
                                     0
                                 );
                                 Log.d(TAG, "立即设置完全勿扰模式");
                             } else {
                                 // 优先勿扰模式
                                 policy = new NotificationManager.Policy(
                                     NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS |
                                     NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS |
                                     NotificationManager.Policy.PRIORITY_CATEGORY_CALLS |
                                     NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                                     NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                                     0
                                 );
                                 Log.d(TAG, "立即设置优先通知勿扰模式");
                             }
                             
                             try {
                                 // 华为手机兼容处理
                                 notificationManagerInstant.setNotificationPolicy(policy);
                                 
                                 // 为防止某些设备直接设置导致崩溃，使用延迟设置过滤器的方式
                                 new Thread(() -> {
                                     try {
                                         Thread.sleep(100); // 延迟100毫秒再设置过滤器
                                         runOnUiThread(() -> {
                                             try {
                                                 notificationManagerInstant.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                                 Log.d(TAG, "已设置勿扰过滤器模式");
                                             } catch (Exception e) {
                                                 Log.e(TAG, "设置勿扰过滤器失败", e);
                                             }
                                         });
                                     } catch (InterruptedException e) {
                                         Log.e(TAG, "延迟设置勿扰被中断", e);
                                     }
                                 }).start();
                                 
                                 Log.d(TAG, "立即应用勿扰模式策略");
                             } catch (Exception e) {
                                 Log.e(TAG, "设置勿扰模式失败", e);
                             }
                         }
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

    private void setNapDoNotDisturb() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                // 权限未授予，引导用户去设置页面授权
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "请在设置中授予勿扰模式权限", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                // 获取选择的午睡时长
                int totalMinutes = napHour * 60 + napMinute;

                if (totalMinutes == 0) {
                    Toast.makeText(this, "请设置午睡时长", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 计算结束时间
                Calendar endTime = Calendar.getInstance();
                endTime.add(Calendar.HOUR_OF_DAY, napHour);
                endTime.add(Calendar.MINUTE, napMinute);
                Log.d(TAG, "午睡将在 " + endTime.getTime() + " 结束，持续时间: " + napHour + "小时" + napMinute + "分钟");
                
                // 保存结束时间到SharedPreferences，以便可能的恢复使用
                SharedPreferences sp = getSharedPreferences("nap_settings", MODE_PRIVATE);
                sp.edit()
                    .putLong("nap_end_time", endTime.getTimeInMillis())
                    .putInt("nap_hours", napHour)
                    .putInt("nap_minutes", napMinute)
                    .apply();
                
                // 创建结束勿扰的广播接收器
                Intent endNapIntent = new Intent(this, NapAlarmReceiver.class);
                endNapIntent.setAction(NapAlarmReceiver.ACTION_NAP_ALARM_END);
                PendingIntent endNapPendingIntent = PendingIntent.getBroadcast(
                    this,
                    1002,
                    endNapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // 设置结束午睡的闹钟
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    // 取消之前可能存在的闹钟
                    try {
                        alarmManager.cancel(endNapPendingIntent);
                        Log.d(TAG, "已取消之前的午睡结束闹钟");
                    } catch (Exception e) {
                        Log.e(TAG, "取消之前闹钟出错", e);
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                endTime.getTimeInMillis(),
                                endNapPendingIntent
                            );
                            Log.d(TAG, "已设置午睡结束精确闹钟，允许唤醒设备");
                        } catch (Exception e) {
                            Log.e(TAG, "设置精确闹钟失败，尝试使用普通闹钟", e);
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                endTime.getTimeInMillis(),
                                endNapPendingIntent
                            );
                            Log.d(TAG, "已使用普通精确闹钟");
                        }
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            endTime.getTimeInMillis(),
                            endNapPendingIntent
                        );
                        Log.d(TAG, "已设置普通精确闹钟");
                    }
                }

                // 创建并立即触发开始勿扰的广播接收器
                Intent startNapIntent = new Intent(this, NapAlarmReceiver.class);
                startNapIntent.setAction(NapAlarmReceiver.ACTION_NAP_ALARM_START);
                PendingIntent startNapPendingIntent = PendingIntent.getBroadcast(
                    this,
                    1001,
                    startNapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // 立即执行开始广播
                try {
                    startNapPendingIntent.send();
                    Log.d(TAG, "已发送开始午睡勿扰广播");
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "发送开始午睡勿扰广播失败", e);
                }

                // 可选：设置系统闹钟（用于用户手动取消午睡）
                Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, endTime.get(Calendar.HOUR_OF_DAY));
                alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, endTime.get(Calendar.MINUTE));
                alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "午睡结束提醒");
                alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);  // 不显示闹钟设置界面
                
                if (alarmIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(alarmIntent);
                    Log.d(TAG, "已设置系统闹钟提醒");
                } else {
                    Log.d(TAG, "无法设置系统闹钟，未找到相应应用");
                }

                // 显示通知和Toast
                String notificationTitle = "午睡勿扰已开启";
                String notificationText = String.format("将在%d小时%d分钟后结束勿扰模式", napHour, napMinute);
                AlarmHelper.createDndSettingNotification(this, notificationTitle, notificationText);
                Toast.makeText(this, notificationTitle + "\n" + notificationText, Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                Log.e(TAG, "设置午睡勿扰模式失败", e);
                Toast.makeText(this, "设置午睡勿扰模式失败，请重试", Toast.LENGTH_SHORT).show();
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