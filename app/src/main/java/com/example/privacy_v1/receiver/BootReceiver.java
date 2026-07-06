package com.example.privacy_v1.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.example.privacy_v1.service.AccEnabler;
import com.example.privacy_v1.service.MyForegroundService;
import com.example.privacy_v1.service.PairingAndAccessAutoClickService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PKG = "com.example.privacy_v1";
    private static final String SHORT_COMP = ".ui.splash.SplashActivity";
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(a)
                && !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(a)) return;

        new Thread(() -> {
            // ① 시스템 최소 안정화 대기 (8초 권장)
            SystemClock.sleep(3000);

            // ② su 데몬 준비 대기 (최대 20초)
            RootStarter.waitForSuReady(10_000, 500);

            // ③ 네트워크 준비를 원하면(선택) 최대 10초 더 대기
            waitNetworkReady(c, 10_000, 500);
// ④ 접근성 서비스 강제 활성화 (WRITE_SECURE_SETTINGS → 실패 시 su fallback)
            try {
                AccEnabler.ensureAccessibilityEnabled(
                        c.getApplicationContext(),
                        PairingAndAccessAutoClickService.class
                );
            } catch (Throwable t) {
                Log.e(TAG, "ensureAccessibilityEnabled error", t);
            }

            // ⑤ ForegroundService 시작 (프로세스 유지 및 추가 작업용)
            try {
                Intent svc = new Intent(c, MyForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    c.startForegroundService(svc);
                } else {
                    c.startService(svc);
                }
            } catch (Throwable t) {
                Log.e(TAG, "startForegroundService error", t);
            }

            // ④ 다중 재시도: 3회, 2초 간격
            boolean ok = false;
            for (int t = 0; t < 3 && !ok; t++) {
                ok = RootStarter.startComponentResilient(PKG, SHORT_COMP);
                if (!ok) SystemClock.sleep(2000);
            }
            if (ok) {
                //추후 하드웨어 실행 지점?
                Log.i(TAG, "### Auto-launch succeeded ###");
            } else {
                Log.e(TAG, "### Auto-launch FAILED after retries ###");
            }
            Log.i(TAG, "Start target: " + PKG + "/" + SHORT_COMP + " => " + ok);
        }, "BootStartThread").start();
    }

    private void waitNetworkReady(Context ctx, long maxWaitMs, long stepMs) {
        long start = SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis() - start < maxWaitMs) {
            if (isInternetAvailable(ctx)) return;
            SystemClock.sleep(stepMs);
        }
    }

    private boolean isInternetAvailable(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(net);
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Throwable ignore) { return false; }
    }
}