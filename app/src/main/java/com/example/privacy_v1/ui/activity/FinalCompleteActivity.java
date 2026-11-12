package com.example.privacy_v1.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.server.model.MonitoringRequest;
import com.example.privacy_v1.server.model.MonitoringResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
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

public class FinalCompleteActivity extends AppCompatActivity {
    private static final String TAG = "FinalCompleteActivity";
    private static final String JSON_FILE_name = "connected_devices.json";
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;
    private BluetoothManager bluetoothManager;
    private Ringtone alarmRingtone;
    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalcomplete_process);
        Log.d(TAG, "FinalComplete 전달 완료");
        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
        int count = SharedPrefsUtil.getCount(this);
        Log.d(TAG, "Step count:: "+count);
        if (savedInstanceState == null) {
            playAlarmSound();
        }


//        bluetoothManager = new BluetoothManager(this);
//        bluetoothManager.unpairAllBondedDevices();   // ✅ 화면 뜨자마자 페어링 해제



        // Retrofit ApiService 생성
//        apiService = RetrofitClient.getInstance()
//                .create(ApiService.class);

        // 파일에서 값을 읽어오기
//        String fileContent = getnamesFromFile();
//        if (fileContent == null) {
//            Log.e(TAG, "파일을 읽을 수 없거나 값이 없습니다.");
//            return;
//        }

        // TextView에 count 값 표시
        TextView vehi_info_TextView = findViewById(R.id.deleted_vehi_info);
        String vehi_info = tk_prefs.getString("vehicle_number", null);
        vehi_info_TextView.setText(vehi_info);

        //서버에서 받아온 크레딧
        int credit =  tk_prefs.getInt("amount", 0);
        String time = tk_prefs.getString("timestamp", null);
//        LocalDateTime ended_at_now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String ended_at = ended_at_now.format(formatter);
        TextView dateTextView = findViewById(R.id.deleted_date_info);
//        dateTextView.setText("" +ended_at);
        dateTextView.setText("" + time);
        TextView creditTextView = findViewById(R.id.deleted_credit_info);
        creditTextView.setText("" + credit);


        Button final_button = findViewById(R.id.complete_Button);
        final_button.setOnClickListener(v -> {
//           count를 초기화
            SharedPrefsUtil.resetCount(FinalCompleteActivity.this);
            // 기존 사용 MAC 목록도 초기화
            SharedPrefsUtil.resetUsedMacs(FinalCompleteActivity.this);

            checkFileExists();
            // writeFileDirectly 호출
            Set<String> usedMacs = SharedPrefsUtil.getUsedMacs(FinalCompleteActivity.this);

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
                SharedPrefsUtil.addUsedMac(FinalCompleteActivity.this, randomMacAddress);

                unpairAllBluetoothDevices();
                rebootSetting();
            } else {
                Log.e(TAG, "파일 쓰기 실패: " + randomMacAddress);
            }
        });
    }

//        final_button.setOnClickListener(v -> {
////           count를 초기화
//            sharedPreferences.edit().putInt("count", 1).apply();
//            //idle로 상태변경
//            sendIdle();
//            checkFileExists();
//            // writeFileDirectly 호출
//            String randomMacAddress = "11:11:11:11:11:11";
//            if (writeFileDirectly(randomMacAddress)) {
//                Log.d(TAG, "파일 쓰기 성공: " + randomMacAddress);
//                unpairAllBluetoothDevices();
//                rebootSetting();
//            } else {
//                Log.e(TAG, "파일 쓰기 실패: " + randomMacAddress);
//            }
//        });
//    }

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

    public String getnamesFromFile() {
        SharedPreferences ble_prefs = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
        String name = ble_prefs.getString("device_name", null);

        if (name != null) {
            return name;
        } else {
            Log.w(TAG, "No device_name found in SharedPreferences");
            return "";
        }
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
            SharedPreferences ble_prefs = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
            return ble_prefs.contains("device_name") && ble_prefs.contains("device_mac");
        } catch (Exception e) {
            Log.e(TAG, "bluetooth_prefs 파일 확인 중 오류 발생: ", e);
            return false;
        }
    }
    private void clearDevicePrefs() {
        try {
            SharedPreferences ble_prefs = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = ble_prefs.edit();
            editor.remove("device_name");
            editor.remove("device_mac");
            editor.apply();
            Log.d(TAG, "SharedPreferences device_name/device_mac 삭제 완료");
        } catch (Exception e) {
            Log.e(TAG, "SharedPreferences device_name/device_mac 삭제 중 오류 발생: ", e);
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
    private void playAlarmSound() {
        Uri alarmUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
        );

        // 기본 알람음이 없으면 알림음 사용
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION
            );
        }

        alarmRingtone = RingtoneManager.getRingtone(this, alarmUri);

        if (alarmRingtone != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmRingtone.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                                )
                                .build()
                );
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                alarmRingtone.setLooping(false);
            }

            alarmRingtone.play();

            // 5초 후 강제 정지
            handler.postDelayed(this::stopAlarmSound, 3000);
        }
    }

    private void stopAlarmSound() {
        if (alarmRingtone != null && alarmRingtone.isPlaying()) {
            alarmRingtone.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // 다른 화면으로 이동하면 소리 정지
        handler.removeCallbacksAndMessages(null);
        stopAlarmSound();
    }

}
