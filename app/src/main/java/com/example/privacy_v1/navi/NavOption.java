package com.example.privacy_v1.navi;

public class NavOption {
    private final String name;
    private final String action;
    private final int num;

    public NavOption(String name, String action, int num) {
        this.name = name;
        this.action = action;
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }
    public int getNum() {
        return num;
    }
}
