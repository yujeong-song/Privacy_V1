package com.example.privacy_v1.ui.carinfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.model.Keys;
import com.example.privacy_v1.model.MakersMap;
import com.example.privacy_v1.ui.adapter.MakerSectionAdapter;
import com.example.privacy_v1.utils.BrandRepository;

import java.util.LinkedHashMap;

public class MakerSelectActivity  extends AppCompatActivity {

    private TextView tvCrumb;
    private RecyclerView rv;

    private final BrandRepository repo = new BrandRepository();

    // ✅ 필드로 선언
    private MakerSectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_list);

        tvCrumb = findViewById(R.id.tvCrumb);
        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        tvCrumb.setVisibility(RecyclerView.GONE);

//        tvCrumb.setText("제조사를 \n 선택해주세요");
//        tvCrumb.setTextSize(24);

        try {
            MakersMap makers = repo.loadMakers(this);

            LinkedHashMap<String, LinkedHashMap<String, String>> sectionMap = new LinkedHashMap<>();
            if (makers.국내 != null) sectionMap.put("국내", new LinkedHashMap<>(makers.국내));
            if (makers.해외 != null) sectionMap.put("해외", new LinkedHashMap<>(makers.해외));
            if (makers.기타 != null) sectionMap.put("기타", new LinkedHashMap<>(makers.기타));

            // ✅ 필드에 대입
            adapter = new MakerSectionAdapter(sectionMap, new MakerSectionAdapter.Listener() {
                @Override
                public void onToggleSection(String sectionKey) {
                    // ✅ 필드 adapter 사용
                    if (MakerSelectActivity.this.adapter == null) return;
                    MakerSelectActivity.this.adapter.setExpanded(
                            sectionKey,
                            !MakerSelectActivity.this.adapter.isExpanded(sectionKey)
                    );
                }

                @Override
                public void onClickMaker(String sectionKey, String makerKey, String makerName, String fileName) {
                    Intent i = new Intent(MakerSelectActivity.this, CarSelectActivity.class);
                    i.putExtra(Keys.CATEGORY, sectionKey);
                    i.putExtra(Keys.MAKER_KEY, makerKey);
                    i.putExtra(Keys.MAKER_NAME, makerName);
                    i.putExtra(Keys.BRAND_FILE, fileName);
                    startActivity(i);
                }
            });

            adapter.setExpanded("국내", true);
            adapter.setExpanded("해외", false);
            adapter.setExpanded("기타", false);

            rv.setAdapter(adapter);

        } catch (Exception e) {
            tvCrumb.setText("makers.json 로드 실패: " + e.getMessage());
        }
    }
}

//            Map<String, String> map = getCategoryMap(makers, category);
//            makerKeys.clear();
//            makerFiles.clear();
//
//            if (map != null) {
//                for (Map.Entry<String, String> e : map.entrySet()) {
//                    makerKeys.add(e.getKey());     // ex) hyundai
//                    makerFiles.add(e.getValue());  // ex) hyundai_data.json
//                }
//            }
//
//            SimpleTextAdapter adapter = new SimpleTextAdapter(makerKeys, pos -> {
//                String makerKey = makerKeys.get(pos);
//                String brandFile = makerFiles.get(pos);
//
//                Intent i = new Intent(this, CarSelectActivity.class);
//                i.putExtra(Keys.CATEGORY, category);
//                i.putExtra(Keys.MAKER_KEY, makerKey);
//                i.putExtra(Keys.BRAND_FILE, brandFile);
//                startActivity(i);
//            });
//            rv.setAdapter(adapter);
//
//        } catch (Exception e) {
//            tvCrumb.setText("makers.json 로드 실패: " + e.getMessage());
//        }
//    }
//
//    private Map<String, String> getCategoryMap(MakersMap makers, String category) {
//        if ("국내".equals(category)) return makers.국내;
//        if ("해외".equals(category)) return makers.해외;
//        if ("기타".equals(category)) return makers.기타;
//        return null;
//    }
//}