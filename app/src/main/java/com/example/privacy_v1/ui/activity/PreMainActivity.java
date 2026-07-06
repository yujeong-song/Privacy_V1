//package com.example.tab_credit_v1_02.ui.activity;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.Manifest;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.TextView;
//
//import com.example.tab_credit_v1_02.bluetooth.BluetoothManager;
//import com.example.tab_credit_v1_02.ui.dialog.CustomDeconnectedDialog;
//import com.example.tab_credit_v1_02.ui.dialog.CustomResetDialog;
//import com.example.tab_credit_v1_02.R;
//import com.example.tab_credit_v1_02.service.LogMonitoringService;
//import com.example.tab_credit_v1_02.utils.SharedPrefsUtil;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//public class PreMainActivity extends AppCompatActivity {
//    private static final String TAG = "PreMainActivity";
//    private static final String JSON_FILE_name = "connected_devices.json";
//    private BluetoothManager bluetoothManager;
//    private BluetoothAdapter bluetoothAdapter;
//    private Button startButton;
//    private ImageButton resetButton;
//    private boolean isMatchingDeviceFound;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_premain);
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        startButton = findViewById(R.id.startButton);
//
//        // 앱이 시작될 때 로그 모니터링 서비스 시작
//        Intent serviceIntent = new Intent(this, LogMonitoringService.class);
//        startService(serviceIntent);
//
//        checkBluetoothPermissions();
//
//        bluetoothManager = new BluetoothManager(this);
//        File file = new File(getFilesDir(), JSON_FILE_name);
//
////        if (!file.exists()) {
////            List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices();
////            if (connectedDevices.isEmpty()) {
////                // 연결된 기기가 없을 때
////                Log.d(TAG, "No connected devices found.");
////                showNoDeviceDialog();
////            }
////        }
//
//        resetButton = findViewById(R.id.reset_menu);
//        resetButton.setOnClickListener(view ->{
//            showResetDeviceDialog();
//        });
//        String savedMacAddress = bluetoothManager.getSavedMacAddress();
//        startButton.setOnClickListener(view -> {
//            List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices();
//            if (!connectedDevices.isEmpty()) {
//                // 연결된 기기가 있을 때
//                for (BluetoothDevice device : connectedDevices) {
//                    isMatchingDeviceFound = false;
//                    if (savedMacAddress != null) {
//                        if (device.getAddress().equals(savedMacAddress)) {
//                            Log.d(TAG, "Matching device found: " + device.getName() + " [" + device.getAddress() + "]");
//                            isMatchingDeviceFound = true;
//                            break;
//                        }
//                        connectWithBluetoothConnector(savedMacAddress);
//                    }else{
//                        Log.d(TAG, "No JSON file found. Checking connected devices...");
//                        findAndSaveMatchingDevices();
//                    }
//                }
//            }else {
//                // Step 1: Check if MAC address file exists
//                if (savedMacAddress != null) {
//                    Log.d(TAG, "Saved MAC address found: " + savedMacAddress);
//                    // Step 2: BluetoothConnector를 사용하여 Bluetooth 연결 시도
//                    connectWithBluetoothConnector(savedMacAddress);
//                } else {
//                    Log.d(TAG, "No JSON file found. Checking connected devices...");
//                    findAndSaveMatchingDevices();
//                }
//            }
//        });
//    }
//
//    private void checkBluetoothPermissions() {
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                requestPermissions(new String[]{
//                        android.Manifest.permission.BLUETOOTH_SCAN,
//                        android.Manifest.permission.BLUETOOTH_CONNECT,
//                        android.Manifest.permission.ACCESS_FINE_LOCATION
//                }, 1001);
//            }
//        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
//            }
//        }
//    }
//    private void connectWithBluetoothConnector(String macAddress) {
//        Intent intent = new Intent(PreMainActivity.this, BluetoothScaningActivity.class); // 다음 화면으로 이동
//        intent.putExtra("MAC_ADDRESS", macAddress); // String 인자 추가
//        startActivity(intent);
//        finish(); // 현재 액티비티 종료
//    }
//
//    private void findAndSaveMatchingDevices() {
//        // Step 2: 현재 연결된 장치 확인 및 필터링
//        List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices();
//        if (connectedDevices.isEmpty()) {
//            // 연결된 기기가 없을 때
//            Log.d(TAG, "No connected devices found.");
//            showNoDeviceDialog();
//            return;
//        }
//        List<BluetoothDevice> matchingDevices = new ArrayList<>();
//        for (BluetoothDevice device : connectedDevices) {
//            if (bluetoothManager.isDeviceMatchingCategory(device)) {
//                matchingDevices.add(device);
//            }
//        }
//        if (!matchingDevices.isEmpty()) {
//            // Step 3: JSON 파일에 저장
//            bluetoothManager.saveConnectedDevices(matchingDevices);
//            moveToDeleteActivity();
//        } else {
//            Log.d(TAG, "No matching devices found.");
//            showNoDeviceDialog();
//        }
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // SharedPreferences에서 count 값을 읽어오기
////        SharedPreferences sharedPreferences = getSharedPreferences("RandomMacPrefs", Context.MODE_PRIVATE);
////        int count = sharedPreferences.getInt("count", 1);
//        int count = SharedPrefsUtil.getCount(this);
//
//        // TextView에 count 값 표시
//        TextView countTextView = findViewById(R.id.step_message);
//        countTextView.setText("Step " + count +" / 7");
//    }
//
//
//    private void showNoDeviceDialog() {
//        CustomDeconnectedDialog deconnectedDialog = new CustomDeconnectedDialog(PreMainActivity.this);
//        deconnectedDialog.show();
//    }
//    private void showResetDeviceDialog() {
//        CustomResetDialog resetDialog = new CustomResetDialog(PreMainActivity.this);
//        resetDialog.show();
//    }
//    private void moveToDeleteActivity() {
//        Intent intent = new Intent(PreMainActivity.this, DeleteActivity.class); // 다음 화면으로 이동
//        startActivity(intent);
//        finish(); // 현재 액티비티 종료
//    }
//}