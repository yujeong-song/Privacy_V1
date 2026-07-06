package com.example.privacy_v1.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;

import java.util.List;

public class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.VH> {

    public interface OnItemClick { void onClick(int position); }

    private final List<String> items;
    private final OnItemClick onItemClick;
    private int selectedPos = RecyclerView.NO_POSITION;

    public SimpleTextAdapter(List<String> items, OnItemClick onItemClick) {
        this.items = items;
        this.onItemClick = onItemClick;
    }
    public void setSelectedPos(int pos) {
        int old = selectedPos;
        selectedPos = pos;
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
        if (selectedPos != RecyclerView.NO_POSITION) notifyItemChanged(selectedPos);
    }


    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_one_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.title.setText(items.get(position));
        holder.itemView.setSelected(position == selectedPos);
        holder.itemView.setOnClickListener(v -> {
            setSelectedPos(position);
            onItemClick.onClick(position);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }
}