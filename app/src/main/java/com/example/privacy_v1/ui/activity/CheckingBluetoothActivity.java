package com.example.privacy_v1.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private Ringtone alarmRingtone;
    private final Handler handler =
            new Handler(Looper.getMainLooper());
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checking_bluetooth);

        if (savedInstanceState == null) {
            playAlarmSound();
        }

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
    private void showResetDeviceDialog() {
        CustomResetDialog resetDialog = new CustomResetDialog(CheckingBluetoothActivity.this);
        resetDialog.show();
    }
}