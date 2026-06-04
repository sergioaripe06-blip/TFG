package com.sergio.flatshare.shared.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sergio.flatshare.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {
    public static class Slice {
        public final int color;
        public final float value;

        public Slice(int color, float value) {
            this.color = color;
            this.value = value;
        }
    }

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private final List<Slice> slices = new ArrayList<>();
    private final DecimalFormat amountFormat = new DecimalFormat("0.##");

    private float density = 1f;

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        slicePaint.setStyle(Paint.Style.STROKE);
        slicePaint.setStrokeCap(Paint.Cap.BUTT);
        slicePaint.setStrokeJoin(Paint.Join.ROUND);

        ringBasePaint.setStyle(Paint.Style.STROKE);
        ringBasePaint.setStrokeCap(Paint.Cap.ROUND);

        innerFillPaint.setStyle(Paint.Style.FILL);
        innerStrokePaint.setStyle(Paint.Style.STROKE);

        topLabelPaint.setTextAlign(Paint.Align.CENTER);
        valueLabelPaint.setTextAlign(Paint.Align.CENTER);
        valueLabelPaint.setFakeBoldText(true);
    }

    private float dp(float value) {
        return value * density;
    }

    private static int withAlpha(int color, float alphaFactor) {
        int alpha = Math.round(Math.max(0f, Math.min(1f, alphaFactor)) * 255f);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void setSlices(List<Slice> values) {
        slices.clear();
        if (values != null) slices.addAll(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float outerRadius = Math.max(0f, (Math.min(width, height) / 2f) - dp(24f));
        float ringWidth = Math.max(dp(18f), outerRadius * 0.23f);
        float innerRadius = Math.max(dp(38f), outerRadius - ringWidth);

        arcRect.set(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius);

        int dividerColor = ContextCompat.getColor(getContext(), R.color.divider_soft);
        int textLightColor = ContextCompat.getColor(getContext(), R.color.text_light);
        int textMutedColor = ContextCompat.getColor(getContext(), R.color.text_muted);
        int surfaceColor = ContextCompat.getColor(getContext(), R.color.surface_dark);

        ringBasePaint.setColor(withAlpha(dividerColor, 0.35f));
        ringBasePaint.setStrokeWidth(ringWidth);
        canvas.drawArc(arcRect, 0f, 360f, false, ringBasePaint);

        innerFillPaint.setColor(surfaceColor);
        canvas.drawCircle(cx, cy, innerRadius + dp(2f), innerFillPaint);

        innerStrokePaint.setColor(withAlpha(dividerColor, 0.7f));
        innerStrokePaint.setStrokeWidth(dp(1.2f));
        canvas.drawCircle(cx, cy, innerRadius + dp(1f), innerStrokePaint);

        float total = 0f;
        for (Slice slice : slices) total += slice.value;

        topLabelPaint.setTextSize(dp(11f));
        topLabelPaint.setColor(textMutedColor);
        valueLabelPaint.setTextSize(dp(16f));
        valueLabelPaint.setColor(textLightColor);

        if (total <= 0f) {
            canvas.drawText("Sin datos", cx, cy + dp(4f), valueLabelPaint);
            return;
        }

        slicePaint.setStrokeWidth(ringWidth);
        float start = -90f;
        float maxGap = slices.size() <= 1 ? 0f : 1.6f;
        for (Slice slice : slices) {
            if (slice.value <= 0f) continue;
            float rawSweep = (slice.value / total) * 360f;
            float gap = Math.min(maxGap, rawSweep * 0.18f);
            float sweep = rawSweep - gap;
            if (sweep > 0f) {
                slicePaint.setColor(slice.color);
                canvas.drawArc(arcRect, start + (gap / 2f), sweep, false, slicePaint);
            }
            start += rawSweep;
        }

        canvas.drawText("Total", cx, cy - dp(2f), topLabelPaint);
        String totalLabel = amountFormat.format(total) + " EUR";
        canvas.drawText(totalLabel, cx, cy + dp(14f), valueLabelPaint);
    }
}
