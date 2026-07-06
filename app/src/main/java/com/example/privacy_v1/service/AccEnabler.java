package com.example.privacy_v1.service;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

import com.example.privacy_v1.receiver.RootStarter;

public class AccEnabler {

    private static final String TAG = "AccEnabler";

    // 이 프로세스가 살아 있는 동안, 한 번이라도 "성공적으로 켰다"고 판단되면 true
    private static volatile boolean sAlreadyEnabledOnce = false;

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        String expectedId = context.getPackageName() + "/" + serviceClass.getName();

        AccessibilityManager am =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo service : enabledServices) {
            if (expectedId.equals(service.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1) 프로세스 내에서 이미 한 번 성공한 적 있으면 바로 리턴
     * 2) 이미 켜져 있으면 플래그 세우고 리턴
     * 3) WRITE_SECURE_SETTINGS 있으면 Settings.Secure로 정상 추가
     * 4) 실패하면 RootStarter.enableAccessibilityWithSu() 로 fallback
     */
    public static void ensureAccessibilityEnabled(Context context, Class<?> serviceClass) {
        // (0) 이번 프로세스에서 이미 한 번 성공한 적 있으면 그냥 스킵
        if (sAlreadyEnabledOnce) {
            Log.d(TAG, "skip ensureAccessibilityEnabled: already enabled once in this process");
            return;
        }

        // (1) 이미 시스템에 켜져 있으면 플래그 세우고 리턴
        if (isAccessibilityServiceEnabled(context, serviceClass)) {
            Log.i(TAG, "Accessibility already enabled for " + serviceClass.getSimpleName());
            sAlreadyEnabledOnce = true;
            return;
        }

        String serviceId = context.getPackageName() + "/" + serviceClass.getName();
        Log.i(TAG, "Trying to enable accessibility: " + serviceId);

        boolean ok = false;

        // (2) WRITE_SECURE_SETTINGS 경로
        try {
            ContentResolver cr = context.getContentResolver();

            String enabled = Settings.Secure.getString(
                    cr,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            if (enabled == null || enabled.isEmpty()) {
                enabled = serviceId;
            } else if (!enabled.contains(serviceId)) {
                enabled = enabled + ":" + serviceId;
            }

            Settings.Secure.putString(
                    cr,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    enabled
            );

            Settings.Secure.putInt(
                    cr,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
            );

            ok = true;
            Log.i(TAG, "Enabled via WRITE_SECURE_SETTINGS");
        } catch (SecurityException se) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted, fallback to su", se);
        } catch (Exception e) {
            Log.e(TAG, "Error using Settings.Secure", e);
        }

        if (ok) {
            // Settings.Secure 경로로 켜는 데 성공했으면 이후 호출은 모두 스킵
            sAlreadyEnabledOnce = true;
            return;
        }

        // (3) su fallback
        RootStarter.enableAccessibilityWithSu(serviceId);
        // su 경로는 성공 여부를 바로 확인하기 어렵지만,
        // 여기까지 왔으면 "한 번 시도" 했다고 보고 중복 호출은 막아준다.
        sAlreadyEnabledOnce = true;
    }
}
