package com.example.privacy_v1.server.model;

public class CreditInfoRequest {

    private String device_id;
    private String month;

    public CreditInfoRequest(String device_id, String month) {
        this.device_id = device_id;
        this.month = month;
    }

    // getter (필요시 setter도 추가)
    public String getDevice_id() { return device_id; }
    public String getMonth() { return month; }
}
