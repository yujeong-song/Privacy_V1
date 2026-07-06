//package com.example.tab_credit_v1_02.bluetooth;
//
//import android.Manifest;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothClass;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import androidx.core.app.ActivityCompat;
//
///**
// * 자동차(헤드유닛) 전용 BluetoothConnector
// *
// * 역할:
// *  - 지정한 MAC(targetMac)을 스캔해서 찾고
// *  - 필요하면 createBond() 로 페어링 진행
// *  - BOND_BONDED 상태가 되면 "연결 완료"로 간주
// *
// * 하지 않는 것:
// *  - 어떤 형태의 BluetoothSocket.connect() 도 사용하지 않는다
// *  - HFP/A2DP/PBAP 프로파일 직접 connect 시도 X
// *
// * 프로파일 연결은 차량이 요청하고,
// * 접근성 서비스가 뜨는 팝업(연락처 허용, 메시지 허용)을 대신 눌러주는 구조.
// */
//public class BluetoothConnector {
//
//    /* ======================= Listener ======================= */
//
//    public interface Listener {
//        /** 타깃 MAC 기기를 스캔에서 처음 발견했을 때 */
//        void onCarFound(BluetoothDevice device);
//
//        /** createBond() 호출로 본딩이 시작될 때 */
//        void onBondingStarted(BluetoothDevice device);
//
//        /** BOND_BONDED 되었을 때 */
//        void onBonded(BluetoothDevice device);
//
//        /**
//         * "연결 완료" 신호.
//         * 예전 이름(onProfilesConnected)을 그대로 쓰지만,
//         * 이제는 HFP/A2DP 여부와 관계 없이
//         *  - target MAC 발견 + BOND_BONDED 이면 호출된다.
//         *
//         * hfp/a2dp 파라미터는 항상 false/false 로만 넘겨도 무방하다.
//         */
//        void onProfilesConnected(BluetoothDevice device, boolean hfp, boolean a2dp);
//
//        /** n번째 재시도 중인지 UI에 알려줄 때 (예: "재시도 2/5") */
//        void onAttempt(int attempt, int maxAttempts);
//
//        /** autoReconnectEnabled=true 인 경우, 다음 재시도까지의 딜레이 안내 */
//        void onReconnectScheduled(long delayMs);
//
//        /** 더 이상 시도하지 않고 완전히 실패했을 때 */
//        void onFailure(String reason);
//    }
//
//    /* ======================= 상수/필드 ======================= */
//
//    private static final String TAG = "BluetoothConnector";
//
//    // 시도 횟수 / 타임아웃 관련
//    private static final int   MAX_ATTEMPTS        = 5;
//    private static final long SCAN_TIMEOUT_MS     = 15_000L;  // 한 번의 scanAttempt 최대 15초
//    private static final long BOND_TIMEOUT_MS     = 20_000L;  // 페어링 최대 20초 대기
//    private static final long RETRY_DELAY_MS      = 2_000L;   // 재시도 간격
//
//    private final Context app;
//    private final BluetoothAdapter adapter;
//    private final String targetMac;
//    private final Listener listener;
//    private final boolean autoReconnectEnabled;
//    private final boolean debug;
//
//    private final Handler main = new Handler(Looper.getMainLooper());
//
//    // 진행 상태
//    private int  attempt = 0;
//    private boolean running = false;
//
//    // 스캔 관련
//    private boolean discoveryReceiverRegistered = false;
//    private BroadcastReceiver discoveryReceiver;
//    private Runnable scanTimeout;
//
//    // 본딩 관련
//    private boolean bondReceiverRegistered = false;
//    private BroadcastReceiver bondReceiver;
//    private Runnable bondTimeout;
//
//    public BluetoothConnector(Context context,
//                              String targetMac,
//                              Listener listener,
//                              boolean autoReconnectEnabled,
//                              boolean debug) {
//        this.app = context.getApplicationContext();
//        this.adapter = BluetoothAdapter.getDefaultAdapter();
//        this.targetMac = targetMac;
//        this.listener = listener;
//        this.autoReconnectEnabled = autoReconnectEnabled;
//        this.debug = debug;
//    }
//
//    /* ======================= 퍼블릭 API ======================= */
//
//    /** 전체 흐름 시작 */
//    public void start() {
//        if (running) return;
//        running = true;
//        attempt = 0;
//        logD("start() targetMac=" + targetMac);
//        startNewAttempt();
//    }
//
//    /** 모든 동작/리시버/타임아웃 정리 */
//    public void stop() {
//        logD("stop()");
//        running = false;
//        cancelScan();
//        unregisterBondReceiver();
//        clearBondTimeout();
//    }
//
//    /** 현재 동작 중인지 */
//    public boolean isRunning() {
//        return running;
//    }
//
//    /** 즉시 시도 카운터를 리셋하고 다시 시작 */
//    public void restartNow() {
//        logD("restartNow()");
//        stop();
//        start();
//    }
//
//    /* ======================= 내부 흐름 ======================= */
//
//    private void startNewAttempt() {
//        if (!running) return;
//
//        attempt++;
//        if (listener != null) listener.onAttempt(attempt, MAX_ATTEMPTS);
//
//        if (adapter == null) {
//            fail("No Bluetooth adapter");
//            return;
//        }
//
//        if (!hasScanPerm() || !hasConnectPerm()) {
//            fail("Bluetooth permissions not granted");
//            return;
//        }
//
//        if (!adapter.isEnabled()) {
//            fail("Bluetooth is OFF");
//            return;
//        }
//
//        if (targetMac == null || targetMac.isEmpty()) {
//            fail("Target MAC is empty");
//            return;
//        }
//
//        if (attempt > MAX_ATTEMPTS) {
//            fail("Max attempts reached");
//            return;
//        }
//
//        logD("=== Attempt " + attempt + "/" + MAX_ATTEMPTS + " ===");
//
//        // 혹시 남아있을지 모르는 이전 스캔/타임아웃 정리
//        cancelScan();
//
//        // 스캔 리시버 생성 및 등록
//        discoveryReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    if (device == null) return;
//
//                    String addr = device.getAddress();
//                    if (addr == null) return;
//
//                    logD(String.format("[SCAN] FOUND %s [%s]", safeName(device), addr));
//
//                    // 타깃 MAC 일치하면
//                    if (targetMac.equalsIgnoreCase(addr)) {
//                        logI(String.format("[SCAN] MATCH target %s [%s]", safeName(device), addr));
//
//                        if (listener != null) listener.onCarFound(device);
//
//                        // 스캔 종료
//                        try {
//                            adapter.cancelDiscovery();
//                        } catch (Exception ignore) {}
//                        unregisterDiscoveryReceiver();
//                        clearScanTimeout();
//
//                        // 이 기기에 대해 페어링/본딩 처리로 넘어감
//                        proceedAfterFound(device);
//                    }
//                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                    logD("[SCAN] DISCOVERY_FINISHED");
//                    unregisterDiscoveryReceiver();
//                    clearScanTimeout();
//                    // 이번 시도에서는 타깃을 못 찾음 → 재시도 또는 실패
//                    handleRetryOrFail("Target not found in scan");
//                }
//            }
//        };
//
//        IntentFilter f = new IntentFilter();
//        f.addAction(BluetoothDevice.ACTION_FOUND);
//        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        app.registerReceiver(discoveryReceiver, f);
//        discoveryReceiverRegistered = true;
//        logD("Receiver + ACTION_FOUND / DISCOVERY_FINISHED registered");
//
//        // 스캔 시작
//        boolean started = adapter.startDiscovery();
//        logI("startDiscovery(): " + started);
//
//        // 스캔 타임아웃
//        scanTimeout = () -> {
//            logE("SCAN timeout (" + SCAN_TIMEOUT_MS + "ms)");
//            try {
//                adapter.cancelDiscovery();
//            } catch (Exception ignore) {}
//            unregisterDiscoveryReceiver();
//            handleRetryOrFail("Scan timeout");
//        };
//        main.postDelayed(scanTimeout, SCAN_TIMEOUT_MS);
//    }
//
//    private void proceedAfterFound(BluetoothDevice device) {
//        if (!hasConnectPerm()) {
//            fail("BLUETOOTH_CONNECT not granted");
//            return;
//        }
//
//        int bs = device.getBondState();
//        logI("BOND_CHECK | state=" + bondStateStr(bs));
//
//        // 1) 아직 본딩 안 되어 있으면 → createBond()
//        if (bs != BluetoothDevice.BOND_BONDED) {
//            logI("BONDING start → createBond()");
//            if (listener != null) listener.onBondingStarted(device);
//
//            registerBondReceiver();
//
//            boolean started;
//            try {
//                started = device.createBond();   // 페어링 팝업 → 접근성 서비스가 자동 허용
//            } catch (SecurityException se) {
//                started = false;
//            }
//            logI("createBond(): " + started);
//
//            if (!started) {
//                handleRetryOrFail("createBond() returned false");
//                return;
//            }
//
//            // 본딩 타임아웃
//            clearBondTimeout();
//            bondTimeout = () -> {
//                logE("BOND timeout (" + BOND_TIMEOUT_MS + "ms)");
//                unregisterBondReceiver();
//                handleRetryOrFail("Bond timeout");
//            };
//            main.postDelayed(bondTimeout, BOND_TIMEOUT_MS);
//            return;
//        }
//
//        // 2) 이미 BOND_BONDED 상태라면 → 바로 성공 처리
//        logI("Already BOND_BONDED → success");
//        notifyConnected(device);
//    }
//
//    /* ======================= 본딩 리시버 ======================= */
//
//    private void registerBondReceiver() {
//        if (bondReceiverRegistered) return;
//        bondReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context c, Intent intent) {
//                if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) return;
//
//                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (d == null || d.getAddress() == null) return;
//                if (!targetMac.equalsIgnoreCase(d.getAddress())) return;
//
//                int newState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
//                int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
//                logI("BOND_EVT | " + bondStateStr(prevState) + " -> " + bondStateStr(newState));
//
//                if (newState == BluetoothDevice.BOND_BONDED) {
//                    clearBondTimeout();
//                    unregisterBondReceiver();
//                    if (listener != null) listener.onBonded(d);
//                    // 여기서 바로 성공 처리
//                    notifyConnected(d);
//                } else if (newState == BluetoothDevice.BOND_NONE && prevState != BluetoothDevice.BOND_NONE) {
//                    // 사용자가 거부/해제했거나 실패
//                    clearBondTimeout();
//                    unregisterBondReceiver();
//                    handleRetryOrFail("Bonding failed/cancelled");
//                }
//            }
//        };
//        IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        app.registerReceiver(bondReceiver, f);
//        bondReceiverRegistered = true;
//        logD("Receiver + ACTION_BOND_STATE_CHANGED registered");
//    }
//
//    private void unregisterBondReceiver() {
//        if (!bondReceiverRegistered) return;
//        try {
//            app.unregisterReceiver(bondReceiver);
//        } catch (Exception ignore) {}
//        bondReceiverRegistered = false;
//        bondReceiver = null;
//        logD("Receiver - ACTION_BOND_STATE_CHANGED unregistered");
//    }
//
//    /* ======================= 성공/실패/재시도 ======================= */
//
//    private void notifyConnected(BluetoothDevice device) {
//        if (!running) return;
//        logI("notifyConnected() " + safeName(device) + " [" + device.getAddress() + "]");
//        // 여기서는 HFP/A2DP 여부를 체크하지 않으므로 false/false 고정
//        if (listener != null) listener.onProfilesConnected(device, false, false);
//        // 자동 재연결 모드라도, 일단 이번 세션은 종료
//        stop();
//    }
//
//    private void handleRetryOrFail(String reason) {
//        if (!running) return;
//
//        logE("handleRetryOrFail(): " + reason);
//
//        if (!autoReconnectEnabled || attempt >= MAX_ATTEMPTS) {
//            fail(reason);
//            return;
//        }
//
//        long delay = RETRY_DELAY_MS;
//        logI("Retry scheduled after " + delay + "ms (attempt " + attempt + "/" + MAX_ATTEMPTS + ")");
//        if (listener != null) listener.onReconnectScheduled(delay);
//
//        main.postDelayed(() -> {
//            if (!running) return;
//            startNewAttempt();
//        }, delay);
//    }
//
//    private void fail(String reason) {
//        logE("FAIL: " + reason);
//        if (listener != null) listener.onFailure(reason);
//        stop();
//    }
//
//    /* ======================= 스캔/타임아웃 유틸 ======================= */
//
//    private void cancelScan() {
//        try {
//            adapter.cancelDiscovery();
//        } catch (Exception ignore) {}
//        unregisterDiscoveryReceiver();
//        clearScanTimeout();
//    }
//
//    private void unregisterDiscoveryReceiver() {
//        if (!discoveryReceiverRegistered) return;
//        try {
//            app.unregisterReceiver(discoveryReceiver);
//        } catch (Exception ignore) {}
//        discoveryReceiverRegistered = false;
//        discoveryReceiver = null;
//        logD("Receiver - ACTION_FOUND / DISCOVERY_FINISHED unregistered");
//    }
//
//    private void clearScanTimeout() {
//        if (scanTimeout != null) {
//            main.removeCallbacks(scanTimeout);
//            scanTimeout = null;
//        }
//    }
//
//    private void clearBondTimeout() {
//        if (bondTimeout != null) {
//            main.removeCallbacks(bondTimeout);
//            bondTimeout = null;
//        }
//    }
//
//    /* ======================= 권한/로그 유틸 ======================= */
//
//    private boolean hasScanPerm() {
//        return ActivityCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_SCAN)
//                == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private boolean hasConnectPerm() {
//        return ActivityCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT)
//                == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private String safeName(BluetoothDevice d) {
//        try {
//            return (d.getName() != null ? d.getName() : d.getAddress());
//        } catch (SecurityException se) {
//            return d.getAddress();
//        }
//    }
//
//    private String bondStateStr(int s) {
//        switch (s) {
//            case BluetoothDevice.BOND_NONE: return "NONE";
//            case BluetoothDevice.BOND_BONDING: return "BONDING";
//            case BluetoothDevice.BOND_BONDED: return "BONDED";
//        }
//        return "UNKNOWN(" + s + ")";
//    }
//
//    private void logD(String msg) {
//        if (debug) Log.d(TAG, msg);
//    }
//
//    private void logI(String msg) {
//        if (debug) Log.i(TAG, msg);
//    }
//
//    private void logE(String msg) {
//        Log.e(TAG, msg);
//    }
//}
package com.example.privacy_v1.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothConnector {
    private static final String TAG = "BluetoothConnector";

    // 최대 재시도 횟수와 현재 시도 횟수 추가
    private int connectionAttempts = 0;
    private static final int MAX_CONNECTION_ATTEMPTS = 3;

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private final String targetMacAddress;
    private BluetoothSocket socket;

//    private BluetoothConnectionListener connectionListener;
    private BluetoothPairingListener pairingListener;
    private boolean isScanning = false;
    private boolean bondReceiverRegistered = false;
    private boolean discoveryReceiverRegistered = false;


    public BluetoothConnector(Context context, String targetMacAddress) {
        this.context = context;
        this.targetMacAddress = targetMacAddress;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    public void setPairingListener(BluetoothPairingListener listener) {
        this.pairingListener = listener;
    }

//    public void setConnectionListener(BluetoothConnectionListener listener) {
//        this.connectionListener = listener;
//    }

    private static final int SCAN_TIMEOUT = 15000; // 15초

    public void startScanning() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            if (pairingListener != null) {
                pairingListener.onPairingError(null, "Bluetooth not supported");
            }
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled");
            if (pairingListener != null) {
                pairingListener.onPairingError(null, "Bluetooth disabled");
            }
            return;
        }

        if (isScanning) {
            Log.d(TAG, "Already scanning");
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        context.registerReceiver(discoveryReceiver, filter);
        discoveryReceiverRegistered = true;

        boolean started = bluetoothAdapter.startDiscovery();
        Log.d(TAG, "startDiscovery(): " + started);

        isScanning = started;

        if (!started && pairingListener != null) {
            pairingListener.onPairingError(null, "Failed to start discovery");
        }
    }

    public void stopScanning() {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (discoveryReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver);
            } catch (IllegalArgumentException e) {
                // 이미 해제된 경우 예외 방지
            }
            discoveryReceiverRegistered = false;
        }

        isScanning = false;
    }

    // ===========================
    // 2) 디바이스 발견 브로드캐스트
    // ===========================
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                String address = device.getAddress();
                Log.d(TAG, "Found device: " + device.getName() + " [" + address + "]");

                if (targetMacAddress != null &&
                        targetMacAddress.equalsIgnoreCase(address)) {

                    Log.d(TAG, "Target device found: " + device.getName()
                            + " [" + address + "]");

                    // 타겟 찾았으면 더 이상 스캔할 필요 없음
                    stopScanning();
                    ensureBonded(device);
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Discovery finished");
                isScanning = false;
                if (pairingListener != null) {
                    pairingListener.onScanFinished();
                }
            }
        }
    };

    // ===========================
    // 3) 페어링(본딩) 진행
    // ===========================
    /**
     * 이미 BluetoothDevice 인스턴스를 알고 있을 때 직접 호출해도 됨.
     * (예: BluetoothManager에서 찾은 device를 넘겨줄 때)
     */
    public void ensureBonded(BluetoothDevice device) {
        if (device == null) {
            if (pairingListener != null) {
                pairingListener.onPairingError(null, "Device is null");
            }
            return;
        }

        int bondState = device.getBondState();
        Log.d(TAG, "ensureBonded() device=" + device.getName()
                + " [" + device.getAddress() + "], bondState=" + bondState);

        switch (bondState) {
            case BluetoothDevice.BOND_BONDED:
                // 이미 등록(페어링 완료)된 상태
                Log.d(TAG, "Device already bonded");
                if (pairingListener != null) {
                    pairingListener.onDeviceAlreadyBonded(device);
                }
                break;

            case BluetoothDevice.BOND_BONDING:
                // 이미 누군가 페어링 진행 중
                registerBondReceiver();
                if (pairingListener != null) {
                    pairingListener.onPairingStarted(device);
                }
                break;

            case BluetoothDevice.BOND_NONE:
            default:
                // 아직 페어링 안 됨 → createBond() 호출
                registerBondReceiver();

                boolean started = device.createBond();
                Log.d(TAG, "createBond() called, result=" + started);

                if (!started) {
                    if (pairingListener != null) {
                        pairingListener.onPairingError(device, "createBond() failed");
                    }
                } else {
                    if (pairingListener != null) {
                        pairingListener.onPairingStarted(device);
                    }
                }
                break;
        }
    }

    private void registerBondReceiver() {
        if (bondReceiverRegistered) return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(bondStateReceiver, filter);
        bondReceiverRegistered = true;
    }

    private void unregisterBondReceiver() {
        if (!bondReceiverRegistered) return;

        try {
            context.unregisterReceiver(bondStateReceiver);
        } catch (IllegalArgumentException e) {
            // 이미 해제된 경우 무시
        }
        bondReceiverRegistered = false;
    }

    // ===========================
    // 4) BOND 상태 변경 수신
    // ===========================
    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }

            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) return;

            int bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.BOND_NONE);
            int prevBondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                            BluetoothDevice.BOND_NONE);

            Log.d(TAG, "BOND_STATE_CHANGED: " + device.getName()
                    + " [" + device.getAddress() + "] "
                    + prevBondState + " -> " + bondState);

            if (bondState == BluetoothDevice.BOND_BONDED) {
                // 페어링(등록) 끝
                unregisterBondReceiver();
                if (pairingListener != null) {
                    pairingListener.onDeviceBonded(device);
                }
            } else if (prevBondState == BluetoothDevice.BOND_BONDING
                    && bondState == BluetoothDevice.BOND_NONE) {
                // 사용자가 취소했거나 실패한 경우
                unregisterBondReceiver();
                if (pairingListener != null) {
                    pairingListener.onPairingFailed(device);
                }
            }
        }
    };

    // ===========================
    // 5) 리소스 정리
    // ===========================
    public void release() {
        stopScanning();
        unregisterBondReceiver();
        pairingListener = null;
    }

    // ===========================
    // 콜백 인터페이스
    // ===========================
    public interface BluetoothPairingListener {
        /** 스캔이 모두 끝났을 때 (타겟을 못 찾았거나, 정상 종료) */
        default void onScanFinished() {}

        /** 이미 BONDED 상태인 경우 (이미 등록된 기기) */
        default void onDeviceAlreadyBonded(BluetoothDevice device) {}

        /** createBond() 시작되었을 때 */
        default void onPairingStarted(BluetoothDevice device) {}

        /** BOND_BONDED 된 순간 = "등록 완료" 지점 */
        default void onDeviceBonded(BluetoothDevice device) {}

        /** BOND_BONDING -> BOND_NONE 으로 돌아온 경우 등 */
        default void onPairingFailed(BluetoothDevice device) {}

        /** 기타 에러 (Bluetooth 미지원 등) */
        default void onPairingError(BluetoothDevice device, String message) {}
    }
}