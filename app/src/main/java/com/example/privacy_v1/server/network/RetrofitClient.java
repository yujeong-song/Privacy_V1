package com.example.privacy_v1.server.network;

import android.content.Context;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://220.149.254.67/";
//    private static final String BASE_URL = "http://220.149.254.67:8081/"; //개발용 서버url
    private static Retrofit instance;      // 토큰 없는 요청용
    private static Retrofit authInstance;  // 토큰 있는 요청용
    public static Retrofit getInstance() {
        if (instance == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                    message -> Log.d("OkHttp", message)
            );
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return instance;
    }

    /**
     * 토큰 필요한 요청용
     * 예: /credit/deleting-started
     */
    public static Retrofit getInstance(Context context) {
        if (authInstance == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                    message -> Log.d("OkHttp", message)
            );
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context.getApplicationContext()))
                    .addInterceptor(loggingInterceptor)
                    .build();

            authInstance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return authInstance;
    }
}

//    public static Retrofit getInstance() { // 토큰 없는 요청용 (ex. 로그인)
//        if (instance == null) {
////            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
////            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
////
////            OkHttpClient client = new OkHttpClient.Builder()
////                    .addInterceptor(logging)
////                    .build();
//            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
//                    message -> Log.d("OkHttp", message)
//            );
//            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//            OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                    .addInterceptor(new AuthInterceptor(context))
//                    .addInterceptor(loggingInterceptor)
//                    .build();
//
//
//            instance = new Retrofit.Builder()
//                    .baseUrl(BASE_URL)
//                    .client(client)
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .build();
//        }
//        return instance;
//    }
//
//    public static Retrofit getInstance(Context context) {
//        if (instance == null) {
//            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message ->
//                    Log.d("OkHttp", message)
//            );
//            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
////            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
////            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
//            AuthInterceptor auth = new AuthInterceptor(context);
//            OkHttpClient client = new OkHttpClient.Builder()
//                    .addInterceptor(auth)
//                    .addInterceptor(logging)
//                    .build();
//
//            instance = new Retrofit.Builder()
//                    .baseUrl(BASE_URL)
//                    .client(client)
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .build();
//        }
//        return instance;
//    }

