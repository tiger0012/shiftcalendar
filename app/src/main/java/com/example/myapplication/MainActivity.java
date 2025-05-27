//测试git   
package com.example.myapplication;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

import com.nlf.calendar.Lunar;
import com.nlf.calendar.util.HolidayUtil;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
    }

    private void navigateMonth(int monthOffset) {
        currentDate = currentDate.withDayOfMonth(1).plusMonths(monthOffset);
        currentYear = currentDate.getYear();
        currentMonth = currentDate.getMonthValue();

        updateCalendarDisplay();
    }

    private Map<String, DayShiftGroup> readShiftGroupsFromCSV() {
        Map<String, DayShiftGroup> map = new LinkedHashMap<>();
        SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
        SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try (InputStream is = getAssets().open("shift.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            
            String line;
            boolean isFirst = true;
            int weeknumIndex = -1;
            
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
                            break;
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
            android.util.Log.d("CalendarInfo", "当前日期: " + date.toString());
            android.util.Log.d("CalendarInfo", "转换后的日期: " + utilDate.toString());
            Lunar lunar = Lunar.fromDate(utilDate);
            String lunarDateStr = lunar.toString().substring(lunar.toString().indexOf("年") + 1);
            int lunarBgColor = R.color.transparent; // 默认背景色
            
            if(lunarDateStr.contains("初一")) {
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
                .isHoliday(false)
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