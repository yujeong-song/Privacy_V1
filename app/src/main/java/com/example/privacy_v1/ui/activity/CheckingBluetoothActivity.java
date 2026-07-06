package com.example.privacy_v1.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.server.model.MonitoringRequest;
import com.example.privacy_v1.server.model.MonitoringResponse;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.dialog.CustomResetDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckingBluetoothActivity extends AppCompatActivity {
    // IVN에서 신규추가 버튼을 누르도록 요청
    // Checking 상태 보내주기
    // 버튼을 누른 뒤 블루투스 스캔으로 넘어가기
    private static final String TAG = "CheckingBluetoothActivity";
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checking_bluetooth);
        Button checkingButton = findViewById(R.id.checkingButton);
        tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
        checkingButton.setOnClickListener(v -> {
            startActivity(new Intent(CheckingBluetoothActivity.this, BluetoothScaningActivity.class));
            finish();
        });

        ImageButton resetButton = findViewById(R.id.reset_menu);
        resetButton.setOnClickListener(view ->{
            showResetDeviceDialog();
        });

    }
    private void showResetDeviceDialog() {
        CustomResetDialog resetDialog = new CustomResetDialog(CheckingBluetoothActivity.this);
        resetDialog.show();
    }


}