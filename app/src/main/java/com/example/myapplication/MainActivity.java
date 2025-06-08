//测试git   
package com.example.myapplication;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // 补充完整RecyclerView相关导入

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Holiday;
import com.nlf.calendar.util.HolidayUtil;
import androidx.annotation.NonNull;
import android.os.Build;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.TimePicker;
import android.widget.NumberPicker;

import java.io.Serializable; // 添加Serializable导入
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.app.NotificationManager;
import android.app.PendingIntent;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.Date; // 导入 Date 类用于日志打印

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView rvShiftCalendar;
    private RecyclerView rvWeeknumColumn;
    private TextView tvCurrentMonth;
    private Button btnPreviousMonth;
    private Button btnNextMonth;
    private WeeknumColumnAdapter weeknumColumnAdapter;
    private TextView tvSettingsHint; // 添加对设置提示文本的引用
    private Button btnToday;
    private TextView tvTodayDescription; // 添加今天按钮说明引用
    private TextView tvSettingsDescription; // 添加设置按钮说明引用

    private int currentYear;
    private int currentMonth;
    private LocalDate currentDate;

    private Map<String, DayShiftGroup> allData;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private Button btnGoToAlarmSettings; // 跳转到闹钟设置页面的按钮

    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 50;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;

    // 新增类级别方法获取上月最后周数
    private String getPreviousMonthLastWeeknum(int year, int month) {
        try {
            LocalDate firstDayOfPrevMonth = LocalDate.of(year, month - 1, 1).minusMonths(1);
            LocalDate lastDayOfPrevMonth = firstDayOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
            String formattedLastDayOfPrevMonth = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()).format(lastDayOfPrevMonth);
            
            DayShiftGroup group = allData.get(lastDayOfPrevMonth);
            return group != null && group.weeknum != null ? group.weeknum : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 检查并引导用户授权勿扰模式权限
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "请授权勿扰模式控制权限，否则无法自动切换勿扰模式", Toast.LENGTH_LONG).show();
        }
        // 设置勿扰定时任务
        // MainActivity.setDndAlarms(this); // 移除自动设置勿扰定时任务的代码

        // 初始化手势检测器
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // 向右滑动，显示上个月
                                navigateMonth(-1);
                            } else {
                                // 向左滑动，显示下个月
                                navigateMonth(1);
                            }
                            result = true;
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        });

        // 初始化权限请求
        initPermissionRequests();

        // 检查并请求权限
        checkAndRequestPermissions();

        // 请求闹钟权限
        if (Build.VERSION.SDK_INT >= 31) { // Android 12 (API 31)
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    // 华为部分设备可能没有此设置页面，使用try-catch避免崩溃
                    Intent intent = new Intent();
                    intent.setAction("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "无法打开精确闹钟权限设置", e);
                    // 可以选择默默忽略，因为低版本设备不需要此权限
                }
            }
        }

        rvShiftCalendar = findViewById(R.id.rv_shift_calendar);
        rvWeeknumColumn = findViewById(R.id.rv_weeknum_column);
        tvCurrentMonth = findViewById(R.id.tv_current_month);
        btnToday = findViewById(R.id.btn_today);

        // 设置回到今天的按钮点击事件
        btnToday.setOnClickListener(v -> {
            goToToday();
        });

        // 设置日历视图的触摸监听器
        rvShiftCalendar.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        rvShiftCalendar.setLayoutManager(new GridLayoutManager(this, 8));
        rvWeeknumColumn.setLayoutManager(new GridLayoutManager(this, 8));
        weeknumColumnAdapter = new WeeknumColumnAdapter(new ArrayList<>());
        rvWeeknumColumn.setAdapter(weeknumColumnAdapter);
        
        // 初始化周几标题适配器不再需要，因为我们直接在布局中使用include标签

        // 1. 读取CSV，转为Map<String, DayShiftGroup> (只读一次)
        allData = readShiftGroupsFromCSV();

        // 2. 初始化显示为当前月份
        currentDate = LocalDate.now();
        currentYear = currentDate.getYear();
        currentMonth = currentDate.getMonthValue();

        // 3. 更新日历显示
        updateCalendarDisplay();

        // 初始化时更新班组天数显示
        calculateTeamDays();
        updateTeamDaysDisplay();

        // 获取跳转到闹钟设置页面的按钮引用
        btnGoToAlarmSettings = findViewById(R.id.btn_go_to_alarm_settings);
        tvSettingsHint = findViewById(R.id.tv_settings_hint);
        tvTodayDescription = findViewById(R.id.tv_today_description);
        tvSettingsDescription = findViewById(R.id.tv_settings_description);

        // 设置今日日期和班次信息
        updateTodayInfo();

        // 为跳转按钮设置点击事件监听器
        btnGoToAlarmSettings.setOnClickListener(v -> {
            goToAlarmSettings();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private Map<String, Integer> teamDaysMap = new HashMap<>();

    private void calculateTeamDays() {
        teamDaysMap.clear();
        android.util.Log.d("TeamDays", "开始统计，当前月份: " + currentYear + "-" + currentMonth);
        android.util.Log.d("TeamDays", "allData大小: " + allData.size()); // 检查allData是否有数据
        // 获取当前月份的第一天和最后一天
        LocalDate firstDayOfMonth = LocalDate.of(currentYear, currentMonth, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        
        for (DayShiftGroup group : allData.values()) {
            android.util.Log.d("TeamDays", "当前处理日期数据: " + group.date); // 打印当前处理的日期
            try {
                // 解析group的日期字符串为LocalDate（假设日期格式为yyyy-MM-dd）
                LocalDate groupDate = LocalDate.parse(group.date, DateTimeFormatter.ISO_LOCAL_DATE);
                android.util.Log.d("TeamDays", "日期解析成功: " + groupDate);
                // 检查日期是否在当前月份范围内
                if (groupDate.isAfter(lastDayOfMonth) || groupDate.isBefore(firstDayOfMonth)) {
                    android.util.Log.d("TeamDays", "日期不在当前月份，跳过: " + groupDate);
                    continue;
                }
                android.util.Log.d("TeamDays", "日期在当前月份范围内，开始统计班组: dayTeams=" + group.dayTeams + ", nightTeams=" + group.nightTeams); // 打印班组列表
            } catch (DateTimeParseException e) {
                android.util.Log.e("TeamDays", "日期解析失败，格式错误: " + group.date + ", 错误信息: " + e.getMessage());
                continue;
            }
            // 统计白班和夜班的班组天数
            for (String team : group.dayTeams) {
                teamDaysMap.put(team, teamDaysMap.getOrDefault(team, 0) + 1);
                android.util.Log.d("TeamDays", "白班统计: 班组=" + team + ", 当前天数=" + teamDaysMap.get(team)); // 打印白班统计结果
            }
            for (String team : group.nightTeams) {
                teamDaysMap.put(team, teamDaysMap.getOrDefault(team, 0) + 1);
                android.util.Log.d("TeamDays", "夜班统计: 班组=" + team + ", 当前天数=" + teamDaysMap.get(team)); // 打印夜班统计结果
            }
        }
    }

    private static class TeamDaysAdapter extends RecyclerView.Adapter<TeamDaysAdapter.ViewHolder> {
        private List<Map.Entry<String, Integer>> data;

        private String currentSelectedTeam = "";

        public TeamDaysAdapter(List<Map.Entry<String, Integer>> data, String currentSelectedTeam) {
            this.data = data;
            this.currentSelectedTeam = currentSelectedTeam;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_days, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map.Entry<String, Integer> entry = data.get(position);
            if (entry.getKey().equals(currentSelectedTeam)) {
                android.util.Log.d("TeamDays", "当前班组天数: " + entry.getValue());
                holder.tvTeamDays.setText(String.valueOf(entry.getValue()));
                holder.itemView.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTeamDays;

            ViewHolder(View itemView) {
                super(itemView);
                tvTeamDays = itemView.findViewById(R.id.tv_team_days);
            }
        }
    }

    private void updateTeamDaysDisplay() {
        android.util.Log.d("TeamDays", "updateTeamDaysDisplay: currentSelectedTeam=" + currentSelectedTeam + ", teamDaysMap是否为空=" + teamDaysMap.isEmpty()); // 检查团队选择和map状态
        List<Map.Entry<String, Integer>> filteredData = new ArrayList<>();
        if (!teamDaysMap.isEmpty() && !currentSelectedTeam.isEmpty()) {
            android.util.Log.d("TeamDays", "teamDaysMap中当前团队天数: " + teamDaysMap.get(currentSelectedTeam)); // 检查目标团队是否有数据
            filteredData.add(new AbstractMap.SimpleEntry<>(currentSelectedTeam, teamDaysMap.get(currentSelectedTeam)));
        }
        RecyclerView rvTeamDays = findViewById(R.id.rv_team_days);
        rvTeamDays.setLayoutManager(new LinearLayoutManager(this));
        TeamDaysAdapter teamDaysAdapter = new TeamDaysAdapter(filteredData, currentSelectedTeam);
        rvTeamDays.setAdapter(teamDaysAdapter);
    }

    private void updateCalendarDisplay() {
        String monthName = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        tvCurrentMonth.setText(currentYear + " " + monthName);

        List<CalendarDay> days = generateMonthCalendar(currentYear, currentMonth, allData);
        List<String> weeknumColumn = generateWeeknumColumn(days);

        if (rvShiftCalendar.getAdapter() == null) {
            CalendarAdapter adapter = new CalendarAdapter(days);
            rvShiftCalendar.setAdapter(adapter);
        } else {
            ((CalendarAdapter) rvShiftCalendar.getAdapter()).updateData(days);
        }

        weeknumColumnAdapter.updateData(weeknumColumn);

        // 统计班组总天数
        calculateTeamDays();

        // 初始化班组选择Spinner
        Spinner spinnerTeamSelector = findViewById(R.id.spinner_team_selector);
        List<String> teamList = new ArrayList<>(teamDaysMap.keySet());
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamList);
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeamSelector.setAdapter(teamAdapter);

        // 设置默认选中第一个班组
        if (!teamList.isEmpty()) {
            currentSelectedTeam = teamList.get(0);
        }

        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        // 读取上次选择的班组
        currentSelectedTeam = sp.getString("last_selected_team", "");
        if (!teamList.isEmpty() && teamList.contains(currentSelectedTeam)) {
            spinnerTeamSelector.setSelection(teamList.indexOf(currentSelectedTeam));
        }

        // Spinner选择监听
        spinnerTeamSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedTeam = teamList.get(position);
                Log.d("TeamColorDebug", "Selected team: " + currentSelectedTeam);
                // 保存当前选择的班组
                sp.edit().putString("last_selected_team", currentSelectedTeam).apply();
                // 设置背景色
                String colorResName = "team_" + currentSelectedTeam;
                int colorResId = getResources().getIdentifier(colorResName, "color", getPackageName());
                if (colorResId != 0) {
                    spinnerTeamSelector.setBackgroundColor(ContextCompat.getColor(MainActivity.this, colorResId));
                }
                updateTeamDaysDisplay();
                updateTodayInfo(); // 更新今日信息
                
                // 切换班组时，重新设置勿扰定时任务
                setDndAlarms(MainActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 初始化显示
        RecyclerView rvTeamDays = findViewById(R.id.rv_team_days);
        rvTeamDays.setLayoutManager(new LinearLayoutManager(this));
        TeamDaysAdapter teamDaysAdapter = new TeamDaysAdapter(new ArrayList<>(teamDaysMap.entrySet()), currentSelectedTeam);
        rvTeamDays.setAdapter(teamDaysAdapter);
        updateTeamDaysDisplay();
        rvTeamDays.setAdapter(teamDaysAdapter);
    }

    private void checkAndSetAlarm() {
        // 此方法不再用于设置闹钟，保留或移除取决于后续需求
        // 如果只是为了检查权限，可以保留一部分逻辑
         if (!PermissionHelper.checkAlarmPermission(this)) {
            Log.e(TAG, "没有设置闹钟的权限");
            Toast.makeText(this, "需要闹钟权限才能设置班次提醒", Toast.LENGTH_LONG).show();
            PermissionHelper.requestAlarmPermission(this, new PermissionHelper.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    // 权限获取后不做任何操作或根据需要刷新UI
                }

                @Override
                public void onPermissionDenied() {
                    // 权限被拒绝
                    Log.e(TAG, "闹钟权限被拒绝");
                }
            });
        }
    }

    private void goToAlarmSettings() {
        if (currentSelectedTeam == null || currentSelectedTeam.isEmpty()) {
            Toast.makeText(this, "请先选择班组", Toast.LENGTH_SHORT).show();
            return;
        }

         // 获取从今天到下一周今天的班次数据
        LocalDate today = LocalDate.now();
        LocalDate nextWeekToday = today.plusDays(7);

        List<DayShiftGroup> shiftDataInRange = new ArrayList<>();
        for (DayShiftGroup group : allData.values()) {
            if (group.date != null) {
                try {
                    LocalDate date = LocalDate.parse(group.date, DateTimeFormatter.ISO_LOCAL_DATE);
                    if (!date.isBefore(today) && !date.isAfter(nextWeekToday)) {
                        shiftDataInRange.add(group);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析日期失败", e);
                }
            }
        }

        // 检查是否有班次
        boolean hasShift = false;
         for (DayShiftGroup dayData : shiftDataInRange) {
            if (dayData.dayTeams.contains(currentSelectedTeam) || dayData.nightTeams.contains(currentSelectedTeam)) {
                hasShift = true;
                break;
            }
        }

        if (!hasShift) {
             Toast.makeText(this, "从今天到下一周今天没有班次", Toast.LENGTH_SHORT).show();
             return;
        }

        Intent intent = new Intent(this, AlarmSettingsActivity.class);
        intent.putExtra("selectedTeam", currentSelectedTeam);
        // 将班次数据作为Serializable传递
        intent.putExtra("shiftData", (Serializable) shiftDataInRange);
        startActivity(intent);
    }

    private void navigateMonth(int monthOffset) {
        currentDate = currentDate.withDayOfMonth(1).plusMonths(monthOffset);
        currentYear = currentDate.getYear();
        currentMonth = currentDate.getMonthValue();

        updateCalendarDisplay();
        // 月份切换时更新班组天数统计和显示
        calculateTeamDays();
        updateTeamDaysDisplay();
    }

    private String currentSelectedTeam = "";

    private Map<String, DayShiftGroup> readShiftGroupsFromCSV() {
        Map<String, DayShiftGroup> map = new LinkedHashMap<>();
        SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
        SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try (InputStream is = getAssets().open("shift.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            
            String line;
            boolean isFirst = true;
            int weeknumIndex = -1;
            int holidayIndex = -1; // 新增Holiday列索引
            
            // 预分配缓冲区
            StringBuilder lineBuilder = new StringBuilder(256);
            
            while ((line = reader.readLine()) != null) {
                lineBuilder.setLength(0);
                lineBuilder.append(line);
                
                String[] tokens = lineBuilder.toString().split(",");
                if (isFirst) {
                    // 查找Weeknum列索引
                    for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i].trim().equalsIgnoreCase("Weeknum")) {
                        weeknumIndex = i;
                    } else if (tokens[i].trim().equalsIgnoreCase("Holiday")) {
                        holidayIndex = i;
                    }
                    }
                    isFirst = false;
                    continue;
                }
                if (tokens.length < 13 || weeknumIndex == -1) continue;

                String weeknum = tokens[weeknumIndex].trim();
                String csvDateString = tokens[1].trim();
                String date = null;
                try {
                    Date parsedDate = csvDateFormat.parse(csvDateString);
                    date = targetDateFormat.format(parsedDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                    continue;
                }

                String dayNight = tokens[3].trim();
                String team = tokens[2].trim();
                DayShiftGroup group = map.computeIfAbsent(date, k -> new DayShiftGroup());
                group.date = date;
                group.weeknum = weeknum;
                // 读取Holiday列（L列）并设置是否为假期
                if (holidayIndex != -1 && holidayIndex < tokens.length) {
                    String holidayValue = tokens[holidayIndex].trim();
                    group.isPublicHoliday = "Y".equalsIgnoreCase(holidayValue);
                }

                if ("Day".equalsIgnoreCase(dayNight)) {
                    if (!group.dayTeams.contains(team)) group.dayTeams.add(team);
                } else {
                    if (!group.nightTeams.contains(team)) group.nightTeams.add(team);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }

    // 使用cn.6tail:lunar库进行农历转换
    private List<CalendarDay> generateMonthCalendar(int year, int month, Map<String, DayShiftGroup> allData) {
        // 生成公历日期列表
        List<LocalDate> dates = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            dates.add(date);
        }

        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date : dates) {
            // 使用新添加的农历库获取阴历信息
            java.util.Date utilDate = java.sql.Date.valueOf(date.toString());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = sdf.format(utilDate);
            
            // 获取假期信息（参考https://6tail.cn/calendar/api.html）
            // 由于 year 变量已在方法参数中存在，避免重复定义，直接使用方法参数中的 year
            // 此处无需重新定义 year 变量，直接使用方法参数中的 year 参数即可
            // 由于方法参数中已有 month 变量，此处直接使用方法参数中的 month，避免重复定义
            // 因此删除此局部变量的定义，无需重新获取月份值
            // int dayOfMonth = date.getDayOfMonth();
            
            android.util.Log.d("CalendarInfo", "当前日期: " + date.toString());
            android.util.Log.d("CalendarInfo", "转换后的日期: " + utilDate.toString());
            Lunar lunar = Lunar.fromDate(utilDate);
            // int year = date.getYear();
            // int month = date.getMonthValue();
            int dayOfMonth = date.getDayOfMonth();
            Holiday holiday = HolidayUtil.getHoliday(year, month, dayOfMonth);
            boolean isWork = false;
            String holidayName = "";
            String holidayTarget = "";
            if (holiday != null) {
                isWork = holiday.isWork();
                holidayName = holiday.getName();
                Log.d("MainActivity", "holidayName: " + holidayName); // 打印holidayName的值
                holidayTarget = holiday.getTarget();
            }
            String lunarDateStr = lunar.toString().substring(lunar.toString().indexOf("年") + 1);
            int lunarBgColor = R.color.transparent; // 默认背景色
            // 获取CSV中的假期标记
            DayShiftGroup group = allData.get(dateStr);
            boolean isCsvHoliday = group != null && group.isPublicHoliday;
            Log.d("MainActivity", "Date: " + dateStr + ", isPublicHoliday: " + (group != null ? group.isPublicHoliday : "null") + ", holidayName: " + holidayName);
            
            // 结合原假期判断和CSV标记
            boolean isHoliday = isCsvHoliday; // 仅当CSV中的isPublicHoliday为true时标记为假期，显示红色背景
            
            if (isHoliday) {
                lunarBgColor = R.color.holiday_red; // 假期使用红色背景
                // 假期时处理农历显示：月初第一天显示月份，其他显示具体日期
                if (lunarDateStr.contains("初一")) {
                    int monthIndex = lunarDateStr.indexOf("月");
                    lunarDateStr = lunarDateStr.substring(0, monthIndex + 1);
                } else {
                    int monthIndex = lunarDateStr.indexOf("月");
                    lunarDateStr = lunarDateStr.substring(monthIndex + 1);
                }
            } else if(lunarDateStr.contains("初一")) {
                int monthIndex = lunarDateStr.indexOf("月");
                lunarDateStr = lunarDateStr.substring(0, monthIndex + 1);
                lunarBgColor = R.color.cyan; // 初一使用青色背景
            } else {
                int monthIndex = lunarDateStr.indexOf("月");
                lunarDateStr = lunarDateStr.substring(monthIndex + 1);
            }
            
            CalendarDay day = new CalendarDay.Builder()
                .dayOfMonth(date.getDayOfMonth())
                .lunarDate(lunarDateStr)
                .lunarBgColor(lunarBgColor) // 设置农历背景颜色
                .isHoliday(isHoliday)
                .holidayName(holidayName)
                .build();
            // android.util.Log.d("CalendarInfo", "农历日期: " + lunarDateStr);

            // 此处需要根据实际的CalendarDay.Builder API修改
            // 由于不清楚正确方法名，暂时注释掉有问题的代码
            // CalendarDay day = new CalendarDay.Builder()
            //    .setDate(date)
            //    .setLunarDate(lunarDateStr)
            //    .build();
            // 请根据实际API文档完善此处代码
            // 使用 CalendarDay.Builder 构造 CalendarDay 对象
            // CalendarDay day = new CalendarDay.Builder()
            //     .dayOfMonth(date.getDayOfMonth())
            //     .lunarTextColor(textColor)
            //     .lunarDate(lunarDateStr) // 确保正确设置农历日期
            //     .isHoliday(false)
            //     .build();
            android.util.Log.d("CalendarInfo", "生成的 CalendarDay 对象是否为空: " + (day == null));
            if (day != null) {
                days.add(day);
            }
        }

        List<CalendarDay> result = new ArrayList<>();
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7 + 1; // 转换为Calendar的周日=1格式
        int daysInMonth = firstDayOfMonth.lengthOfMonth();

        // 在生成日历数据时处理跨月周数
        for (int i = 1; i < firstDayOfWeek; i++) {
            CalendarDay empty = new CalendarDay();
            empty.isEmpty = true;
            empty.dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()));
            empty.dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()));
            // 清空班组信息
            empty.dayTeams = new ArrayList<>();
            empty.nightTeams = new ArrayList<>();
            // 设置上个月最后一周周数
            empty.prevMonthLastWeeknum = getPreviousMonthLastWeeknum(year, month);
            empty.isPrevMonth = true;
            result.add(empty);
        }

        // 移除此处的方法定义

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        LocalDate today = LocalDate.now();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = LocalDate.of(year, month, d);
            String dateStr = dateFormatter.format(date);
            // 原错误代码（重新创建对象）
            // CalendarDay day = new CalendarDay();
            
            // 修正后（使用days列表中的对象）
            // 假设dates和days顺序一致，通过索引获取已生成的CalendarDay对象
            CalendarDay day = days.get(d - 1); // d从1开始，索引为d-1
            day.date = date;
            day.dateStr = dateStr;
            day.dayOfMonth = d;
            day.isToday = today.equals(date);
            DayShiftGroup group = allData.get(dateStr);
            if (group != null) {
                day.dayTeams = group.dayTeams;
                day.nightTeams = group.nightTeams;
                day.weeknum = group.weeknum;
            }
            result.add(day);
        }

        while (result.size() % 7 != 0) {
            CalendarDay empty = new CalendarDay();
            empty.isEmpty = true;
            empty.dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()));
            empty.dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()));
            // 清空班组信息
            empty.dayTeams = new ArrayList<>();
            empty.nightTeams = new ArrayList<>();
            result.add(empty);
        }
        return result;
    }

    private List<String> generateWeeknumColumn(List<CalendarDay> days) {
    List<String> result = new ArrayList<>();
    int currentMonth = -1;
    String prevWeeknum = "";
    
    for (int i = 0; i < days.size(); i++) {
        if (i % 7 == 0) {
            CalendarDay day = days.get(i);
            String weeknum = "";
            
            // 处理跨月周数
            if (day.isEmpty && !day.prevMonthLastWeeknum.isEmpty()) {
                weeknum = "Week" + day.prevMonthLastWeeknum;
            } else if (day.weeknum != null && !day.weeknum.isEmpty()) {
                weeknum = "Week" + day.weeknum;
            } else if ((i == 0 || days.get(i-1).date.getMonthValue() != day.date.getMonthValue()) && day.weeknum != null) {
                // 处理月初第一周
                weeknum = "Week" + day.weeknum;
            }
            
            // 添加非空周数到结果
            result.add(weeknum);
        } else {
            result.add("");
        }
    }
    return result;
}

    private void initPermissionRequests() {
        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // 通知权限已授予
                    Log.d(TAG, "通知权限已授予");
                } else {
                    // 通知权限被拒绝
                    Log.d(TAG, "通知权限被拒绝");
                    Toast.makeText(this, "需要通知权限才能发送班次提醒", Toast.LENGTH_LONG).show();
                    PermissionHelper.openAppSettings(this);
                }
            }
        );
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
                Log.d(TAG, "闹钟权限被拒绝");
            }
        });
    }

    private void requestNotificationPermission() {
        PermissionHelper.requestNotificationPermission(this, notificationPermissionLauncher,
            new PermissionHelper.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    // 通知权限已授予
                    Log.d(TAG, "通知权限已授予");
                }

                @Override
                public void onPermissionDenied() {
                    // 通知权限被拒绝
                    Log.d(TAG, "通知权限被拒绝");
                }
            });
    }

    public ActivityResultLauncher<String> getNotificationPermissionLauncher() {
        return notificationPermissionLauncher;
    }

    // 添加回到今天的方法
    private void goToToday() {
        currentDate = LocalDate.now();
        currentYear = currentDate.getYear();
        currentMonth = currentDate.getMonthValue();
        updateCalendarDisplay();
        calculateTeamDays();
        updateTeamDaysDisplay();
    }

    /**
     * 根据班组和日期判断班次类型
     * @param team 班组名称
     * @param date 日期
     * @return 0: 无班次, 1: 白班, 2: 夜班
     */
    private int getShiftTypeForTeamOnDate(String team, LocalDate date) {
        if (allData == null || team == null || date == null) {
            return 0; // 无效输入
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr = date.format(formatter);
        DayShiftGroup dayShiftGroup = allData.get(dateStr);

        if (dayShiftGroup != null) {
            if (dayShiftGroup.dayTeams.contains(team)) {
                return 1; // 白班
            } else if (dayShiftGroup.nightTeams.contains(team)) {
                return 2; // 夜班
            }
        }
        return 0; // 无班次
    }

    /**
     * 设置勿扰模式定时任务：计算下一个最近的开启/关闭时间并设置精确闹钟
     */
    public static void setDndAlarms(Context context) {
        Log.d(TAG, "setDndAlarms 被调用");

        // 打印调用栈
        try {
            throw new Exception("Call stack");
        } catch (Exception e) {
            Log.d(TAG, "Call stack: ", e);
        }

        // 检查用户是否已经设置过勿扰模式
        SharedPreferences appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean dndSettingsConfigured = appPrefs.getBoolean("dnd_settings_configured", false); // 默认未设置
        Log.d(TAG, "dnd_settings_configured 读取值为: " + dndSettingsConfigured);

        // 尝试读取AlarmSettings中保存的设置作为备份检查
        SharedPreferences alarmSettingsPrefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE);
        boolean hasSetDndTimes = alarmSettingsPrefs.contains("current_dnd_start_hour") && 
                                alarmSettingsPrefs.contains("current_dnd_end_hour");
                                
        // 如果app_prefs中没有设置标记但AlarmSettings有时间设置，则也认为已配置
        if (!dndSettingsConfigured && hasSetDndTimes) {
            Log.d(TAG, "虽然dnd_settings_configured为false，但找到了勿扰时间设置，将继续设置定时任务");
            // 补充设置标记
            appPrefs.edit().putBoolean("dnd_settings_configured", true).apply();
            dndSettingsConfigured = true;
        }

        if (!dndSettingsConfigured) {
            Log.d(TAG, "用户尚未设置勿扰模式，跳过设置定时任务。");
            return;
        }

        // 检查系统勿扰权限
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager 为空");
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted()) {
            Log.e(TAG, "没有勿扰模式控制权限");
            return;
        }
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager 为空");
            return;
        }

        // 取消之前设置的所有 DND 定时任务，避免重复
        cancelDndAlarms(context);

        // 从 SharedPreferences 读取当前选中的班组
        String currentSelectedTeam = appPrefs.getString("last_selected_team", "");

        if (currentSelectedTeam.isEmpty()) {
            Log.d(TAG, "未选择班组，不设置勿扰定时任务");
            return;
        }

        // Needs to access allData to determine shift type, but allData is a non-static member, cannot be accessed directly
        // Consider moving shift type determination logic here, or pass necessary data via parameters
        // Temporary solution: read DND time based on shift type saved from AlarmSettingsActivity, but this cannot handle team switching

        // Better solution: read all shift data from SharedPreferences and determine shift type in setDndAlarms
        // This requires modifying AlarmSettingsActivity to also save shiftDataInRange to SharedPreferences when setting DND

        // Let's first implement a compromise: read the last set shift type and all DND time intervals from alarm_settings_prefs
        // and decide which interval to use based on the current selected team and today's shift type
        
        // TODO: Here, need to determine whether to use day or night DND interval based on the current selected team and today's shift type
        // 读取已保存的勿扰时间设置
        int startHour = alarmSettingsPrefs.getInt("current_dnd_start_hour", 22);
        int startMinute = alarmSettingsPrefs.getInt("current_dnd_start_minute", 0);
        int endHour = alarmSettingsPrefs.getInt("current_dnd_end_hour", 6);
        int endMinute = alarmSettingsPrefs.getInt("current_dnd_end_minute", 0);

        Log.d(TAG, "读取的当前生效勿扰区间: " + startHour + ":" + startMinute + " - " + endHour + ":" + endMinute);

        // 从设置中读取是否允许重复来电和勿扰模式类型
        boolean allowRepeatedCalls = alarmSettingsPrefs.getBoolean("allow_repeated_calls", true);
        int dndModeType = alarmSettingsPrefs.getInt("dnd_mode_type", 0); // 0=优先，1=完全勿扰
        
        Log.d(TAG, "读取的勿扰模式类型: " + (dndModeType == 1 ? "完全勿扰" : "优先勿扰") + 
              ", 允许重复来电: " + allowRepeatedCalls);
              
        // 检查当前勿扰状态，如果勿扰模式已开启，确保设置正确
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int currentInterruptionFilter = notificationManager.getCurrentInterruptionFilter();
            if (currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                // 勿扰模式已开启，确保设置是正确的
                NotificationManager.Policy currentPolicy = notificationManager.getNotificationPolicy();
                boolean needUpdate = false;
                
                if (dndModeType == 1) {
                    // 完全勿扰模式
                    int expectedCategories = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
                    if (allowRepeatedCalls) {
                        expectedCategories |= NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
                    }
                    
                    if (currentPolicy.priorityCategories != expectedCategories) {
                        needUpdate = true;
                    }
                }
                
                if (needUpdate) {
                    // 重新应用正确的勿扰设置
                    Intent updateIntent = new Intent(context, DndAlarmReceiver.class);
                    updateIntent.putExtra("ENABLE_DND", true);
                    context.sendBroadcast(updateIntent);
                    Log.d(TAG, "发送更新勿扰设置的广播");
                }
            }
        }
        
        // Calculate next DND start time
        Calendar nextOnTime = Calendar.getInstance();
        nextOnTime.set(Calendar.HOUR_OF_DAY, startHour);
        nextOnTime.set(Calendar.MINUTE, startMinute);
        nextOnTime.set(Calendar.SECOND, 0);
        nextOnTime.set(Calendar.MILLISECOND, 0);
        if (nextOnTime.before(Calendar.getInstance())) {
            nextOnTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Calculate next DND end time
        Calendar nextOffTime = Calendar.getInstance();
        nextOffTime.set(Calendar.HOUR_OF_DAY, endHour);
        nextOffTime.set(Calendar.MINUTE, endMinute);
        nextOffTime.set(Calendar.SECOND, 0);
        nextOffTime.set(Calendar.MILLISECOND, 0);
        if (nextOffTime.before(Calendar.getInstance())) {
            nextOffTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        Log.d(TAG, "当前时间: " + Calendar.getInstance().getTime());
        Log.d(TAG, "下一个开启时间: " + nextOnTime.getTime());
        Log.d(TAG, "下一个关闭时间: " + nextOffTime.getTime());

        // 确定下一个最近的定时任务是开启还是关闭
        Intent nextAlarmIntent = null;
        PendingIntent nextAlarmPendingIntent = null;
        long nextAlarmTimeMillis = 0;
        
        // 直接比较下一个开启时间和下一个关闭时间，选择更近的一个设置定时任务
        if (nextOnTime.before(nextOffTime)) {
            // 下一个最近的是开启时间
            nextAlarmIntent = new Intent(context, DndAlarmReceiver.class);
            nextAlarmIntent.putExtra("ENABLE_DND", true);
            nextAlarmPendingIntent = PendingIntent.getBroadcast(context, 1001, nextAlarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
            nextAlarmTimeMillis = nextOnTime.getTimeInMillis();
            Log.d(TAG, "下一个定时任务是开启勿扰: " + nextOnTime.getTime());
        } else {
            // 下一个最近的是关闭时间
            nextAlarmIntent = new Intent(context, DndAlarmReceiver.class);
            nextAlarmIntent.putExtra("ENABLE_DND", false);
            nextAlarmPendingIntent = PendingIntent.getBroadcast(context, 1002, nextAlarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
            nextAlarmTimeMillis = nextOffTime.getTimeInMillis();
            Log.d(TAG, "下一个定时任务是关闭勿扰: " + nextOffTime.getTime());
        }

        // 设置精确的定时任务
        if (nextAlarmPendingIntent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 修改这里，根据API版本检查能否使用精确闹钟
                    boolean canScheduleExact = true; // 默认假设可以
                    
                    // 仅在Android 12 (API 31)及以上版本检查精确闹钟权限
                    if (Build.VERSION.SDK_INT >= 31) { // Build.VERSION_CODES.S
                        canScheduleExact = alarmManager.canScheduleExactAlarms();
                    }
                    
                    if (canScheduleExact) {
                        // 使用 setAlarmClock，它在某些设备上可能有更高优先级
                        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(nextAlarmTimeMillis, null);
                        alarmManager.setAlarmClock(alarmClockInfo, nextAlarmPendingIntent);
                        Log.d(TAG, "已使用 setAlarmClock 设置定时任务 at: " + new Date(nextAlarmTimeMillis).toString());
                    } else {
                        // 如果无法设置精确闹钟，退回到 setExactAndAllowWhileIdle
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmTimeMillis, nextAlarmPendingIntent);
                            Log.d(TAG, "退回到 setExactAndAllowWhileIdle 设置定时任务");
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTimeMillis, nextAlarmPendingIntent);
                            Log.d(TAG, "退回到 setExact 设置定时任务");
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmTimeMillis, nextAlarmPendingIntent);
                            Log.d(TAG, "退回到 set 设置定时任务");
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTimeMillis, nextAlarmPendingIntent);
                    Log.d(TAG, "已使用 setExact 设置定时任务 (pre-M) at: " + new Date(nextAlarmTimeMillis).toString());
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmTimeMillis, nextAlarmPendingIntent);
                    Log.d(TAG, "已使用 set 设置定时任务 (pre-KITKAT) at: " + new Date(nextAlarmTimeMillis).toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "设置定时任务失败", e);
            }
        }
    }

    /**
     * 取消之前设置的所有 DND 定时任务
     */
    public static void cancelDndAlarms(Context context) {
         Log.d(TAG, "cancelDndAlarms 被调用");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // 取消开启勿扰的 PendingIntent
        Intent dndOnIntent = new Intent(context, DndAlarmReceiver.class);
        PendingIntent dndOnPendingIntent = PendingIntent.getBroadcast(context, 1001, dndOnIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(dndOnPendingIntent);
        Log.d(TAG, "已取消勿扰开启定时任务");

        // 取消关闭勿扰的 PendingIntent
        Intent dndOffIntent = new Intent(context, DndAlarmReceiver.class);
        PendingIntent dndOffPendingIntent = PendingIntent.getBroadcast(context, 1002, dndOffIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(dndOffPendingIntent);
        Log.d(TAG, "已取消勿扰关闭定时任务");
    }

    // 添加更新今日信息的方法
    private void updateTodayInfo() {
        if (tvSettingsHint == null) return;
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayStr = today.format(formatter);
        
        String currentShiftStatus = "今日无班";
        int bgColor = ContextCompat.getColor(this, R.color.gray); // 默认灰色背景
        
        if (currentSelectedTeam != null && !currentSelectedTeam.isEmpty() && allData != null) {
            DayShiftGroup todayShift = allData.get(todayStr);
            if (todayShift != null) {
                if (todayShift.dayTeams.contains(currentSelectedTeam)) {
                    currentShiftStatus = "今日白班";
                    bgColor = ContextCompat.getColor(this, R.color.day_shift); // 白班用蓝色
                } else if (todayShift.nightTeams.contains(currentSelectedTeam)) {
                    currentShiftStatus = "今日夜班";
                    bgColor = ContextCompat.getColor(this, R.color.night_shift); // 夜班用绿色
                }
            }
        }
        
        tvSettingsHint.setText(currentShiftStatus);
        tvSettingsHint.setBackgroundColor(bgColor);
    }
}