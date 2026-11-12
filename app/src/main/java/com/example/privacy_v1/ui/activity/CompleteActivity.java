package com.example.privacy_v1.ui.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.server.model.UseCreditRequest;
import com.example.privacy_v1.server.model.UseCreditResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.utils.SharedPrefsUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompleteActivity extends AppCompatActivity {
    private static final String TAG = "CompleteActivity";
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;
    private int MAX_COUNT;
    private static final String PREF_NAME = "RandomMacPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_process);
        Log.d(TAG, "Complete 전달 완료");

        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);

        int count = SharedPrefsUtil.getCount(this);
        BluetoothManager btManager = new BluetoothManager(this);

        // TextView에 count 값 표시
        TextView countTextView = findViewById(R.id.step_title);
        countTextView.setText("Step " + count +" 삭제 완료");

        int num = tk_prefs.getInt("selected_num",0);
        MAX_COUNT = num;
        TextView stepTextView = findViewById(R.id.deleting_message);
        stepTextView.setText("Step "+count+" 삭제가 완료되었습니다.");

//        TextView stepTextView = findViewById(R.id.step_message);
//        stepTextView.setText("Step  " + count + " / "+ num);

        // ① Android 12+ Bluetooth 권한 체크 (필수)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
//                    != PackageManager.PERMISSION_GRANTED) {
//
//                requestPermissions(
//                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
//                        2001
//                );
//                return; // 권한 없으면 아래 코드 실행 금지
//            }
//        }



        // 2초 딜레이 후 함수 호출
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                randomAddressWithPrefs();
//                randomAddressWithFileCheck("/efs/bluetooth/bt_addr");
            }
        }, 5000);
    }
//    private void randomAddressWithFileCheck(String filePath) {
//        SharedPreferences sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
//        // 파일에서 값을 읽어오기
//        String fileContent = readFileFromRoot(filePath);
//        if (fileContent == null) {
//            Log.e(TAG, "파일을 읽을 수 없거나 값이 없습니다.");
//            return;
//        }
//
//        // 읽은 값에 따라 인덱스 결정
//        int currentIndex;
//        switch (fileContent) {
//            case "11:11:11:11:11:11":
//                currentIndex = 1;
//                break;
//            case "22:22:22:22:22:22":
//                currentIndex = 2;
//                break;
//            case "33:33:33:33:33:33":
//                currentIndex = 3;
//                break;
//            case "44:44:44:44:44:44":
//                currentIndex = 4;
//                break;
//            case "55:55:55:55:55:55":
//                currentIndex = 5;
//                break;
//            case "66:66:66:66:66:66":
//                currentIndex = 0;
//                break;
//            default:
//                Log.e(TAG, "예상치 못한 값: " + fileContent);
//                return;
//        }
//
//        // MAC 주소 설정
//        String randomMacAddress;
//        switch (currentIndex) {
//            case 0:
//                randomMacAddress = "11:11:11:11:11:11";
//                //useCredit
//
//                // 새로운 Activity로 전환
//                useCredit();
//                sendComplete();
//                // 이후 코드를 실행하지 않기 위해 함수 종료
//                return;
//            case 1:
//                randomMacAddress = "22:22:22:22:22:22";
//                break;
//            case 2:
//                randomMacAddress = "33:33:33:33:33:33";
//                break;
//            case 3:
//                randomMacAddress = "44:44:44:44:44:44";
//                break;
//            case 4:
//                randomMacAddress = "55:55:55:55:55:55";
//                break;
//            case 5:
//                randomMacAddress = "66:66:66:66:66:66";
//                break;
//            default:
//                throw new IllegalStateException("Unexpected currentIndex: " + currentIndex);
//        }
//        // count 증가
//        int currentCount = sharedPreferences.getInt("count", 1);
//        sharedPreferences.edit().putInt("count", currentCount + 1).apply();
//
//        // writeFileDirectly 호출
//        if (writeFileDirectly(randomMacAddress)) {
//            Log.d(TAG, "파일 쓰기 성공: " + randomMacAddress);
//
//            rebootSetting();
//        } else {
//            Log.e(TAG, "파일 쓰기 실패: " + randomMacAddress);
//        }
//    }
    private void randomAddressWithPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int currentCount = SharedPrefsUtil.getCount(this);

        // count가 가득 찼으면 종료 화면 이동
        if (currentCount >= MAX_COUNT) {
            Log.d(TAG, "MAC 생성 횟수 종료. FinalCompleteActivity로 이동");
            useCredit();
            Intent intent = new Intent(CompleteActivity.this, FinalCompleteActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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
            SharedPrefsUtil.addUsedMac(this, randomMacAddress);
            // count 증가
            SharedPrefsUtil.setCount(this, currentCount + 1);
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
    private String readFileFromRoot(String filePath) {
        StringBuilder output = new StringBuilder();
        try {
            // 파일이 존재하는지 먼저 확인
            Process checkProcess = Runtime.getRuntime().exec("su -c ls " + filePath);
            BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String checkLine = checkReader.readLine();
            checkReader.close();
            checkProcess.waitFor();

            // 파일이 없을 경우 null 반환
            if (checkLine == null) {
                Log.d(TAG, filePath + " 파일이 존재하지 않습니다.");
                return null;
            }

            // 파일이 존재하면 읽기 시도
            Process process = Runtime.getRuntime().exec("su -c cat " + filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();

            if (process.exitValue() != 0) {
                Log.e(TAG, filePath + " 읽기 실패: 프로세스 종료 코드가 0이 아님.");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, filePath + " 파일을 읽는 중 오류 발생: ", e);
            return null;
        }
        return output.toString().trim();
    }

//    private void checkFileExists() {
//        // JSON 파일 존재 여부 확인
//        // 6개의 맥주소변경을 완료하면
//        boolean isFileExists = checkIfJsonFileExists("connected_devices.json");
//
//        if (isFileExists) {
//            // 파일 삭제
//            deleteJsonFile("connected_devices.json");
//        }
//    }
//    private boolean checkIfJsonFileExists(String filename) {
//        try {
//            File file = new File(getFilesDir(), filename);
//            return file.exists();
//        } catch (Exception e) {
//            Log.e(TAG, "JSON 파일 확인 중 오류 발생: ", e);
//            return false;
//        }
//    }
//    private void deleteJsonFile(String filename) {
//        try {
//            File file = new File(getFilesDir(), filename);
//            if (file.exists() && file.delete()) {
//                Log.d(TAG, "JSON 파일 삭제 성공");
//            } else {
//                Log.e(TAG, "JSON 파일 삭제 실패");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "JSON 파일 삭제 중 오류 발생: ", e);
//        }
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
                        Log.d(TAG, "성공: " + result.getCredit());
                        Log.d(TAG, "성공2: " + credit);
                        Intent intent = new Intent(CompleteActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "실패: " + result.getMessage());
                        Intent intent = new Intent(CompleteActivity.this, FinalCompleteActivity.class);
                        startActivity(intent);
                        finish();
//                        Toast.makeText(FinalCompleteActivity.this,"실패: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "실패: " + "서버 오류");
                    Intent intent = new Intent(CompleteActivity.this, FinalCompleteActivity.class);
                    startActivity(intent);
                    finish();
//                    Toast.makeText(FinalCompleteActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<UseCreditResponse> call, Throwable t) {
                Log.e(TAG, "실패 네트워크 오류: " + t.getMessage());
                Intent intent = new Intent(CompleteActivity.this, FinalCompleteActivity.class);
                startActivity(intent);
                finish();
//                Toast.makeText(FinalCompleteActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean writeFileDirectly(String newValue) {
        try {
            Runtime.getRuntime().exec("su -c setenforce 0");

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
