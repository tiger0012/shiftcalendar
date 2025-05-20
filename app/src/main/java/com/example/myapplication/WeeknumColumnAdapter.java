package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeeknumColumnAdapter extends RecyclerView.Adapter<WeeknumColumnAdapter.WeeknumViewHolder> {
    private List<String> items;

    public WeeknumColumnAdapter(List<String> items) {
        this.items = items;
    }

    public void updateData(List<String> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public WeeknumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weeknum_column, parent, false);
        return new WeeknumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WeeknumViewHolder holder, int position) {
        String text = items.get(position);
        if (text != null && !text.isEmpty()) {
            holder.tvWeeknum.setText(text);
            holder.tvWeeknum.setVisibility(View.VISIBLE);
        } else {
            holder.tvWeeknum.setText("");
            holder.tvWeeknum.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class WeeknumViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeeknum;
        WeeknumViewHolder(View itemView) {
            super(itemView);
            tvWeeknum = itemView.findViewById(R.id.tv_weeknum);
        }
    }
} 