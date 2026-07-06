//package com.example.tab_credit_v2.ui.activity;
//
//import android.bluetooth.BluetoothAdapter;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.tab_credit_v2.R;
//import com.example.tab_credit_v2.bluetooth.BluetoothManager;
//import com.example.tab_credit_v2.server.network.ApiService;
//import com.example.tab_credit_v2.service.LogMonitoringService;
//
//import java.io.File;
//import java.io.IOException;
//
//public class PrePrivacySelectActivity extends AppCompatActivity {
//    private static final String TAG = "PrePrivacySelectActivity";
//    private Button normal_btn, short_btn;
//    private ApiService apiService;
//    private String vehinum,mode;
//
//    private BluetoothManager bluetoothManager;
//    private BluetoothAdapter bluetoothAdapter;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_select_privacy);
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        normal_btn = findViewById(R.id.normal_button);
//        short_btn = findViewById(R.id.short_button);
//
//        SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
//        vehinum = tk_prefs.getString("vehiNum",null);
//
//
//        normal_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mode ="Normal";
//                SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
//                editor.putString("mode", mode);
//                editor.apply();
//                selectMonitoring();
//                startLogcatRecording(vehinum);
//                Intent intent = new Intent(PrePrivacySelectActivity.this, PrivacyConfirmActivity.class);
//                startActivity(intent);
//            }
//        });
//        short_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mode ="Short";
//                SharedPreferences.Editor editor = getSharedPreferences("tk_prefs", MODE_PRIVATE).edit();
//                editor.putString("mode", mode);
//                editor.apply();
//                selectMonitoring();
//                startLogcatRecording(vehinum);
//                Intent intent = new Intent(PrePrivacySelectActivity.this, PrivacyConfirmActivity.class);
//                startActivity(intent);
//            }
//        });
//    }
//
//    private void selectMonitoring(){
//        Intent serviceIntent = new Intent(this, LogMonitoringService.class);
//        startService(serviceIntent);
//    }
//
//    private void startLogcatRecording(String name) {
//        try {
//            int logIndex = 1;
//            File logFile = new File(getFilesDir(), String.format("%s_PrivacyV1_logcat_%d.txt",name,logIndex));
//            String[] cmd = new String[]{"logcat", "-v", "time", "-f", logFile.getAbsolutePath()};
//            Process process = Runtime.getRuntime().exec(cmd);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }}
//
//
//
//}