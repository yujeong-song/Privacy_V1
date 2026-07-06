package com.example.privacy_v1.server.model;

import com.google.gson.annotations.SerializedName;

public class UsageItem {
    @SerializedName("datetime")
    private String datetime;        // ex: "2025-06-24"
    @SerializedName("vehicle_number")
    private String vehicle_number; // ex: "차량 번호"
    @SerializedName("index")
    private int index;         // ex: 4500

    // 기본 생성자, getter/setter
    public UsageItem() {
    }

    public String getDate() {
        return datetime;
    }

    public String getDescription() {
        return vehicle_number;
    }

    public int getAmount() {
        return index;
    }
}
