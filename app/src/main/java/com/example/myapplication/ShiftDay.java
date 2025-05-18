package com.example.myapplication;

public class ShiftDay {
    public int year, month, day;
    public int dayShift, nightShift;
    public boolean isPublicHoliday;
    public boolean isSwitchWeek;

    public ShiftDay(int year, int month, int day, int dayShift, int nightShift, boolean isPublicHoliday, boolean isSwitchWeek) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.dayShift = dayShift;
        this.nightShift = nightShift;
        this.isPublicHoliday = isPublicHoliday;
        this.isSwitchWeek = isSwitchWeek;
    }
} 