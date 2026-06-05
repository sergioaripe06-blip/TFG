package com.sergio.flatshare.shared.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.sergio.flatshare.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonthlyBarChartView extends View {
    private static final Pattern DAY_MONTH_YEAR_PATTERN = Pattern.compile("^(\\d{2})/(\\d{2})/(\\d{4})$");

    public static class Bar {
        public final String label;
        public final float value;

        public Bar(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final List<Bar> bars = new ArrayList<>();
    private final DecimalFormat amountFormat = new DecimalFormat("0.00");

    public MonthlyBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(context.getColor(R.color.divider_soft));
        gridPaint.setStrokeWidth(dp(1.0f));

        axisTextPaint.setColor(context.getColor(R.color.text_muted));
        axisTextPaint.setTextSize(sp(11f));

        barPaint.setColor(context.getColor(R.color.primary_green_bright));

        emptyPaint.setColor(context.getColor(R.color.text_muted));
        emptyPaint.setTextSize(sp(13f));
    }

    public void setBars(@Nullable List<Bar> items) {
        bars.clear();
        if (items != null) {
            bars.addAll(items);
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(desiredChartWidth());
        int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) {
            drawCenteredText(canvas, "Sin datos mensuales");
            return;
        }

        float max = 0f;
        for (Bar bar : bars) {
            if (bar.value > max) {
                max = bar.value;
            }
        }
        if (max <= 0f) {
            drawCenteredText(canvas, "Sin pagos en este periodo");
            return;
        }

        float leftPad = dp(16f);
        float rightPad = dp(12f);
        float topPad = dp(16f);
        float bottomPad = dp(64f);
        float yAxisWidth = dp(78f);

        float plotLeft = leftPad + yAxisWidth;
        float plotTop = topPad;
        float plotRight = getWidth() - rightPad;
        float plotBottom = getHeight() - bottomPad;
        float plotHeight = Math.max(1f, plotBottom - plotTop);
        float plotWidth = Math.max(1f, plotRight - plotLeft);

        drawHorizontalGrid(canvas, plotLeft, plotRight, plotTop, plotBottom, max);

        int count = bars.size();
        float slotWidth = plotWidth / count;
        float barWidth = Math.max(dp(10f), slotWidth * 0.56f);
        float radius = dp(4f);

        for (int i = 0; i < count; i++) {
            Bar bar = bars.get(i);
            float centerX = plotLeft + (i + 0.5f) * slotWidth;
            float ratio = Math.max(0f, bar.value / max);
            float barHeight = ratio * plotHeight;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;
            float top = plotBottom - barHeight;
            barRect.set(left, top, right, plotBottom);
            canvas.drawRoundRect(barRect, radius, radius, barPaint);

            drawBarLabel(canvas, bar.label, centerX);
        }
    }

    private void drawBarLabel(Canvas canvas, String label, float centerX) {
        if (label == null) return;
        String normalized = label.trim();
        if (normalized.isEmpty()) return;

        Matcher dateMatcher = DAY_MONTH_YEAR_PATTERN.matcher(normalized);
        if (dateMatcher.matches()) {
            String monthLine = dateMatcher.group(2);
            String yearLine = dateMatcher.group(3);
            float baseY = getHeight() - dp(30f);
            drawCenteredAxisText(canvas, monthLine, centerX, baseY);
            drawCenteredAxisText(canvas, yearLine, centerX, baseY + dp(14f));
            return;
        }

        String[] parts = normalized.split(" ", 2);
        float baseY = getHeight() - dp(28f);
        if (parts.length >= 2) {
            drawCenteredAxisText(canvas, parts[0], centerX, baseY);
            drawCenteredAxisText(canvas, parts[1], centerX, baseY + dp(14f));
            return;
        }
        drawCenteredAxisText(canvas, normalized, centerX, getHeight() - dp(16f));
    }

    private void drawCenteredAxisText(Canvas canvas, String text, float centerX, float baselineY) {
        float labelWidth = axisTextPaint.measureText(text);
        canvas.drawText(text, centerX - labelWidth / 2f, baselineY, axisTextPaint);
    }

    private void drawHorizontalGrid(Canvas canvas, float left, float right, float top, float bottom, float max) {
        float[] ratios = new float[]{0f, 0.5f, 1f};
        for (float ratio : ratios) {
            float y = bottom - ((bottom - top) * ratio);
            canvas.drawLine(left, y, right, y, gridPaint);

            float value = max * ratio;
            String amountLabel = amountFormat.format(value) + " EUR";
            float labelX = dp(6f);
            float labelY = y + dp(3.5f);
            canvas.drawText(amountLabel, labelX, labelY, axisTextPaint);
        }
    }

    private void drawCenteredText(Canvas canvas, String text) {
        float width = emptyPaint.measureText(text);
        float x = (getWidth() - width) / 2f;
        float y = (getHeight() / 2f) + dp(2f);
        canvas.drawText(text, x, y, emptyPaint);
    }

    private float desiredChartWidth() {
        int count = Math.max(1, bars.size());
        float leftPad = dp(16f);
        float rightPad = dp(12f);
        float yAxisWidth = dp(78f);
        float slotWidth = dp(44f);
        return leftPad + rightPad + yAxisWidth + (slotWidth * count);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
