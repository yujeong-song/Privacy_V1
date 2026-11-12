package com.example.privacy_v1.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
    private String country;
    private int num;
    private ConstraintLayout start_activity;
    private LinearLayout domestic_btn,overseas_btn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_start);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start_activity = findViewById(R.id.start_activity);

        privacy_start = findViewById(R.id.btn_privacy_start);
        etVehiNum = findViewById(R.id.editVehiNum);
        domestic_btn = findViewById(R.id.btn_domestic);
        overseas_btn = findViewById(R.id.btn_overseas);
//        btn_bmw = findViewById(R.id.btn_bmw);
        tot_dist_num = findViewById(R.id.tot_dist);

        start_activity.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideKeyboard(v);
                }
                return false;
            }
        });
        selectDomestic();


        domestic_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDomestic();
                hideKeyboard(v);
            }
        });
        overseas_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overseas_btn.setSelected(true);
                domestic_btn.setSelected(false);
                country ="overseas";
                SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
                editor.putString("country", country);
                editor.apply();
                hideKeyboard(v);
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

//        String vehiNum = etVehiNum.getText().toString().trim();
//        String tot_distNum = tot_dist_num.getText().toString().trim();
//        updateNextButton(false);
//        if (vehiNum.isEmpty() || tot_distNum.isEmpty()) {
//            return;
//        }else {
//            updateNextButton(true);
//        }
        privacy_start.setEnabled(false);
        privacy_start.setAlpha(0.5f); // 비활성 상태가 보이도록 흐리게 표시


        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStartButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etVehiNum.addTextChangedListener(inputWatcher);
        tot_dist_num.addTextChangedListener(inputWatcher);

// 기존 입력값이 있을 수도 있으므로 실제 상태를 다시 확인
        updateStartButtonState();

// 기존 입력값이 있을 때도 활성화 상태를 올바르게 설정
        updateStartButtonState();

        privacy_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (country.equals("domestic")){
                    sendVehiNum();
                    Intent intent = new Intent(PrivacyStartActivity.this, NavSelectActivity.class);
                    startActivity(intent);
                }
                if (country.equals("overseas")){
                    sendVehiNum();
                    Intent intent = new Intent(PrivacyStartActivity.this, NavSelectActivity.class);
                    startActivity(intent);
                }

            }
        });
    }

    private void selectDomestic() {
        domestic_btn.setSelected(true);
        overseas_btn.setSelected(false);

        country = "domestic";

        SharedPreferences.Editor editor =
                getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();

        editor.putString("country", country);
        editor.apply();
    }

    private void updateStartButtonState() {
        boolean ready =
                !etVehiNum.getText().toString().trim().isEmpty()
                        && !tot_dist_num.getText().toString().trim().isEmpty();

        privacy_start.setEnabled(ready);
        privacy_start.setAlpha(ready ? 1.0f : 0.5f);
    }

    private void hideKeyboard(View view) {
        View target = getCurrentFocus();

        if (target == null) {
            target = view;
        }

        if (target == null || target.getWindowToken() == null) {
            return;
        }

        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(
                    target.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS
            );
        }
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

//        Intent intent = new Intent(PrivacyStartActivity.this, NavSelectActivity.class);
//        startActivity(intent);
    }

}