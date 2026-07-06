//package com.example.tab_credit_v1_02.bluetooth;
//
//import android.Manifest;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothClass;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import androidx.core.app.ActivityCompat;
//
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.List;
//
//public class BluetoothManager {
//
//    /* ========================= UI 콜백 인터페이스 ========================= */
//    public interface Ui {
//        void showToast(String msg);
//
//        /**
//         * (참고용) "해당 MAC과의 연결/페어링 절차 완료" 콜백
//         *
//         * 이제 BluetoothConnector 는 HFP/A2DP 프로파일까지 확인하지 않고,
//         *  - 타깃 MAC 스캔
//         *  - 필요 시 createBond() 로 본딩
//         *  - BOND_BONDED 까지 완료되면
//         * 이 메서드를 호출합니다.
//         *
//         * hfp/a2dp 파라미터는 항상 false/false 로 올 수 있으므로,
//         * 화면 전환 조건으로 쓰지 말고, 단순 로그/토스트/저장 용도로만 사용하세요.
//         * 실제 화면 전환은 startLinkWatch(...)의 onReady 를 기준으로 처리합니다.
//         */
//        void onConnected(BluetoothDevice device, boolean hfp, boolean a2dp);
//
//        /** createBond() 로 본딩이 시작되었을 때 */
//        void onBonding(BluetoothDevice device);
//
//        /** 스캔 중 타깃 MAC 기기를 발견했을 때 */
//        void onFound(BluetoothDevice device);
//
//        /** n번째 시도 중인지 UI에 알려줄 때 (예: "재시도 2/5") */
//        void onAttempt(int attempt, int maxAttempts);
//
//        /** autoReconnectEnabled=true 인 경우, 다음 재시도까지의 딜레이 안내 */
//        void onReconnectScheduled(long delayMs);
//
//        /** 더 이상 시도하지 않고 완전히 실패했을 때 */
//        void onFailure(String reason);
//    }
//
//    /* ========================= 상수/필드 ========================= */
//
//    private static final String TAG = "BluetoothManager";
//    private static final String PREF_NAME       = "bluetooth_prefs";
//    private static final String KEY_DEVICE_NAME = "device_name";
//    private static final String KEY_DEVICE_MAC  = "device_mac";
//
//    private final Context app;
//    private final BluetoothAdapter adapter;
//
//    /** "MAC 스캔 → 본딩까지"를 담당하는 커넥터 */
//    private BluetoothConnector connector;
//    /** 현재 세션에서 대상으로 삼고 있는 MAC (재시작 등에 사용) */
//    private String currentTargetMac;
//
//    // BONDED + ACL 링크 감시
//    private BroadcastReceiver linkWatcher;
//    private boolean linkWatcherRegistered = false;
//    private boolean bondOk = false;
//    private boolean aclOk  = false;
//    private final Handler mainH = new Handler(Looper.getMainLooper());
//    private Runnable linkTimeout;
//
//    public BluetoothManager(Context context) {
//        this.app = context.getApplicationContext();
//        this.adapter = BluetoothAdapter.getDefaultAdapter();
//    }
//
//    /* ========================= SharedPreferences 유틸 ========================= */
//
//    /** 최근 연결(페어링) 기기 MAC 불러오기 (없으면 null) */
//    public String getSavedMacAddress() {
//        try {
//            SharedPreferences sp = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
//            return sp.getString(KEY_DEVICE_MAC, null);
//        } catch (Exception e) {
//            Log.e(TAG, "getSavedMacAddress error: ", e);
//            return null;
//        }
//    }
//
//    /** 현재 시스템 단에서 "연결됨" 상태로 보이는 기기 목록 (참고용) */
//    public List<BluetoothDevice> getConnectedDevices() {
//        List<BluetoothDevice> connectedDevices = new ArrayList<>();
//        if (adapter == null || !adapter.isEnabled()) {
//            return connectedDevices;
//        }
//
//        for (BluetoothDevice device : adapter.getBondedDevices()) {
//            if (device != null) {
//                if (isDeviceConnected(device)) {
//                    connectedDevices.add(device);
//                }
//            }
//        }
//        return connectedDevices;
//    }
//
//    // 기기가 실제로 연결되어 있는지 확인하는 메서드 (숨은 isConnected 리플렉션)
//    private boolean isDeviceConnected(BluetoothDevice device) {
//        try {
//            Class<?> bluetoothDeviceClass = Class.forName("android.bluetooth.BluetoothDevice");
//            Method isConnectedMethod = bluetoothDeviceClass.getDeclaredMethod("isConnected");
//            return (boolean) isConnectedMethod.invoke(device);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /** 자동차/헤드유닛 카테고리인지 대략 확인 (UI 필터 등에 사용) */
//    public boolean isDeviceMatchingCategory(BluetoothDevice device) {
//        if (device == null) {
//            Log.e(TAG, "Device is null.");
//            return false; // null이면 일치하지 않는 것으로 처리
//        }
//
//        try {
//            BluetoothClass bluetoothClass = device.getBluetoothClass();
//            if (bluetoothClass == null) {
//                Log.e(TAG, "BluetoothClass is null for device: " + device.getName());
//                return false; // BluetoothClass 정보가 없는 경우 일치하지 않음
//            }
//
//            int majorDeviceClass = bluetoothClass.getMajorDeviceClass();
//            int deviceClassCode  = bluetoothClass.getDeviceClass();
//
//            Log.d(TAG, "Checking device: " + device.getName() +
//                    ", MajorDeviceClass: " + majorDeviceClass +
//                    ", DeviceClass: " + deviceClassCode);
//
//            // AUDIO/VIDEO 헤드폰, 핸즈프리, 차량 오디오 등
//            return majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO &&
//                    (deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES   ||
//                            deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE   ||
//                            deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED ||
//                            deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO);
//        } catch (Exception e) {
//            Log.e(TAG, "Error processing device: " + device.getName(), e);
//            return false; // 예외 발생 시 일치하지 않는 것으로 처리
//        }
//    }
//
//    /** 최근 연결(페어링) 기기 이름/주소 저장 */
//    public void saveConnectedDevice(BluetoothDevice device) {
//        if (device == null) return;
//        try {
//            SharedPreferences sp = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
//            SharedPreferences.Editor ed = sp.edit();
//            ed.putString(KEY_DEVICE_NAME, safeName(device));
//            ed.putString(KEY_DEVICE_MAC,  device.getAddress());
//            ed.apply();
//            Log.d(TAG, "Saved device: " + safeName(device) + " / " + device.getAddress());
//        } catch (Exception e) {
//            Log.e(TAG, "saveConnectedDevice error: ", e);
//        }
//    }
//
//    /** 여러 개를 한 번에 저장하고 싶을 때 (마지막 항목으로 갱신) */
//    public void saveConnectedDevices(List<BluetoothDevice> devices) {
//        if (devices == null) return;
//        for (BluetoothDevice d : devices) saveConnectedDevice(d);
//    }
//
//    /* ========================= 시작/중지 API ========================= */
//
//    /** 가장 단순 오버로드: 내부 자동재연결=ON, 디버그=ON */
//    public void startAutoConnect(String mac, Ui ui) {
//        startAutoConnect(mac, ui, /*autoReconnectEnabled=*/true, /*debug=*/true);
//    }
//
//    /**
//     * 권장 오버로드:
//     *  - 스캔 화면(초기 연결 전담): autoReconnectEnabled=false, debug=true
//     *  - 백그라운드/서비스 자율 재연결: autoReconnectEnabled=true
//     *
//     * 내부 동작:
//     *  - 지정한 MAC 을 스캔해서 발견하면
//     *  - 필요 시 createBond() 로 페어링(본딩) 팝업 유도 (접근성 서비스가 자동으로 버튼 클릭)
//     *  - BOND_BONDED 상태가 되면 Listener.onProfilesConnected() 를 "성공"으로 호출
//     *
//     * 주의:
//     *  - HFP/A2DP/PBAP 등의 프로파일 connect 는 이 단계에서 직접 시도하지 않습니다.
//     *  - 화면 전환 조건은 startLinkWatch(...) 의 onReady 를 기준으로 잡으세요.
//     */
//    public void startAutoConnect(String mac, Ui ui, boolean autoReconnectEnabled, boolean debug) {
//        if (adapter == null) {
//            if (ui != null) ui.onFailure("No Bluetooth adapter");
//            return;
//        }
//
//        // Android 12+ 권한 체크 (실제 requestPermissions는 액티비티에서)
//        if (!hasScanPerm() || !hasConnectPerm()) {
//            if (ui != null) ui.onFailure("Bluetooth permissions not granted");
//            return;
//        }
//
//        // 블루투스 OFF
//        if (!adapter.isEnabled()) {
//            if (ui != null) ui.onFailure("Bluetooth is OFF");
//            return;
//        }
//
//        // 중복 방지: 기존 세션 정리
//        stopAutoConnect();
//
//        currentTargetMac = mac;
//
//        // BluetoothConnector는 “자동 연결 시도(스캔→발견→본딩까지)” 전체를 담당
//        connector = new BluetoothConnector(app, mac, new BluetoothConnector.Listener() {
//            @Override public void onCarFound(BluetoothDevice device) {
//                if (ui != null) ui.onFound(device);
//            }
//
//            @Override public void onBondingStarted(BluetoothDevice device) {
//                if (ui != null) ui.onBonding(device);
//            }
//
//            @Override public void onBonded(BluetoothDevice device) {
//                // 참고 토스트/로그
//                if (ui != null) ui.showToast("페어링 완료: " + safeName(device));
//            }
//
//            @Override public void onProfilesConnected(BluetoothDevice device, boolean hfp, boolean a2dp) {
//                // ✅ 이제 이 콜백은 "해당 MAC과 BOND_BONDED 완료" 신호로 사용
//                saveConnectedDevice(device);
//                if (ui != null) ui.onConnected(device, hfp, a2dp);
//            }
//
//            @Override public void onAttempt(int attempt, int maxAttempts) {
//                if (ui != null) ui.onAttempt(attempt, maxAttempts);
//            }
//
//            @Override public void onReconnectScheduled(long delayMs) {
//                if (ui != null) ui.onReconnectScheduled(delayMs);
//            }
//
//            @Override public void onFailure(String reason) {
//                if (ui != null) ui.onFailure(reason);
//            }
//        }, autoReconnectEnabled, debug);
//
//        connector.start();
//    }
//
//    /** 스캔/타임아웃/리시버 정리 (페어링/연결 자체는 시스템이 유지) */
//    public void stopAutoConnect() {
//        if (connector != null) {
//            connector.stop();
//            connector = null;
//        }
//        // 필요 시 currentTargetMac 유지 가능
//        // currentTargetMac = null;
//    }
//
//    /** 즉시 재시작 (시도 카운터 리셋). connector가 없고 target만 남았으면 새로 시작 */
//    public void restartNow(Ui ui, boolean autoReconnectEnabled, boolean debug) {
//        if (connector != null) {
//            connector.restartNow();
//        } else if (currentTargetMac != null) {
//            startAutoConnect(currentTargetMac, ui, autoReconnectEnabled, debug);
//        } else {
//            if (ui != null) ui.onFailure("No target MAC to restart");
//        }
//    }
//
//    public boolean isRunning() { return connector != null; }
//    public String getCurrentTargetMac() { return currentTargetMac; }
//
//    /* ========================= BONDED + ACL 링크 감시 ========================= */
//
//    /**
//     * BONDED + ACL_CONNECTED(링크 업)를 기다려 onReady 실행.
//     * timeoutMs에서 둘 다 못 받으면: (bondOk || aclOk)이면 onReady, 아니면 onTimeout.
//     *
//     * 👉 화면 전환 트리거로 이 메서드의 onReady 를 사용하세요.
//     *    (예: DeleteActivity 진입, 메인 화면으로 전환 등)
//     */
//    public void startLinkWatch(String mac, long timeoutMs, Runnable onReady, Runnable onTimeout) {
//        stopLinkWatch();
//
//        if (mac == null || mac.isEmpty()) {
//            if (onTimeout != null) onTimeout.run();
//            return;
//        }
//
//        bondOk = false;
//        aclOk  = false;
//
//        linkWatcher = new BroadcastReceiver() {
//            @Override public void onReceive(Context c, android.content.Intent i) {
//                final String a = i.getAction();
//                final BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (d == null || !mac.equalsIgnoreCase(d.getAddress())) return;
//
//                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(a)) {
//                    int s = i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
//                    if (s == BluetoothDevice.BOND_BONDED) {
//                        bondOk = true;
//                        Log.d(TAG, "BOND BONDED: " + d.getAddress());
//                    }
//                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(a)) {
//                    aclOk = true;
//                    Log.d(TAG, "ACL CONNECTED: " + d.getAddress());
//                }
//
//                if (bondOk && aclOk) {
//                    Log.d(TAG, "Link ready (BONDED + ACL) → proceed");
//                    stopLinkWatch();
//                    cancelLinkTimeout();
//                    if (onReady != null) onReady.run();
//                }
//            }
//        };
//
//        IntentFilter f = new IntentFilter();
//        f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
//        app.registerReceiver(linkWatcher, f);
//        linkWatcherRegistered = true;
//
//        // 이미 본딩된 상태면 먼저 표시
//        try {
//            BluetoothDevice dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
//            if (dev != null && dev.getBondState() == BluetoothDevice.BOND_BONDED) {
//                bondOk = true;
//            }
//        } catch (Exception ignore) {}
//
//        // 타임아웃
//        if (timeoutMs > 0) {
//            linkTimeout = () -> {
//                Log.w(TAG, "Link wait timeout: bond=" + bondOk + " acl=" + aclOk);
//                stopLinkWatch();
//                if (bondOk || aclOk) {
//                    if (onReady != null) onReady.run();  // 부분 충족 시에도 진행
//                } else {
//                    if (onTimeout != null) onTimeout.run();
//                }
//            };
//            mainH.postDelayed(linkTimeout, timeoutMs);
//        }
//    }
//
//    public void stopLinkWatch() {
//        if (!linkWatcherRegistered) return;
//        try { app.unregisterReceiver(linkWatcher); } catch (Exception ignore) {}
//        linkWatcherRegistered = false;
//        linkWatcher = null;
//        cancelLinkTimeout();
//    }
//
//    private void cancelLinkTimeout() {
//        if (linkTimeout != null) {
//            mainH.removeCallbacks(linkTimeout);
//            linkTimeout = null;
//        }
//    }
//
//    /**
//     * 스캔/본딩(자동 연결 시도) + BONDED&ACL 감시를 동시에 시작하는 편의 메서드.
//     *
//     * 액티비티에서는 이 메서드 하나로
//     *  - "저장된 MAC 기준 자동 연결 시도"
//     *  - "링크(BOND+ACL) 준비되었을 때 onReady 로 화면 전환"
//     * 을 한 번에 처리할 수 있습니다.
//     */
//    public void startConnectAndWatch(
//            String mac,
//            Ui ui,
//            boolean autoReconnectEnabled,
//            boolean debug,
//            long linkTimeoutMs,
//            Runnable onReady,
//            Runnable onTimeout
//    ) {
//        stopAutoConnect();
//        stopLinkWatch();
//
//        // 1) 링크 감시 (화면 전환 트리거)
//        startLinkWatch(mac, linkTimeoutMs, onReady, onTimeout);
//
//        // 2) 자동 연결 시도 (스캔→발견→본딩까지)
//        startAutoConnect(mac, ui, autoReconnectEnabled, debug);
//    }
//
//    /* ========================= 권한 유틸 ========================= */
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
//    /* ========================= 유틸 ========================= */
//
//    private String safeName(BluetoothDevice d) {
//        try {
//            return (d.getName() != null ? d.getName() : d.getAddress());
//        } catch (SecurityException se) {
//            return d.getAddress();
//        }
//    }
//}
package com.example.privacy_v1.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
public class BluetoothManager implements BluetoothConnector.BluetoothPairingListener {

    private static final String TAG = "BluetoothManager";

//    private static final String PREF_NAME = "bt_pref";
    private static final String PREF_NAME = "bluetooth_prefs";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_DEVICE_MAC  = "device_mac";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final SharedPreferences prefs;

    private BluetoothConnector connector;
    private BluetoothManagerListener managerListener;

    public BluetoothManager(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getSavedMacAddress() {
        try {
            SharedPreferences prefs =
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_DEVICE_MAC, null);
        } catch (Exception e) {
            Log.e(TAG, "Error reading MAC address", e);
        }
        return null;
    }
    // 기기가 실제로 연결되어 있는지 확인하는 메서드
//    private boolean isDeviceConnected(BluetoothDevice device) {
//        try {
//            Class<?> bluetoothDeviceClass = Class.forName("android.bluetooth.BluetoothDevice");
//            Method isConnectedMethod = bluetoothDeviceClass.getDeclaredMethod("isConnected");
//            return (boolean) isConnectedMethod.invoke(device);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
    private boolean isDeviceConnected(BluetoothDevice device) {
        try {
            Method m = BluetoothDevice.class.getDeclaredMethod("isConnected");
            return (boolean) m.invoke(device);
        } catch (Exception e) {
            return false;
        }
    }


    public void setManagerListener(BluetoothManagerListener listener) {
        this.managerListener = listener;
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    // 필요하면 Activity 에서 Bluetooth 켜는 Intent 를 띄우고, 여기서는 상태만 보고만 있어도 됨
    public void requestEnableBluetooth() {
        if (managerListener != null) {
            managerListener.onNeedEnableBluetooth();
        }
    }

    // ===========================
    // 1) MAC 기준으로 페어링 시작
    // ===========================
    public void startPairingByMac(String macAddress) {
        if (connector != null) connector.release();

        connector = new BluetoothConnector(context, macAddress);
        connector.setPairingListener(this);
        connector.startScanning();  // BluetoothConnector 내부에서 MAC 매칭 -> createBond()
    }

    // ===========================
    // 2) 직접 찾은 BluetoothDevice 를 가지고 페어링
    // ===========================
    public void startPairingWithDevice(BluetoothDevice device) {
        if (!isBluetoothSupported()) {
            if (managerListener != null) {
                managerListener.onPairingError(null, "Bluetooth not supported");
            }
            return;
        }

        if (!isBluetoothEnabled()) {
            if (managerListener != null) {
                managerListener.onNeedEnableBluetooth();
            }
            return;
        }

        releaseConnector();

        connector = new BluetoothConnector(context, null);
        connector.setPairingListener(this);
        connector.ensureBonded(device);
    }

    // ===========================
    // 3) BluetoothConnector 콜백 구현
    // ===========================

    @Override
    public void onScanFinished() {
        Log.d(TAG, "Scan finished");
        if (managerListener != null) {
            managerListener.onScanFinished();
        }
    }

//    @Override
//    public void onDeviceAlreadyBonded(BluetoothDevice device) {
//        Log.d(TAG, "Already bonded: " + device.getName());
//        saveConnectedDevices(device);
//        if (managerListener != null) {
//            managerListener.onDeviceAlreadyRegistered(device);
//        }
//    }
    @Override
    public void onDeviceAlreadyBonded(BluetoothDevice device) {
        Log.d(TAG, "Already bonded: " + device.getName());

        // ★ 저장 (SharedPreferences 방식)
        List<BluetoothDevice> list = new ArrayList<>();
        list.add(device);
        saveConnectedDevices(list);

        if (managerListener != null) managerListener.onDeviceAlreadyRegistered(device);
    }

    @Override
    public void onPairingStarted(BluetoothDevice device) {
        Log.d(TAG, "Pairing started: " + device.getName());
        if (managerListener != null) {
            managerListener.onPairingStarted(device);
        }
    }

    @Override
    public void onDeviceBonded(BluetoothDevice device) {
        Log.d(TAG, "Bonded: " + device.getName());

        // ★ 저장 (SharedPreferences 방식)
        List<BluetoothDevice> list = new ArrayList<>();
        list.add(device);
        saveConnectedDevices(list);

        if (managerListener != null) managerListener.onDeviceRegistered(device);
    }

    @Override
    public void onPairingFailed(BluetoothDevice device) {
        Log.e(TAG, "Pairing failed: " + (device != null ? device.getName() : "null"));
        if (managerListener != null) {
            managerListener.onPairingFailed(device);
        }
    }

    @Override
    public void onPairingError(BluetoothDevice device, String message) {
        Log.e(TAG, "Pairing error: " + message);
        if (managerListener != null) {
            managerListener.onPairingError(device, message);
        }
    }

    // ===========================
    // 4) "차량/헤드폰" 여과 여부 -> 필요시 사용
    // ===========================
    public boolean isCarOrAudioDevice(BluetoothDevice device) {
        try {
            BluetoothClass btClass = device.getBluetoothClass();
            if (btClass == null) {
                Log.w(TAG, "BluetoothClass is null for device: " + device.getName());
                return false;
            }

            int majorDeviceClass = btClass.getMajorDeviceClass();
            int deviceClassCode = btClass.getDeviceClass();

            Log.d(TAG, "Device: " + device.getName()
                    + ", MajorClass: " + majorDeviceClass
                    + ", DeviceClass: " + deviceClassCode);

            // AUDIO/VIDEO 계열이면서 헤드폰, 핸즈프리, 카오디오 등
            return majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO &&
                    (deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                            deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
                            deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED ||
                            deviceClassCode == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO);
        } catch (Exception e) {
            Log.e(TAG, "Error processing device: " + device.getName(), e);
            return false;
        }
    }

    // ===========================
    // 6) JSON 파일에 등록 기기 리스트 저장
    // ===========================
    public void saveConnectedDevices(List<BluetoothDevice> devices) {
        try {
            SharedPreferences prefs =
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            for (BluetoothDevice device : devices) {
                editor.putString(KEY_DEVICE_NAME, device.getName());
                editor.putString(KEY_DEVICE_MAC, device.getAddress());
                Log.d(TAG, "Saved: " + device.getName() + " / " + device.getAddress());
            }
            editor.apply();

        } catch (Exception e) {
            Log.e(TAG, "Error saving device info", e);
        }
    }
    public List<BluetoothDevice> getConnectedDevices() {
        List<BluetoothDevice> connectedDevices = new ArrayList<>();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return connectedDevices;

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device != null && isDeviceConnected(device)) {
                connectedDevices.add(device);
            }
        }
        return connectedDevices;
    }

    // ===========================
    // 7) 정리
    // ===========================
    public void releaseConnector() {
        if (connector != null) {
            connector.release();
            connector = null;
        }
    }

    // Activity/Service 종료 시 호출 권장
    public void releaseAll() {
        releaseConnector();
    }

    // ===========================
    // 외부에 알려줄 콜백 인터페이스
    // ===========================
    public interface BluetoothManagerListener {
        /** 블루투스를 켜야 할 때 (Activity에서 Intent 띄우는 등) */
        default void onNeedEnableBluetooth() {}

        default void onScanFinished() {}

        /** 이미 BONDED 상태인 기기가 다시 선택된 경우 */
        default void onDeviceAlreadyRegistered(BluetoothDevice device) {}

        /** createBond() 시작 */
        default void onPairingStarted(BluetoothDevice device) {}

        /** BOND_BONDED == 페어링(등록) 완료 */
        default void onDeviceRegistered(BluetoothDevice device) {}

        default void onPairingFailed(BluetoothDevice device) {}

        default void onPairingError(BluetoothDevice device, String message) {}
    }
    public void unpairAllBondedDevices() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled");
            return;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices == null || bondedDevices.isEmpty()) {
            Log.d(TAG, "No bonded devices to unpair");
            return;
        }

        for (BluetoothDevice device : bondedDevices) {
            Log.d(TAG, "Unpairing device: " + device.getName() + " [" + device.getAddress() + "]");
            unpairDevice(device);
        }
    }


    private boolean unpairDevice(BluetoothDevice device) {
        try {
            Method removeBondMethod = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            Boolean result = (Boolean) removeBondMethod.invoke(device, (Object[]) null);
            Log.d(TAG, "removeBond(" + device.getAddress() + ") result=" + result);
            return result != null && result;
        } catch (Exception e) {
            Log.e(TAG, "Error unpairing device: " + device.getAddress(), e);
            return false;
        }
    }
}