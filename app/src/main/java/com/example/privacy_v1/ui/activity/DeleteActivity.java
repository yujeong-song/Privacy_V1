package com.example.privacy_v1.ui.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.bluetooth.BluetoothStateReceiver;
import com.example.privacy_v1.server.model.UseCreditRequest;
import com.example.privacy_v1.server.model.UseCreditResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.service.DummyDataManager;
import com.example.privacy_v1.ui.adapter.StepProgressAdapter;
import com.example.privacy_v1.ui.dialog.CustomResetDialog;
import com.example.privacy_v1.R;
import com.example.privacy_v1.utils.SharedPrefsUtil;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeleteActivity extends AppCompatActivity {
    private static final String TAG = "DeleteActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ImageView centerIcon;
    private TextView percentText;
    private ImageButton resetButton;
    private Button btn_ex,btn_sms;
    private SharedPreferences tk_prefs;
    private int MAX_COUNT;
    private TextView deletingText;
    private Handler handler = new Handler();
    private boolean isAnimating = true; // 애니메이션 상태를 제어
    private int dotCount = 0; // 점 개수를 세기 위한 변수
    // ✅ 액티비티 전용 리시버(ACL 끊김 감지)
    private BroadcastReceiver aclReceiver;
    private BluetoothStateReceiver bluetoothStateReceiver;

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {

    }

    // 브로드캐스트 리시버 정의
    private BroadcastReceiver patternReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.ACTION_PATTERN_DETECTED".equals(intent.getAction())) {
                // 패턴 감지 시 화면 업데이트
                updateToDeleteCompletedScreen();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_process);
        centerIcon = findViewById(R.id.deleting_icon);
        percentText = findViewById(R.id.percentage_message);
        deletingText = findViewById(R.id.deleting_message);
        startDotAnimation();
//        btn_ex = findViewById(R.id.btn_ex);
//        btn_sms = findViewById(R.id.btn_sms);

        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        StepProgressAdapter stepProgressView =
                findViewById(R.id.stepProgressView);
        int num = tk_prefs.getInt("selected_num",0);
        MAX_COUNT = num;
        int count = SharedPrefsUtil.getCount(this);
        stepProgressView.setStepInfo(num, count);

        // 안드로이드 5.0(Lollipop) 이상에서 Lock Task 모드 시작
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//            // 현재 Lock Task 모드가 실행 중이 아니면 시작
//            if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
//                startLockTask();
//            }
//        }
//        btn_ex.setOnClickListener(view ->{
//            Log.d(TAG, "MAC 생성 횟수 종료. FinalCompleteActivity로 이동");
//            useCredit();
//        });
//        btn_sms.setOnClickListener(view ->{
//            Log.d(TAG, "SMS 생성");
//            createSms();
//        });

        resetButton = findViewById(R.id.reset_menu);
        resetButton.setOnClickListener(view ->{
            showResetDeviceDialog();
        });

        // BluetoothStateReceiver 등록
//        bluetoothStateReceiver = new BluetoothStateReceiver(this);
//        IntentFilter btfilter = new IntentFilter();
//        btfilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); // 연결 끊김 이벤트
//        registerReceiver(bluetoothStateReceiver, btfilter);
        // ✅ ACL 끊김 수신자: 이 화면에서만 사용
        aclReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)
                        || BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // 끊김 감지 → 재연결 화면으로
                    goReconnectionScreen(d);
                }
            }
        };
        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(aclReceiver, btFilter);

        // ✅ 패턴 감지 리시버(기존 유지)
        patternReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if ("com.example.ACTION_PATTERN_DETECTED".equals(intent.getAction())) {
                    updateToDeleteCompletedScreen();
                }
            }
        };

        // 브로드캐스트 리시버 등록
        IntentFilter filter = new IntentFilter("com.example.ACTION_PATTERN_DETECTED");
        registerReceiver(patternReceiver, filter);
    }
//    private void createSms() {
//        Log.d(TAG, "버튼3 클릭 - SMS 생성 시작");
//        executor.execute(() -> {
//            try {
//                Log.d(TAG, "createSms() 호출");
//                DummyDataManager.createSms(
//                        DeleteActivity.this,
//                        10,
//                        (current, total, message) -> runOnUiThread(() -> {
//                        })
//                );
//                Log.d(TAG, "SMS 생성 완료");
//            } catch (Exception e) {
//                Log.e(TAG, "SMS 생성 실패", e);
//            }
//        });
//    }
    private ApiService getApiService() {
        return RetrofitClient.getInstance(this).create(ApiService.class);
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
        String odometer_info = tk_prefs.getString("tot_dist",null);
        String mode_info = tk_prefs.getString("mode",null);

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
                        Log.d(TAG, "시연 종료성공: " + result.getCredit());
                        Log.d(TAG, "시연 종료성공2: " + credit);
                        Intent intent = new Intent(DeleteActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "실패: " + result.getMessage());
                        Intent intent = new Intent(DeleteActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
//                        Toast.makeText(FinalCompleteActivity.this,"실패: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "실패: " + "서버 오류");
                    Intent intent = new Intent(DeleteActivity.this, FinalCompleteActivity.class);
                    startActivity(intent);
                    finish();
//                    Toast.makeText(FinalCompleteActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<UseCreditResponse> call, Throwable t) {
                Log.e(TAG, "실패 네트워크 오류: " + t.getMessage());
                Intent intent = new Intent(DeleteActivity.this, FinalCompleteActivity.class);
                startActivity(intent);
                finish();
//                Toast.makeText(FinalCompleteActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void showResetDeviceDialog() {
        CustomResetDialog resetDialog = new CustomResetDialog(DeleteActivity.this);
        resetDialog.show();
    }

    private void startDotAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAnimating) {
                    // 점 추가
                    String text = "개인정보 삭제 중입니다" + getDots(dotCount);
                    percentText.setText(text);

                    // 점 개수 조정 (최대 3개로 반복)
                    dotCount = (dotCount + 1) % 4;

                    // 반복 호출
                    handler.postDelayed(this, 500); // 500ms 간격
                }
            }
        }, 500); // 초기 딜레이 500ms
    }

    private String getDots(int count) {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < count; i++) {
            dots.append(".");
        }
        return dots.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 브로드캐스트 리시버 해제
        if (bluetoothStateReceiver != null) {
            unregisterReceiver(bluetoothStateReceiver);
        }
        unregisterReceiver(patternReceiver);
        // Lock Task 모드 종료
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            stopLockTask();
//        }
    }

    // 화면을 삭제 완료 상태로 업데이트하는 메서드
    private void updateToDeleteCompletedScreen() {
        isAnimating = false; // 애니메이션 정지
        Intent intent = new Intent(DeleteActivity.this, CompleteActivity.class); // 다음 화면으로 이동
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }

    /** 끊김 감지 시 재연결 화면으로 이동 */
    private void goReconnectionScreen(BluetoothDevice device) {
        // 저장된 MAC 또는 디바이스에서 주소 추출
        String mac = null;
        if (device != null) mac = device.getAddress();
        if (mac == null) {
            // 저장된 값 사용 (없으면 그냥 화면만 전환)
            BluetoothManager mgr = new BluetoothManager(this);
            mac = mgr.getSavedMacAddress();
        }

        // ReConnectionActivity 사용 중이라면:
//        Intent i = new Intent(this, ReConnectionActivity.class);
//        if (mac != null) i.putExtra(ReConnectionActivity.EXTRA_MAC, mac);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(i);

//         또는 단순 재스캔 화면으로 가려면 아래로 교체 가능:
         Intent j = new Intent(this, ReScanActivity.class);
         startActivity(j);

        finish();
    }
}
