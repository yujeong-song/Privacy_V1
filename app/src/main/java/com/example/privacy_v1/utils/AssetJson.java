package com.example.privacy_v1.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetJson {
//    public static String read(Context ctx, String filename) {
//        try (BufferedReader br = new BufferedReader(
//                new InputStreamReader(ctx.getAssets().open(filename), StandardCharsets.UTF_8))) {
//            StringBuilder sb = new StringBuilder();
//            String line; while ((line = br.readLine()) != null) sb.append(line);
//            return sb.toString();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to read assets/" + filename, e);
//        }
//    }
private static final String TAG = "AssetJson";

    public static String read(Context ctx, String fileName) throws Exception {
        String safe = normalizeFileName(fileName);

        try {
            InputStream is = ctx.getAssets().open(safe);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            // UTF-8 BOM 제거
            if (sb.length() > 0 && sb.charAt(0) == '\uFEFF') {
                sb.deleteCharAt(0);
            }
            return sb.toString();

        } catch (Exception e) {
            // 어떤 파일들이 assets에 있는지 로그로 확인
            try {
                String[] list = ctx.getAssets().list("");
                Log.e(TAG, "open 실패 파일=" + safe + " / assets 목록:", e);
                if (list != null) {
                    for (String s : list) Log.e(TAG, " - " + s);
                }
            } catch (Exception ignore) {}
            throw e;
        }
    }

    private static String normalizeFileName(String fileName) {
        if (fileName == null) return "";
        String f = fileName.trim();
        f = f.replace("\\", "/");
        if (f.startsWith("assets/")) f = f.substring("assets/".length());
        if (f.startsWith("/")) f = f.substring(1);
        // 확장자 누락 대비
        if (!f.endsWith(".json")) f = f + ".json";
        return f;
    }
}
