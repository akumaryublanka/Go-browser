package com.gobrowser.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Draw-only remote pointer overlay. It never intercepts website touches;
 * MainActivity routes remote click/drag gestures directly into the WebView.
 */
public final class RemotePointerView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint feedback = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();

    private float cursorX;
    private float cursorY;
    private int color = Color.rgb(25, 215, 232);
    private int sizeDp = 30;
    private String shape = "ARROW";
    private boolean showOutline = true;
    private boolean showShadow = true;
    private boolean showClickFeedback = true;
    private long clickStartedAt = 0L;

    public RemotePointerView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // Keep the full-screen overlay hardware accelerated; shadows are drawn as
        // compact offset silhouettes to avoid a large software bitmap on 4K TVs.
        setLayerType(View.LAYER_TYPE_NONE, null);
    }

    public void configure(String newShape, int newColor, int newSizeDp,
                          boolean outlineEnabled, boolean shadowEnabled,
                          boolean clickFeedbackEnabled) {
        shape = newShape == null ? "ARROW" : newShape;
        color = newColor;
        sizeDp = newSizeDp;
        showOutline = outlineEnabled;
        showShadow = shadowEnabled;
        showClickFeedback = clickFeedbackEnabled;
        invalidate();
    }

    public void setCursorPosition(float x, float y) {
        cursorX = x;
        cursorY = y;
        invalidate();
    }

    public void showClickFeedback() {
        if (!showClickFeedback) return;
        clickStartedAt = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getVisibility() != VISIBLE) return;

        float density = getResources().getDisplayMetrics().density;
        float size = sizeDp * density;
        float stroke = Math.max(2f, 2.2f * density);
        float x = cursorX;
        float y = cursorY;

        if ("RING".equals(shape)) {
            drawRing(canvas, x, y, size, stroke);
        } else if ("CROSSHAIR".equals(shape)) {
            drawCrosshair(canvas, x, y, size, stroke);
        } else {
            drawArrow(canvas, x, y, size, stroke);
        }

        if (showClickFeedback && clickStartedAt > 0L) {
            long elapsed = System.currentTimeMillis() - clickStartedAt;
            if (elapsed < 280L) {
                float progress = elapsed / 280f;
                float radius = size * (0.22f + progress * 0.72f);
                feedback.setStyle(Paint.Style.STROKE);
                feedback.setStrokeWidth(Math.max(1.5f, 2.5f * density));
                feedback.setColor(color);
                feedback.setAlpha((int) (190 * (1f - progress)));
                canvas.drawCircle(x, y, radius, feedback);
                postInvalidateDelayed(16L);
            } else {
                clickStartedAt = 0L;
            }
        }
    }

    private void drawArrow(Canvas canvas, float x, float y, float size, float stroke) {
        arrow.reset();
        arrow.moveTo(x, y);
        arrow.lineTo(x + size * 0.08f, y + size * 0.93f);
        arrow.lineTo(x + size * 0.34f, y + size * 0.72f);
        arrow.lineTo(x + size * 0.52f, y + size * 1.16f);
        arrow.lineTo(x + size * 0.73f, y + size * 1.07f);
        arrow.lineTo(x + size * 0.55f, y + size * 0.64f);
        arrow.lineTo(x + size * 0.96f, y + size * 0.64f);
        arrow.close();

        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setStrokeCap(Paint.Cap.ROUND);
        if (showShadow) {
            canvas.save();
            canvas.translate(0, size * 0.055f);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(Color.argb(175, 0, 0, 0));
            canvas.drawPath(arrow, fill);
            canvas.restore();
        }

        outline.setStrokeWidth(showOutline ? stroke * 2.4f : stroke);
        outline.setColor(showOutline ? Color.rgb(7, 10, 20) : Color.WHITE);
        outline.clearShadowLayer();
        canvas.drawPath(arrow, outline);

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
        fill.clearShadowLayer();
        canvas.drawPath(arrow, fill);
    }

    private void drawRing(Canvas canvas, float x, float y, float size, float stroke) {
        float radius = size * 0.42f;
        outline.setStyle(Paint.Style.STROKE);
        if (showShadow) {
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(Math.max(stroke * 3f, size * 0.12f));
            outline.setColor(Color.argb(170, 0, 0, 0));
            canvas.drawCircle(x, y + size * 0.045f, radius, outline);
        }
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(showOutline ? stroke * 3.2f : stroke);
        outline.setColor(showOutline ? Color.rgb(7, 10, 20) : Color.WHITE);
        outline.clearShadowLayer();
        canvas.drawCircle(x, y, radius, outline);

        fill.setStyle(Paint.Style.STROKE);
        fill.setStrokeWidth(Math.max(stroke * 1.8f, size * 0.12f));
        fill.setColor(color);
        fill.clearShadowLayer();
        canvas.drawCircle(x, y, radius, fill);
        fill.clearShadowLayer();
        fill.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, Math.max(2f, stroke * 0.9f), fill);
    }

    private void drawCrosshair(Canvas canvas, float x, float y, float size, float stroke) {
        float radius = size * 0.36f;
        outline.setStyle(Paint.Style.STROKE);
        if (showShadow) {
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(Math.max(stroke * 3f, size * 0.075f));
            outline.setStrokeCap(Paint.Cap.ROUND);
            outline.setColor(Color.argb(170, 0, 0, 0));
            float sy = size * 0.045f;
            canvas.drawCircle(x, y + sy, radius, outline);
            canvas.drawLine(x - size * 0.65f, y + sy, x - radius * 0.45f, y + sy, outline);
            canvas.drawLine(x + radius * 0.45f, y + sy, x + size * 0.65f, y + sy, outline);
            canvas.drawLine(x, y - size * 0.65f + sy, x, y - radius * 0.45f + sy, outline);
            canvas.drawLine(x, y + radius * 0.45f + sy, x, y + size * 0.65f + sy, outline);
        }
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(showOutline ? stroke * 3.1f : stroke);
        outline.setStrokeCap(Paint.Cap.ROUND);
        outline.setColor(showOutline ? Color.rgb(7, 10, 20) : Color.WHITE);
        outline.clearShadowLayer();
        canvas.drawCircle(x, y, radius, outline);
        canvas.drawLine(x - size * 0.65f, y, x - radius * 0.45f, y, outline);
        canvas.drawLine(x + radius * 0.45f, y, x + size * 0.65f, y, outline);
        canvas.drawLine(x, y - size * 0.65f, x, y - radius * 0.45f, outline);
        canvas.drawLine(x, y + radius * 0.45f, x, y + size * 0.65f, outline);

        fill.setStyle(Paint.Style.STROKE);
        fill.setStrokeWidth(Math.max(stroke * 1.8f, size * 0.075f));
        fill.setStrokeCap(Paint.Cap.ROUND);
        fill.setColor(color);
        fill.clearShadowLayer();
        canvas.drawCircle(x, y, radius, fill);
        canvas.drawLine(x - size * 0.65f, y, x - radius * 0.45f, y, fill);
        canvas.drawLine(x + radius * 0.45f, y, x + size * 0.65f, y, fill);
        canvas.drawLine(x, y - size * 0.65f, x, y - radius * 0.45f, fill);
        canvas.drawLine(x, y + radius * 0.45f, x, y + size * 0.65f, fill);
        fill.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, Math.max(2f, stroke), fill);
    }
}
