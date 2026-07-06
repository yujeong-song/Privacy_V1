package com.example.privacy_v1.common;

public class Crumb {
    public static String build(String category, String makerName, String carName, String modelName, String last) {
        StringBuilder sb = new StringBuilder();
        append(sb, category);
        append(sb, makerName);
        append(sb, carName);
        append(sb, modelName);
        append(sb, last);
        return sb.toString();
    }
    private static void append(StringBuilder sb, String s) {
        if (s == null) return;
        String v = s.trim();
        if (v.isEmpty()) return;
        if (sb.length() > 0) sb.append(" > ");
        sb.append(v);
    }
}