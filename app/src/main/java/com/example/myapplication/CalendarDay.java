package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

public class CalendarDay {
    public String date; // yyyy-MM-dd
    public int dayOfMonth; // 1~31
    public List<String> dayTeams = new ArrayList<>();
    public List<String> nightTeams = new ArrayList<>();
    public boolean isToday;
    public boolean isEmpty; // 是否是补齐的空白格

    public boolean getIsEmpty() {
        return isEmpty;
    }
    public String weeknum; // 周数
}