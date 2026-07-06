package com.example.privacy_v1.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.privacy_v1.R;
import com.example.privacy_v1.service.LogMonitoringService;

import java.io.IOException;
import java.io.File;

public class PrivacyStartActivity extends AppCompatActivity {
    private static final String TAG = "PrivacyStartActivity";
    private Button privacy_start;
    private BluetoothAdapter bluetoothAdapter;
    private EditText etVehiNum, tot_dist_num;
    private String mode;
    private int num;
    private ConstraintLayout start_activity;
    private LinearLayout pro_btn,standard_btn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_start);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start_activity = findViewById(R.id.start_activity);

        privacy_start = findViewById(R.id.btn_privacy_start);
        etVehiNum = findViewById(R.id.editVehiNum);
        pro_btn = findViewById(R.id.btn_pro);
        standard_btn = findViewById(R.id.btn_standard);
//        btn_bmw = findViewById(R.id.btn_bmw);
        tot_dist_num = findViewById(R.id.tot_dist);

        start_activity.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                hideKeyboard();
                return false;
            }
        });

        pro_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pro_btn.setSelected(true);
                standard_btn.setSelected(false);
                mode ="Pro";
                num = 6;
                SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
                editor.putString("mode", mode);
                editor.putInt("num", num);
                editor.apply();
                hideKeyboard();
            }
        });
        standard_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                standard_btn.setSelected(true);
                pro_btn.setSelected(false);
                mode ="Standard";
                num = 6;
                SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
                editor.putString("mode", mode);
                editor.putInt("num", num);
                editor.apply();
                hideKeyboard();
            }
        });
//        btn_bmw.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mode ="bmw";
//                num=20;
//                SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
//                editor.putString("mode", mode);
//                editor.putInt("num", num);
//                editor.apply();
//                sendVehiNum();
//            }
//        });

        privacy_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVehiNum();
            }
        });
    }

    void hideKeyboard()
    {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void selectMonitoring(){
        Intent serviceIntent = new Intent(this, LogMonitoringService.class);
        startService(serviceIntent);
    }

    private void startLogcatRecording(String name) {
        try {
            int logIndex = 1;
            File logFile = new File(getFilesDir(), String.format("%s_PrivacyV1_logcat_%d.txt",name,logIndex));
            String[] cmd = new String[]{"logcat", "-v", "time", "-f", logFile.getAbsolutePath()};
            Process process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }}
    private void sendVehiNum(){
        String vehiNum = etVehiNum.getText().toString().trim();
        String tot_distNum = tot_dist_num.getText().toString().trim();
        selectMonitoring();
        startLogcatRecording(vehiNum);
        if (vehiNum.isEmpty()) {
            Toast.makeText(this, "차량 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tot_distNum.isEmpty()) {
            Toast.makeText(this, "총 주행거리를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 2) SharedPreferences에 저장
        SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = tk_prefs.edit();
        editor.putString("vehicle_number", vehiNum);
        editor.putString("tot_dist", tot_distNum);
        editor.apply();

        Intent intent = new Intent(PrivacyStartActivity.this, PrivacyConfirmActivity.class);
        startActivity(intent);
    }

}