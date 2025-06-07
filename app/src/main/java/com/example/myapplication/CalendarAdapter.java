package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import com.example.myapplication.utils.DateDisplayUtils;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class CalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_WEEKNUM_HEADER = 0;
    private static final int VIEW_TYPE_DAY = 1;

    private List<CalendarDay> days;
    private Map<Integer, String> weeknumCache = new HashMap<>();

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
        precomputeWeeknumCache();
    }

    public void updateData(List<CalendarDay> newDays) {
        this.days = newDays;
        precomputeWeeknumCache();
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

private void precomputeWeeknumCache() {
        weeknumCache.clear();
        if (days == null || days.isEmpty()) return;
        
        for (int i = 0; i < days.size(); i += 7) {
            int weekIndex = i / 7;
            String weeknumValue = computeWeeknumForPosition(i);
            weeknumCache.put(weekIndex, weeknumValue);
        }
    }
    
    private String computeWeeknumForPosition(int position) {
        int weekIndex = position / 7;
        int actualPosition = weekIndex * 7;
        
        boolean isMonthStartWeek = weekIndex == 0 || 
            (actualPosition > 0 && days.get(actualPosition).month != days.get(actualPosition - 1).month);
            
        if (isMonthStartWeek) {
            // 优先检查是否有跨月的上个月天数
            boolean hasPrevMonthDays = days.subList(actualPosition, Math.min(actualPosition + 7, days.size()))
                .stream().anyMatch(d -> d.isPrevMonth);
                
            // 如果是月初第一周，优先使用上个月最后一周的周数
            if (hasPrevMonthDays) {
                String prevWeeknum = days.subList(actualPosition, Math.min(actualPosition + 7, days.size()))
                    .stream()
                    .filter(d -> d.isPrevMonth && !TextUtils.isEmpty(d.prevMonthLastWeeknum))
                    .findFirst()
                    .map(d -> d.prevMonthLastWeeknum)
                    .orElse("");
                if (!TextUtils.isEmpty(prevWeeknum)) {
                    return prevWeeknum;
                }
            }
            
            // 检查当前周是否有周数
            for (int i = actualPosition; i < Math.min(actualPosition + 7, days.size()); i++) {
                CalendarDay day = days.get(i);
                if (!TextUtils.isEmpty(day.weeknum)) {
                    return day.weeknum;
                }
            }
            
            // 如果没有周数，检查是否是跨月周
            if (actualPosition > 0 && days.get(actualPosition).month != days.get(actualPosition - 1).month) {
                CalendarDay prevDay = days.get(actualPosition - 1);
                if (!TextUtils.isEmpty(prevDay.weeknum)) {
                    return prevDay.weeknum;
                }
            }
        } else if (actualPosition < days.size()) {
            return days.get(actualPosition).weeknum;
        }
        return "";
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    int viewType = getItemViewType(position);
    if (viewType == VIEW_TYPE_WEEKNUM_HEADER) {
        WeeknumHeaderViewHolder weeknumHolder = (WeeknumHeaderViewHolder) holder;
        int weekIndex = position / 8;
        String weeknumValue = weeknumCache.getOrDefault(weekIndex, "");
        
        if (!TextUtils.isEmpty(weeknumValue)) {
            weeknumHolder.tvWeeknum.setVisibility(View.VISIBLE);
            weeknumHolder.tvWeeknum.setText(String.format(Locale.getDefault(), "第%s周", weeknumValue));
        } else {
            weeknumHolder.tvWeeknum.setVisibility(View.GONE);
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
                // 组合公历、农历和节假日信息，使用工具类方法
                String displayText = DateDisplayUtils.getDayDisplayText(day.dayOfMonth, day.getLunarDate(), day.isHoliday(), day.getHolidayName());
                // 已通过工具类生成完整显示文本，设置到TextView
                dayHolder.tvDay.setText(displayText);

                // 添加日志检查参数是否传入
                android.util.Log.d("CalendarAdapter", "当前日期参数：公历=" + day.dayOfMonth + ", 农历=" + day.getLunarDate() + ", 节假日=" + day.isHoliday()+  day.getHolidayName() + ", 今天=" + day.isToday);

                // 创建班组视图
                createTeamViews(dayHolder.teamContainerDay, day.dayTeams, dayHolder.itemView.getContext());
                createTeamViews(dayHolder.teamContainerNight, day.nightTeams, dayHolder.itemView.getContext());
                
                // 应用新的样式
                Context context = dayHolder.itemView.getContext();
                
                // 设置日期项背景
                if(day.isToday) {
                    // 今天的样式
                    dayHolder.itemView.setBackgroundResource(R.drawable.today_background);
                    dayHolder.tvDay.setSelected(true);
                } else {
                    // 判断是否是周末（周六或周日）
                    if (day.date != null) {
                        int dayOfWeek = day.date.getDayOfWeek().getValue() % 7; // 0 = 周日, 6 = 周六
                        if (dayOfWeek == 0 || dayOfWeek == 6) {
                            // 周末样式
                            dayHolder.itemView.setBackgroundResource(R.drawable.weekend_day_background);
                            dayHolder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.weekend_text_color));
                        } else {
                            // 工作日样式
                            dayHolder.itemView.setBackgroundResource(R.drawable.calendar_day_background);
                            dayHolder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary_text_color));
                        }
                    } else {
                        // 默认样式
                        dayHolder.itemView.setBackgroundResource(R.drawable.calendar_day_background);
                        dayHolder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary_text_color));
                    }
                    
                    // 设置农历背景色
                    if (day.getLunarBgColor() != R.color.transparent) {
                        dayHolder.tvDay.setBackgroundColor(ContextCompat.getColor(context, day.getLunarBgColor()));
                    } else {
                        dayHolder.tvDay.setBackgroundColor(Color.TRANSPARENT);
                    }
                    
                    dayHolder.tvDay.setSelected(false);
                }
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

    private void createTeamViews(LinearLayout container, List<String> teams, Context context) {
        container.removeAllViews();
        for (String team : teams) {
            TextView teamView = new TextView(context);
            teamView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            teamView.setTextSize(12);
            teamView.setPadding(4, 2, 4, 2);
            teamView.setText(team);
            setTeamBackground(teamView, team);
            container.addView(teamView);
        }
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