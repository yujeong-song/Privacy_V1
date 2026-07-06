package com.example.privacy_v1.server.network;

import com.example.privacy_v1.server.model.AutoLoginRequest;
import com.example.privacy_v1.server.model.AutoLoginResponse;
import com.example.privacy_v1.server.model.CreditInfoRequest;
import com.example.privacy_v1.server.model.LoginResponse;
import com.example.privacy_v1.server.model.LoginRequest;
import com.example.privacy_v1.server.model.MonitoringRequest;
import com.example.privacy_v1.server.model.MonitoringResponse;
import com.example.privacy_v1.server.model.StartedRequest;
import com.example.privacy_v1.server.model.StartedResponse;
import com.example.privacy_v1.server.model.UsageItem;
import com.example.privacy_v1.server.model.UseCreditRequest;
import com.example.privacy_v1.server.model.UseCreditResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("account/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("account/auto-login")
    Call<AutoLoginResponse> autologin(@Body AutoLoginRequest body);

    @POST("credit/deleting-started")
    Call<StartedResponse> started(@Body StartedRequest body);
    @POST("credit/use-credit")
    Call<UseCreditResponse> usecredit(@Body UseCreditRequest body);

    @POST("credit/monthly-credit-history")
    Call<List<UsageItem>> creditinfo(@Body CreditInfoRequest body);

//    @POST("monitoring/update-by-device")
//    Call<MonitoringResponse> monitoring(@Body MonitoringRequest body);
}
