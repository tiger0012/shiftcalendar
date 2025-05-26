package com.example.myapplication;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarDay {
    private String lunarDate;
    private boolean isHoliday;

    public String getLunarDate() {
        return lunarDate;
    }

    public void setLunarDate(String lunarDate) {
        this.lunarDate = lunarDate;
    }

    public boolean isHoliday() {
        return isHoliday;
    }

    public void setHoliday(boolean holiday) {
        isHoliday = holiday;
    }
    public LocalDate date;

    public LocalDate getDate() {
        return date;
    }

    // 保留原有字符串date字段用于显示
    public String dateStr;
    public int dayOfMonth; // 1~31
    public int month; // 月份（1-12）
    public List<String> dayTeams = new ArrayList<>();
    public List<String> nightTeams = new ArrayList<>();
    public boolean isToday;
    public boolean isEmpty; // 是否是补齐的空白格

    public int dayOfWeek; // 新增星期字段（1=周日~7=周六）

    public boolean getIsEmpty() {
        return isEmpty;
    }
    public String weeknum; // 周数
    public String prevMonthLastWeeknum; // 上个月最后一周周数
    public boolean isPrevMonth; // 标记是否为上月日期

    public static class Builder {
        private int dayOfMonth;
        private String lunarDate;
        private boolean isHoliday;

        public Builder dayOfMonth(int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }

        public Builder lunarDate(String lunarDate) {
            this.lunarDate = lunarDate;
            return this;
        }

        public Builder isHoliday(boolean isHoliday) {
            this.isHoliday = isHoliday;
            return this;
        }

        public CalendarDay build() {
            CalendarDay day = new CalendarDay();
            day.dayOfMonth = this.dayOfMonth;
            day.lunarDate = this.lunarDate;
            day.isHoliday = this.isHoliday;
            // 可添加参数校验逻辑（如日期范围）
            return day;
        }
    }
}