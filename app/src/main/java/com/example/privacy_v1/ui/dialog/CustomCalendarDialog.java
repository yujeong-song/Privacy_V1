package com.example.privacy_v1.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.privacy_v1.R;

import java.util.Calendar;

public class CustomCalendarDialog extends DialogFragment {

    public interface OnYearMonthSetListener {
        void onYearMonthSet(int year, int month);
    }

    private static final String ARG_YEAR  = "arg_year";
    private static final String ARG_MONTH = "arg_month";

    private OnYearMonthSetListener listener;

    public static CustomCalendarDialog newInstance(int year, int month) {
        CustomCalendarDialog f = new CustomCalendarDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH, month);
        f.setArguments(args);
        return f;
    }

    public void setOnYearMonthSetListener(OnYearMonthSetListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        int initYear  = args.getInt(ARG_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        int initMonth = args.getInt(ARG_MONTH, Calendar.getInstance().get(Calendar.MONTH)) + 1;

        View view = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_calender_custom, null);

        NumberPicker npYear  = view.findViewById(R.id.npYear);
        NumberPicker npMonth = view.findViewById(R.id.npMonth);

        // 1) 커스텀 타이틀 뷰 생성
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("년·월 선택");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 30, 0, 30);

        // 연도 범위: 예) 1900 ~ (현재년 + 10)
        int yearNow = Calendar.getInstance().get(Calendar.YEAR);
        npYear.setMinValue(1900);
        npYear.setMaxValue(yearNow + 10);
        npYear.setValue(initYear);

        // 월 범위: 1 ~ 12
        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(initMonth);

        // 달력처럼 글씨를 흰색으로 바꾸고 싶다면 (API21+)
        npYear.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npMonth.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        // 필요하다면 NumberPicker 내부 TextView 색상 커스터마이징 추가 가능
        // 2) 다이얼로그 빌더에 커스텀 타이틀과 뷰, 버튼 설정
        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setCustomTitle(tvTitle)
                .setView(view)
                .setPositiveButton("확인", (d, which) -> {
                    if (listener != null) {
                        listener.onYearMonthSet(npYear.getValue(), npMonth.getValue());
                    }
                })
                .setNegativeButton("취소", null)
                .create();

        // 3) 다이얼로그가 뜰 때 내부 텍스트들 화이트로 변경
        dlg.setOnShowListener(dialog -> {
            // EditText + selector wheel paint 모두 흰색으로 변경
//            setNumberPickerTextColor(npYear, Color.WHITE);
//            setNumberPickerTextColor(npMonth, Color.WHITE);
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.WHITE);
            dlg.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.WHITE);
        });
        return dlg;
    }
}