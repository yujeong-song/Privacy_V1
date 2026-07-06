package com.example.privacy_v1.service;

public class PatternDetector {
    private static PatternDetector instance;
    private boolean isPatternDetected = false; // 패턴 감지 여부를 저장하는 변수

    private PatternDetector() {}

    public static synchronized PatternDetector getInstance() {
        if (instance == null) {
            instance = new PatternDetector();
        }
        return instance;
    }

    public void setPatternDetected(boolean detected) {
        this.isPatternDetected = detected;
    }

    public boolean isPatternDetected() {
        return isPatternDetected;
    }

    public void resetPatternDetected() {
        this.isPatternDetected = false;
    }
}

