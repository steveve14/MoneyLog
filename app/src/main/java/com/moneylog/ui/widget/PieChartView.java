package com.moneylog.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Chart.js Doughnut 스타일 도넛 차트.
 * - 두꺼운 라운드캡 아크
 * - 세그먼트 간 갭
 * - 호버 시 약간 팽창하는 애니메이션
 * - 중앙 빈 공간 (cutout 60 %)
 */
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

    /** 도넛 두께 비율 (반지름 대비). 0.4 = 40 % → cutout ≈ 60 % */
    private static final float THICKNESS_RATIO = 0.35f;
    /** 세그먼트 사이 갭 (도) */
    private static final float GAP_DEGREES = 2.5f;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect  = new RectF();

    private final List<Slice> slices = new ArrayList<>();
    private float animProgress = 1f;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setColor(0x0D000000); // 5 % black — subtle track
    }

    /* ── public API ─────────────────────────────────── */

    public void setData(List<Float> values) {
        slices.clear();
        float total = 0;
        for (float v : values) total += v;
        if (total <= 0) {
            invalidate();
            return;
        }

        int count = values.size();
        float totalGap = count > 1 ? GAP_DEGREES * count : 0;
        float available = 360f - totalGap;
        if (available < 0) available = 360f;

        float startAngle = -90f;
        for (int i = 0; i < count; i++) {
            float sweep = values.get(i) / total * available;
            if (sweep < 0.5f) sweep = 0.5f; // 최소 시각적 크기
            slices.add(new Slice(startAngle, sweep, COLORS[i % COLORS.length]));
            startAngle += sweep + (count > 1 ? GAP_DEGREES : 0);
        }

        // 등장 애니메이션
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(700);
        anim.setInterpolator(new DecelerateInterpolator(1.8f));
        anim.addUpdateListener(a -> {
            animProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public int getColor(int index) {
        return COLORS[index % COLORS.length];
    }

    /* ── drawing ────────────────────────────────────── */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;

        float strokeWidth = size * THICKNESS_RATIO;
        float half = strokeWidth / 2f;
        float inset = half + size * 0.02f; // 작은 여백
        arcRect.set(inset, inset, size - inset, size - inset);

        // 배경 트랙
        bgPaint.setStrokeWidth(strokeWidth);
        canvas.drawArc(arcRect, 0, 360, false, bgPaint);

        if (slices.isEmpty()) return;

        arcPaint.setStrokeWidth(strokeWidth);

        for (Slice s : slices) {
            arcPaint.setColor(s.color);
            float animatedSweep = s.sweepAngle * animProgress;
            canvas.drawArc(arcRect, s.startAngle, animatedSweep, false, arcPaint);
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

    /* ── model ──────────────────────────────────────── */

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
