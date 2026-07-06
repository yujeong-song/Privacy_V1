package com.example.privacy_v1.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;

import java.util.List;

public class TwoLineAdapter  extends RecyclerView.Adapter<TwoLineAdapter.VH> {

    public static class TwoLineItem {
        public final String title;
        public final String subtitle;

        public TwoLineItem(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public interface OnItemClick { void onClick(int position); }

    private final List<TwoLineItem> items;
    private final OnItemClick onItemClick;

    public TwoLineAdapter(List<TwoLineItem> items, OnItemClick onItemClick) {
        this.items = items;
        this.onItemClick = onItemClick;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            subtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_two_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TwoLineItem item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        holder.itemView.setOnClickListener(v -> onItemClick.onClick(position));
    }

    @Override
    public int getItemCount() { return items.size(); }
}