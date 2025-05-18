
package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MONTH = 0;
    private static final int TYPE_DAY = 1;
    private List<DayShiftGroup> data;

    // 班组颜色映射
    private static final Map<String, Integer> TEAM_COLOR_MAP = new HashMap<>();
    static {
        TEAM_COLOR_MAP.put("1", Color.parseColor("#F44336"));
        TEAM_COLOR_MAP.put("2", Color.parseColor("#2196F3"));
        TEAM_COLOR_MAP.put("3", Color.parseColor("#4CAF50"));
        TEAM_COLOR_MAP.put("4", Color.parseColor("#FF9800"));
        TEAM_COLOR_MAP.put("5", Color.parseColor("#9C27B0"));
        TEAM_COLOR_MAP.put("6", Color.parseColor("#009688"));
        TEAM_COLOR_MAP.put("7", Color.parseColor("#FFC107"));
    }

    public ShiftGroupAdapter(List<DayShiftGroup> data) {
        this.data = data;
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).monthTitle != null ? TYPE_MONTH : TYPE_DAY;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_MONTH) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_title, parent, false);
            return new MonthViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_day_group, parent, false);
            return new DayViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        DayShiftGroup item = data.get(position);
        if (holder instanceof MonthViewHolder) {
            ((MonthViewHolder) holder).tvMonthTitle.setText(item.monthTitle);
        } else if (holder instanceof DayViewHolder) {
            DayViewHolder h = (DayViewHolder) holder;
            // 显示日
            if (item.date != null && item.date.length() >= 10) {
                String[] dateParts = item.date.split("-");
                h.tvDate.setText(dateParts[2]);
            } else {
                h.tvDate.setText("");
            }
            // 白班
            StringBuilder daySb = new StringBuilder("白班: ");
            for (String team : item.dayTeams) {
                daySb.append(team).append(" ");
            }
            h.tvDayTeams.setText(daySb.toString().trim());
            // 夜班
            StringBuilder nightSb = new StringBuilder("夜班: ");
            for (String team : item.nightTeams) {
                nightSb.append(team).append(" ");
            }
            h.tvNightTeams.setText(nightSb.toString().trim());
            // 假期标记
            h.markView.setVisibility(item.isPublicHoliday ? View.VISIBLE : View.GONE);
            // 你可以在这里用SpannableString给班组数字上色（进阶美化）
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthTitle;
        MonthViewHolder(View itemView) {
            super(itemView);
            tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
        }
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayTeams, tvNightTeams;
        View markView;
        DayViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDayTeams = itemView.findViewById(R.id.tv_day_teams);
            tvNightTeams = itemView.findViewById(R.id.tv_night_teams);
            markView = itemView.findViewById(R.id.mark_view);
        }
    }
}
