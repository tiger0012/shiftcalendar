package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
        if (actualPosition < days.size()) {
            String weeknumValue = days.get(actualPosition).weeknum;
            weeknumHolder.tvWeeknum.setText(weeknumValue != null ? "Week " + weeknumValue : "");
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
            dayHolder.tvTeams.setText("");
            dayHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            dayHolder.tvDay.setText(String.valueOf(day.dayOfMonth));
            StringBuilder sb = new StringBuilder();
            if (!day.dayTeams.isEmpty()) sb.append("白:").append(joinTeams(day.dayTeams)).append(" ");
            if (!day.nightTeams.isEmpty()) sb.append("夜:").append(joinTeams(day.nightTeams));
            dayHolder.tvTeams.setText(sb.toString().trim());
            // 高亮今天
            dayHolder.tvDay.setTextColor(day.isToday ? Color.RED : Color.BLACK);
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

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTeams;

        DayViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvTeams = itemView.findViewById(R.id.tv_day_teams);
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