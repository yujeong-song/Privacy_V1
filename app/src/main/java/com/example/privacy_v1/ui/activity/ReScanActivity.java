package com.example.privacy_v1.ui.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.server.model.MonitoringRequest;
import com.example.privacy_v1.server.model.MonitoringResponse;
import com.example.privacy_v1.server.model.UseCreditRequest;
import com.example.privacy_v1.server.model.UseCreditResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.dialog.CustomRestartDialog;
import com.example.privacy_v1.R;
import com.example.privacy_v1.utils.SharedPrefsUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReScanActivity extends AppCompatActivity {
    private static final String TAG = "ReScanActivity";
    private BluetoothManager bluetoothManager;
    private Button rescan_button;
    private Button complete_button;
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;
    private int MAX_COUNT;
    private static final String PREF_NAME = "RandomMacPrefs";

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // 뒤로가기 동작을 방지합니다.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescan_process);

        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);

        bluetoothManager = new BluetoothManager(this);
        rescan_button = findViewById(R.id.rescan_button);
        complete_button = findViewById(R.id.complete_button);

        String savedMacAddress = bluetoothManager.getSavedMacAddress();

        // 재스캔 버튼 클릭 시
        rescan_button.setOnClickListener(view -> {
            if (savedMacAddress == null) {
                Toast.makeText(this, "저장된 차량 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(ReScanActivity.this, BluetoothReScaningActivity.class); // 연결 후 DeleteActivity로 이동
            startActivity(intent);
            finish(); // 현재 액티비티 종료


//            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//            if (adapter == null || !adapter.isEnabled()) {
//                Toast.makeText(this, "블루투스가 꺼져 있습니다.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // 재연결 시도 (내부 자동 재연결 OFF, 디버그 ON)
//            final boolean autoReconnectEnabled = false;
//            final boolean debug = true;
//
//            // 이전 세션이 존재하면 정리
//            bluetoothManager.stopAutoConnect();
//
//            // 블루투스 연결 시도
//            bluetoothManager.startAutoConnect(savedMacAddress, new BluetoothManager.Ui() {
//                @Override
//                public void showToast(String msg) {
//                    Toast.makeText(ReScanActivity.this, msg, Toast.LENGTH_SHORT).show();
//                }
//
//                @Override
//                public void onConnected(BluetoothDevice device, boolean hfp, boolean a2dp) {
//                    Log.d(TAG, "✅ 재연결 성공: " + safe(device) + " (HFP=" + hfp + ", A2DP=" + a2dp + ")");
//                    moveToDeleteActivity(); // 연결 성공 시 DeleteActivity로 이동
//                }
//
//                @Override
//                public void onBonding(BluetoothDevice device) {
//                    Log.d(TAG, "페어링 진행 중: " + safe(device));
//                }
//
//                @Override
//                public void onFound(BluetoothDevice device) {
//                    Log.d(TAG, "차량 발견: " + safe(device));
//                }
//
//                @Override
//                public void onAttempt(int attempt, int maxAttempts) {
//                    Log.d(TAG, "재시도 " + attempt + "/" + maxAttempts);
//                }
//
//                @Override
//                public void onReconnectScheduled(long delayMs) {
//                    Log.d(TAG, (delayMs / 1000) + "초 후 재시도 예정");
//                }
//
//                @Override
//                public void onFailure(String reason) {
//                    Log.e(TAG, "재연결 실패: " + reason);
//                    showResetDialog(); // 실패 시 리셋 다이얼로그 표시
//                }
//            }, autoReconnectEnabled, debug);
        });

        // 완료 버튼 클릭 시
        complete_button.setOnClickListener(view -> {
            checkFileExists();
            randomAddressWithPrefs();
            useCredit();
        });
    }
//    @Override
//    protected void onPause() {
//        super.onPause();
//        // 화면 이탈 시 스캔/리시버/타이머 정리
//        if (bluetoothManager != null) bluetoothManager.stopAutoConnect();
//    }
    private void randomAddressWithPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int currentCount = SharedPrefsUtil.getCount(this);

        // 사용한 MAC 목록 가져오기
        Set<String> usedMacs = SharedPrefsUtil.getUsedMacs(this);

        // 중복되지 않는 랜덤 MAC 생성
        String randomMacAddress;
        try {
            randomMacAddress = generateUniqueRandomMacAddress(usedMacs);
        } catch (Exception e) {
            Log.e(TAG, "중복되지 않는 랜덤 MAC 생성 실패", e);
            return;
        }

        Log.d(TAG, "생성된 랜덤 MAC: " + randomMacAddress);

        // 파일 쓰기
        if (writeFileDirectly(randomMacAddress)) {
            Log.d(TAG, "파일 쓰기 성공: " + randomMacAddress);
            // 사용한 MAC 저장
            SharedPrefsUtil.resetCount(this);
            // 기존 사용 MAC 목록도 초기화
            SharedPrefsUtil.resetUsedMacs(this);
//            SharedPrefsUtil.addUsedMac(this, randomMacAddress);
//            // count 증가
//            SharedPrefsUtil.setCount(this, currentCount + 1);
            unpairAllBluetoothDevices();
            rebootSetting();
        } else {
            Log.e(TAG, "파일 쓰기 실패: " + randomMacAddress);
        }
    }

    /**
     * 중복되지 않는 실제 랜덤 MAC 생성
     */
    private String generateUniqueRandomMacAddress(Set<String> usedMacs) {
        final int maxRetry = 1000;

        for (int i = 0; i < maxRetry; i++) {
            String mac = generateRandomMacAddress();
            if (!usedMacs.contains(mac)) {
                return mac;
            }
        }

        throw new IllegalStateException("중복되지 않는 MAC 생성 실패");
    }

    /**
     * 실제 랜덤 MAC 생성
     * - unicast
     * - locally administered
     */
    private String generateRandomMacAddress() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] macBytes = new byte[6];
        secureRandom.nextBytes(macBytes);

        // 첫 바이트 조정
        // bit0 = 0 (multicast 아님)
        // bit1 = 1 (locally administered)
        macBytes[0] = (byte) (macBytes[0] & 0xFE);
        macBytes[0] = (byte) (macBytes[0] | 0x02);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < macBytes.length; i++) {
            sb.append(String.format(Locale.US, "%02X", macBytes[i]));
            if (i < macBytes.length - 1) {
                sb.append(":");
            }
        }

        return sb.toString();
    }

    /* ---------------------------------------------------------------------- */
    /* 파일 및 리셋 관련 */

//    private void checkFileExists() {
//        // JSON 파일 존재 여부 확인
//        boolean isFileExists = checkIfJsonFileExists(this, "connected_devices.json");
//
//        if (isFileExists) {
//            // 파일 삭제
//            deleteJsonFile(this, "connected_devices.json");
//        }
//    }
//
//    private boolean checkIfJsonFileExists(Context context, String filename) {
//        try {
//            File file = new File(context.getFilesDir(), filename);
//            return file.exists();
//        } catch (Exception e) {
//            Log.e(TAG, "JSON 파일 확인 중 오류 발생: ", e);
//            return false;
//        }
//    }
//
//    private void deleteJsonFile(Context context, String filename) {
//        try {
//            File file = new File(context.getFilesDir(), filename);
//            if (file.exists() && file.delete()) {
//                Log.d(TAG, "JSON 파일 삭제 성공");
//            } else {
//                Log.e(TAG, "JSON 파일 삭제 실패");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "JSON 파일 삭제 중 오류 발생: ", e);
//        }
//    }
    private void checkFileExists() {
        // 파일 존재 여부 확인
        boolean isPrefsExist = checkIfPrefsExist();

        if (isPrefsExist) {
            clearDevicePrefs();
        }
    }
    private boolean checkIfPrefsExist() {
        try {
            SharedPreferences ble_prefs = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
            return ble_prefs.contains("device_name") && ble_prefs.contains("device_mac");
        } catch (Exception e) {
            Log.e(TAG, "bluetooth_prefs 파일 확인 중 오류 발생: ", e);
            return false;
        }
    }
    private void clearDevicePrefs() {
        try {
            SharedPreferences ble_prefs =  getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = ble_prefs.edit();
            editor.remove("device_name");
            editor.remove("device_mac");
            editor.apply();
            Log.d(TAG, "SharedPreferences device_name/device_mac 삭제 완료");
        } catch (Exception e) {
            Log.e(TAG, "JSON 파일 삭제 중 오류 발생: ", e);
        }
    }

    private boolean writeFileDirectly(String newValue) {
        try {
            // SELinux 모드 변경 시도 (보안에 유의할 것)
            Runtime.getRuntime().exec("su -c setenforce 0");

            // 파일 쓰기 명령 실행
            String command = String.format("su -c echo \"%s\" > /efs/bluetooth/bt_addr", newValue);
            Log.d(TAG, "실행할 명령어: " + command);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (process.exitValue() != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                errorReader.close();
                Log.e(TAG, "명령어 실행 중 오류 발생: " + errorOutput.toString());
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "파일 쓰는 중 오류 발생: ", e);
            return false;
        }
    }

    private void rebootSetting() {
        try {
            Process process = Runtime.getRuntime().exec("su -c reboot");
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "재시작 중 오류 발생: ", e);
        }
    }
    private void unpairAllBluetoothDevices() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "블루투스 어댑터를 찾을 수 없습니다.");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            try {
                Method removeBondMethod = device.getClass().getMethod("removeBond");
                boolean result = (Boolean) removeBondMethod.invoke(device);
                Log.d(TAG, "기기 " + device.getName() + " 페어링 해제 결과: " + result);
            } catch (Exception e) {
                Log.e(TAG, "기기 " + device.getName() + " 페어링 해제 중 오류 발생", e);
            }
        }
    }

    private void moveToDeleteActivity() {
        Intent intent = new Intent(ReScanActivity.this, DeleteActivity.class); // 연결 후 DeleteActivity로 이동
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }

    private void showResetDialog() {
        CustomRestartDialog failDialog = new CustomRestartDialog(ReScanActivity.this);
        failDialog.show();
    }

    private String safe(BluetoothDevice d) {
        try {
            return (d.getName() != null ? d.getName() : d.getAddress());
        } catch (SecurityException se) {
            return d.getAddress();
        }
    }
    private ApiService getApiService() {
        return RetrofitClient.getInstance(this).create(ApiService.class);
    }

    private void useCredit() {
//        String use_result = "DONE";
//        // 1. SharedPreferences에서 id 꺼내기
//        String id = tk_prefs.getString("device_id", null);
//        String vehiNum = tk_prefs.getString("vehicle_number", null);
//        String started_at = tk_prefs.getString("started_at", null);
//        LocalDateTime ended_at_now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String ended_at = ended_at_now.format(formatter);
//        String odometer_info = tk_prefs.getString("tot_dist",null);
//        String mode_info = tk_prefs.getString("mode",null);
//
//        // 2. IdRequest 모델 생성
//        UseCreditRequest useCreditRequest = new UseCreditRequest(id,vehiNum,use_result,started_at,ended_at,odometer_info,mode_info);
        String use_result = "DONE";
        // 1. SharedPreferences에서 id 꺼내기
        String id = tk_prefs.getString("device_id", null);
        String vehiNum = tk_prefs.getString("vehicle_number", null);
        String started_at = tk_prefs.getString("started_at", null);
        LocalDateTime ended_at_now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String ended_at = ended_at_now.format(formatter);
        String odometer_info = tk_prefs.getString("tot_dist",null);
        String mode_info = tk_prefs.getString("version",null);
        String manufacturer = tk_prefs.getString("selected_brand_name",null);
        String avn_version = tk_prefs.getString("selected_nav_name",null);
        String country = tk_prefs.getString("country",null);

        // 2. IdRequest 모델 생성
//        UseCreditRequest(String device_id, String vehicle_number, String use_result, String odometer, String version, String manufacturer, String avn_version, String country, String started_at, String ended_at)
        UseCreditRequest useCreditRequest = new UseCreditRequest(id,vehiNum,use_result,odometer_info,mode_info,manufacturer,avn_version,country, started_at,ended_at);
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
                        Log.d(TAG, "ReScan 완료: " + result.getCredit());
                        Log.d(TAG, "ReScan 완료: " + credit);
                        Intent intent = new Intent(ReScanActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "실패: " + result.getMessage());
                        Intent intent = new Intent(ReScanActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
//                        Toast.makeText(FinalCompleteActivity.this,"실패: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "실패: " + "서버 오류");
                    Intent intent = new Intent(ReScanActivity.this, FinalCompleteActivity.class);
                    startActivity(intent);
                    finish();
//                    Toast.makeText(FinalCompleteActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<UseCreditResponse> call, Throwable t) {
                Log.e(TAG, "실패 네트워크 오류: " + t.getMessage());
                Intent intent = new Intent(ReScanActivity.this, FinalCompleteActivity.class);
                startActivity(intent);
                finish();
//                Toast.makeText(FinalCompleteActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
