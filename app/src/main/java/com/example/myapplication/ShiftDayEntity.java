package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ShiftDayEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String date; // yyyy-MM-dd
    public String dayTeam;
    public String nightTeam;
    public boolean isPublicHoliday;
    public boolean isSwitchWeek;
}