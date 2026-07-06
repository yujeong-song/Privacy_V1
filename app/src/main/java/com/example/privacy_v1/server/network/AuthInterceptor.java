package com.example.privacy_v1.server.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class AuthInterceptor implements Interceptor {
    private Context context;
    private HttpLoggingInterceptor logging;
    public AuthInterceptor(Context context) {
        this.context = context;
    }
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();
        // 로그인/회원가입/리프레시 등 토큰 없는 요청 예외처리
        if (path.equals("/account/login") || path.equals("/account/auto-login")) {
            return chain.proceed(original);
        }
        SharedPreferences tk_prefs = context.getSharedPreferences("tk_prefs", Context.MODE_PRIVATE);
        String accessToken = tk_prefs.getString("access_token", null);

        // 디버그 로그: 이 값이 null인지, 제대로 읽혔는지 확인
        Log.d("AuthInterceptor", "Loaded token: " + accessToken);

        Request.Builder builder = original.newBuilder();
        if (accessToken != null && !accessToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken); // 서버에 맞게 헤더명/값 수정
        }
        Request authorized = builder.build();

        // 디버그 로그: 헤더 실제 상태 확인
        Log.d("AuthInterceptor", "Final headers:\n" + authorized.headers("Authorization"));
        Log.d("AuthInterceptor", "URL: " + authorized.url());
//        return chain.proceed(builder.build());
        return chain.proceed(authorized);
    }
}
