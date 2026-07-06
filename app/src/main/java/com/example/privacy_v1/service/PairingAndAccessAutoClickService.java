package com.example.privacy_v1.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PairingAndAccessAutoClickService extends AccessibilityService {

    private static final String TAG = "BT_AutoClick";

    // ===== 우리 앱 패키지 & 역할 분리 =====
    private static final String APP_PACKAGE = "com.example.privacy_v1";

    // A: 페어링 전용(여러 액티비티)
    private static final Set<String> PAIRING_ACTIVITIES = new HashSet<>(Arrays.asList(
            "com.example.privacy_v1.ui.activity.BluetoothScaningActivity",
            "com.example.privacy_v1.ui.activity.BluetoothReScaningActivity" // 재스캔 화면
    ));
    // 재스캔 진입 시 pairDone을 다시 false로 내려야 하는 특정 액티비티
    private static final String RESCAN_ACTIVITY =
            "com.example.tab_credit_v1_02.ui.activity.BluetoothReScaningActivity";

    // B: 연락처/메시지 전용
    private static final String ACT_ACCESS  = "com.example.privacy_v1.ui.activity.DeleteActivity";

    // ===== 제목/뷰 ID 후보 =====
    private static final String[] TITLE_IDS = {
            "com.android.settings:id/alertTitle",
            "android:id/alertTitle",
            "com.samsung.android.settings:id/alertTitle"
    };
    private static final String ID_OK      = "android:id/button1";
    private static final String ID_PINCODE = "com.android.settings:id/pincode"; // 페어링 창 힌트(없어도 진행 가능)

    // ===== 타이틀/버튼 텍스트 =====
    private static final String TITLE_PAIR    = "블루투스 연결 요청";
    private static final String TITLE_CONTACT = "연락처에 접근 허용";
    private static final String TITLE_MESSAGE = "메시지에 접근 허용";

    private static final String[] OK_TEXT_REGISTER = {"등록", "확인", "OK"};
    private static final String[] OK_TEXT_ALLOW    = {"허용", "확인", "OK"};

    // ===== 스로틀/디바운스/코얼레싱 =====
    private static final long SCAN_MIN_INTERVAL_MS = 200L;   // 스캔 최소 간격
    private static final long SCAN_DELAY_MS        = 0L;     // 스케줄 지연
    private static final long DEBOUNCE_CLICK_MS    = 1200L;  // 동일 키 중복 클릭 방지
    private static final long CONTENT_COALESCE_MS  = 200L;   // CONTENT_CHANGED 코얼레싱

    // ===== 핸들러 =====
    private final Handler uiH = new Handler(Looper.getMainLooper());
    private HandlerThread scanThread;
    private Handler scanH;

    // ===== 상태 =====
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final Map<String, Long> lastClickByKey = new HashMap<>();
    private boolean scanScheduled = false;
    private long lastScanAt = 0L;
    private long lastContentEventAt = 0L;

    // 우리 앱에서 마지막으로 보인 전면 Activity (설정 다이얼로그 떠도 유지)
    private volatile String lastAppActivity = null;

    // 세션(프로세스 생존) 1회 성공 플래그 — 재부팅/재시작 시 자동 초기화
    private volatile boolean pairDone = false; // A에서만 처리 (DeleteActivity 도달 시 true로 가정)
    private volatile boolean pbapDone = false; // B 또는 A에서 처리
    private volatile boolean mapDone  = false; // B 또는 A에서 처리

    // 타임스탬프 로깅
    private static long T0 = SystemClock.uptimeMillis();
    private static String ts() { return String.format("+%dms", SystemClock.uptimeMillis() - T0); }

    // ===== 라이프사이클 =====
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, ts() + " onServiceConnected()");
        // 무거운 탐색은 백그라운드 스레드에서
        scanThread = new HandlerThread("bt_autoclick_scan");
        scanThread.start();
        scanH = new Handler(scanThread.getLooper());
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, ts() + " onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, ts() + " onDestroy()");
        // 스레드 정리
        if (scanThread != null) {
            scanThread.quitSafely();
            scanThread = null;
            scanH = null;
        }
        super.onDestroy();
    }

    // ===== 이벤트 =====
    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;

        final CharSequence pkg = e.getPackageName();
        final int et = e.getEventType();
        final CharSequence cls = e.getClassName();

        // 우리 앱 액티비티가 전면이 되었을 때만 업데이트(설정 다이얼로그 떠도 지우지 않음)
        if (et == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != null && APP_PACKAGE.contentEquals(pkg) && cls != null) {
                lastAppActivity = cls.toString();

                // ReScan 액티비티 진입 시: pairDone 다시 내려줌
                if (RESCAN_ACTIVITY.equals(lastAppActivity)) {
                    if (pairDone) {
                        pairDone = false;
                        clearPairingDebounce();
                        Log.i(TAG, ts() + " ReScan reached → reset pairDone=false and cleared debounce");
                    }
                }

                // DeleteActivity 진입 시: 페어링은 이미 끝난 것으로 간주
                if (ACT_ACCESS.equals(lastAppActivity)) {
                    if (!pairDone) {
                        pairDone = true;
                        Log.i(TAG, ts() + " DeleteActivity reached → assume pairDone=true");
                    }
                }

                Log.d(TAG, ts() + " front(app)=" + lastAppActivity);
            } else {
                Log.d(TAG, ts() + " front(non-app)=" + cls + " pkg=" + pkg);
            }
        }

        // === 세션 종료 여부 (페어링 액티비티일 때만 의미 있음) ===
        if (allDone()) return;

        // === 현재 액티비티에서 감시할지 ===
        if (!shouldScanInCurrentActivity()) return;

        // *************************************
        // [FAST PATH] 설정 다이얼로그 팝업 뜬 직후
        //  - 로그: front(non-app)=android.app.Dialog pkg=com.android.settings
        // *************************************
        if (et == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && pkg != null
                && "com.android.settings".contentEquals(pkg)
                && cls != null
        ) {
            String clsName = cls.toString();
            String lower = clsName.toLowerCase(Locale.ROOT);
            if (lower.contains("dialog")) {
                Log.d(TAG, ts() + " fast-path scan for settings dialog cls=" + clsName);
                fastScanOnceForDialog();
                // 팝업 뜰 때는 빠른 경로만 태우고 종료
                return;
            }
        }

        // ===== 여기부터는 '일반 스캔' =====

        // 이벤트 필터: 우리 앱 or 설정 팝업에서만
        boolean interestingPkg =
                (pkg != null && (APP_PACKAGE.contentEquals(pkg) || "com.android.settings".contentEquals(pkg)));
        if (!interestingPkg) return;

        // 타입 필터
        if (et != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && et != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        long now = SystemClock.uptimeMillis();

        // CONTENT_CHANGED는 우리 앱 + 감시 대상에서만, 코얼레싱까지
        if (et == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // settings 쪽 CONTENT_CHANGED는 fast-path로만 처리하고 여기서는 무시
            if (pkg == null || !APP_PACKAGE.contentEquals(pkg) || !shouldScanInCurrentActivity()) {
                return;
            }
            if (now - lastContentEventAt < CONTENT_COALESCE_MS) {
                return;
            }
            lastContentEventAt = now;
        }

        if (scanScheduled) return;
        if (now - lastScanAt < SCAN_MIN_INTERVAL_MS) return;

        scanScheduled = true;
        Handler target = (scanH != null) ? scanH : uiH;
        Log.d(TAG, ts() + " schedule scan (etype=" + et + ") pkg=" + pkg);
        target.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    Log.d(TAG, ts() + " -> scanOnceBg() enter");
                    scanOnceBg();
                } finally {
                    lastScanAt = SystemClock.uptimeMillis();
                    scanScheduled = false;
                    Log.d(TAG, ts() + " <- scanOnceBg() leave");
                }
            }
        }, SCAN_DELAY_MS);
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
        /* no-op */
    }

    // ===== 현재(세션 상) 감시 여부 — our app의 "마지막" 전면 액티비티 기준 =====
    private boolean shouldScanInCurrentActivity() {
        if (lastAppActivity == null) return false;

        // 페어링 액티비티: 세 가지 중 하나라도 미완료면 스캔
        if (PAIRING_ACTIVITIES.contains(lastAppActivity)) {
            return !(pairDone && pbapDone && mapDone);
        }

        // DeleteActivity(ACT_ACCESS) 위에서는 항상 팝업 감시
        if (ACT_ACCESS.equals(lastAppActivity)) {
            return true;
        }

        return false;
    }

    // 세션 종료 판정 (페어링 액티비티일 때만 의미 있음)
    private boolean allDone() {
        return lastAppActivity != null
                && PAIRING_ACTIVITIES.contains(lastAppActivity)
                && pairDone && pbapDone && mapDone;
    }

    // ===== [FAST PATH] 팝업 뜬 직후 1회 스캔 =====
    private void fastScanOnceForDialog() {
        if (allDone() || !shouldScanInCurrentActivity()) return;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            Log.d(TAG, ts() + " fast-scan skip: screen off");
            return;
        }

        // 이미 백그라운드 스캔이 돌고 있는 경우: re-enter 경로 (ACTIVE만 훑기)
        if (!scanning.compareAndSet(false, true)) {
            Log.d(TAG, ts() + " fast-scan: scanning already running -> ACTIVE re-scan");

            try {
                AccessibilityNodeInfo active = getRootInActiveWindow();
                if (active != null) {
                    if (tryDialogsForCurrentActivityBg(active, "FAST_ACTIVE_REENTER")) {
                        Log.d(TAG, ts() + " fast-scan handled in ACTIVE (reenter)");
                    } else {
                        // 필요하다면 전체 윈도우도 한 번만
                        List<AccessibilityWindowInfo> wins = getWindows();
                        if (wins != null) {
                            for (AccessibilityWindowInfo w : wins) {
                                if (w == null) continue;
                                AccessibilityNodeInfo r = w.getRoot();
                                if (r == null) continue;
                                if (tryDialogsForCurrentActivityBg(r, "FAST_WIN_REENTER#" + w.getType())) {
                                    Log.d(TAG, ts() + " fast-scan handled in WIN_REENTER#" + w.getType());
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "fastScanOnceForDialog reenter error", t);
            }

            // re-enter에서는 scanning 플래그를 건드리지 않음 (scanOnceBg가 알아서 정리)
            return;
        }

        // 평소 경로: scanning=false 였던 경우
        try {
            AccessibilityNodeInfo active = getRootInActiveWindow();
            if (active != null) {
                if (tryDialogsForCurrentActivityBg(active, "FAST_ACTIVE")) {
                    Log.d(TAG, ts() + " fast-scan handled in ACTIVE");
                    return;
                }

                // 혹시 모를 케이스를 위해 전체 윈도우도 한 번만 훑어보기
                List<AccessibilityWindowInfo> wins = getWindows();
                if (wins != null) {
                    for (AccessibilityWindowInfo w : wins) {
                        if (w == null) continue;
                        AccessibilityNodeInfo r = w.getRoot();
                        if (r == null) continue;
                        if (tryDialogsForCurrentActivityBg(r, "FAST_WIN#" + w.getType())) {
                            Log.d(TAG, ts() + " fast-scan handled in WIN#" + w.getType());
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "fastScanOnceForDialog error", t);
        } finally {
            scanning.set(false);
        }
    }

    // ===== 1회 스캔(백그라운드) — 클릭은 메인에서 수행 =====
    private void scanOnceBg() {
        if (allDone() || !shouldScanInCurrentActivity()) return;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            Log.d(TAG, ts() + " skip: screen off");
            return;
        }

        if (!scanning.compareAndSet(false, true)) {
            Log.d(TAG, ts() + " skip: scanning already running");
            return;
        }
        try {
            AccessibilityNodeInfo active = getRootInActiveWindow();
            if (active != null) {
                if (tryDialogsForCurrentActivityBg(active, "ACTIVE")) return;

                // 활성 루트에 타이틀이 없을 때만 전체 윈도우 1회 순회
                if (!hasAnyTargetTitle(active)) {
                    List<AccessibilityWindowInfo> wins = getWindows();
                    if (wins != null) {
                        for (AccessibilityWindowInfo w : wins) {
                            if (w == null) continue;
                            AccessibilityNodeInfo r = w.getRoot();
                            if (r == null) continue;
                            if (tryDialogsForCurrentActivityBg(r, "WIN#" + w.getType())) return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "scanOnce error", t);
        } finally {
            scanning.set(false);
        }
    }

    // 타이틀 존재 빠른 검사(전수 순회 이전에 비용 줄이기)
    private boolean hasAnyTargetTitle(AccessibilityNodeInfo root) {
        return titleMatches(root, TITLE_PAIR)
                || titleMatches(root, TITLE_CONTACT)
                || titleMatches(root, TITLE_MESSAGE);
    }

    // ===== 현재 세션의 역할에 맞는 팝업 처리(백그라운드) — 실제 클릭은 메인 스레드 =====
    private boolean tryDialogsForCurrentActivityBg(AccessibilityNodeInfo root, String where) {
        if (lastAppActivity == null) return false;

        // A: 페어링 액티비티 — 등록/연락처/메시지 모두 처리
        if (PAIRING_ACTIVITIES.contains(lastAppActivity)) {

            // 1) 페어링 등록
            if (!pairDone && titleMatches(root, TITLE_PAIR)) {
                AccessibilityNodeInfo ok = resolveOkButton(root, OK_TEXT_REGISTER);
                if (ok != null && isVisible(ok)) {
                    postClick(ok, "PAIR|" + TITLE_PAIR, new Runnable() {
                        @Override public void run() {
                            pairDone = true;
                            Log.i(TAG, ts() + " PAIR done");
                        }
                    });
                    return true;
                }
            }

            // 2) 연락처 허용
            if (!pbapDone && titleMatches(root, TITLE_CONTACT)) {
                AccessibilityNodeInfo ok = resolveOkButton(root, OK_TEXT_ALLOW);
                if (ok != null && isVisible(ok)) {
                    postClick(ok, "PBAP|" + TITLE_CONTACT, new Runnable() {
                        @Override public void run() {
                            pbapDone = true;
                            Log.i(TAG, ts() + " PBAP done (in pairing activity)");
                        }
                    });
                    return true;
                }
            }

            // 3) 메시지 허용
            if (!mapDone && titleMatches(root, TITLE_MESSAGE)) {
                AccessibilityNodeInfo ok = resolveOkButton(root, OK_TEXT_ALLOW);
                if (ok != null && isVisible(ok)) {
                    postClick(ok, "MAP|" + TITLE_MESSAGE, new Runnable() {
                        @Override public void run() {
                            mapDone = true;
                            Log.i(TAG, ts() + " MAP done (in pairing activity)");
                        }
                    });
                    return true;
                }
            }

            return false;
        }

        // B: DeleteActivity 위의 접근 허용 창 처리 — 여기서는 해당 타이틀이면 항상 눌러줌
        if (ACT_ACCESS.equals(lastAppActivity)) {

            // 연락처에 접근 허용
            if (titleMatches(root, TITLE_CONTACT)) {
                AccessibilityNodeInfo ok = resolveOkButton(root, OK_TEXT_ALLOW);
                if (ok != null && isVisible(ok)) {
                    postClick(ok, "PBAP|" + TITLE_CONTACT, new Runnable() {
                        @Override public void run() {
                            pbapDone = true; // 필요하다면 상태 플래그로 사용
                            Log.i(TAG, ts() + " PBAP allowed (in DeleteActivity)");
                        }
                    });
                    return true;
                }
            }

            // 메시지에 접근 허용
            if (titleMatches(root, TITLE_MESSAGE)) {
                AccessibilityNodeInfo ok = resolveOkButton(root, OK_TEXT_ALLOW);
                if (ok != null && isVisible(ok)) {
                    postClick(ok, "MAP|" + TITLE_MESSAGE, new Runnable() {
                        @Override public void run() {
                            mapDone = true;
                            Log.i(TAG, ts() + " MAP allowed (in DeleteActivity)");
                        }
                    });
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    // ===== 메인 스레드에서 클릭 + 디바운스 =====
    private void postClick(final AccessibilityNodeInfo n, final String key, final Runnable onSuccess) {
        uiH.post(new Runnable() {
            @Override public void run() {
                long now = SystemClock.uptimeMillis();
                Long last = lastClickByKey.get(key);
                if (last != null && now - last < DEBOUNCE_CLICK_MS) {
                    Log.d(TAG, ts() + " CLICK skip(debounce) key=" + key);
                    return;
                }
                if (!isVisible(n)) {
                    Log.d(TAG, ts() + " CLICK skip(invisible) key=" + key);
                    return;
                }

                Log.d(TAG, ts() + " CLICK try key=" + key);
                boolean ok = n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (!ok && n.getParent() != null) {
                    ok = n.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                Log.d(TAG, ts() + " CLICK result ok=" + ok + " key=" + key);
                if (ok) {
                    lastClickByKey.put(key, now);
                    if (onSuccess != null) onSuccess.run();
                }
            }
        });
    }

    // ===== 유틸 =====
    private boolean titleMatches(AccessibilityNodeInfo root, String expect) {
        String b = normalize(expect);
        for (String id : TITLE_IDS) {
            AccessibilityNodeInfo n = firstById(root, id);
            if (n != null && n.getText() != null) {
                String a = normalize(n.getText().toString());
                if (a.equals(b) || a.contains(b)) return true;
            }
        }
        return false;
    }

    private AccessibilityNodeInfo resolveOkButton(AccessibilityNodeInfo root, String[] okTexts) {
        AccessibilityNodeInfo byId = firstById(root, ID_OK);
        if (byId != null && byId.isClickable() && textAny(byId, okTexts)) return byId;

        AccessibilityNodeInfo byText = findClickableButtonByText(root, okTexts);
        if (byText != null) return byText;

        return findRightMostClickableButton(root);
    }

    private boolean textAny(AccessibilityNodeInfo n, String[] expects) {
        if (n == null || n.getText() == null) return false;
        String a = normalize(n.getText().toString());
        for (String e : expects) {
            String b = normalize(e);
            if (a.equals(b) || a.contains(b)) return true;
        }
        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        s = s.replace("\u200E","").replace("\u200F","")
                .replace("\u202A","").replace("\u202C","")
                .replace("\u2066","").replace("\u2067","")
                .replace("\u2068","").replace("\u2069","");
        s = s.trim();
        try { return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC); }
        catch (Throwable t) { return s; }
    }

    private AccessibilityNodeInfo findClickableButtonByText(AccessibilityNodeInfo root, String[] okTexts) {
        java.util.ArrayDeque<AccessibilityNodeInfo> q = new java.util.ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            AccessibilityNodeInfo n = q.removeFirst();
            if (n == null) continue;
            if ("android.widget.Button".contentEquals(n.getClassName()) && n.isClickable() && textAny(n, okTexts)) {
                return n;
            }
            for (int i = 0; i < n.getChildCount(); i++) q.addLast(n.getChild(i));
        }
        return null;
    }

    private AccessibilityNodeInfo findRightMostClickableButton(AccessibilityNodeInfo root) {
        java.util.ArrayDeque<AccessibilityNodeInfo> q = new java.util.ArrayDeque<>();
        q.add(root);
        AccessibilityNodeInfo best = null;
        int bestRight = -1;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo n = q.removeFirst();
            if (n == null) continue;
            if ("android.widget.Button".contentEquals(n.getClassName()) && n.isClickable()) {
                Rect r = new Rect();
                n.getBoundsInScreen(r);
                if (r.right > bestRight) { bestRight = r.right; best = n; }
            }
            for (int i = 0; i < n.getChildCount(); i++) q.addLast(n.getChild(i));
        }
        return best;
    }

    private boolean isVisible(AccessibilityNodeInfo n) {
        Rect r = new Rect();
        n.getBoundsInScreen(r);
        return r.width() > 0 && r.height() > 0;
    }

    private AccessibilityNodeInfo firstById(AccessibilityNodeInfo root, String id) {
        try {
            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId(id);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Throwable ignore) {
            return null;
        }
    }

    // 디바운스(페어링 키) 초기화
    private void clearPairingDebounce() {
        try {
            java.util.Iterator<Map.Entry<String, Long>> it = lastClickByKey.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                if (e.getKey() != null && e.getKey().startsWith("PAIR|")) {
                    it.remove();
                }
            }
        } catch (Throwable ignore) { }
    }
}
