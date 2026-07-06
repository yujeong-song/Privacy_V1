package com.example.privacy_v1.receiver;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
public class RootStarter {
    private static final String TAG = "RootStarter";
    /** 부팅 직후 su 데몬이 준비될 때까지 대기 (최대 20초, 0.5초 간격) */
    public static boolean waitForSuReady(long timeoutMs, long stepMs) {
        long start = android.os.SystemClock.uptimeMillis();
        while (android.os.SystemClock.uptimeMillis() - start < timeoutMs) {
            if (suExec(new String[]{"id"})) {
                Log.i(TAG, "su ready");
                return true;
            }
            android.os.SystemClock.sleep(stepMs);
        }
        Log.w(TAG, "su not ready in time");
        return false;
    }

    /** 홈/AMS 준비까지 약간 더 기다렸다가 실행 (다중 시도) */
    public static boolean startComponentResilient(String pkg, String shortComp) {
        String comp = pkg + "/" + shortComp; // 예: com.example.tab_credit_v1_02/.ui.splash.SplashActivity

        String[] attempts = new String[] {
                // 1) 기본 MAIN/LAUNCHER로 실행
                "am start -n " + comp + " -a android.intent.action.MAIN -c android.intent.category.LAUNCHER",
                // 2) 컴포넌트만 직접 실행
                "am start -n " + comp,
                // 3) monkey로 패키지 실행
                "monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1"
        };

//        String[] attempts = new String[] {
//                // 1) 표준 (MAIN/LAUNCHER, user 0)
//                "am start -W --user 0 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n " + comp,
//                // 2) cmd activity 경로
//                "cmd activity start-activity -W --user 0 -n " + comp,
//                // 3) 패키지 런처로 monkey
//                "monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1"
//        };

        // 화면 깨우기(무해)
//        suExec(new String[]{"input keyevent 224"});

        for (int i = 0; i < attempts.length; i++) {
            if (suExec(new String[]{ attempts[i] })) {
                Log.i(TAG, "launch success via #" + (i+1));
                return true;
            } else {
                Log.w(TAG, "launch attempt #" + (i+1) + " failed");
            }
        }
        return false;
    }
    /**
     * su로 접근성 서비스 강제 ON
     *  - enabled_accessibility_services에 해당 서비스만 세팅 (다른 서비스는 날아갈 수 있음)
     *  - accessibility_enabled = 1
     */
    public static boolean enableAccessibilityWithSu(String serviceId) {
        String cmd1 = "settings put secure enabled_accessibility_services " + serviceId;
        String cmd2 = "settings put secure accessibility_enabled 1";

        boolean r1 = suExec(new String[]{cmd1});
        boolean r2 = suExec(new String[]{cmd2});
        Log.i(TAG, "enableAccessibilityWithSu: cmd1=" + r1 + ", cmd2=" + r2);
        return r1 && r2;
    }

    private static boolean suExec(String[] lines) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()))) {
                for (String s : lines) { w.write(s); w.newLine(); }
                w.write("exit"); w.newLine();
                w.flush();
            }
            String out = readAll(p.getInputStream());
            String err = readAll(p.getErrorStream());
            int code = p.waitFor();
            Log.i(TAG, "cmd=" + String.join(" && ", lines)
                    + " exit=" + code + " out=" + out + " err=" + err);
            return code == 0;
        } catch (Throwable t) {
            Log.e(TAG, "suExec error", t);
            return false;
        } finally {
            if (p != null) p.destroy();
        }
    }

    private static String readAll(java.io.InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line; while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
