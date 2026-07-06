package com.example.privacy_v1.ui.carinfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.common.Crumb;
import com.example.privacy_v1.model.BrandData;
import com.example.privacy_v1.model.Car;
import com.example.privacy_v1.model.CarModel;
import com.example.privacy_v1.model.Keys;
import com.example.privacy_v1.ui.activity.PrivacyStartActivity;
import com.example.privacy_v1.ui.adapter.SimpleTextAdapter;
import com.example.privacy_v1.utils.BrandRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YearSelectActivity extends AppCompatActivity {

    private TextView tvCrumb,select_title;
    private RecyclerView rv;
    private final BrandRepository repo = new BrandRepository();

    private String category, makerName, brandFile, carName, modelName;

    private final List<Integer> years = new ArrayList<>();
    private final List<String> yearStrings = new ArrayList<>();
    private SimpleTextAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_list);

        tvCrumb = findViewById(R.id.tvCrumb);
        select_title = findViewById(R.id.select_title);
        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));

        category = getIntent().getStringExtra(Keys.CATEGORY);
        makerName = getIntent().getStringExtra(Keys.MAKER_NAME);
        brandFile = getIntent().getStringExtra(Keys.BRAND_FILE);
        carName = getIntent().getStringExtra(Keys.CAR_NAME);
        modelName = getIntent().getStringExtra(Keys.MODEL_NAME);

        select_title.setText("연식을 \n선택해주세요");
        tvCrumb.setText(Crumb.build(category, makerName, carName,modelName, null));

        adapter = new SimpleTextAdapter(yearStrings, pos -> {
            int year = years.get(pos);

            SharedPreferences.Editor editor = getSharedPreferences("car_info", MODE_PRIVATE).edit();
            editor.putString("category", category);
            editor.putString("makerName", makerName);
            editor.putString("carName", carName);
            editor.putString("modelName", modelName);
            editor.putInt("year", year);
            editor.apply();

            Intent i = new Intent(this, PrivacyStartActivity.class);
            i.putExtra(Keys.CATEGORY, category);
            i.putExtra(Keys.MAKER_NAME, makerName);
            i.putExtra(Keys.CAR_NAME, carName);
            i.putExtra(Keys.YEAR, year);
            startActivity(i);
            finish();
        });
        rv.setAdapter(adapter);

        try {
            BrandData data = repo.loadBrandData(this, brandFile);
            CarModel model = findModel(data, carName, modelName);

            years.clear();
            yearStrings.clear();

            if (model != null && model.years != null) {
                years.addAll(model.years);
                Collections.sort(years, Collections.reverseOrder());
                for (Integer y : years) yearStrings.add(String.valueOf(y));
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            tvCrumb.setText("연식 로드 실패: " + e.getMessage());
        }
    }

    private CarModel findModel(BrandData data, String carName, String modelName) {
        if (data == null || data.cars == null) return null;
        for (Car c : data.cars) {
            if (carName == null || !carName.equals(c.name)) continue;
            if (c.models == null) return null;
            for (CarModel m : c.models) {
                if (modelName != null && modelName.equals(m.modelname)) return m;
            }
        }
        return null;
    }
}
//public class YearSelectActivity extends AppCompatActivity {
//
//    private TextView tvCrumb;
//    private RecyclerView rv;
//
//    private final BrandRepository repo = new BrandRepository();
//
//    private String category, makerKey, brandFile, carName, modelName;
//
//    private final List<Integer> years = new ArrayList<>();
//    private final List<String> yearStrings = new ArrayList<>();
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
//        carName = getIntent().getStringExtra(Keys.CAR_NAME);
//        modelName = getIntent().getStringExtra(Keys.MODEL_NAME);
//
//        tvCrumb.setText(category + " > " + makerKey + " > " + carName + " > " + modelName);
//
//        try {
//            BrandData data = repo.loadBrandData(this, brandFile);
//            CarModel model = findModel(data, carName, modelName);
//
//            years.clear();
//            yearStrings.clear();
//
//            if (model != null && model.years != null) {
//                years.addAll(model.years);
//                // 최신년도가 위로 오게 정렬(원하면 삭제)
//                Collections.sort(years, Collections.reverseOrder());
//                for (Integer y : years) yearStrings.add(String.valueOf(y));
//            }
//
//            SimpleTextAdapter adapter = new SimpleTextAdapter(yearStrings, pos -> {
//                int year = years.get(pos);
//
//                Intent i = new Intent(this, PrePrivacySelectActivity.class);
//                i.putExtra(Keys.CATEGORY, category);
//                i.putExtra(Keys.MAKER_KEY, makerKey);
//                i.putExtra(Keys.BRAND_FILE, brandFile);
//                i.putExtra(Keys.CAR_NAME, carName);
//                i.putExtra(Keys.MODEL_NAME, modelName);
//                i.putExtra(Keys.YEAR, year);
//                startActivity(i);
//            });
//            rv.setAdapter(adapter);
//
//        } catch (Exception e) {
//            tvCrumb.setText("연식 로드 실패: " + e.getMessage());
//        }
//    }
//
//    private CarModel findModel(BrandData data, String carName, String modelName) {
//        if (data == null || data.cars == null) return null;
//        for (Car c : data.cars) {
//            if (!carName.equals(c.name)) continue;
//            if (c.models == null) return null;
//            for (CarModel m : c.models) {
//                if (modelName.equals(m.modelname)) return m;
//            }
//        }
//        return null;
//    }
//}