package com.example.privacy_v1.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.bluetooth.BluetoothManager;

import com.example.privacy_v1.R;

public class                                 ReConnectionActivity extends AppCompatActivity {
    private static final String TAG = "ReConnectionActivity";
    private BluetoothManager bluetoothManager;
    private Button reconnectionButton;
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reconnection_process);
        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
        reconnectionButton = findViewById(R.id.reconnectionButton);
        bluetoothManager = new BluetoothManager(this);
        String savedMacAddress = bluetoothManager.getSavedMacAddress();

        reconnectionButton.setOnClickListener(view -> {
            //버튼 누르고 ReScaning으로 넘기기
            Intent intent = new Intent(ReConnectionActivity.this, BluetoothReScaningActivity.class);
            startActivity(intent);
            finish();
//            connectWithBluetoothConnector(savedMacAddress);
        });
    }

//    private void connectWithBluetoothConnector(String macAddress) {
//        // BluetoothConnector를 사용하여 연결 시도
//        BluetoothConnector bluetoothConnector = new BluetoothConnector(this, macAddress);
//        bluetoothConnector.setConnectionListener(new BluetoothConnector.BluetoothConnectionListener() {
//            @Override
//            public void onConnected(BluetoothDevice device) {
//                Log.d(TAG, "Connected to device: " + device.getName());
//                moveToDeleteActivity();
//            }
//            @Override
//            public void onConnectionFailed(BluetoothDevice device) {
//                Log.e(TAG, "Connection failed.");
//                Toast.makeText(ReConnectionActivity.this, "Connection failed.", Toast.LENGTH_SHORT).show();
//                showResetDialog();
//            }
//
//            @Override
//            public void onConnectionTimeout(BluetoothDevice device) {
//                Log.e(TAG, "Connection failed.");
//                Toast.makeText(ReConnectionActivity.this, "Connection failed.", Toast.LENGTH_SHORT).show();
//                showResetDialog();
//            }
//        });
//        bluetoothConnector.startScanning();
//    }
//    private void moveToDeleteActivity() {
//        Intent intent = new Intent(ReConnectionActivity.this, DeleteActivity.class); // 다음 화면으로 이동
//        startActivity(intent);
//        finish(); // 현재 액티비티 종료
//    }
//    private void showResetDialog() {
//        CustomRestartDialog failDialog = new CustomRestartDialog(ReConnectionActivity.this);
//        failDialog.show();
//    }
}
