package com.example.myapplication;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class CalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_WEEKNUM_HEADER = 0;
    private static final int VIEW_TYPE_DAY = 1;

    private List<CalendarDay> days;

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
    }

    public void updateData(List<CalendarDay> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position % 8 == 0) { // 每8个item插入一个Weeknum header
            return VIEW_TYPE_WEEKNUM_HEADER;
        } else {
            return VIEW_TYPE_DAY;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_WEEKNUM_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weeknum_header, parent, false);
            return new WeeknumHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new DayViewHolder(view);
        }
    }

@Override
public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    int viewType = getItemViewType(position);
    if (viewType == VIEW_TYPE_WEEKNUM_HEADER) {
        WeeknumHeaderViewHolder weeknumHolder = (WeeknumHeaderViewHolder) holder;
        // 从数据源获取实际周数
        int weekIndex = position / 8;
        int actualPosition = weekIndex * 7;
        
        // 处理跨月周数显示（优化首周非周一的情况）
        // 判断当前周是否为新月份的首周（可能跨月）
        boolean isMonthStartWeek = weekIndex == 0 || (actualPosition > 0 && days.get(actualPosition).month != days.get(actualPosition - 1).month);
        if (isMonthStartWeek) {
            // 检查当前周是否存在上月日期（本月首日非周一的情况）
            boolean hasPrevMonthDays = days.subList(actualPosition, Math.min(actualPosition + 7, days.size())).stream().anyMatch(d -> d.isPrevMonth);
            if (hasPrevMonthDays) {
                // 获取当前周内第一个有有效周数的跨月日期的上月周数
                String weeknumValue = days.subList(actualPosition, Math.min(actualPosition + 7, days.size())).stream()
                    .filter(d -> d.isPrevMonth && !TextUtils.isEmpty(d.prevMonthLastWeeknum)) 
                    .findFirst()
                    .map(d -> d.prevMonthLastWeeknum)
                    .orElse("");
                if (!TextUtils.isEmpty(weeknumValue)) {
                    weeknumHolder.tvWeeknum.setVisibility(View.VISIBLE);
                    weeknumHolder.tvWeeknum.setText(String.format(Locale.getDefault(), "第%s周", weeknumValue));
                } else {
                    weeknumHolder.tvWeeknum.setVisibility(View.GONE);
                }
            } else {
                // 首周无跨月日期，遍历当前周日期获取有效周数
                String weeknumValue = "";
                for (int i = actualPosition; i < Math.min(actualPosition + 7, days.size()); i++) {
                    CalendarDay day = days.get(i);
                    if (!TextUtils.isEmpty(day.weeknum)) {
                        weeknumValue = day.weeknum;
                        break;
                    }
                }
                if (!TextUtils.isEmpty(weeknumValue)) {
                    weeknumHolder.tvWeeknum.setVisibility(View.VISIBLE);
                    weeknumHolder.tvWeeknum.setText(String.format(Locale.getDefault(), "第%s周", weeknumValue));
                } else {
                    weeknumHolder.tvWeeknum.setVisibility(View.GONE);
                }
            }
        } else if (actualPosition < days.size()) {
            // 非首周正常取当前位置周数
            String weeknumValue = days.get(actualPosition).weeknum;
            if (!TextUtils.isEmpty(weeknumValue)) {
                weeknumHolder.tvWeeknum.setVisibility(View.VISIBLE);
                weeknumHolder.tvWeeknum.setText(String.format(Locale.getDefault(), "第%s周", weeknumValue));
            } else {
                weeknumHolder.tvWeeknum.setVisibility(View.GONE);
            }
        }
        weeknumHolder.tvDayShiftTitle.setText("白班");
        weeknumHolder.tvNightShiftTitle.setText("夜班");
    } else {
        int dayPosition = position - (position / 8 + 1);
        if (dayPosition < 0 || dayPosition >= days.size()) return;
        DayViewHolder dayHolder = (DayViewHolder) holder;
        CalendarDay day = days.get(dayPosition); // 正确索引

        if (day.getIsEmpty()) {
            dayHolder.tvDay.setText("");
            if (dayHolder.tvDayTeams != null) dayHolder.tvDayTeams.setText("");
            if (dayHolder.tvNightTeams != null) dayHolder.tvNightTeams.setText("");
            dayHolder.teamContainerDay.removeAllViews();
            dayHolder.teamContainerNight.removeAllViews();
            dayHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            if (!day.getIsEmpty()) {
                dayHolder.tvDay.setText(String.valueOf(day.dayOfMonth));
                
                // 动态创建白班班组视图
                Log.d("ShiftDebug", "白班数据：" + day.dayTeams.toString());
                dayHolder.teamContainerDay.removeAllViews();
                for (String team : day.dayTeams) {
                    TextView teamView = new TextView(dayHolder.itemView.getContext());
            teamView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            teamView.setTextSize(12);
            teamView.setPadding(4, 2, 4, 2);
                    teamView.setText(team);
                    setTeamBackground(teamView, team);
                    dayHolder.teamContainerDay.addView(teamView);
                }
                
                // 动态创建夜班班组视图
                Log.d("ShiftDebug", "夜班数据：" + day.nightTeams.toString());
                dayHolder.teamContainerNight.removeAllViews();
                for (String team : day.nightTeams) {
                    TextView teamView = new TextView(dayHolder.itemView.getContext());
            teamView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            teamView.setTextSize(12);
            teamView.setPadding(4, 2, 4, 2);
                    teamView.setText(team);
                    setTeamBackground(teamView, team);
                    dayHolder.teamContainerNight.addView(teamView);
                }
                
                // 高亮今天
                dayHolder.tvDay.setTextColor(day.isToday ? Color.RED : Color.BLACK);
            }
        }
    }
}

    @Override
    public int getItemCount() {
        if (days == null || days.isEmpty()) {
            return 0;
        }
        // 每7天添加一个Weeknum header，因此总项数为 days.size() + (days.size() / 7)
        // 总项数 = 天数 + 周数标题数（每7天一个）
        return days.size() + (days.size() + 6) / 7;
    }

    static class WeeknumHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeeknum, tvDayShiftTitle, tvNightShiftTitle;

        WeeknumHeaderViewHolder(View itemView) {
            super(itemView);
            tvWeeknum = itemView.findViewById(R.id.tv_weeknum);
            tvDayShiftTitle = itemView.findViewById(R.id.tv_day_shift_title);
            tvNightShiftTitle = itemView.findViewById(R.id.tv_night_shift_title);
        }
    }

    private static final Map<String, Integer> TEAM_COLOR_MAP = new HashMap<>();
    static {
        TEAM_COLOR_MAP.put("1", R.color.team_1);
        TEAM_COLOR_MAP.put("2", R.color.team_2);
        TEAM_COLOR_MAP.put("3", R.color.team_3);
        TEAM_COLOR_MAP.put("4", R.color.team_4);
        TEAM_COLOR_MAP.put("5", R.color.team_5);
        TEAM_COLOR_MAP.put("6", R.color.team_6);
        TEAM_COLOR_MAP.put("7", R.color.team_7);
    }

    private void setTeamBackground(TextView textView, String team) {
        String teamNumber = team.replaceAll("\\D+", "");
        int teamIndex = (Integer.parseInt(teamNumber.isEmpty() ? "0" : teamNumber) - 1) % 7 + 1;
        Integer colorRes = TEAM_COLOR_MAP.getOrDefault(String.valueOf(teamIndex), R.color.team_default);
        textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), colorRes));
        textView.setPadding(8, 4, 8, 4);
        textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.black));
    }

static class DayViewHolder extends RecyclerView.ViewHolder {
    TextView tvDay, tvDayTeams, tvNightTeams;
    LinearLayout teamContainerDay, teamContainerNight;

    DayViewHolder(View itemView) {
        super(itemView);
        tvDay = itemView.findViewById(R.id.tv_day);
        tvDayTeams = itemView.findViewById(R.id.tv_day_teams);
        tvNightTeams = itemView.findViewById(R.id.tv_night_teams);
        teamContainerDay = itemView.findViewById(R.id.teamContainerDay);
        teamContainerNight = itemView.findViewById(R.id.teamContainerNight);
    }
}

    private String joinTeams(List<String> teams) {
        StringBuilder sb = new StringBuilder();
        for (String t : teams) {
            sb.append(t).append(" ");
        }
        return sb.toString().trim();
    }
}