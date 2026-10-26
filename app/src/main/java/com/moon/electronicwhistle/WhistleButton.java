package com.moon.electronicwhistle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

/** A large, accessible native control with a whistle silhouette and a live state ring. */
final class WhistleButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring = new RectF();
    private final Path whistle = new Path();
    private String label;
    private boolean sounding;

    WhistleButton(Context context) {
        super(context);
        setId(R.id.whistle_button);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(MainActivity.LIME);
        setBackground(new RippleDrawable(ColorStateList.valueOf(0x40101715), circle, null));
        setElevation(dp(8));
        whistle.moveTo(3, 17);
        whistle.lineTo(27, 17);
        whistle.lineTo(27, 7);
        whistle.lineTo(58, 7);
        whistle.lineTo(58, 23);
        whistle.lineTo(40, 23);
        whistle.lineTo(40, 32);
        whistle.cubicTo(40, 43, 32, 51, 21, 51);
        whistle.cubicTo(10, 51, 3, 43, 3, 32);
        whistle.close();
    }

    void update(String label, boolean sounding, String description) {
        this.label = label;
        this.sounding = sounding;
        setContentDescription(description);
        setActivated(sounding);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(sounding ? 4 : 1));
        paint.setColor(sounding ? 0xFF1D3925 : 0x40405229);
        ring.set(dp(10), dp(10), width - dp(10), height - dp(10));
        canvas.drawOval(ring, paint);

        canvas.save();
        canvas.translate(width / 2 - dp(43), height / 2 - dp(51));
        canvas.scale(dp(1.45f), dp(1.45f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF172317);
        canvas.drawPath(whistle, paint);
        paint.setColor(MainActivity.LIME);
        canvas.drawCircle(21, 32, 7, paint);
        canvas.drawRect(33, 10, 39, 17, paint);
        paint.setColor(0xFF172317);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.3f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(45, 0, 47, -6, paint);
        canvas.drawLine(53, 1, 59, -3, paint);
        canvas.restore();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF172317);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(20 * getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.CENTER);
        if (label != null) canvas.drawText(label, width / 2, height / 2 + dp(52), paint);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
