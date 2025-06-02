package com.example.myapplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DayShiftGroup implements Serializable {
    public String date; // yyyy-MM-dd
    public List<String> dayTeams = new ArrayList<>();   // 白班组
    public List<String> nightTeams = new ArrayList<>(); // 夜班组
    public boolean isPublicHoliday;
    public boolean isSwitchWeek;
    public String monthTitle; // 如果是月份标题项，填月份名，否则为null
    public String weeknum; // 周数
}