package com.example.privacy_v1.ui.carinfo;

import android.content.Intent;
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
import com.example.privacy_v1.ui.adapter.SimpleTextAdapter;
import com.example.privacy_v1.utils.BrandRepository;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectActivity extends AppCompatActivity {

    private TextView tvCrumb,select_title;
    private RecyclerView rv;
    private final BrandRepository repo = new BrandRepository();

    private String category, makerName, brandFile, carName;
    private final List<String> modelNames = new ArrayList<>();
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

        select_title.setText("세부차량모델을 \n선택해주세요");
        tvCrumb.setText(Crumb.build(category, makerName, carName, null,null));

        adapter = new SimpleTextAdapter(modelNames, pos -> {
            String modelName = modelNames.get(pos);
            Intent i = new Intent(this, YearSelectActivity.class);
            i.putExtra(Keys.CATEGORY, category);
            i.putExtra(Keys.MAKER_NAME, makerName);
            i.putExtra(Keys.BRAND_FILE, brandFile);
            i.putExtra(Keys.CAR_NAME, carName);
            i.putExtra(Keys.MODEL_NAME, modelName);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        try {
            BrandData data = repo.loadBrandData(this, brandFile);
            Car car = findCar(data, carName);

            modelNames.clear();
            if (car != null && car.models != null) {
                for (CarModel m : car.models) modelNames.add(m.modelname);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            tvCrumb.setText("모델 로드 실패: " + e.getMessage());
        }
    }

    private Car findCar(BrandData data, String carName) {
        if (data == null || data.cars == null) return null;
        for (Car c : data.cars) {
            if (carName != null && carName.equals(c.name)) return c;
        }
        return null;
    }
}

//public class ModelSelectActivity extends AppCompatActivity {
//
//    private TextView tvCrumb;
//    private RecyclerView rv;
//
//    private final BrandRepository repo = new BrandRepository();
//
//    private String category, makerKey, brandFile, carName;
//    private final List<String> modelNames = new ArrayList<>();
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
//
//        tvCrumb.setText(category + " > " + makerKey + " > " + carName);
//
//        try {
//            BrandData data = repo.loadBrandData(this, brandFile);
//            Car car = findCar(data, carName);
//
//            modelNames.clear();
//            if (car != null && car.models != null) {
//                for (CarModel m : car.models) modelNames.add(m.modelname);
//            }
//
//            SimpleTextAdapter adapter = new SimpleTextAdapter(modelNames, pos -> {
//                String modelName = modelNames.get(pos);
//
//                Intent i = new Intent(this, YearSelectActivity.class);
//                i.putExtra(Keys.CATEGORY, category);
//                i.putExtra(Keys.MAKER_KEY, makerKey);
//                i.putExtra(Keys.BRAND_FILE, brandFile);
//                i.putExtra(Keys.CAR_NAME, carName);
//                i.putExtra(Keys.MODEL_NAME, modelName);
//                startActivity(i);
//            });
//            rv.setAdapter(adapter);
//
//        } catch (Exception e) {
//            tvCrumb.setText("모델 로드 실패: " + e.getMessage());
//        }
//    }
//
//    private Car findCar(BrandData data, String carName) {
//        if (data == null || data.cars == null) return null;
//        for (Car c : data.cars) {
//            if (carName.equals(c.name)) return c;
//        }
//        return null;
//    }
//}