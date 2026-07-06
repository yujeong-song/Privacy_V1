package com.example.privacy_v1.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.privacy_v1.R;

public class CustomDeconnectedDialog extends Dialog{
    private static final String TAG = "CustomDeconnectedDialog";
    private AppCompatActivity context;
    private Button confirmButton;

    public CustomDeconnectedDialog(@NonNull AppCompatActivity context) {
        super(context);
        this.context = context;
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // 앱 종료
        context.finishAffinity(); // 현재 스택에 있는 모든 Activity를 종료
        System.exit(0); // 프로세스 강제 종료 (선택 사항)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_deconnected_device); // 커스텀 레이아웃 적용
        confirmButton = findViewById(R.id.confirm_button);
//        // Window 설정
        Window window = getWindow();
        if (window != null) {
//            int width = (int) (ViewGroup.LayoutParams.MATCH_PARENT*0.7);
            window.setLayout(600, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
//        setOnKeyListener((dialog, keyCode, event) -> {
//            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
//                // 앱 종료
//                ((Activity) getContext()).finish();
//                return true; // 이벤트 처리 완료
//            }
//            return false; // 기본 동작 수행
//        });


        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                context.startActivity(intent);
                dismiss();
            }
        });
    }

}