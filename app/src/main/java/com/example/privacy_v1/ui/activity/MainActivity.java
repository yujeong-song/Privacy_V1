package com.example.privacy_v1.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.privacy_v1.R;
import com.example.privacy_v1.ui.auth.MyPageActivity;
import com.example.privacy_v1.ui.carinfo.MakerSelectActivity;

public class MainActivity extends AppCompatActivity {
    private static final String Tag = "MainActivity";
    private static final int REQ_PERMS = 1001;
    private LinearLayout privacy_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //idle 상태 보내기
        ensurePermissions();

        privacy_btn = findViewById(R.id.btn_privacy);

        // 첫 번째 버튼 클릭 이벤트 처리
        privacy_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //API 시작 트리거 전송
                    //로그인 정보 확인 //기기 안에 로그인 정보 넣어놓기
                Intent intent = new Intent(MainActivity.this, PrivacyStartActivity.class);
                startActivity(intent);
//                Intent intent = new Intent(MainActivity.this, MakerSelectActivity.class);
//                startActivity(intent);

            }
        });

        // 두 번째 버튼 클릭 이벤트 처리
        findViewById(R.id.btn_go_to_mypage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
                startActivity(intent);
                // 버튼 클릭 시 Toast 메시지 표시
//                Toast.makeText(MainActivity.this, "마이페이지로 이동 버튼이 클릭되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void ensurePermissions() {
        String[] perms = new String[] {
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS
        };

        boolean need = false;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                need = true;
                Log.d(Tag, "권한 필요: " + perm);
                break;
            }
        }

        if (need) {
            Log.d(Tag, "권한 요청 시작");
            ActivityCompat.requestPermissions(this, perms, REQ_PERMS);
        }else {
            Log.d(Tag, "권한 이미 허용됨");
        }
    }


}