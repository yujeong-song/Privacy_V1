package com.example.privacy_v1.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.privacy_v1.R;

public class MyForegroundService extends Service{
    private static final String TAG = "MyForegroundService";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("앱이 백그라운드에서 실행 중입니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
        createNotificationChannel();
        startForeground(1, buildNotification());
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d("MyForegroundService", "Foreground service started");
//        return START_STICKY;
//    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 앱 아이콘 등으로 교체
                .setContentTitle("자동화 서비스 동작 중")
                .setContentText("블루투스 자동 클릭 및 접근성 서비스 유지")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        new Thread(() -> {
            SystemClock.sleep(2000); // 부팅 직후라면 여유
            AccEnabler.ensureAccessibilityEnabled(
                    MyForegroundService.this,
                    PairingAndAccessAutoClickService.class
            );
            // 여기서 추가 root 작업 넣고 싶으면 이어서 가능
        }).start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }
}