package com.example.privacy_v1.ui.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.ui.dialog.CustomResetDialog;
import com.example.privacy_v1.ui.dialog.CustomRestartDialog;

public class BluetoothScaningActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothScaningActivity";

    private Handler handler = new Handler();
    private boolean isAnimating = true; // 애니메이션 상태를 제어
    private int dotCount = 0; // 점 개수를 세기 위한 변수
    private TextView searching_message;
    private BluetoothManager bluetoothManager;
    private ImageButton resetButton;

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // 뒤로가기 막기
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btsearching_process);
        Log.d(TAG, "Bluetooth scanning...");

        searching_message = findViewById(R.id.searching_message);
        startDotAnimation();

        resetButton = findViewById(R.id.reset_menu);
        resetButton.setOnClickListener(view -> showResetDeviceDialog());

        

        bluetoothManager = new BluetoothManager(this);

        // 1) BluetoothManager 리스너 설정 (페어링 결과/스캔 결과 처리)
        bluetoothManager.setManagerListener(new BluetoothManager.BluetoothManagerListener() {
            @Override
            public void onNeedEnableBluetooth() {
                Toast.makeText(BluetoothScaningActivity.this,
                        "블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceAlreadyRegistered(BluetoothDevice device) {
                // 이미 BONDED 상태인 경우 = 성공으로 간주하고 다음 화면으로
                Log.d(TAG, "onDeviceAlreadyRegistered: " + device.getName());
                moveToDeleteActivity();
            }

            @Override
            public void onPairingStarted(BluetoothDevice device) {
                Log.d(TAG, "onPairingStarted: " + device.getName());
                // 페어링 진행 중 - 필요하면 안내 문구 등을 보여줄 수 있음
            }

            @Override
            public void onDeviceRegistered(BluetoothDevice device) {
                // BOND_BONDED == 페어링(등록) 완료
                Log.d(TAG, "onDeviceRegistered (bonded): " + device.getName());
                moveToDeleteActivity();
            }

            @Override
            public void onPairingFailed(BluetoothDevice device) {
                Log.e(TAG, "onPairingFailed: " +
                        (device != null ? device.getName() : "null"));
                Toast.makeText(BluetoothScaningActivity.this,
                        "블루투스 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
//                showFailDialog();
                Intent intent = new Intent(BluetoothScaningActivity.this, ReScanActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onPairingError(BluetoothDevice device, String message) {
                Log.e(TAG, "onPairingError: " + message);
                Toast.makeText(BluetoothScaningActivity.this,
                        "블루투스 오류: " + message, Toast.LENGTH_SHORT).show();
                showFailDialog();
            }

            @Override
            public void onScanFinished() {
                // 스캔이 끝났는데 타겟 기기를 못 찾은 경우 = 타임아웃/재검색
                Log.d(TAG, "onScanFinished: target device not found -> ReScanActivity");
                Intent intent = new Intent(BluetoothScaningActivity.this, ReScanActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 2) bluetooth_prefs 에 저장된 MAC 가져오기
        String mac = bluetoothManager.getSavedMacAddress();
        if (mac != null) {
            Log.d(TAG, "Received MAC Address from bluetooth_prefs: " + mac);
            connectWithBluetoothManager(mac);
        } else {
            Log.e(TAG, "MAC Address is null in bluetooth_prefs");
            // 블루투스 재 연결이 필요하므로 설정 창으로 이동? 다이얼로그 띄우기?
            // showFailDialog();
        }
    }

    private void showResetDeviceDialog() {
        CustomResetDialog resetDialog = new CustomResetDialog(BluetoothScaningActivity.this);
        resetDialog.show();
    }

    private void startDotAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAnimating) {
                    String text = "차량 정보 확인 중입니다" + getDots(dotCount);
                    searching_message.setText(text);

                    dotCount = (dotCount + 1) % 4;
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

    private void connectWithBluetoothManager(String macAddress) {
        String callingActivity = getClass().getSimpleName();
        Log.d(TAG, "connectWithBluetoothManager called from: " + callingActivity
                + ", mac=" + macAddress);

        if (!bluetoothManager.isBluetoothSupported()) {
            Toast.makeText(this, "이 기기는 블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            showFailDialog();
            return;
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            bluetoothManager.requestEnableBluetooth();
            // 사용자가 직접 켜고 다시 돌아오면 onCreate/onResume 에서 재시도하는 구조로 가도 됨
            return;
        }

        // 🔹 실제 페어링/등록 절차 시작 (내부에서 BluetoothConnector 사용)
        bluetoothManager.startPairingByMac(macAddress);
    }

//    private void showResetDialog() {
//        CustomRestartDialog failDialog = new CustomRestartDialog(BluetoothScaningActivity.this);
//        failDialog.show();
//    }

    private void showFailDialog() {
        CustomRestartDialog failDialog = new CustomRestartDialog(BluetoothScaningActivity.this);
        failDialog.show();
    }

    private void moveToDeleteActivity() {
        Intent intent = new Intent(BluetoothScaningActivity.this, DeleteActivity.class);
        startActivity(intent);
        finish();
    }
}
