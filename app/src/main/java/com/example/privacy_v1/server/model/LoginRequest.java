package com.example.privacy_v1.server.model;

public class LoginRequest {

        private String device_id;
        private String password;

        public LoginRequest(String username, String password) {
            this.device_id = username;
            this.password = password;
        }
        // (getter/setter 생략)
}