package com.sergio.flatshare.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

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

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Slice> slices = new ArrayList<>();

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSlices(List<Slice> values) {
        slices.clear();
        if (values != null) slices.addAll(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = Math.min(getWidth(), getHeight());
        float pad = 16f;
        rect.set(
                (getWidth() - size) / 2f + pad,
                (getHeight() - size) / 2f + pad,
                (getWidth() + size) / 2f - pad,
                (getHeight() + size) / 2f - pad
        );

        float total = 0f;
        for (Slice slice : slices) total += slice.value;
        if (total <= 0f) {
            paint.setColor(0xFF29402C);
            canvas.drawOval(rect, paint);
            return;
        }

        float start = -90f;
        for (Slice slice : slices) {
            float sweep = (slice.value / total) * 360f;
            paint.setColor(slice.color);
            canvas.drawArc(rect, start, sweep, true, paint);
            start += sweep;
        }
    }
}
