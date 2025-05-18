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
import java.util.*;
import java.text.ParseException;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvShiftCalendar;
    private RecyclerView rvWeeknumColumn;
    private TextView tvCurrentMonth;
    private Button btnPreviousMonth;
    private Button btnNextMonth;
    private WeeknumColumnAdapter weeknumColumnAdapter;

    private int currentYear;
    private int currentMonth;

    private Map<String, DayShiftGroup> allData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        rvShiftCalendar = findViewById(R.id.rv_shift_calendar);
        rvWeeknumColumn = findViewById(R.id.rv_weeknum_column);
        tvCurrentMonth = findViewById(R.id.tv_current_month);
        btnPreviousMonth = findViewById(R.id.btn_previous_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        rvShiftCalendar.setLayoutManager(new GridLayoutManager(this, 8)); // 8列（7天+周数标题）
        rvWeeknumColumn.setLayoutManager(new GridLayoutManager(this, 8));
        weeknumColumnAdapter = new WeeknumColumnAdapter(new ArrayList<>());
        rvWeeknumColumn.setAdapter(weeknumColumnAdapter);

        // 1. 读取CSV，转为Map<String, DayShiftGroup> (只读一次)
        allData = readShiftGroupsFromCSV();

        // 2. 初始化显示为当前月份
        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH 是 0-based

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
        tvCurrentMonth.setText(currentYear + "年" + currentMonth + "月");

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
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth - 1, 1);
        cal.add(Calendar.MONTH, monthOffset);

        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH) + 1;

        updateCalendarDisplay();
    }

    private Map<String, DayShiftGroup> readShiftGroupsFromCSV() {
        Map<String, DayShiftGroup> map = new LinkedHashMap<>();
        SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
        SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            InputStream is = getAssets().open("shift.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            boolean isFirst = true;
            int weeknumIndex = -1;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
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
                DayShiftGroup group = map.getOrDefault(date, new DayShiftGroup());
                group.date = date;
                group.weeknum = weeknum;

                if ("Day".equalsIgnoreCase(dayNight)) {
                    if (!group.dayTeams.contains(team)) group.dayTeams.add(team);
                } else {
                    if (!group.nightTeams.contains(team)) group.nightTeams.add(team);
                }
                map.put(date, group);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("DATA_RANGE", "Data map size: " + map.size());
        String testDate = "2025-05-18";
        Log.d("DATA_RANGE", "Does map contain key " + testDate + "? " + map.containsKey(testDate));
        return map;
    }

    private List<CalendarDay> generateMonthCalendar(int year, int month, Map<String, DayShiftGroup> allData) {
        List<CalendarDay> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i < firstDayOfWeek; i++) {
            CalendarDay empty = new CalendarDay();
            empty.isEmpty = true;
            result.add(empty);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        for (int d = 1; d <= daysInMonth; d++) {
            cal.set(year, month - 1, d);
            String dateStr = sdf.format(cal.getTime());
            CalendarDay day = new CalendarDay();
            day.date = dateStr;
            day.dayOfMonth = d;
            day.isToday = (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month - 1 && today.get(Calendar.DAY_OF_MONTH) == d);
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
            result.add(empty);
        }
        return result;
    }

    private List<String> generateWeeknumColumn(List<CalendarDay> days) {
        List<String> result = new ArrayList<>();
        int total = days.size();
        for (int i = 0; i < total; i++) {
            if (i % 7 == 0) {
                // 找到本周第一个有weeknum的日期
                String weeknum = "";
                for (int j = 0; j < 7 && (i + j) < total; j++) {
                    CalendarDay day = days.get(i + j);
                    if (day.weeknum != null && !day.weeknum.isEmpty()) {
                        weeknum = day.weeknum;
                        break;
                    }
                }
                result.add(weeknum);
            } else {
                result.add("");
            }
        }
        return result;
    }
}