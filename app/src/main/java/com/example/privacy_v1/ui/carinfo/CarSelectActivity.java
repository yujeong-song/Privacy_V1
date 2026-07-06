package com.example.privacy_v1.ui.carinfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.common.Crumb;
import com.example.privacy_v1.model.BrandData;
import com.example.privacy_v1.model.Car;
import com.example.privacy_v1.model.Keys;
import com.example.privacy_v1.ui.adapter.SimpleTextAdapter;
import com.example.privacy_v1.utils.BrandRepository;

import java.util.ArrayList;
import java.util.List;

public class CarSelectActivity extends AppCompatActivity {

    private TextView tvCrumb, btn_not_list, select_title;
    private RecyclerView rv;
    private final BrandRepository repo = new BrandRepository();

    private String category, makerName, brandFile;
    private final List<String> carNames = new ArrayList<>();
    private SimpleTextAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_list);

        tvCrumb = findViewById(R.id.tvCrumb);
        select_title = findViewById(R.id.select_title);
        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new GridLayoutManager(this, 2));

        category = getIntent().getStringExtra(Keys.CATEGORY);
        makerName = getIntent().getStringExtra(Keys.MAKER_NAME);
        brandFile = getIntent().getStringExtra(Keys.BRAND_FILE);

        select_title.setText("모델을 \n선택해주세요");

        tvCrumb.setText(Crumb.build(category, makerName, null, null,null));

        adapter = new SimpleTextAdapter(carNames, pos -> {
            String carName = carNames.get(pos);
            Intent i = new Intent(this, ModelSelectActivity.class);
            i.putExtra(Keys.CATEGORY, category);
            i.putExtra(Keys.MAKER_NAME, makerName);
            i.putExtra(Keys.BRAND_FILE, brandFile);
            i.putExtra(Keys.CAR_NAME, carName);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        try {
            BrandData data = repo.loadBrandData(this, brandFile);
            carNames.clear();
            if (data != null && data.cars != null) {
                for (Car c : data.cars) carNames.add(c.name);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            tvCrumb.setText("브랜드 데이터 로드 실패: " + e.getMessage());
        }
    }
}

//public class CarSelectActivity  extends AppCompatActivity {
//
//    private TextView tvCrumb;
//    private RecyclerView rv;
//
//    private final BrandRepository repo = new BrandRepository();
//
//    private String category, makerKey, brandFile;
//    private BrandData brandData;
//
//    private final List<TwoLineAdapter.TwoLineItem> items = new ArrayList<>();
//    private final List<String> carNames = new ArrayList<>(); // 클릭 시 사용할 실제 carName
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_common_list);
//
//        tvCrumb = findViewById(R.id.tvCrumb);
//        rv = findViewById(R.id.rv);
//        rv.setLayoutManager(new LinearLayoutManager(this));
//
//        category = getIntent().getStringExtra(Keys.CATEGORY);
//        makerKey = getIntent().getStringExtra(Keys.MAKER_KEY);
//        brandFile = getIntent().getStringExtra(Keys.BRAND_FILE);
//
//        tvCrumb.setText(category + " > " + makerKey);
//
//        try {
//            brandData = repo.loadBrandData(this, brandFile);
//
//            items.clear();
//            carNames.clear();
//
//            if (brandData.cars != null) {
//                for (Car c : brandData.cars) {
//                    String title = c.name;
//
//                    int modelCount = (c.models == null) ? 0 : c.models.size();
//
//                    int minYear = Integer.MAX_VALUE;
//                    int maxYear = Integer.MIN_VALUE;
//
//                    if (c.models != null) {
//                        for (CarModel m : c.models) {
//                            if (m.years != null && !m.years.isEmpty()) {
//                                int localMin = Collections.min(m.years);
//                                int localMax = Collections.max(m.years);
//                                if (localMin < minYear) minYear = localMin;
//                                if (localMax > maxYear) maxYear = localMax;
//                            }
//                        }
//                    }
//
//                    String yearRange;
//                    if (minYear == Integer.MAX_VALUE) yearRange = "연식 정보 없음";
//                    else yearRange = minYear + " ~ " + maxYear;
//
//                    String subtitle = "모델 " + modelCount + "개 · " + yearRange;
//
//                    items.add(new TwoLineAdapter.TwoLineItem(title, subtitle));
//                    carNames.add(c.name);
//                }
//            }
//
//            TwoLineAdapter adapter = new TwoLineAdapter(items, pos -> {
//                String carName = carNames.get(pos);
//
//                Intent i = new Intent(this, ModelSelectActivity.class);
//                i.putExtra(Keys.CATEGORY, category);
//                i.putExtra(Keys.MAKER_KEY, makerKey);
//                i.putExtra(Keys.BRAND_FILE, brandFile);
//                i.putExtra(Keys.CAR_NAME, carName);
//                startActivity(i);
//            });
//            rv.setAdapter(adapter);
//
//        } catch (Exception e) {
//            tvCrumb.setText("브랜드 데이터 로드 실패: " + e.getMessage());
//        }
//    }
//}