package com.example.privacy_v1.ui.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.server.model.MonitoringRequest;
import com.example.privacy_v1.server.model.MonitoringResponse;
import com.example.privacy_v1.server.model.StartedRequest;
import com.example.privacy_v1.server.model.StartedResponse;
import com.example.privacy_v1.server.model.UseCreditRequest;
import com.example.privacy_v1.server.model.UseCreditResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.dialog.CustomDeconnectedDialog;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrivacyConfirmActivity extends AppCompatActivity {
    private static final String TAG = "PrivacyConfirmActivity";
    private Button start_btn, token_str, token_end;
    private TextView vehinum, delete_mode, deleted_vehi_maker, deleted_vehi_car, deleted_vehi_model, deleted_vehi_year,odometer_view;
    private String vehinum_info, mode_info,odometer_info;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    // 앱 밖에서 연결되었을 때 MAC만 저장하는 용도 (자동 전환은 옵션)
    private BroadcastReceiver aclReceiver;
    private boolean aclReceiverRegistered = false;
    // 돌아왔을 때 1회 자동 진행을 켜고 싶으면 true (원치 않으면 false 유지)
    private boolean autoAdvanceOnce = false;
    private boolean movingNext = false; // 중복 이동 방지
    private SharedPreferences tk_prefs,car_info;
    private SharedPreferences sharedPreferences;
    private int total_cycles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_confirm);
        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        car_info = getSharedPreferences("car_info",MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothManager = new BluetoothManager(this);

        start_btn = findViewById(R.id.btn_privacy_confirm);
        vehinum = findViewById(R.id.deleted_vehi_info);
        delete_mode = findViewById(R.id.delete_mode_info);
//        token_str = findViewById(R.id.token_str);
//        token_end = findViewById(R.id.token_end);
        odometer_view = findViewById(R.id.deleted_odometer_info);
//        deleted_vehi_maker = findViewById(R.id.deleted_vehi_maker);
//        deleted_vehi_car = findViewById(R.id.deleted_vehi_car);
//        deleted_vehi_model = findViewById(R.id.deleted_vehi_model);
//        deleted_vehi_year = findViewById(R.id.deleted_vehi_year);

        checkBluetoothPermissions();

//        deleted_vehi_maker.setText(car_info.getString("makerName",null));
//        deleted_vehi_car.setText(car_info.getString("carName", null));
//        deleted_vehi_model.setText(car_info.getString("modelName", null));
//        deleted_vehi_year.setText(Integer.toString(car_info.getInt("year", 0)));

        vehinum_info = tk_prefs.getString("vehicle_number", null);
        mode_info = tk_prefs.getString("mode", null);
        odometer_info = tk_prefs.getString("tot_dist", null);

        odometer_view.setText(odometer_info+" km");
        vehinum.setText(vehinum_info);
//        delete_mode.setText(mode_info);
        if(mode_info.equals("Standard")){
            delete_mode.setText("Standard (예상 시간:약 15분)");
        }else if(mode_info.equals("Pro")) {
            delete_mode.setText("Pro (예상 시간:약 30분)");
        }else {
            delete_mode.setText("BMW (예상 시간:약 1시간)");
        }

        // 버튼 눌렀을 때만 링크 상태(BOND/ACL)로 판단
        start_btn.setOnClickListener(v -> findAndSaveMatchingDevices());

//        token_str.setOnClickListener(v -> sendVehiNum(vehinum_info));
//        token_end.setOnClickListener(v -> useCredit());

        // 필요하면 돌아오자마자 자동 진행을 1회 켜고 싶을 때:
        // autoAdvanceOnce = true;
    }

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
            sendVehiNum(vehinum_info);
            moveToDeleteActivity();
        } else {
            Log.d(TAG, "No matching devices found.");
            showNoDeviceDialog();
        }
    }
    private void showNoDeviceDialog() {
        CustomDeconnectedDialog deconnectedDialog = new CustomDeconnectedDialog(PrivacyConfirmActivity.this);
        deconnectedDialog.show();
    }
    private void moveToDeleteActivity() {
        Intent intent = new Intent(PrivacyConfirmActivity.this, DeleteActivity.class); // 다음 화면으로 이동
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }
    private ApiService getApiService() {
        return RetrofitClient.getInstance(this).create(ApiService.class);
    }
    private void sendVehiNum(String vehinum) {
        String device_id = tk_prefs.getString("device_id", null);
        String result = "start";
//        String device_id, String vehicle_number, String version, String manufacturer
//        String avn_version, String country, String odometer
        LocalDateTime started_at_now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String started_at = started_at_now.format(formatter);
        SharedPreferences.Editor editor = tk_prefs.edit();
        editor.putString("started_at", started_at);
        editor.apply();
        StartedRequest startedRequest = new StartedRequest(device_id,vehinum,mode_info,started_at,odometer_info,result);
        ApiService apiService = getApiService();

        apiService.started(startedRequest).enqueue(new Callback<StartedResponse>() {
            @Override
            public void onResponse(Call<StartedResponse> call, Response<StartedResponse> res) {
                if (res.isSuccessful() && res.body() != null) {
                    Log.d(TAG, "Started 성공: " + res.body().getMessage());
                } else {
                    Log.e(TAG, "Started 실패: 서버 오류");
                    Toast.makeText(PrivacyConfirmActivity.this, "서버 오류(Started)", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<StartedResponse> call, Throwable t) {
                Log.e(TAG, "Started 네트워크 오류: " + t.getMessage());
                Toast.makeText(PrivacyConfirmActivity.this, "네트워크 오류(Started): " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void useCredit() {
        String use_result = "DONE";
        // 1. SharedPreferences에서 id 꺼내기
        String id = tk_prefs.getString("device_id", null);
        String vehiNum = tk_prefs.getString("vehicle_number", null);
        String started_at = tk_prefs.getString("started_at", null);
        LocalDateTime ended_at_now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String ended_at = ended_at_now.format(formatter);

        // 2. IdRequest 모델 생성
        UseCreditRequest useCreditRequest = new UseCreditRequest(id,vehiNum,use_result,started_at,ended_at,odometer_info,mode_info);
        // 3. ApiService 생성 (토큰 포함 Interceptor 붙은 Retrofit 인스턴스 사용)
        ApiService apiService = getApiService();
        // 4. 서버로 전송
        apiService.usecredit(useCreditRequest).enqueue(new Callback<UseCreditResponse>() {
            @Override
            public void onResponse(Call<UseCreditResponse> call, Response<UseCreditResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UseCreditResponse result = response.body();
                    int httpCode = response.code();
                    if (httpCode==200) {
                        int credit = result.getCredit(); // getId() 메서드가 LoginResponse에 있어야 함
                        String time = result.getTime(); // getAccessToken()
                        SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
                        editor.putString("timestamp", time);
                        editor.putInt("amount", credit);
                        editor.apply();
                        Log.d(TAG, "성공: " + result.getCredit());
                        Log.d(TAG, "성공2: " + credit);
                        Intent intent = new Intent(PrivacyConfirmActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "실패: " + result.getMessage());
                        Intent intent = new Intent(PrivacyConfirmActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
//                        Toast.makeText(FinalCompleteActivity.this,"실패: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "실패: " + "서버 오류");
                    Intent intent = new Intent(PrivacyConfirmActivity.this, FinalCompleteActivity.class);
                    startActivity(intent);
                    finish();
//                    Toast.makeText(FinalCompleteActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<UseCreditResponse> call, Throwable t) {
                Log.e(TAG, "실패 네트워크 오류: " + t.getMessage());
                Intent intent = new Intent(PrivacyConfirmActivity.this, FinalCompleteActivity.class);
                startActivity(intent);
                finish();
//                Toast.makeText(FinalCompleteActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
