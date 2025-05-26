package com.example.myapplication.utils;

import android.text.TextUtils;

public class DateDisplayUtils {
    public static String getDayDisplayText(int solarDay, String lunarDate, boolean isHoliday) {
        // 使用TextUtils判断空字符串
        StringBuilder sb = new StringBuilder();
        sb.append(solarDay);
        if (!TextUtils.isEmpty(lunarDate)) {
            sb.append("\n").append(lunarDate);
        }
        if (isHoliday) {
            sb.append("（假）");
        }
        return sb.toString();
    }
}