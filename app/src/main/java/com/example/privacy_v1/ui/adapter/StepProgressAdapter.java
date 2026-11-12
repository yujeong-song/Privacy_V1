package com.example.privacy_v1.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.privacy_v1.utils.SharedPrefsUtil;

public class StepProgressAdapter extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 전체 회차
    private int num;

    // 현재 회차: 1부터 시작
    private int count;

    private final int greenColor = Color.parseColor("#D7C9FF");
    private final int grayColor = Color.parseColor("#9A9A9A");

    private final float circleRadius;
    private final float selectedCircleRadius;
    private final float horizontalPadding;
    private final float lineY;
    private SharedPreferences tk_prefs;
    private SharedPreferences sharedPreferences;

    public StepProgressAdapter(Context context) {
        this(context, null);
    }

    public StepProgressAdapter(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StepProgressAdapter(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);

        circleRadius = dpToPx(4);
        selectedCircleRadius = dpToPx(7);
        horizontalPadding = dpToPx(16);
        lineY = dpToPx(18);

        linePaint.setStrokeWidth(dpToPx(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextSize(spToPx(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * @param num   전체 회차
     * @param count 현재 회차(1부터 시작)
     */
    public void setStepInfo(int num, int count) {
        this.num = Math.max(1, num);
        this.count = Math.max(1, Math.min(count, this.num));

        invalidate();
    }

    public void setNum(int num) {
        this.num = Math.max(1, num);
        this.count = Math.min(count, this.num);

        invalidate();
    }

    public void setCount(int count) {
        this.count = Math.max(1, Math.min(count, num));

        invalidate();
    }

    public int getNum() {
        return num;
    }

    public int getCount() {
        return count;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float startX = horizontalPadding;
        float endX = getWidth() - horizontalPadding;

        // 전체 배경선
        linePaint.setColor(grayColor);
        canvas.drawLine(startX, lineY, endX, lineY, linePaint);

        // num이 1이면 분할 계산 없이 중앙에 표시
        float interval = num > 1
                ? (endX - startX) / (num - 1)
                : 0;

        for (int i = 0; i < num; i++) {

            float x = num == 1
                    ? getWidth() / 2f
                    : startX + (interval * i);

            int step = i + 1;
            boolean isSelected = step == count;

            circlePaint.setColor(isSelected ? greenColor : grayColor);

            float radius = isSelected
                    ? selectedCircleRadius
                    : circleRadius;

            canvas.drawCircle(x, lineY, radius, circlePaint);

            textPaint.setColor(isSelected ? greenColor : grayColor);
            textPaint.setFakeBoldText(isSelected);

            canvas.drawText(
                    step + "회",
                    x,
                    lineY + dpToPx(25),
                    textPaint
            );
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dpToPx(55);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}