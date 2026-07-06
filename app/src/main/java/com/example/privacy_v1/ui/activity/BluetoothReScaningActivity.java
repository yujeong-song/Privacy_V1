package com.example.privacy_v1.ui.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.bluetooth.BluetoothManager;
import com.example.privacy_v1.ui.dialog.CustomResetDialog;
import com.example.privacy_v1.ui.dialog.CustomRestartDialog;

public class BluetoothReScaningActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothReScaningActivity";

    private Handler handler = new Handler();
    private boolean isAnimating = true;
    private int dotCount = 0;

    private TextView searching_message;
    private ImageButton resetButton;

    private BluetoothManager bluetoothManager;

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // 뒤로가기 막기
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btresearching_process);
        Log.d(TAG, "Bluetooth rescaning...");

        searching_message = findViewById(R.id.searching_message);
        startDotAnimation();

        resetButton = findViewById(R.id.reset_menu);
        resetButton.setOnClickListener(view -> showResetDeviceDialog());

        bluetoothManager = new BluetoothManager(this);

        // BluetoothManager listener 설정
        bluetoothManager.setManagerListener(new BluetoothManager.BluetoothManagerListener() {

            @Override
            public void onDeviceAlreadyRegistered(BluetoothDevice device) {
                Log.d(TAG, "Already bonded (registered): " + device.getName());
                moveToDeleteActivity();
            }

            @Override
            public void onPairingStarted(BluetoothDevice device) {
                Log.d(TAG, "Pairing started: " + device.getName());
            }

            @Override
            public void onDeviceRegistered(BluetoothDevice device) {
                Log.d(TAG, "Pairing completed: " + device.getName());
                moveToDeleteActivity();
            }

            @Override
            public void onPairingFailed(BluetoothDevice device) {
                Log.e(TAG, "Pairing failed.");
                Intent intent = new Intent(BluetoothReScaningActivity.this, ReConnectionActivity.class); // 연결 후 DeleteActivity로 이동
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            }

            @Override
            public void onPairingError(BluetoothDevice device, String message) {
                Log.e(TAG, "Pairing error: " + message);
                showFailDialog();
            }

            @Override
            public void onScanFinished() {
                Log.e(TAG, "Scan finished without finding target → showResetDialog()");
                showRestartDialog();
            }
        });

        // Intent 로부터 MAC 읽어오기
//        String macAddress = getIntent().getStringExtra("MAC_ADDRESS");
        String savedMacAddress = bluetoothManager.getSavedMacAddress();
        if (savedMacAddress != null) {
            Log.d(TAG, "Received MAC Address: " + savedMacAddress);
            startReScanWithManager(savedMacAddress);
        } else {
            Log.e(TAG, "MAC Address is null");
            showFailDialog();
        }
    }

    private void startReScanWithManager(String macAddress) {
        Log.d(TAG, "Start pairing/scan with BluetoothManager. mac=" + macAddress);

        if (!bluetoothManager.isBluetoothSupported()) {
            Log.e(TAG, "Bluetooth is not supported.");
            showFailDialog();
            return;
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            bluetoothManager.requestEnableBluetooth();
            return;
        }

        // 🔹 페어링 또는 BONDED 상태 확인 시작
        bluetoothManager.startPairingByMac(macAddress);
    }

    private void startDotAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAnimating) {
                    String text = "차량 정보 확인 중입니다" + getDots(dotCount);
                    searching_message.setText(text);
                    dotCount = (dotCount + 1) % 4;
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    private String getDots(int count) {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < count; i++) dots.append(".");
        return dots.toString();
    }

    private void showResetDeviceDialog() {
        CustomResetDialog resetDialog = new CustomResetDialog(this);
        resetDialog.show();
    }

    private void showRestartDialog() {
        CustomRestartDialog dialog = new CustomRestartDialog(this);
        dialog.show();
    }

    private void showFailDialog() {
        CustomRestartDialog dialog = new CustomRestartDialog(this);
        dialog.show();
    }

    private void moveToDeleteActivity() {
        Intent intent = new Intent(this, DeleteActivity.class);
        startActivity(intent);
        finish();
    }
}
