package com.example.personalplanner.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.personalplanner.R;

import java.util.Locale;

public class WeeklyPlanChartView extends View {
    private static final String[] DAY_LABELS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pendingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint completedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overduePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int[] totals = new int[7];
    private int[] completed = new int[7];
    private int[] overdue = new int[7];
    private int maxValue = 1;

    public WeeklyPlanChartView(Context context) {
        super(context);
        init();
    }

    public WeeklyPlanChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeeklyPlanChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.outline));
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setAlpha(150);

        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.surface_variant));
        pendingPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
        completedPaint.setColor(ContextCompat.getColor(getContext(), R.color.success));
        overduePaint.setColor(ContextCompat.getColor(getContext(), R.color.danger));

        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(11));
        labelPaint.setFakeBoldText(true);

        valuePaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(sp(12));
        valuePaint.setFakeBoldText(true);
    }

    public void setData(int[] totals, int[] completed, int[] overdue) {
        this.totals = normalize(totals);
        this.completed = normalize(completed);
        this.overdue = normalize(overdue);
        this.maxValue = Math.max(1, findMax(this.totals));
        setContentDescription(buildDescription());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float left = getPaddingLeft() + dp(6);
        float right = width - getPaddingRight() - dp(6);
        float top = getPaddingTop() + dp(20);
        float bottom = height - getPaddingBottom() - dp(30);
        float chartHeight = Math.max(dp(80), bottom - top);

        drawGrid(canvas, left, right, top, bottom);

        if (findMax(totals) == 0) {
            drawEmpty(canvas, width, top, bottom);
            drawDayLabels(canvas, left, right, bottom);
            return;
        }

        float cell = (right - left) / DAY_LABELS.length;
        float barWidth = Math.max(dp(16), Math.min(dp(28), cell * 0.46f));
        float radius = dp(8);

        for (int i = 0; i < DAY_LABELS.length; i++) {
            float centerX = left + cell * i + cell / 2f;
            int total = Math.max(0, totals[i]);
            int done = Math.max(0, Math.min(completed[i], total));
            int late = Math.max(0, Math.min(overdue[i], total - done));
            int pending = Math.max(0, total - done - late);

            float totalHeight = chartHeight * total / maxValue;
            float barLeft = centerX - barWidth / 2f;
            float barRight = centerX + barWidth / 2f;
            float barTop = bottom - totalHeight;

            rect.set(barLeft, barTop, barRight, bottom);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            float cursorBottom = bottom;
            cursorBottom = drawSegment(canvas, pendingPaint, barLeft, barRight, cursorBottom,
                    totalHeight * pending / Math.max(1, total), radius);
            cursorBottom = drawSegment(canvas, completedPaint, barLeft, barRight, cursorBottom,
                    totalHeight * done / Math.max(1, total), radius);
            drawSegment(canvas, overduePaint, barLeft, barRight, cursorBottom,
                    totalHeight * late / Math.max(1, total), radius);

            canvas.drawText(String.valueOf(total), centerX, Math.max(top + dp(10), barTop - dp(7)),
                    valuePaint);
        }

        drawDayLabels(canvas, left, right, bottom);
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom) {
        int lines = 4;
        for (int i = 0; i <= lines; i++) {
            float y = top + (bottom - top) * i / lines;
            canvas.drawLine(left, y, right, y, gridPaint);
        }
    }

    private float drawSegment(Canvas canvas, Paint paint, float left, float right,
                              float bottom, float height, float radius) {
        if (height <= 0) {
            return bottom;
        }
        float top = Math.max(getPaddingTop(), bottom - height);
        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, radius, radius, paint);
        return top;
    }

    private void drawDayLabels(Canvas canvas, float left, float right, float bottom) {
        float cell = (right - left) / DAY_LABELS.length;
        float y = bottom + dp(22);
        for (int i = 0; i < DAY_LABELS.length; i++) {
            float centerX = left + cell * i + cell / 2f;
            canvas.drawText(DAY_LABELS[i], centerX, y, labelPaint);
        }
    }

    private void drawEmpty(Canvas canvas, int width, float top, float bottom) {
        Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(sp(13));
        canvas.drawText("Chưa có dữ liệu tuần này", width / 2f, (top + bottom) / 2f, emptyPaint);
    }

    private int[] normalize(int[] source) {
        int[] result = new int[7];
        if (source == null) {
            return result;
        }
        System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
        return result;
    }

    private int findMax(int[] values) {
        int max = 0;
        if (values == null) {
            return max;
        }
        for (int value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private String buildDescription() {
        StringBuilder builder = new StringBuilder("Thống kê tuần: ");
        for (int i = 0; i < DAY_LABELS.length; i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(String.format(Locale.US, "%s %d kế hoạch, %d hoàn thành, %d quá hạn",
                    DAY_LABELS[i], totals[i], completed[i], overdue[i]));
        }
        return builder.toString();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
