package com.example.privacy_v1.service;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.privacy_v1.ui.activity.DeleteActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogMonitoringService extends Service {
    private static final String MAP_CLICK_SUCCESS =
            "CLICK result ok=true key=MAP|메시지에 접근 허용";
    // 한 번의 차량 연결 과정에서 중복 실행 방지
    private final AtomicBoolean smsFunctionCalled = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // 서비스가 시작되면 로그 모니터링을 시작
        SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        String mode = tk_prefs.getString("mode", null);
        // null(또는 빈값)이라면 기본값("Normal")로 치환
        if (mode == null || mode.trim().isEmpty()) {
            mode = "Pro";
        }

        switch (mode) {
            case "Standard":
                monitorPbapLogTimeout();
                break;
            case "bmw":
                bmwmonitorPbapLogAndHandleCompletion();
                break;
            case "Pro":
            default:
                monitorPbapLogAndHandleCompletion();
                break;
        }
        return START_STICKY;  // 서비스가 종료되지 않고 계속 실행되도록 설정
    }

//    private void monitorPrivacy() {
//        new Thread(() -> {
//            try {
//                Log.d("PBAPMonitor", "Logcat 로그 버퍼 초기화");
//                // 루트 권한으로 logcat 버퍼 비우기 (과거 로그 삭제)
//                Process clearLogcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat", "-c"});
//                clearLogcatProcess.waitFor();
//
//                // 루트 권한으로 logcat 실행 (실시간 로그 모니터링)
//                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat", "-v", "time", "BluetoothPbapVcardManager:V", "*:S"});
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                String line;
//                bufferedReader.close();
//            } catch (Exception e) {
//                Log.e("PBAPMonitor", "에러 발생: " + e.getMessage(), e);
//            }
//        }).start();
//    }

    private void monitorPbapLogTimeout() {
        new Thread(() -> {
            try {
                Log.d("LOGMonitor", "TimeOUT Short 버전 시작");
                Log.d("PBAPMonitor", "Logcat 로그 버퍼 초기화");

                // 루트 권한으로 logcat 버퍼 비우기 (과거 로그 삭제)
                Process clearLogcatProcess = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat", "-c"});
                clearLogcatProcess.waitFor();

                Log.d("PBAPMonitor", "로그 버퍼가 비워졌습니다.");

                // 루트 권한으로 logcat 실행 (실시간 로그 모니터링)
                Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat", "-v", "time", "BluetoothPbapVcardManager:V", "*:S"});
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int matchCount = 0; // 패턴이 발견된 횟수 카운팅 변수

                Log.d("PBAPMonitor", "로그 읽기 시작");
                long patternFoundTime = -1; // 특정 로그가 처음 발견된 시점을 저장 (아직 발견되지 않으면 -1)
                Timer timer = new Timer(); // 타이머 객체 생성
                boolean timerScheduled = false; // 타이머가 이미 예약되었는지 확인

                // 로그를 한 줄씩 실시간으로 읽어오면서 처리
                while ((line = bufferedReader.readLine()) != null) {
                    Log.d("PBAPMonitor", "로그 내용: " + line);
                    // MAP 메시지 접근 허용 버튼 클릭 성공 감지
                    if (line.contains(MAP_CLICK_SUCCESS)) {
                        Log.d("PBAPMonitor", "MAP 메시지 접근 허용 성공 로그 감지");
                        if (smsFunctionCalled.compareAndSet(false, true)) {
                            scheduleExistingSmsFunction();
                        }
                    }

                    // 특정 로그 패턴을 찾으면 전송 완료로 인식
                    if (line.contains("contactIdCursor size: ")) {
                        //                        ---여기서부터 수정
                        // 패턴이 처음 발견되었을 때
                        if (patternFoundTime == -1) {
                            patternFoundTime = System.currentTimeMillis();
                            Date patternDate = new Date(patternFoundTime);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            String patternFormattedTime = sdf.format(patternDate);
                            Log.d("startTime", "패턴 발견 시작 시간: " + patternFormattedTime);

                            // 패턴이 처음 발견된 시점부터 3분 후에 실행할 작업 예약 (단, 아직 threshold에 도달하지 않은 경우)
                            if (!timerScheduled) {
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Log.e("PBAPMonitor", "패턴 발견 후 1분 경과 - 자동 처리 실행");
                                        Intent intent = new Intent("com.example.ACTION_PATTERN_DETECTED");
                                        sendBroadcast(intent);
                                        try {
                                            bufferedReader.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        // 프로세스 종료 (선택 사항, 필요에 따라)
                                        process.destroy();
                                    }
                                }, 60000); // 180,000밀리초 == 3분 60000밀리초 = 1분
                                timerScheduled = true;
                            }
                        }
//                        ---여기까지 수정
                        matchCount++; // 패턴이 발견될 때마다 카운팅
                        Log.d("PBAPMonitor", "contactIdCursor size:  패턴 발견 (횟수: " + matchCount + ")");


                        // 두 번째로 패턴이 발견되었을 때 처리
                        if (matchCount == 2) {
                            // 패턴이 두 번째로 감지되었을 때 싱글톤 클래스에 상태 저장
                            PatternDetector.getInstance().setPatternDetected(true);
                            Log.d("PBAPMonitor", "Pattern 감지됨, PatternDetector 상태 업데이트 완료");

                            // 브로드캐스트 송신
                            Intent intent = new Intent("com.example.ACTION_PATTERN_DETECTED");
                            sendBroadcast(intent); // 패턴 감지에 대한 브로드캐스트 송신
                            break;
                        }
                    }
                }
                bufferedReader.close();
            } catch (Exception e) {
                Log.e("PBAPMonitor", "에러 발생: " + e.getMessage(), e);
            }
        }).start();
    }

    private void scheduleExistingSmsFunction() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            new Thread(() -> {
                try {
                    // 이 부분만 현재 사용 중인 SMS 함수로 변경
                    createSms();
                    Log.d(TAG, "기존 SMS 함수 실행 완료");
                } catch (Exception e) {
                    Log.e(TAG, "기존 SMS 함수 실행 실패", e);
                    // 실패 시 로그를 다시 감지하여 재실행하도록 허용
                    smsFunctionCalled.set(false);
                }
            }).start();

        }, 0); // MAP 버튼 클릭 성공 2초 후 실행
    }

    private void createSms() {
        Log.d(TAG, "SMS 생성 요청");
        executor.execute(() -> {
            try {
                Log.d(TAG, "DummyDataManager.createSms() 호출");

                DummyDataManager.createSms(
                        getApplicationContext(),
                        10,
                        (current, total, message) -> {
                            Log.d(TAG,
                                    "SMS 생성 진행: "
                                            + current + "/" + total
                                            + ", " + message);
                        }
                );
                Log.d(TAG, "SMS 생성 완료");
            } catch (Exception e) {
                Log.e(TAG, "SMS 생성 실패", e);
                // 생성 실패 시 다시 시도할 수 있도록 초기화
                smsFunctionCalled.set(false);
            }
        });
    }

    private void monitorPbapLogAndHandleCompletion() {
        new Thread(() -> {
            try {
                Log.d("LOGMonitor", "Delete Normal 버전 시작");
                Log.d("PBAPMonitor", "Logcat 로그 버퍼 초기화");

                // 루트 권한으로 logcat 버퍼 비우기 (과거 로그 삭제)
                Process clearLogcatProcess = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat", "-c"});
                clearLogcatProcess.waitFor();

                Log.d("PBAPMonitor", "로그 버퍼가 비워졌습니다.");

                // 루트 권한으로 logcat 실행 (실시간 로그 모니터링)
                Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat", "-v", "time", "BluetoothPbapVcardManager:V", "*:S"});
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int matchCount = 0; // 패턴이 발견된 횟수 카운팅 변수

                Log.d("PBAPMonitor", "로그 읽기 시작");

                // 로그를 한 줄씩 실시간으로 읽어오면서 처리
                while ((line = bufferedReader.readLine()) != null) {
                    Log.d("PBAPMonitor", "로그 내용: " + line);

                    // 특정 로그 패턴을 찾으면 전송 완료로 인식
                    if (line.contains("contactIdCursor size: ")) {
                        matchCount++; // 패턴이 발견될 때마다 카운팅
                        Log.d("PBAPMonitor", "contactIdCursor size:  패턴 발견 (횟수: " + matchCount + ")");

                        // 두 번째로 패턴이 발견되었을 때 처리
                        if (matchCount == 2) {
                            // msg manager 실행으로 넘기기
                            // msg manager가 끝났을 때 아래 사항 실행시키기
                            // 패턴이 두 번째로 감지되었을 때 싱글톤 클래스에 상태 저장
                            PatternDetector.getInstance().setPatternDetected(true);
                            Log.d("PBAPMonitor", "Pattern 감지됨, PatternDetector 상태 업데이트 완료");

                            // 브로드캐스트 송신
                            Intent intent = new Intent("com.example.ACTION_PATTERN_DETECTED");
                            sendBroadcast(intent); // 패턴 감지에 대한 브로드캐스트 송신
                            break;
                        }
                    }
                }
                bufferedReader.close();
            } catch (Exception e) {
                Log.e("PBAPMonitor", "에러 발생: " + e.getMessage(), e);
            }
        }).start();
    }
    private void bmwmonitorPbapLogAndHandleCompletion() {
        new Thread(() -> {
            try {
                Log.d("LOGMonitor", "Delete Normal 버전 시작");
                Log.d("PBAPMonitor", "Logcat 로그 버퍼 초기화");

                // 루트 권한으로 logcat 버퍼 비우기 (과거 로그 삭제)
                Process clearLogcatProcess = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat", "-c"});
                clearLogcatProcess.waitFor();

                Log.d("PBAPMonitor", "로그 버퍼가 비워졌습니다.");

                // 루트 권한으로 logcat 실행 (실시간 로그 모니터링)
                Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat", "-v", "time", "BluetoothPbapVcardManager:V", "*:S"});
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int matchCount = 0; // 패턴이 발견된 횟수 카운팅 변수

                Log.d("PBAPMonitor", "로그 읽기 시작");

                // 로그를 한 줄씩 실시간으로 읽어오면서 처리
                while ((line = bufferedReader.readLine()) != null) {
                    Log.d("PBAPMonitor", "로그 내용: " + line);

                    // 특정 로그 패턴을 찾으면 전송 완료로 인식
                    if (line.contains("contactIdCursor size: ")) {
                        matchCount++; // 패턴이 발견될 때마다 카운팅
                        Log.d("PBAPMonitor", "contactIdCursor size:  패턴 발견 (횟수: " + matchCount + ")");

                        // 두 번째로 패턴이 발견되었을 때 처리
                        if (line.contains("contactIdCursor size: 499")){
                            PatternDetector.getInstance().setPatternDetected(true);
                            Log.d("PBAPMonitor", "Pattern 감지됨, PatternDetector 상태 업데이트 완료");

                            // 브로드캐스트 송신
                            Intent intent = new Intent("com.example.ACTION_PATTERN_DETECTED");
                            sendBroadcast(intent); // 패턴 감지에 대한 브로드캐스트 송신
                            break;

                        }
//                        if (matchCount == 2) {
//                            // 패턴이 두 번째로 감지되었을 때 싱글톤 클래스에 상태 저장
//                            PatternDetector.getInstance().setPatternDetected(true);
//                            Log.d("PBAPMonitor", "Pattern 감지됨, PatternDetector 상태 업데이트 완료");
//
//                            // 브로드캐스트 송신
//                            Intent intent = new Intent("com.example.ACTION_PATTERN_DETECTED");
//                            sendBroadcast(intent); // 패턴 감지에 대한 브로드캐스트 송신
//                            break;
//                        }
                    }
                }
                bufferedReader.close();
            } catch (Exception e) {
                Log.e("PBAPMonitor", "에러 발생: " + e.getMessage(), e);
            }
        }).start();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // 이 서비스는 바인딩되지 않으므로 null 반환
        return null;
    }
}