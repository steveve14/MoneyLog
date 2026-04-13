package com.moneylog.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    private static final int[] COLORS = {
        0xFF506356, // Sage Green (primary)
        0xFF7B9E87, // Light sage
        0xFF3D4C42, // Dark sage
        0xFFA8C4B0, // Pale sage
        0xFF2E3A32, // Deep green
        0xFFBFD4C7, // Mint
        0xFF6B8F7A, // Medium sage
        0xFF8CB49E, // Soft green
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Slice> slices = new ArrayList<>();

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setData(List<Float> values) {
        slices.clear();
        float total = 0;
        for (float v : values) total += v;
        if (total <= 0) {
            invalidate();
            return;
        }
        float startAngle = -90f;
        for (int i = 0; i < values.size(); i++) {
            float sweep = values.get(i) / total * 360f;
            slices.add(new Slice(startAngle, sweep, COLORS[i % COLORS.length]));
            startAngle += sweep;
        }
        invalidate();
    }

    public int getColor(int index) {
        return COLORS[index % COLORS.length];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slices.isEmpty()) return;

        int size = Math.min(getWidth(), getHeight());
        float padding = size * 0.05f;
        rect.set(padding, padding, size - padding, size - padding);

        for (Slice s : slices) {
            paint.setColor(s.color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawArc(rect, s.startAngle, s.sweepAngle, true, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) (160 * getResources().getDisplayMetrics().density);
        int w = resolveSize(defaultSize, widthMeasureSpec);
        int h = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(w, h);
        if (size <= 0) size = defaultSize;
        setMeasuredDimension(size, size);
    }

    private static class Slice {
        final float startAngle;
        final float sweepAngle;
        final int color;

        Slice(float startAngle, float sweepAngle, int color) {
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.color = color;
        }
    }
}
