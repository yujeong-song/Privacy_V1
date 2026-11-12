package com.example.privacy_v1.ui.dialog;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.example.privacy_v1.R;
import com.example.privacy_v1.server.model.MonitoringRequest;
import com.example.privacy_v1.server.model.MonitoringResponse;
import com.example.privacy_v1.server.model.UseCreditRequest;
import com.example.privacy_v1.server.model.UseCreditResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.activity.CompleteActivity;
import com.example.privacy_v1.ui.activity.FinalCompleteActivity;
import com.example.privacy_v1.utils.SharedPrefsUtil;

import java.io.BufferedReader;
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

public class CustomResetDialog extends Dialog{
    private static final String TAG = "CustomResetDialog";
    private Context context;
    private Button resetButton, cancleButton;
    private SharedPreferences sharedPreferences;

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        dismiss();
    }

    public CustomResetDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_reset_device); // 커스텀 레이아웃 적용
        // Window 설정
        Window window = getWindow();
        if (window != null) {
            window.setLayout(600, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        cancleButton = findViewById(R.id.button_cancel);
        cancleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // 다이얼로그 닫기
            }
        });
        resetButton = findViewById(R.id.button_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPrefsUtil.resetCount(getContext());
                // 기존 사용 MAC 목록도 초기화
                SharedPrefsUtil.resetUsedMacs(getContext());

                checkFileExists();
                useCredit();
                // writeFileDirectly 호출
                Set<String> usedMacs = SharedPrefsUtil.getUsedMacs(getContext());

                String randomMacAddress;
                try {
                    randomMacAddress = generateUniqueRandomMacAddress(usedMacs);
                } catch (Exception e) {
                    Log.e(TAG, "중복 없는 랜덤 MAC 생성 실패", e);
                    return;
                }

                if (writeFileDirectly(randomMacAddress)) {
                    Log.d(TAG, "파일 쓰기 성공: " + randomMacAddress);

                    // 방금 생성한 MAC은 사용 목록에 저장
                    SharedPrefsUtil.addUsedMac(getContext(), randomMacAddress);

                    unpairAllBluetoothDevices();
                    rebootSetting();
                } else {
                    Log.e(TAG, "파일 쓰기 실패: " + randomMacAddress);
                }
//                String randomMacAddress = "11:11:11:11:11:11";
//                if (writeFileDirectly(randomMacAddress)) {
//                    Log.d(TAG, "파일 쓰기 성공: " + randomMacAddress);
//                    SharedPrefsUtil.resetCount(context);
//                    rebootSetting();
//                } else {
//                    Log.e(TAG, "파일 쓰기 실패: " + randomMacAddress);
//                }
                dismiss();
            }
        });
    }
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
    private String generateRandomMacAddress() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] macBytes = new byte[6];
        secureRandom.nextBytes(macBytes);

        // 첫 바이트 보정
        // bit0 = 0 : multicast 아님
        // bit1 = 1 : locally administered
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
    private void useCredit() {
        SharedPreferences tk_prefs = context.getSharedPreferences("tk_prefs", MODE_PRIVATE);
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
//        String use_result = "DONE";
//        // 1. SharedPreferences에서 id 꺼내기
//        String id = tk_prefs.getString("device_id", null);
//        String vehiNum = tk_prefs.getString("vehicle_number", null);
//        String started_at = tk_prefs.getString("started_at", null);
//        String odometer_info = tk_prefs.getString("tot_dist",null);
//        String mode_info = tk_prefs.getString("mode",null);
//        LocalDateTime ended_at_now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String ended_at = ended_at_now.format(formatter);
//        // 2. IdRequest 모델 생성
//        UseCreditRequest useCreditRequest = new UseCreditRequest(id,vehiNum,use_result,started_at,ended_at,odometer_info,mode_info);
        // 3. ApiService 생성 (토큰 포함 Interceptor 붙은 Retrofit 인스턴스 사용)
        ApiService apiService = RetrofitClient.getInstance(context).create(ApiService.class);
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
                        SharedPreferences.Editor editor = context.getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
                        editor.putString("timestamp", time);
                        editor.putInt("amount", credit);
                        editor.apply();
                        Log.d(TAG, "성공: " + result.getCredit());
                        Log.d(TAG, "성공2: " + credit);
                    } else {
                        Log.e(TAG, "실패: " + result.getMessage());
//                        Toast.makeText(FinalCompleteActivity.this,"실패: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "실패: " + "서버 오류");
//                    Toast.makeText(FinalCompleteActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<UseCreditResponse> call, Throwable t) {
                Log.e(TAG, "실패 네트워크 오류: " + t.getMessage());
//                Toast.makeText(FinalCompleteActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkFileExists() {
        // 파일 존재 여부 확인
        boolean isPrefsExist = checkIfPrefsExist();

        if (isPrefsExist) {
            clearDevicePrefs();
        }
    }
    private boolean checkIfPrefsExist() {
        try {
            SharedPreferences ble_prefs = context.getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
            return ble_prefs.contains("device_name") && ble_prefs.contains("device_mac");
        } catch (Exception e) {
            Log.e(TAG, "bluetooth_prefs 파일 확인 중 오류 발생: ", e);
            return false;
        }
    }
    private void clearDevicePrefs() {
        try {
            SharedPreferences ble_prefs =  context.getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
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
}