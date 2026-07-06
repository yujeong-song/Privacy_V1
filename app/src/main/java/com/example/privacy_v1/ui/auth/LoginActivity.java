package com.example.privacy_v1.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.server.model.LoginRequest;
import com.example.privacy_v1.server.model.LoginResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.activity.CheckingBluetoothActivity;
import com.example.privacy_v1.ui.activity.MainActivity;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.editID);
        etPassword = findViewById(R.id.editPassword);

        // Retrofit ApiService 생성
        apiService = RetrofitClient.getInstance()
                .create(ApiService.class);

        Button loginButton = findViewById(R.id.loginbutton);
        loginButton.setOnClickListener(v -> {
            attemptLogin();
//            startActivity(new Intent(LoginActivity.this, MainActivity.class));
//            finish();
        });

        ImageButton beforeButton = findViewById(R.id.login_before);
        beforeButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, AuthMainActivity.class));
            finish();
        });
    }
    private void attemptLogin() {
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
//        ProgressDialog dialog = new ProgressDialog(this);
//        dialog.setMessage("로그인 중...");
//        dialog.setCancelable(false);
//        dialog.show();
        Call<LoginResponse> call = apiService.login(new LoginRequest(user, pass));
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> res) {
//                dialog.dismiss();
                if (res.isSuccessful() && res.body() != null) {
                    // 1) 서버에서 값 꺼내기
                    String id = res.body().getId(); // getId() 메서드가 LoginResponse에 있어야 함
                    String accessToken = res.body().getAc_token(); // getAccessToken()
                    String refreshToken = res.body().getRe_token(); // getRefreshToken()
                    Log.d(TAG,"res.body ::"+res.body());

                    // 2) SharedPreferences에 저장
                    SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = tk_prefs.edit();
                    editor.putString("device_id", id);
//                    editor.putString("user_id", id);
                    editor.putString("access_token", accessToken);
                    editor.putString("refresh_token", refreshToken);
                    editor.apply();
                    bleConnected();
                } else {
                    Log.e(TAG, "onFailure: 서버응답 오류 ");
                    //다이얼로그 : 서버 응답에 오류가 있습니다. 다시 한번 로그인 해주시고, 문제 있을 시 담당자 연락 바랍니다.
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
//                dialog.dismiss();
                Log.e(TAG, "onFailure: "+ t.getMessage());
                Toast.makeText(LoginActivity.this, "서버에 문제가 있어 다시 로그인 부탁드립니다. 계속되면 담당자 연락 바랍니다.", Toast.LENGTH_SHORT).show();
                //다이얼로그 : 서버 응답에 오류가 있습니다. 다시 한번 로그인 해주시고, 문제 있을 시 담당자 연락 바랍니다.
//                Toast.makeText(LoginActivity.this,
//                        "로그인 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void bleConnected() {
        // 블루투스 연결 흔적 확인
        SharedPreferences ble_prefs = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
        String devicename = ble_prefs.getString("device_name", null);
        String deviceMac = ble_prefs.getString("device_mac", null);

        if (devicename != null && deviceMac != null) {
            // 블루투스 연결 화면으로 이동
            Log.d(TAG, "블루투스 연결한 기록 있어 블루투스연결진행");
            SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
            String vehi_num = tk_prefs.getString("vehicle_number", null);
            SharedPreferences sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
            int currentCount = sharedPreferences.getInt("count", 1);
            startLogcatRecording(currentCount, vehi_num);
            Intent intent = new Intent(LoginActivity.this, CheckingBluetoothActivity.class);
//            Intent intent = new Intent(LoginActivity.this, BluetoothScaningActivity.class);
            startActivity(intent);
            finish();
        } else {
            // SharedPreferences에 값이 없으면 메인 화면 등으로 이동
            Log.d(TAG, "블루투스 연결한적없어 메인이동");
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
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
}