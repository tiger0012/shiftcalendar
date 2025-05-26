package com.example.myapplication;

import android.text.TextUtils;

public class DateDisplayUtils {
    /**
     * 组合公历、农历和节假日的显示文本
     * @param solarDay 公历日期（号数）
     * @param lunarDate 农历日期字符串（如"初一"、"十五"）
     * @param isHoliday 是否为节假日
     * @return 组合后的显示文本（如"15\n初一"或"15\n春节"）
     */
    public static String getDayDisplayText(int solarDay, String lunarDate, boolean isHoliday) {
        StringBuilder sb = new StringBuilder();
        // 公历日期
        sb.append(solarDay);
        // 农历日期（非空时添加）
        if (!TextUtils.isEmpty(lunarDate)) {
            sb.append("\n").append(lunarDate);
        }
        // 节假日标记（可根据需求扩展具体节日名称）
        if (isHoliday) {
            sb.append("\n节日"); // 可替换为具体节日名称
        }
        return sb.toString();
    }
}