package com.example.privacy_v1.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;
import com.example.privacy_v1.ui.activity.MainActivity;

public class MyPageActivity extends AppCompatActivity {
    private static final String TAG = "MyPageActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        TextView name_label = findViewById(R.id.name_label);
        SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        String name =  tk_prefs.getString("device_id", null);

        name_label.setText("" +name+" 님");
        TextView credit_info = findViewById(R.id.credit_info);
        credit_info.setOnClickListener(v -> {
            // Navigate to PreMainActivity
            startActivity(new Intent(MyPageActivity.this, CreditActivity.class));
            finish();
        });

        ImageButton beforeButton = findViewById(R.id.mypage_before);
        beforeButton.setOnClickListener(v -> {
//            startActivity(new Intent(MyPageActivity.this, MainActivity.class));
            finish();
        });

    }
}