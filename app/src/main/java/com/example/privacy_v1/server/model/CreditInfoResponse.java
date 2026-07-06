package com.example.privacy_v1.server.model;

import java.util.List;


public class CreditInfoResponse {
    private boolean success;
    private String message;
    private List<UsageItem> data;

    // 기본 생성자, getter/setter
    public CreditInfoResponse() {}
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<UsageItem> getData() { return data; }
}


