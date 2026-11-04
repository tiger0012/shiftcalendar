package com.example.myapplication.utils;

import android.text.TextUtils;

public class DateDisplayUtils {
    public static String getDayDisplayText(int solarDay, String lunarDate, boolean isHoliday, String holidayName) {
        // 使用TextUtils判断空字符串
        StringBuilder sb = new StringBuilder();
        sb.append(solarDay);
        if (isHoliday && !TextUtils.isEmpty(holidayName)) {
            // 节假日：用节假日名替换农历显示（与小程序 2.2.0 一致），
            // 同时保持两行以内（tv_day maxLines=2）
            sb.append("\n").append(holidayName);
        } else if (!TextUtils.isEmpty(lunarDate)) {
            sb.append("\n").append(lunarDate);
        }
        return sb.toString();
    }
}