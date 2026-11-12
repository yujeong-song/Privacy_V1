package com.example.privacy_v1.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.navi.BrandMenu;
import com.example.privacy_v1.navi.NavOption;
import com.example.privacy_v1.navi.NavTypeAdapter;
import com.example.privacy_v1.server.model.StartedRequest;
import com.example.privacy_v1.server.model.StartedResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.dialog.CustomDeconnectedDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavSelectActivity  extends AppCompatActivity {
    private static final String TAG = "NavSelectActivity";

    private LinearLayout brandTabsContainer, layoutNavigationHelp;
    private RecyclerView navTypeRecyclerView;
    private Button nextButton;
    private BrandMenu selectedBrand;
    private final List<BrandMenu> brandMenus = new ArrayList<>();
    private final List<View> brandTabViews = new ArrayList<>();

    private NavTypeAdapter navAdapter;
    private NavOption selectedOption;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private SharedPreferences tk_prefs;
    private HorizontalScrollView brandTabScrollView;
    private TextView supportPreparingTextView, nav_title;
    private String country;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_select);

        getWindow().setStatusBarColor(Color.parseColor("#1D1A2D"));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothManager = new BluetoothManager(this);

        nav_title = findViewById(R.id.nav_title);

        brandTabsContainer = findViewById(R.id.brandTabsContainer);
        navTypeRecyclerView = findViewById(R.id.navTypeRecyclerView);
        nextButton = findViewById(R.id.btn_nav_next);
        supportPreparingTextView = findViewById(R.id.supportPreparingTextView);
        layoutNavigationHelp = findViewById(R.id.layoutNavigationHelp);
        checkBluetoothPermissions();
        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        brandTabScrollView =
                findViewById(R.id.brandTabScrollView);
        country =  tk_prefs.getString("country", null);

        setupRecyclerView();
        loadBrandMenus(country);
        /*
         * HorizontalScrollView의 너비가 측정된 다음
         * 제조사 탭 생성
         */
        brandTabScrollView.post(() -> {
            createBrandTabs();

            if (!brandMenus.isEmpty()) {
                selectBrand(0);
            }
        });

//        nextButton.setOnClickListener(view -> {
//            if (selectedOption != null) {
//                executeSelectedAction(selectedOption.getAction());
//            }
//        });
        nextButton.setOnClickListener(view -> {
            if (selectedBrand == null || selectedOption == null) {
                return;
            }
            saveSelectedData();
            findAndSaveMatchingDevices();
        });



        layoutNavigationHelp.setOnClickListener(v -> {
            showManualDialog();
        });
    }

    private void setupRecyclerView() {
        navAdapter = new NavTypeAdapter(option -> {
            selectedOption = option;
//            nextButton.setEnabled(true);
//            nextButton.setAlpha(1.0f);
            updateNextButton(true);
        });

        navTypeRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        navTypeRecyclerView.setAdapter(navAdapter);
//        nextButton.setEnabled(false);
//        nextButton.setAlpha(0.45f);
        updateNextButton(false);
    }
    private void updateNextButton(boolean enabled) {
        nextButton.setEnabled(enabled);
        // 활성화 여부를 좀 더 분명하게 표시
        nextButton.setAlpha(enabled ? 1.0f : 0.55f);
    }

    private void createBrandTabs() {
        brandTabsContainer.removeAllViews();
        brandTabViews.clear();

//        for (int i = 0; i < brandMenus.size(); i++) {
//            BrandMenu brand = brandMenus.get(i);
//
//            View tabView = getLayoutInflater().inflate(
//                    R.layout.item_brand_tab,
//                    brandTabsContainer,
//                    false
//            );
//
//            LinearLayout.LayoutParams params =
//                    new LinearLayout.LayoutParams(
//                            0,
//                            LinearLayout.LayoutParams.MATCH_PARENT,
//                            1f
//                    );
//
//            tabView.setLayoutParams(params);
//
//            ImageView logoImageView =
//                    tabView.findViewById(R.id.brandLogoImageView);
//
//            TextView nameTextView =
//                    tabView.findViewById(R.id.brandNameTextView);
//
//            logoImageView.setImageResource(brand.getTabLogoResId());
//            nameTextView.setText(brand.getName());
//
//            final int tabPosition = i;
//
//            tabView.setOnClickListener(view ->
//                    selectBrand(tabPosition)
//            );
//
//            brandTabsContainer.addView(tabView);
//            brandTabViews.add(tabView);
//        }
//    }
        int brandCount = brandMenus.size();

        if (brandCount == 0) {
            return;
        }

        int availableWidth =
                brandTabScrollView.getWidth();

        for (int i = 0;
             i < brandCount;
             i++) {

            BrandMenu brand =
                    brandMenus.get(i);

            View tabView =
                    getLayoutInflater().inflate(
                            R.layout.item_brand_tab,
                            brandTabsContainer,
                            false
                    );

            int tabWidth;

            if (brandCount <= 3) {
                // 국내 3개는 화면 전체를 균등하게 나눔
                tabWidth =
                        availableWidth / brandCount;
            } else {
                // 해외 탭은 화면에 약 3개씩 표시
                tabWidth = Math.max(
                        dpToPx(115),
                        availableWidth / 3
                );
            }

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            tabWidth,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );

            tabView.setLayoutParams(params);

            ImageView logoImageView =
                    tabView.findViewById(
                            R.id.brandLogoImageView
                    );

            TextView nameTextView =
                    tabView.findViewById(
                            R.id.brandNameTextView
                    );

            logoImageView.setImageResource(
                    brand.getTabLogoResId()
            );

            nameTextView.setText(
                    brand.getName()
            );

            final int tabPosition = i;

            tabView.setOnClickListener(view -> {
                selectBrand(tabPosition);
                scrollToSelectedTab(view);
            });

            brandTabsContainer.addView(tabView);
            brandTabViews.add(tabView);
        }
    }
    private int dpToPx(int dp) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }
    private void scrollToSelectedTab(View selectedTab) {
        selectedTab.post(() -> {
            int scrollX =
                    selectedTab.getLeft()
                            - (brandTabScrollView.getWidth()
                            - selectedTab.getWidth()) / 2;

            brandTabScrollView.smoothScrollTo(
                    Math.max(scrollX, 0),
                    0
            );
        });
    }

//    private void selectBrand(int selectedIndex) {
//        for (int i = 0; i < brandTabViews.size(); i++) {
//            View tabView = brandTabViews.get(i);
//
//            ImageView logoImageView =
//                    tabView.findViewById(R.id.brandLogoImageView);
//
//            TextView nameTextView =
//                    tabView.findViewById(R.id.brandNameTextView);
//
//            View indicator =
//                    tabView.findViewById(R.id.tabIndicator);
//
//            boolean selected = i == selectedIndex;
//
//            nameTextView.setTextColor(
//                    selected
//                            ? Color.WHITE
//                            : Color.parseColor("#9D99A8")
//            );
//
//            logoImageView.setAlpha(selected ? 1.0f : 0.55f);
//
//            indicator.setVisibility(
//                    selected ? View.VISIBLE : View.INVISIBLE
//            );
//        }
//
//        BrandMenu selectedBrand = brandMenus.get(selectedIndex);
//
//        // 브랜드가 바뀌면 기존 선택 초기화
//        selectedOption = null;
//        nextButton.setEnabled(false);
//        nextButton.setAlpha(0.45f);
//        // 상단 브랜드 탭 디자인 변경
//        updateBrandTabSelection(selectedIndex);
//
//        navAdapter.setItems(
//                selectedBrand.getItems(),
//                selectedBrand.getItemLogoResId()
//        );
//    }
    private void selectBrand(int selectedIndex) {
        if (selectedIndex < 0 ||
                selectedIndex >= brandMenus.size()) {
            return;
        }

        // 현재 선택한 브랜드 저장
        selectedBrand = brandMenus.get(selectedIndex);

        // 브랜드가 변경됐으므로 기존 리스트 선택 초기화
        selectedOption = null;

        // 다음 버튼 다시 비활성화
        updateNextButton(false);
        updateBrandTabSelection(selectedIndex);

        List<NavOption> items =
                selectedBrand.getItems();

        boolean hasItems =
                items != null && !items.isEmpty();

        if (hasItems) {
            showSupportedScreen(items);
        } else {
            showPreparingScreen();
        }

//        // 상단 탭 색상 변경
//        for (int i = 0; i < brandTabViews.size(); i++) {
//            View tabView = brandTabViews.get(i);
//
//            ImageView logoImageView =
//                    tabView.findViewById(
//                            R.id.brandLogoImageView
//                    );
//
//            TextView nameTextView =
//                    tabView.findViewById(
//                            R.id.brandNameTextView
//                    );
//
//            View indicator =
//                    tabView.findViewById(
//                            R.id.tabIndicator
//                    );
//
//            boolean isSelected = i == selectedIndex;
//
//            nameTextView.setTextColor(
//                    isSelected
//                            ? Color.WHITE
//                            : Color.parseColor("#9D99A8")
//            );
//
//            logoImageView.setAlpha(
//                    isSelected ? 1.0f : 0.55f
//            );
//
//            indicator.setVisibility(
//                    isSelected
//                            ? View.VISIBLE
//                            : View.INVISIBLE
//            );
//        }

        // 선택한 브랜드의 리스트 표시
        navAdapter.setItems(
                selectedBrand.getItems(),
                selectedBrand.getItemLogoResId()
        );
    }
    private void saveSelectedData() {
        if (selectedBrand == null || selectedOption == null) {
            return;
        }
//        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = tk_prefs.edit();
        // 선택한 브랜드 정보
        editor.putString(
                "selected_brand_id", selectedBrand.getId());
        editor.putString(
                "selected_brand_name", selectedBrand.getName());
        // 선택한 리스트 항목 정보
        editor.putString(
                "selected_nav_name", selectedOption.getName());
        editor.putString(
                "selected_nav_action", selectedOption.getAction());
        editor.putInt(
                "selected_num", selectedOption.getNum());

        editor.apply();
        Log.d(TAG, selectedBrand.getId()+"/"+selectedBrand.getName()+"/"+selectedOption.getName()+"/"+selectedOption.getAction());
        //kia/기아/표준형 5W/Gen5W
    }
    private void updateBrandTabSelection(
            int selectedIndex
    ) {
        for (int i = 0;
             i < brandTabViews.size();
             i++) {

            View tabView = brandTabViews.get(i);

            ImageView logoImageView =
                    tabView.findViewById(
                            R.id.brandLogoImageView
                    );

            TextView nameTextView =
                    tabView.findViewById(
                            R.id.brandNameTextView
                    );

            View indicator =
                    tabView.findViewById(
                            R.id.tabIndicator
                    );

            boolean isSelected =
                    i == selectedIndex;

            logoImageView.setVisibility(View.VISIBLE);

            if (isSelected) {
                nameTextView.setTextColor(Color.WHITE);

                nameTextView.setTypeface(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                );

                // 선택된 이미지는 원본 색상
                logoImageView.clearColorFilter();
                logoImageView.setAlpha(1.0f);

                indicator.setVisibility(View.VISIBLE);

            } else {
                nameTextView.setTextColor(
                        Color.parseColor("#8E8A99")
                );

                nameTextView.setTypeface(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                );

                /*
                 * 원본 명암은 유지하면서 회색으로 변경합니다.
                 * BMW 로고 내부 문양도 유지됩니다.
                 */
                ColorMatrix colorMatrix =
                        new ColorMatrix();

                colorMatrix.setSaturation(0f);

                logoImageView.setColorFilter(
                        new ColorMatrixColorFilter(
                                colorMatrix
                        )
                );

                logoImageView.setAlpha(0.45f);

                indicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showSupportedScreen(
            List<NavOption> items
    ) {
        navTypeRecyclerView.setVisibility(
                View.VISIBLE
        );

        supportPreparingTextView.setVisibility(
                View.GONE
        );

        nextButton.setVisibility(
                View.VISIBLE
        );

        // 새 브랜드를 선택했으므로 다음 버튼 비활성화
        updateNextButton(false);

        navAdapter.setItems(
                items,
                selectedBrand.getItemLogoResId()
        );
    }
    private void showPreparingScreen() {
        selectedOption = null;

        // 이전에 표시되던 리스트 초기화
        navAdapter.setItems(
                new ArrayList<>(),
                0
        );

        navTypeRecyclerView.setVisibility(
                View.GONE
        );

        nextButton.setVisibility(
                View.GONE
        );

        nextButton.setEnabled(false);

        supportPreparingTextView.setVisibility(
                View.VISIBLE
        );
    }

    private void loadBrandMenus(String group) {
        brandMenus.clear();

        String jsonFileName;

        if ("overseas".equals(group)) {
            jsonFileName = "overseas_brand.json";
            nav_title.setText("세부 차종을 \n선택해주세요.");
            layoutNavigationHelp.setVisibility(View.GONE);

        } else {
            jsonFileName = "maker_brand.json";
        }
        try {
            InputStream inputStream =
                    getAssets().open(jsonFileName);


            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            );

            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            reader.close();

            JSONArray brandArray =
                    new JSONArray(jsonBuilder.toString());

            for (int i = 0; i < brandArray.length(); i++) {
                JSONObject brandObject = brandArray.getJSONObject(i);

                String id = brandObject.getString("id");
                String name = brandObject.getString("name");

                // JSON에서 이미지 파일 이름 읽기
                String tabLogoName = brandObject.getString("tabLogo");
//                String itemLogoName = brandObject.getString("itemLogo");
                // 리스트가 없는 제조사는 itemLogo 생략 가능
                String itemLogoName =
                        brandObject.optString(
                                "itemLogo",
                                ""
                        );

                // drawable 파일 이름을 리소스 ID로 변환
                int tabLogoResId = getResources().getIdentifier(
                        tabLogoName,
                        "drawable",
                        getPackageName()
                );
                int itemLogoResId = 0;

//                int itemLogoResId = getResources().getIdentifier(
//                        itemLogoName,
//                        "drawable",
//                        getPackageName()
//                );
                if (!itemLogoName.isEmpty()) {
                    itemLogoResId =
                            getResources().getIdentifier(
                                    itemLogoName,
                                    "drawable",
                                    getPackageName()
                            );
                }

                List<NavOption> options = new ArrayList<>();

                JSONArray itemArray = brandObject.getJSONArray("items");

                for (int j = 0; j < itemArray.length(); j++) {
                    JSONObject itemObject = itemArray.getJSONObject(j);

                    options.add(new NavOption(
                            itemObject.getString("name"),
                            itemObject.getString("action"),
                            itemObject.getInt("num")
                    ));
                }

                // 위에서 선언한 변수를 여기서 사용
                brandMenus.add(new BrandMenu(
                        id,
                        name,
                        tabLogoResId,
                        itemLogoResId,
                        options
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(
                    this,
                    "차량 정보를 불러오지 못했습니다.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * JSON의 action 값에 따라 서로 다른 함수를 실행합니다.
     */
//
//    private void startCcnc() {
//        Toast.makeText(this, "현대 CCNC 실행", Toast.LENGTH_SHORT).show();
//    }
//    private void startGenesisHigh6() {
//        Toast.makeText(this, "제네시스 CCIC 실행", Toast.LENGTH_SHORT).show();
//    }
    @Override
    protected void onResume() {
        super.onResume();
        // 블루투스 연결 상태 재확인
        if (bluetoothManager == null) {
            bluetoothManager = new BluetoothManager(this);
        }
        List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices();
        if (!connectedDevices.isEmpty()) {
            findAndSaveMatchingDevices();
        }
    }

    private void findAndSaveMatchingDevices() {
        // Step 2: 현재 연결된 장치 확인 및 필터링
        List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices();
        if (connectedDevices.isEmpty()) {
            // 연결된 기기가 없을 때
            Log.d(TAG, "No connected devices found.");
            showNoDeviceDialog();
            return;
        }
        List<BluetoothDevice> matchingDevices = new ArrayList<>();
        for (BluetoothDevice device : connectedDevices) {
            if (bluetoothManager.isCarOrAudioDevice(device)) {
                matchingDevices.add(device);
            }
        }
        if (!matchingDevices.isEmpty()) {
            // Step 3: 파일에 저장
//            bluetoothManager.saveConnectedDevices(matchingDevices);
            bluetoothManager.saveConnectedDevices(matchingDevices);
            String vehinum_info = tk_prefs.getString("vehicle_number", null);
            sendVehiNum(vehinum_info);
            moveToDeleteActivity();
        } else {
            Log.d(TAG, "No matching devices found.");
            showNoDeviceDialog();
        }
    }
    private void showNoDeviceDialog() {
        CustomDeconnectedDialog deconnectedDialog = new CustomDeconnectedDialog(NavSelectActivity.this);
        deconnectedDialog.show();
    }
    private void moveToDeleteActivity() {
        Intent intent = new Intent(NavSelectActivity.this, DeleteActivity.class); // 다음 화면으로 이동
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }
    private ApiService getApiService() {
        return RetrofitClient.getInstance(this).create(ApiService.class);
    }
    private void sendVehiNum(String vehinum) {
        String device_id = tk_prefs.getString("device_id", null);
        String result = "start";
        String odometer_info = tk_prefs.getString("tot_dist", null);
//        String device_id, String vehicle_number, String version, String manufacturer
//        String avn_version, String country, String odometer
        String version = "standard";
        String manufacturer =  selectedBrand.getName();
        String avn_version =  selectedOption.getName();
//        String country = "domestic";
        LocalDateTime started_at_now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String started_at = started_at_now.format(formatter);
        SharedPreferences.Editor editor = tk_prefs.edit();
        editor.putString("version", version);
        editor.putString("started_at", started_at);
        editor.apply();
//        StartedRequest(device_id, vehinum,version, manufacturer, avn_version, country,odometer_info,started_at, result);
        StartedRequest startedRequest = new StartedRequest(device_id, vehinum,version, manufacturer, avn_version, country,odometer_info,started_at, result);
        ApiService apiService = getApiService();

        apiService.started(startedRequest).enqueue(new Callback<StartedResponse>() {
            @Override
            public void onResponse(Call<StartedResponse> call, Response<StartedResponse> res) {
                if (res.isSuccessful() && res.body() != null) {
                    Log.d(TAG, "Started 성공: " + res.body().getMessage());
                } else {
                    Log.e(TAG, "Started 실패: 서버 오류");
                    Toast.makeText(NavSelectActivity.this, "서버 오류(Started)", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<StartedResponse> call, Throwable t) {
                Log.e(TAG, "Started 네트워크 오류: " + t.getMessage());
                Toast.makeText(NavSelectActivity.this, "네트워크 오류(Started): " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showManualDialog() {
        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_manual, null);

        ImageButton btnClose =
                dialogView.findViewById(R.id.btnManualClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Window window = dialog.getWindow();

        if (window != null) {
            int screenWidth = getResources()
                    .getDisplayMetrics().widthPixels;
            int screenHeight = getResources()
                    .getDisplayMetrics().heightPixels;

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (screenWidth * 0.90f);
            params.height = (int) (screenHeight * 0.85f);
            window.setAttributes(params);

            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /* ---------------- 권한 (프로젝트 정책 유지) ---------------- */
    private void checkBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1001);
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            }
        }
    }
}