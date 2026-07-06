//package com.example.tab_credit_v1_02.bluetooth;
//
//import android.bluetooth.BluetoothAdapter;
//import android.content.BroadcastReceiver;
//import android.bluetooth.BluetoothDevice;
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//
//import com.example.tab_credit_v1_02.ui.activity.ReConnectionActivity;
//
//public class BluetoothStateReceiver extends BroadcastReceiver {
//    private static final String TAG = "BluetoothStateReceiver";
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        final String action = intent.getAction();
//        Log.d(TAG, "onReceive: " + action);
//
//        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//            if (state == BluetoothAdapter.STATE_ON) {
//                // 블루투스가 켜지면 앱의 자동 연결(스캔) 화면으로 유도
//                launchScanUi(context);
//            }
//        }
//    }
//
//    private void launchScanUi(Context context) {
//        Intent i = new Intent();
//        i.setClassName(context.getPackageName(), "com.example.tab_credit_v1_02.ui.BluetoothScaningActivity");
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);
//    }
//}
//
////    public BluetoothStateReceiver(Context context) {
////        this.context = context;
////    }
////    @Override
////    public void onReceive(Context context, Intent intent) {
////        String action = intent.getAction();
////        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
////
////        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
////            Log.e(TAG, "Device disconnected: " + (device != null ? device.getName() : "Unknown device"));
////            showWarningScreen(); // 연결 끊김 시 경고 화면으로 전환
////        }
////    }
////
////    private void showWarningScreen() {
////        Intent warningIntent = new Intent(context, ReConnectionActivity.class);
////        warningIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////        context.startActivity(warningIntent);
////    }
////}
package com.example.privacy_v1.bluetooth;

import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.privacy_v1.ui.activity.ReConnectionActivity;

public class BluetoothStateReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothStateReceiver";
    private final Context context;

    public BluetoothStateReceiver(Context context) {
        this.context = context.getApplicationContext();
    }

//    public BluetoothStateReceiver(Context context) {
//        this.context = context;
//    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            Log.e(TAG, "Device disconnected: " + (device != null ? device.getName() : "Unknown device"));
            showWarningScreen(); // 연결 끊김 시 경고 화면으로 전환
        }
    }

    private void showWarningScreen() {
        Intent warningIntent = new Intent(context, ReConnectionActivity.class);
        warningIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(warningIntent);
    }
}
