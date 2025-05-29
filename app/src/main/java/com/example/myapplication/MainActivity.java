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
public class MainActivity extends AppCompatActivity {

    private RecyclerView rvShiftCalendar;
    private RecyclerView rvWeeknumColumn;
    private TextView tvCurrentMonth;
    private Button btnPreviousMonth;
    private Button btnNextMonth;
    private WeeknumColumnAdapter weeknumColumnAdapter;

    private int currentYear;
    private int currentMonth;
    private LocalDate currentDate;

    private Map<String, DayShiftGroup> allData;

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

        rvShiftCalendar = findViewById(R.id.rv_shift_calendar);
        rvWeeknumColumn = findViewById(R.id.rv_weeknum_column);
        RecyclerView rvWeekdayHeader = findViewById(R.id.rv_weekday_header);
        tvCurrentMonth = findViewById(R.id.tv_current_month);
        btnPreviousMonth = findViewById(R.id.btn_previous_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        rvShiftCalendar.setLayoutManager(new GridLayoutManager(this, 8)); // 8列（7天+周数标题）
        rvWeeknumColumn.setLayoutManager(new GridLayoutManager(this, 8));
        rvWeekdayHeader.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        weeknumColumnAdapter = new WeeknumColumnAdapter(new ArrayList<>());
        rvWeeknumColumn.setAdapter(weeknumColumnAdapter);
        
        // 初始化周几标题适配器
        List<String> weekdays = Arrays.asList("周日", "周一", "周二", "周三", "周四", "周五", "周六");
        WeekdayHeaderAdapter weekdayHeaderAdapter = new WeekdayHeaderAdapter(weekdays);
        rvWeekdayHeader.setAdapter(weekdayHeaderAdapter);

        // 1. 读取CSV，转为Map<String, DayShiftGroup> (只读一次)
        allData = readShiftGroupsFromCSV();

        // 2. 初始化显示为当前月份
        currentDate = LocalDate.now();
        currentYear = currentDate.getYear();
        currentMonth = currentDate.getMonthValue();

        // 3. 更新日历显示
        updateCalendarDisplay();

        // 4. 设置导航按钮点击事件
        btnPreviousMonth.setOnClickListener(v -> {
            navigateMonth(-1); // 上个月
        });

        btnNextMonth.setOnClickListener(v -> {
            navigateMonth(1); // 下个月
        });

        // 初始化时更新班组天数显示
        calculateTeamDays();
        updateTeamDaysDisplay();

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
                // 设置背景色（假设colors.xml中有对应颜色，如color_+currentSelectedTeam）
                String colorResName = "team_" + currentSelectedTeam;
                int colorResId = getResources().getIdentifier(colorResName, "color", getPackageName());
                if (colorResId != 0) {
                    // 使用ContextCompat避免过时API
                    spinnerTeamSelector.setBackgroundColor(ContextCompat.getColor(MainActivity.this, colorResId));
                }
                updateTeamDaysDisplay();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 初始化显示
        RecyclerView rvTeamDays = findViewById(R.id.rv_team_days);
        rvTeamDays.setLayoutManager(new LinearLayoutManager(this)); // 补充LayoutManager设置
        TeamDaysAdapter teamDaysAdapter = new TeamDaysAdapter(new ArrayList<>(teamDaysMap.entrySet()), currentSelectedTeam);
        rvTeamDays.setAdapter(teamDaysAdapter);
        updateTeamDaysDisplay();
        rvTeamDays.setAdapter(teamDaysAdapter);
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
            // 此处无需重新定义 year 变量，直接使用传入的 year 参数即可
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
}