package com.example.privacy_v1.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;

public class AuthMainActivity extends AppCompatActivity {
    private static final String TAG = "AuthMainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_main);
        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(v -> {
            // Navigate to PreMainActivity
            startActivity(new Intent(AuthMainActivity.this, LoginActivity.class));
            finish();
        });

    }
}