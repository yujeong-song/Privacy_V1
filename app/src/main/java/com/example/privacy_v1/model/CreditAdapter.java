package com.example.privacy_v1.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.server.model.UsageItem;

import java.util.ArrayList;
import java.util.List;

public class CreditAdapter extends RecyclerView.Adapter<CreditAdapter.VH> {
    private final List<UsageItem> items = new ArrayList<>();

    public void setItems(List<UsageItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usage, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UsageItem it = items.get(position);
//        holder.tvDate.setText(it.getDate().substring(5)); // "MM-DD"
        holder.tvDate.setText(it.getDate());
        holder.tvDesc.setText(it.getDescription());
        holder.tvAmount.setText(String.format("%,d", it.getAmount()));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvDesc, tvAmount;
        VH(View itemView) {
            super(itemView);
            tvDate   = itemView.findViewById(R.id.tvDate);
            tvDesc   = itemView.findViewById(R.id.tvDesc);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}