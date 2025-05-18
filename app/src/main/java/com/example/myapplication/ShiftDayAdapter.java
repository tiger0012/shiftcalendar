package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftDayAdapter extends RecyclerView.Adapter<ShiftDayAdapter.ViewHolder> {
    private List<ShiftDayEntity> shiftDays;
    private List<DayShiftGroup> data;

    private static final Map<String, Integer> TEAM_COLOR_MAP = new HashMap<>();
    static {
        TEAM_COLOR_MAP.put("Team1", Color.parseColor("#F44336")); // 红
        TEAM_COLOR_MAP.put("Team2", Color.parseColor("#2196F3")); // 蓝
        TEAM_COLOR_MAP.put("Team3", Color.parseColor("#4CAF50")); // 绿
        TEAM_COLOR_MAP.put("Team4", Color.parseColor("#FF9800")); // 橙
        TEAM_COLOR_MAP.put("Team5", Color.parseColor("#9C27B0")); // 紫
        TEAM_COLOR_MAP.put("Team6", Color.parseColor("#009688")); // 青
        TEAM_COLOR_MAP.put("Team7", Color.parseColor("#FFC107")); // 黄
    }

    public ShiftDayAdapter(List<ShiftDayEntity> shiftDays) {
        this.shiftDays = shiftDays;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftDayEntity day = shiftDays.get(position);
        holder.tvDate.setText(day.date.substring(day.date.length()-2)); // 显示日
        holder.tvDayShift.setText(day.dayTeam);
        holder.tvDayShift.setTextColor(TEAM_COLOR_MAP.getOrDefault(day.dayTeam, Color.BLACK));
        holder.tvNightShift.setText(day.nightTeam);
        holder.tvNightShift.setTextColor(TEAM_COLOR_MAP.getOrDefault(day.nightTeam, Color.GRAY));
        holder.markView.setVisibility(day.isPublicHoliday ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return shiftDays.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayShift, tvNightShift;
        View markView;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDayShift = itemView.findViewById(R.id.tv_day_shift);
            tvNightShift = itemView.findViewById(R.id.tv_night_shift);
            markView = itemView.findViewById(R.id.mark_view);
        }
    }
}