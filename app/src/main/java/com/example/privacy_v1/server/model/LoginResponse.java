package com.example.privacy_v1.server.model;

public class LoginResponse {
    private String device_id;
    private String access_token;
    private String refresh_token;
    // 필요 시 추가 필드 선언

    public String getId() { return device_id; }
    // (setter 생략)
    public String getAc_token() { return access_token; }
    public String getRe_token() { return refresh_token; }

}
