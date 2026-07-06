package com.example.privacy_v1.common;

public class DisplayName {
    public static String makerKo(String makerKey) {
        if (makerKey == null) return "";
        switch (makerKey) {
            case "hyundai": return "현대";
            case "kia": return "기아";
            case "genesis": return "제네시스";
            default: return makerKey;
        }
    }
}