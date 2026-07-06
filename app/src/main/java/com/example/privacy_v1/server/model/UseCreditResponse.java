package com.example.privacy_v1.server.model;

public class UseCreditResponse {

    private int amount;
    private String timestamp;
    private String message;
    // 필요 시 추가 필드 선언

    public int getCredit() { return amount; }
    public String getTime() { return timestamp; }
    // (setter 생략)
    public String getMessage() { return message; }
    // (setter 생략)
}
