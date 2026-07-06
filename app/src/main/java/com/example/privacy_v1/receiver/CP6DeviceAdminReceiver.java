package com.example.privacy_v1.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class CP6DeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        // 디바이스 관리자 권한이 활성화됐을 때 콜백
        Toast.makeText(context, "Device Admin 권한 활성화됨", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // 디바이스 관리자 권한이 비활성화됐을 때 콜백
        Toast.makeText(context, "Device Admin 권한 비활성화됨", Toast.LENGTH_SHORT).show();
    }
}