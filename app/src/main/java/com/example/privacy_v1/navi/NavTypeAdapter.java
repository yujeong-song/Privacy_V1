package com.example.privacy_v1.navi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;

import java.util.ArrayList;
import java.util.List;

public class NavTypeAdapter  extends RecyclerView.Adapter<NavTypeAdapter.NavViewHolder> {

    public interface OnOptionSelectedListener {
        void onOptionSelected(NavOption option);
    }

    private final List<NavOption> items = new ArrayList<>();
    private final OnOptionSelectedListener listener;

    private int selectedPosition = RecyclerView.NO_POSITION;
    private int brandLogoResId, itemLogoResId;

    public NavTypeAdapter(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NavOption> newItems, int logoResId) {
        items.clear();
        items.addAll(newItems);

        brandLogoResId = logoResId;
        selectedPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NavViewHolder  onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_brand_type, parent, false);

        return new NavViewHolder (view);
    }
    @Override
    public void onBindViewHolder(
            @NonNull NavViewHolder holder,
            int position
    ) {
        NavOption option = items.get(position);

        boolean isSelected =
                position == selectedPosition;

        holder.itemRoot.setActivated(isSelected);
        holder.itemNameTextView.setActivated(isSelected);

        holder.itemNameTextView.setText(
                option.getName()
        );

        if (itemLogoResId != 0) {
            holder.itemLogoImageView.setVisibility(
                    View.VISIBLE
            );

            holder.itemLogoImageView.setImageResource(
                    itemLogoResId
            );
        } else {
            holder.itemLogoImageView.setVisibility(
                    View.GONE
            );
        }

        holder.itemRoot.setOnClickListener(view -> {
            int clickedPosition =
                    holder.getBindingAdapterPosition();

            if (clickedPosition ==
                    RecyclerView.NO_POSITION) {
                return;
            }

            int oldPosition = selectedPosition;
            selectedPosition = clickedPosition;

            if (oldPosition !=
                    RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition);
            }

            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onOptionSelected(
                        items.get(selectedPosition)
                );
            }
        });
    }


//    @Override
//    public void onBindViewHolder(
//            @NonNull NavTypeViewHolder holder,
//            int position
//    ) {
//        NavOption option = items.get(position);
//        boolean selected = position == selectedPosition;
//
//        holder.itemRoot.setActivated(selected);
//        holder.itemNameTextView.setText(option.getName());
//        holder.itemLogoImageView.setImageResource(brandLogoResId);
//
//        holder.itemRoot.setOnClickListener(view -> {
//            int clickedPosition =
//                    holder.getBindingAdapterPosition();
//
//            if (clickedPosition == RecyclerView.NO_POSITION) {
//                return;
//            }
//
//            int oldPosition = selectedPosition;
//            selectedPosition = clickedPosition;
//
//            if (oldPosition != RecyclerView.NO_POSITION) {
//                notifyItemChanged(oldPosition);
//            }
//
//            notifyItemChanged(selectedPosition);
//
//            if (listener != null) {
//                listener.onOptionSelected(
//                        items.get(selectedPosition)
//                );
//            }
//        });
//    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    static class NavViewHolder
            extends RecyclerView.ViewHolder {

        View itemRoot;
        ImageView itemLogoImageView;
        TextView itemNameTextView;

        public NavViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            itemRoot =
                    itemView.findViewById(
                            R.id.itemRoot
                    );

            itemLogoImageView =
                    itemView.findViewById(
                            R.id.itemLogoImageView
                    );

            itemNameTextView =
                    itemView.findViewById(
                            R.id.itemNameTextView
                    );
        }
    }

//    static class NavTypeViewHolder extends RecyclerView.ViewHolder {
//
//        View itemRoot;
//        ImageView itemLogoImageView;
//        TextView itemNameTextView;
//
//        public NavTypeViewHolder(@NonNull View itemView) {
//            super(itemView);
//
//            itemRoot = itemView.findViewById(R.id.itemRoot);
//            itemLogoImageView =
//                    itemView.findViewById(R.id.itemLogoImageView);
//            itemNameTextView =
//                    itemView.findViewById(R.id.itemNameTextView);
//        }
}