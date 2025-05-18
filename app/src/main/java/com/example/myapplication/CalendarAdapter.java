package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {
    private List<CalendarDay> days;

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
    }

    public void updateData(List<CalendarDay> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    @Override
    public DayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        if (day.isEmpty) {
            holder.tvDay.setText("");
            holder.tvTeams.setText("");
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            holder.tvDay.setText(String.valueOf(day.dayOfMonth));
            StringBuilder sb = new StringBuilder();
            if (!day.dayTeams.isEmpty()) sb.append("白:").append(joinTeams(day.dayTeams)).append(" ");
            if (!day.nightTeams.isEmpty()) sb.append("夜:").append(joinTeams(day.nightTeams));
            holder.tvTeams.setText(sb.toString().trim());
            // 高亮今天
            holder.tvDay.setTextColor(day.isToday ? Color.RED : Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return days == null ? 0 : days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTeams;
        DayViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvTeams = itemView.findViewById(R.id.tv_teams);
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
