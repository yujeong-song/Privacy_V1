package com.example.privacy_v1.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.common.DisplayName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MakerSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VT_HEADER = 1;
    private static final int VT_ITEM = 2;

    public interface Listener {
        void onToggleSection(String sectionKey);
        void onClickMaker(String sectionKey, String makerKey, String makerName, String fileName);
    }

    private final LinkedHashMap<String, LinkedHashMap<String, String>> sectionMap;
    private final LinkedHashMap<String, Boolean> expandedMap = new LinkedHashMap<>();
    private final Listener listener;

    private final List<Row> rows = new ArrayList<>();
    private String selectedMakerKey = null;

    public MakerSectionAdapter(LinkedHashMap<String, LinkedHashMap<String, String>> sectionMap,
                               Listener listener) {
        this.sectionMap = sectionMap;
        this.listener = listener;
        for (String key : sectionMap.keySet()) expandedMap.put(key, true);
        rebuildRows();
    }

    public boolean isExpanded(String sectionKey) {
        Boolean v = expandedMap.get(sectionKey);
        return v != null && v;
    }

    public void setExpanded(String sectionKey, boolean expanded) {
        expandedMap.put(sectionKey, expanded);
        rebuildRows();
        notifyDataSetChanged();
    }

    private void rebuildRows() {
        rows.clear();
        for (Map.Entry<String, LinkedHashMap<String, String>> sec : sectionMap.entrySet()) {
            String sectionKey = sec.getKey();
            rows.add(Row.header(sectionKey));
            if (isExpanded(sectionKey)) {
                for (Map.Entry<String, String> mk : sec.getValue().entrySet()) {
                    rows.add(Row.item(sectionKey, mk.getKey(), mk.getValue()));
                }
            }
        }
    }

    @Override public int getItemViewType(int position) { return rows.get(position).type; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VT_HEADER) {
            return new HeaderVH(inf.inflate(R.layout.item_section_header, parent, false));
        } else {
            return new ItemVH(inf.inflate(R.layout.item_maker_logo, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (holder instanceof HeaderVH) {
            HeaderVH vh = (HeaderVH) holder;
            vh.tvSection.setText(row.sectionKey);
            vh.tvArrow.setText(isExpanded(row.sectionKey) ? "▾" : "▸");
            vh.itemView.setOnClickListener(v -> listener.onToggleSection(row.sectionKey));

        } else {
            ItemVH vh = (ItemVH) holder;

            String makerName = DisplayName.makerKo(row.makerKey);
            vh.tvName.setText(makerName);
            vh.ivLogo.setImageResource(logoResId(vh.itemView.getContext(), row.makerKey));

            boolean isSel = row.makerKey != null && row.makerKey.equals(selectedMakerKey);

            // ✅ 전체 아이템 선택 효과
            vh.itemView.setSelected(isSel);
            // ✅ 로고 링(동그라미)도 선택 상태로 만들어 링이 진해지게
            vh.logoRing.setSelected(isSel);

            vh.itemView.setOnClickListener(v -> {
                selectedMakerKey = row.makerKey;
                notifyDataSetChanged(); // 단순/확실 (리스트가 크지 않으니 OK)

                listener.onClickMaker(row.sectionKey, row.makerKey, makerName, row.fileName);
            });
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvSection, tvArrow;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvSection = itemView.findViewById(R.id.tvSection);
            tvArrow = itemView.findViewById(R.id.tvArrow);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        FrameLayout logoRing;
        ImageView ivLogo;
        TextView tvName;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            logoRing = itemView.findViewById(R.id.logoRing);
            ivLogo = itemView.findViewById(R.id.ivLogo);
//            tvName = itemView.findViewById(R.id.tvMakerName);
        }
    }

    static class Row {
        final int type;
        final String sectionKey;
        final String makerKey;
        final String fileName;

        private Row(int type, String sectionKey, String makerKey, String fileName) {
            this.type = type;
            this.sectionKey = sectionKey;
            this.makerKey = makerKey;
            this.fileName = fileName;
        }

        static Row header(String sectionKey) { return new Row(VT_HEADER, sectionKey, null, null); }
        static Row item(String sectionKey, String makerKey, String fileName) { return new Row(VT_ITEM, sectionKey, makerKey, fileName); }
    }

    private int logoResId(Context ctx, String makerKey) {
        if (makerKey == null) return android.R.drawable.ic_menu_gallery;
        switch (makerKey) {
            case "hyundai": return R.drawable.img_hyundai_circle;
            case "kia": return R.drawable.img_kia_circle;
            case "genesis": return R.drawable.img_genesis_circle;
            default: return android.R.drawable.ic_menu_gallery;
        }
    }
}