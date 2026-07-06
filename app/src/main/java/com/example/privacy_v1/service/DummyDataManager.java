package com.example.privacy_v1.service;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class DummyDataManager {
    private static final String TAG = "DummyDataManager";
    private static final String NAME_PREFIX = "ACAT_Privacy_";
    private static final String NUMBER_PREFIX = "099";
    private static String lastDebugLog = "";

    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
    }

    public static void createContactPool(Context context, int count, ProgressCallback callback) throws Exception {
        Log.d(TAG, "createContactPool() 시작, count=" + count);

        List<PersonItem> personItems = generatePersonItems(count);
        Log.d(TAG, "생성된 personItems size=" + personItems.size());
        ContentResolver resolver = context.getContentResolver();

        final int batchSize = 100;
        int inserted = 0;

        for (int start = 0; start < personItems.size(); start += batchSize) {
            int end = Math.min(start + batchSize, personItems.size());
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            for (int i = start; i < end; i++) {
                PersonItem item = personItems.get(i);
                int rawContactInsertIndex = ops.size();

                // RawContact 생성
                ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build());

                // 이름 저장
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, item.name)
                        .build());

                // 번호 저장
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, item.number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build());
            }

            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            inserted = end;

            if (callback != null) {
                callback.onProgress(inserted, count, "전화번호부(이름+번호) 생성 중");
            }
        }
    }

//    public static void createCallLogs(Context context, int count, ProgressCallback callback) throws Exception {
    public static void createCallLogs(Context context, ProgressCallback callback) throws Exception{
        Log.d(TAG, "createCallLogs() 시작, count=");

        List<PersonItem> personItems = loadContactsFromProvider(context);
        Log.d(TAG, "loadContactsFromProvider() 결과 size=" + personItems.size());

        if (personItems.size() < 200) {
            throw new IllegalStateException("먼저 버튼 1로 전화번호부를 생성하세요. 현재 개수=" + personItems.size());
        }

        final int incomingCount = 100;
        final int outgoingCount = 100;
        final int missedCount = 100;
        final int totalCount = incomingCount + outgoingCount + missedCount;

        Random random = new Random();

        long now = System.currentTimeMillis();
        long oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000);
        Log.d(TAG, "최근 1주 시작 시각 oneWeekAgo=" + oneWeekAgo + ", now=" + now);

        // 타입 목록 구성
        List<Integer> callTypes = new ArrayList<>();
        for (int i = 0; i < incomingCount; i++) callTypes.add(1); // 수신
        for (int i = 0; i < outgoingCount; i++) callTypes.add(2); // 발신
        for (int i = 0; i < missedCount; i++) callTypes.add(3);   // 부재중

        // 순서를 섞어서 자연스럽게 보이게 함
        Collections.shuffle(callTypes, random);

        for (int i = 0; i < totalCount; i++) {
            PersonItem item = personItems.get(random.nextInt(personItems.size()));
            int type = callTypes.get(i);

            // 최근 1주 사이 랜덤 시간
            long date = oneWeekAgo + (long) (random.nextDouble() * (now - oneWeekAgo));

            // 수신/발신만 통화시간 부여, 부재중은 0
            int duration;
            if (type == 3) {
                duration = 0;
            } else {
                duration = 5 + random.nextInt(600);
            }

            String cmd = "content insert " +
                    "--uri content://call_log/calls " +
                    "--bind number:s:" + item.number + " " +
                    "--bind date:l:" + date + " " +
                    "--bind duration:i:" + duration + " " +
                    "--bind type:i:" + type + " " +
                    "--bind new:i:1";

            if (i < 5) {
                Log.d(TAG, "샘플 명령[" + i + "] = " + cmd);
            }

            Process process = null;
            BufferedReader stdOutReader = null;
            BufferedReader errorReader = null;

            try {
                long start = System.currentTimeMillis();

                process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});

                stdOutReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

                int exitCode = process.waitFor();
                long end = System.currentTimeMillis();

                String stdOut = readAll(stdOutReader);
                String err = readAll(errorReader);

                boolean providerError = err != null &&
                        (err.contains("Error while accessing provider")
                                || err.contains("IllegalArgumentException")
                                || err.contains("Exception")
                                || err.contains("invalid")
                                || err.contains("Invalid"));

                if (exitCode != 0 || providerError) {
                    Log.e(TAG, "통화기록 개별 insert 실패 index=" + i +
                            ", exitCode=" + exitCode +
                            ", stdout=" + stdOut +
                            ", stderr=" + err);
                    throw new RuntimeException(
                            "통화기록 insert 실패 index=" + i +
                                    ", exitCode=" + exitCode +
                                    ", stderr=" + err
                    );
                }

                if (i < 5) {
                    Log.d(TAG, "통화기록 개별 insert 성공 index=" + i + ", 소요(ms)=" + (end - start));
                }

            } finally {
                closeQuietly(stdOutReader);
                closeQuietly(errorReader);
                if (process != null) {
                    process.destroy();
                }
            }

            if ((i + 1) % 10 == 0 || i == totalCount - 1) {
                Log.d(TAG, "통화기록 생성 진행: " + (i + 1) + " / " + totalCount);
                if (callback != null) {
                    callback.onProgress(i + 1, totalCount, "통화기록 생성 중");
                }
            }
        }

        Log.d(TAG, "createCallLogs() 완료");
    }

//    public static void createSms(Context context, int count, ProgressCallback callback) throws Exception {
//        Log.d(TAG, "createSms() 시작, count=" + count);
//
//        List<PersonItem> personItems = loadContactsFromProvider(context);
//        Log.d(TAG, "loadContactsFromProvider() 결과 size=" + personItems.size());
//
//        if (personItems.size() < 15000) {
//            throw new IllegalStateException("먼저 버튼 1로 전화번호부 15000개를 생성하세요. 현재 개수=" + personItems.size());
//        }
//
//        if (count > personItems.size()) {
//            throw new IllegalArgumentException("SMS 건수는 전화번호부 개수보다 클 수 없습니다.");
//        }
//
//        // 같은 번호 반복 사용 방지 -> 같은 대화방으로 묶일 가능성 최소화
//        List<PersonItem> shuffled = new ArrayList<>(personItems);
//        Collections.shuffle(shuffled, new Random());
//        List<PersonItem> selected = shuffled.subList(0, count);
//        Log.d(TAG, "SMS용 selected size=" + selected.size());
//
//        long now = System.currentTimeMillis();
//        long startTime = now - (120L * 24 * 60 * 60 * 1000L);
//        Log.d(TAG, "SMS 시간 범위 시작값 startTime=" + startTime);
//
//        for (int i = 0; i < selected.size(); i++) {
//            PersonItem item = selected.get(i);
//            long date = startTime + (i * 11L * 60L * 1000L);
//
//            // 한글 본문은 쉘 인코딩 문제를 피하려고 영문 유지
//            String body = "dummy message from " + item.name;
//
//            String cmd = "content insert " +
//                    "--uri content://sms/inbox " +
//                    "--bind address:s:" + item.number + " " +
//                    "--bind body:s:" + quote(body) + " " +
//                    "--bind date:l:" + date + " " +
//                    "--bind read:i:0 " +
//                    "--bind seen:i:1 " +
//                    "--bind type:i:1";
//
//            if (i < 5) {
//                Log.d(TAG, "SMS 샘플 명령[" + i + "] = " + cmd);
//            }
//
//            Process process = null;
//            BufferedReader stdOutReader = null;
//            BufferedReader errorReader = null;
//
//            try {
//                long start = System.currentTimeMillis();
//
//                process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
//
//                stdOutReader = new BufferedReader(
//                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
//                errorReader = new BufferedReader(
//                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
//
//                int exitCode = process.waitFor();
//                long end = System.currentTimeMillis();
//
//                String stdOut = readAll(stdOutReader);
//                String err = readAll(errorReader);
//
//                boolean providerError = err != null &&
//                        (err.contains("Error while accessing provider")
//                                || err.contains("IllegalArgumentException")
//                                || err.contains("Exception")
//                                || err.contains("invalid")
//                                || err.contains("Invalid"));
//
//                if (exitCode != 0 || providerError) {
//                    Log.e(TAG, "SMS 개별 insert 실패 index=" + i +
//                            ", exitCode=" + exitCode +
//                            ", stdout=" + stdOut +
//                            ", stderr=" + err);
//
//                    throw new RuntimeException(
//                            "SMS insert 실패 index=" + i +
//                                    ", exitCode=" + exitCode +
//                                    ", stderr=" + err
//                    );
//                }
//
//                if (i < 5) {
//                    Log.d(TAG, "SMS 개별 insert 성공 index=" + i + ", 소요(ms)=" + (end - start));
//                    if (stdOut != null && !stdOut.isEmpty()) {
//                        Log.d(TAG, "SMS stdout[" + i + "]=" + stdOut);
//                    }
//                    if (err != null && !err.isEmpty()) {
//                        Log.d(TAG, "SMS stderr[" + i + "]=" + err);
//                    }
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, "SMS 개별 insert 예외 발생 index=" + i, e);
//                throw e;
//            } finally {
//                closeQuietly(stdOutReader);
//                closeQuietly(errorReader);
//
//                if (process != null) {
//                    process.destroy();
//                }
//            }
//
//            if ((i + 1) % 10 == 0 || i == selected.size() - 1) {
//                Log.d(TAG, "SMS 생성 진행: " + (i + 1) + " / " + count);
//
//                if (callback != null) {
//                    callback.onProgress(i + 1, count, "SMS 생성 중 (이름은 연락처 매칭)");
//                }
//            }
//        }
//
//        Log.d(TAG, "createSms() 완료");
//    }
    public static void createSms(Context context, int count, ProgressCallback callback) throws Exception {
        Log.d(TAG, "createSms() 시작, count=" + count);

        // 1. 기존 코드 방식: 연락처 Provider에서 전화번호 로드
        List<PersonItem> personItems = loadContactsFromProvider(context);
        Log.d(TAG, "loadContactsFromProvider() 결과 size=" + personItems.size());

        if (personItems.size() < count) {
            throw new IllegalStateException(
                    "SMS 생성에 필요한 연락처가 부족합니다. 필요=" + count + ", 현재=" + personItems.size()
            );
        }

        // 2. 같은 번호 반복 방지: 연락처 랜덤 섞기 후 count만큼 선택
        List<PersonItem> shuffled = new ArrayList<>(personItems);
        Collections.shuffle(shuffled, new Random());
        List<PersonItem> selected = shuffled.subList(0, count);

        Log.d(TAG, "SMS용 selected size=" + selected.size());

        int successCount = 0;
        int failCount = 0;

        long now = System.currentTimeMillis();

        /*
         * 이전 Root 방식:
         * 최신 메시지처럼 보이도록 1초 간격으로 생성
         * 마지막 메시지가 가장 최신 시간이 됩니다.
         */
        long startTime = now - ((long) count - 1L) * 1000L;

        for (int i = 0; i < selected.size(); i++) {
            PersonItem item = selected.get(i);

            long messageTime = startTime + (i * 1000L);

            String address = item.number;

            /*
             * content insert --bind는 ':' 문자에 민감할 수 있으므로
             * 본문에는 ':'가 들어가지 않도록 구성합니다.
             */
            String body = "ACAT Privacy message from "
                    + item.name
                    + " / "
                    + formatTimeForBody(messageTime);

            boolean inserted = insertInboxSmsByRoot(
                    context,
                    address,
                    body,
                    messageTime
            );

            if (inserted) {
                successCount++;
            } else {
                failCount++;
            }

            notifySmsProviderChanged(context.getContentResolver());

            if ((i + 1) % 10 == 0 || i == selected.size() - 1) {
                Log.d(TAG, "SMS 생성 진행: " + (i + 1) + " / " + count
                        + ", success=" + successCount
                        + ", fail=" + failCount);

                if (callback != null) {
                    callback.onProgress(i + 1, count, "SMS 생성 중 (연락처 번호 사용)");
                }
            }

            sleep(500);
        }

        notifySmsProviderChanged(context.getContentResolver());

        Log.d(TAG, "createSms() 완료, success=" + successCount + ", fail=" + failCount);

        if (failCount > 0) {
            throw new RuntimeException(
                    "SMS 생성 중 일부 실패. 성공=" + successCount
                            + ", 실패=" + failCount
                            + "\n마지막 로그:\n" + lastDebugLog
            );
        }
    }
    private static boolean insertInboxSmsByRoot(
            Context context,
            String address,
            String body,
            long dateMillis
    ) {
        try {
            long threadId = getOrCreateThreadIdSafely(context, address);
            int subId = getDefaultSmsSubId(context);

            String packageName = context.getPackageName();

            String safeAddress = makeContentBindSafeText(address);
            String safeBody = makeContentBindSafeText(body);

            String escapedAddress = escapeShellText(safeAddress);
            String escapedBody = escapeShellText(safeBody);
            String escapedCreator = escapeShellText(packageName);

            String command =
                    "content insert --uri content://sms/inbox "
                            + "--bind address:s:'" + escapedAddress + "' "
                            + "--bind body:s:'" + escapedBody + "' "
                            + "--bind date:l:" + dateMillis + " "
                            + "--bind date_sent:l:" + dateMillis + " "
                            + "--bind read:i:0 "
                            + "--bind seen:i:0 "
                            + "--bind type:i:1 "
                            + "--bind protocol:i:0 "
                            + "--bind status:i:-1 "
                            + "--bind reply_path_present:i:0 "
                            + "--bind thread_id:l:" + threadId + " "
                            + "--bind sub_id:i:" + subId + " "
                            + "--bind creator:s:'" + escapedCreator + "'";

            CommandResult insertResult = runRootCommand(command);

            StringBuilder debug = new StringBuilder();
            debug.append("INSERT COMMAND:\n").append(command).append("\n\n");
            debug.append("INSERT exitCode=").append(insertResult.exitCode).append("\n");
            debug.append("INSERT stdout=\n").append(insertResult.stdout).append("\n");
            debug.append("INSERT stderr=\n").append(insertResult.stderr).append("\n");

            Log.d(TAG, debug.toString());

            if (insertResult.exitCode != 0
                    || containsContentCommandError(insertResult.stdout)
                    || containsContentCommandError(insertResult.stderr)) {

                debug.append("\ncontent insert 명령이 실패했습니다.\n");
                lastDebugLog = debug.toString();
                return false;
            }

            boolean exists = verifySmsExistsByRoot(
                    safeAddress,
                    dateMillis,
                    debug
            );

            lastDebugLog = debug.toString();
            return exists;

        } catch (Exception e) {
            Log.e(TAG, "insertInboxSmsByRoot 오류", e);
            lastDebugLog = "insertInboxSmsByRoot 오류: " + e.getMessage();
            return false;
        }
    }
    private static boolean verifySmsExistsByRoot(
            String address,
            long dateMillis,
            StringBuilder debug
    ) {
        String safeAddress = makeContentBindSafeText(address);

        String queryCommand =
                "content query --uri content://sms/inbox "
                        + "--projection _id:address:body:date:read:seen:type:thread_id "
                        + "--where \"address='" + safeAddress + "' AND date=" + dateMillis + "\"";

        CommandResult queryResult = runRootCommand(queryCommand);

        debug.append("\nVERIFY COMMAND:\n").append(queryCommand).append("\n\n");
        debug.append("VERIFY exitCode=").append(queryResult.exitCode).append("\n");
        debug.append("VERIFY stdout=\n").append(queryResult.stdout).append("\n");
        debug.append("VERIFY stderr=\n").append(queryResult.stderr).append("\n");

        Log.d(TAG, debug.toString());

        return queryResult.exitCode == 0
                && queryResult.stdout != null
                && queryResult.stdout.contains("address=" + safeAddress)
                && queryResult.stdout.contains("date=" + dateMillis);
    }


    private static List<PersonItem> generatePersonItems(int count) {
        Log.d(TAG, "generatePersonItems() 시작, count=" + count);

        Set<String> uniqueNumbers = new LinkedHashSet<>();
        Random random = new Random();

        while (uniqueNumbers.size() < count) {
            int numberPart = random.nextInt(100_000_000);
            String number = String.format(Locale.US, "%s%08d", NUMBER_PREFIX, numberPart);
            uniqueNumbers.add(number);

            if (uniqueNumbers.size() % 1000 == 0) {
                Log.d(TAG, "번호 생성 진행: " + uniqueNumbers.size() + " / " + count);
            }
        }

        List<PersonItem> list = new ArrayList<>();
        int index = 1;
        for (String number : uniqueNumbers) {
            String name = String.format(Locale.US, "%s%05d", NAME_PREFIX, index);
            list.add(new PersonItem(name, number));
            index++;
        }

        Log.d(TAG, "generatePersonItems() 완료, list size=" + list.size());
        return list;
    }

    private static List<PersonItem> loadContactsFromProvider(Context context) {
        Log.d(TAG, "loadContactsFromProvider() 시작");

        List<PersonItem> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{NUMBER_PREFIX + "%"},
                null
        );

        if (cursor != null) {
            try {
                int idxName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int idxNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                Log.d(TAG, "연락처 cursor count=" + cursor.getCount());

                while (cursor.moveToNext()) {
                    String name = cursor.getString(idxName);
                    String number = cursor.getString(idxNumber);
                    String normalized = normalizeNumber(number);

                    if (normalized.startsWith(NUMBER_PREFIX) && !unique.contains(normalized)) {
                        unique.add(normalized);
                        result.add(new PersonItem(name, normalized));

                        if (result.size() <= 5) {
                            Log.d(TAG, "샘플 연락처: name=" + name + ", number=" + normalized);
                        }

                        if (result.size() % 1000 == 0) {
                            Log.d(TAG, "연락처 로드 진행: " + result.size());
                        }
                    }
                }
            } finally {
                cursor.close();
                Log.d(TAG, "연락처 cursor close()");
            }
        } else {
            Log.e(TAG, "연락처 query 결과 cursor=null");
        }

        Log.d(TAG, "loadContactsFromProvider() 완료, result size=" + result.size());
        return result;
    }
    private static boolean containsContentCommandError(String text) {
        if (text == null) {
            return false;
        }

        String lower = text.toLowerCase(Locale.US);

        return lower.contains("[error]")
                || lower.contains("binding not well formed")
                || lower.contains("usage: adb shell content")
                || lower.contains("exception")
                || lower.contains("java.lang")
                || lower.contains("error while accessing provider");
    }

    private static long getOrCreateThreadIdSafely(Context context, String address) {
        try {
            return Telephony.Threads.getOrCreateThreadId(context, address);
        } catch (Exception e) {
            Log.e(TAG, "thread_id 생성 실패. 기본값 0 사용", e);
            return 0;
        }
    }

    private static int getDefaultSmsSubId(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED) {

                    int subId = SubscriptionManager.getDefaultSmsSubscriptionId();

                    if (subId >= 0) {
                        return subId;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "sub_id 조회 실패", e);
        }

        return -1;
    }

    private static CommandResult runRootCommand(String command) {
        Process process = null;

        try {
            process = Runtime.getRuntime().exec(new String[]{
                    "su",
                    "-c",
                    command
            });

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            int exitCode = process.waitFor();

            return new CommandResult(exitCode, stdout, stderr);

        } catch (Exception e) {
            Log.e(TAG, "runRootCommand 오류", e);
            return new CommandResult(-1, "", e.getMessage());

        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String readStream(java.io.InputStream inputStream) {
        StringBuilder builder = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

        } catch (Exception e) {
            Log.e(TAG, "readStream 오류", e);
        }

        return builder.toString();
    }

    private static void notifySmsProviderChanged(ContentResolver resolver) {
        try {
            resolver.notifyChange(Uri.parse("content://sms"), null);
            resolver.notifyChange(Uri.parse("content://sms/inbox"), null);
            resolver.notifyChange(Uri.parse("content://mms-sms"), null);
            resolver.notifyChange(Uri.parse("content://mms-sms/conversations"), null);
        } catch (Exception e) {
            Log.e(TAG, "notifySmsProviderChanged 오류", e);
        }
    }

    private static String makeContentBindSafeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace(":", "-")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    private static String escapeShellText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("'", "'\\''");
    }

    private static String formatTimeForBody(long millis) {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH-mm-ss",
                Locale.KOREA
        ).format(new Date(millis));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class CommandResult {
        int exitCode;
        String stdout;
        String stderr;

        CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    private static String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[^0-9]", "");
    }

    private static String quote(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static String readAll(BufferedReader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                Log.e(TAG, "closeQuietly() 실패", e);
            }
        }
    }

    private static class PersonItem {
        final String name;
        final String number;

        PersonItem(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }
}