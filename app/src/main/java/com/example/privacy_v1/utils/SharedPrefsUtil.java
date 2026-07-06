package com.example.privacy_v1.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class SharedPrefsUtil {
//    private static final String PREF_name = "RandomMacPrefs";
//
//    public static void setCount(Context context, int count) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_name, Context.MODE_PRIVATE);
//        sharedPreferences.edit().putInt("count", count).apply();
//    }
//
//    public static int getCount(Context context) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_name, Context.MODE_PRIVATE);
//        return sharedPreferences.getInt("count", 1);
//    }
//
//    public static void resetCount(Context context) {
//        setCount(context, 1);
//    }
    private static final String PREF_NAME = "RandomMacPrefs";
    private static final String KEY_COUNT = "count";
    private static final String KEY_USED_MACS = "used_macs";

    public static void setCount(Context context, int count) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(KEY_COUNT, count).apply();
    }

    public static int getCount(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_COUNT, 1);
    }

    public static void resetCount(Context context) {
        setCount(context, 1);
    }

    public static Set<String> getUsedMacs(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(sharedPreferences.getStringSet(KEY_USED_MACS, new HashSet<>()));
    }

    public static void addUsedMac(Context context, String macAddress) {
        Set<String> usedMacs = getUsedMacs(context);
        usedMacs.add(macAddress);
        setUsedMacs(context, usedMacs);
    }

    public static void setUsedMacs(Context context, Set<String> usedMacs) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putStringSet(KEY_USED_MACS, new HashSet<>(usedMacs)).apply();
    }

    public static void resetUsedMacs(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(KEY_USED_MACS).apply();
    }
}

