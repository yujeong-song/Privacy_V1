package com.example.privacy_v1.ui.splash;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.server.model.AutoLoginRequest;
import com.example.privacy_v1.server.model.AutoLoginResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.service.AccEnabler;
import com.example.privacy_v1.service.LogMonitoringService;
import com.example.privacy_v1.service.PairingAndAccessAutoClickService;
import com.example.privacy_v1.ui.activity.CheckingBluetoothActivity;
import com.example.privacy_v1.ui.activity.MainActivity;
import com.example.privacy_v1.ui.auth.AuthMainActivity;
import com.example.privacy_v1.ui.auth.LoginActivity;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 2000;
    private static final String TAG = "SplashActivity";
    private BluetoothManager bluetoothManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
//        startLoading();
//         Retrofit ApiService 생성
        // 로그 모니터링 서비스 시작

        //추후 하드웨어 이벤트 시작 지점?
        boolean launchedFromBoot = getIntent().getBooleanExtra("started_from_boot", false);
        if (launchedFromBoot) {
            Log.i(TAG, "### Auto-launch confirmed: entered SplashActivity ###");
        }
        //
        // ① Android 12+ Bluetooth 권한 체크 (필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        2001
                );
                return; // 권한 없으면 아래 코드 실행 금지
            }
        }
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.unpairAllBondedDevices();   // ✅ 화면 뜨자마자 페어링 해제

        apiService = RetrofitClient.getInstance()
                .create(ApiService.class);

        SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        String refreshToken = tk_prefs.getString("refresh_token", null);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            // refreshToken이 있으면 서버에 전달
            requestNewAccessToken(refreshToken);
        } else {
            // 없으면 로그인 화면으로 이동
            Log.e(TAG,"로그인정보가 없어 로그인이 필요");
            Toast.makeText(SplashActivity.this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SplashActivity.this, AuthMainActivity.class));
            finish();
        }

    }
    @Override
    protected void onResume() {
        super.onResume();

        // 앱이 켜질 때마다 접근성 서비스 강제 ON 시도
        AccEnabler.ensureAccessibilityEnabled(
                this,
                PairingAndAccessAutoClickService.class
        );
    }

    private void requestNewAccessToken(String refreshToken) {
        Call<AutoLoginResponse> call = apiService.autologin(new AutoLoginRequest(refreshToken));
        call.enqueue(new Callback<AutoLoginResponse>() {
            @Override
            public void onResponse(Call<AutoLoginResponse> call, Response<AutoLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 새 토큰 저장
                    String id = response.body().getId(); // getId() 메서드가 LoginResponse에 있어야 함
                    String accessToken = response.body().getAc_token(); // getAccessToken()
                    String refreshToken = response.body().getRe_token(); // getRefreshToken()
                    Log.d(TAG,"res.body ::"+response.body());
                    SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
                    editor.putString("device_id", id);
                    editor.putString("access_token", accessToken);
                    editor.putString("refresh_token", refreshToken);
                    editor.apply();

                    // 로그인 토큰이 있을 경우 블루투스 연결 흔적이 있는지 확인
                    bleConnected();
//                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
//                    finish();
                } else {
                    // 실패 시 로그인 화면
                    Log.e(TAG, "onFailure: 로그아웃 ");
                    Toast.makeText(SplashActivity.this, "로그아웃되어 로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();

                }
            }
            @Override
            public void onFailure(Call<AutoLoginResponse> call, Throwable t) {
                // 네트워크 오류 등
                Log.e(TAG, "onFailure: "+ t.getMessage());
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }

        });
    }
    private void bleConnected() {
        // 블루투스 연결 흔적 확인
        SharedPreferences ble_prefs = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
        String devicename = ble_prefs.getString("device_name", null);
        String deviceMac = ble_prefs.getString("device_mac", null);

        if (devicename != null && deviceMac != null) {
            // 블루투스 check 화면으로 이동
            Log.d(TAG, "블루투스 연결한 기록 있어 블루투스연결진행");
            Intent serviceIntent = new Intent(this, LogMonitoringService.class);
            startService(serviceIntent);
            SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
            String vehi_num = tk_prefs.getString("vehicle_number", null);
            SharedPreferences sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
            int currentCount = sharedPreferences.getInt("count", 1);
            startLogcatRecording(currentCount, vehi_num);
//            Intent intent = new Intent(SplashActivity.this, BluetoothScaningActivity.class);
            Intent intent = new Intent(SplashActivity.this, CheckingBluetoothActivity.class);
            startActivity(intent);
            finish();
        } else {
            // SharedPreferences에 값이 없으면 메인 화면 등으로 이동
            Log.d(TAG, "블루투스 연결한적없어 메인이동");
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
    private void startLogcatRecording(int logIndex, String name) {
        try {
            File logFile = new File(getFilesDir(), String.format("%s_PrivacyV1_logcat_%d.txt",name,logIndex));
            String[] cmd = new String[]{"logcat", "-v", "time", "-f", logFile.getAbsolutePath()};
            Process process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//        private void startLoading() {
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Log.d(TAG,"Application Running.....");
//                    // Splash Screen이 뜨고 나서 실행될 Activity 연결
//                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
//                    finish();
//                }
//            }, SPLASH_DELAY);
//        }

//        new Handler(Looper.getMainLooper()).postDelayed(() -> 2000);

//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            SharedPreferences prefs = getSharedPreferences(
//                    YearSelectionActivity.PREFS_name, MODE_PRIVATE);
//
//            boolean hasHistory = prefs.contains(YearSelectionActivity.KEY_OEM)
//                    && prefs.contains(YearSelectionActivity.KEY_CAR)
//                    && prefs.contains(YearSelectionActivity.KEY_MODEL)
//                    && prefs.contains(YearSelectionActivity.KEY_YEAR);
//
//            Intent it = new Intent(
//                    this,
//                    hasHistory ? PreMainActivity.class : OEMSelectionActivity.class
//            );
//            startActivity(it);
//            finish();
//        }, SPLASH_DELAY);

}