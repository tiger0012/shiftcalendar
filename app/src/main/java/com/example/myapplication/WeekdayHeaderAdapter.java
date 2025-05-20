package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeekdayHeaderAdapter extends RecyclerView.Adapter<WeekdayHeaderAdapter.WeekdayViewHolder> {
    private List<String> weekdays;

    public WeekdayHeaderAdapter(List<String> weekdays) {
        this.weekdays = weekdays;
    }

    @Override
    public WeekdayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weekday_header, parent, false);
        return new WeekdayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WeekdayViewHolder holder, int position) {
        holder.tvWeekday.setText(weekdays.get(position));
    }

    @Override
    public int getItemCount() {
        return weekdays == null ? 0 : weekdays.size();
    }

    static class WeekdayViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeekday;

        WeekdayViewHolder(View itemView) {
            super(itemView);
            tvWeekday = itemView.findViewById(R.id.tv_weekday);
        }
    }
}