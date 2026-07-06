package com.example.privacy_v1.server.model;

public class AutoLoginRequest {
    private String refresh_token;

    public AutoLoginRequest(String refresh_token) {
        this.refresh_token = refresh_token;
    }
}
