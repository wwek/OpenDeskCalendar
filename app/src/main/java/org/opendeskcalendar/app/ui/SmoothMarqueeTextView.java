package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

public final class SmoothMarqueeTextView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "";
    private float textWidth;
    private float gapPx;
    private float fadeEdgePx;
    private float speedPxPerSecond;
    private int fadeColor = Color.TRANSPARENT;

    public SmoothMarqueeTextView(Context context) {
        super(context);
        paint.setTypeface(Typeface.DEFAULT);
        gapPx = dp(8);
        fadeEdgePx = dp(12);
        speedPxPerSecond = dp(22);
    }

    public void setText(String value) {
        text = value == null ? "" : value;
        textWidth = paint.measureText(text);
        requestLayout();
        invalidate();
    }

    public void setTextColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setTextSizePx(float textSizePx) {
        paint.setTextSize(textSizePx);
        textWidth = paint.measureText(text);
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        paint.setTypeface(typeface == null ? Typeface.DEFAULT : typeface);
        textWidth = paint.measureText(text);
        requestLayout();
        invalidate();
    }

    public void setFadeColor(int color) {
        fadeColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        int desiredHeight = (int) Math.ceil(metrics.descent - metrics.ascent);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (text.length() == 0) {
            return;
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = (getHeight() - metrics.descent - metrics.ascent) / 2f;
        int width = getWidth();
        if (textWidth <= width) {
            canvas.drawText(text, 0f, baseline, paint);
            return;
        }

        float cycleWidth = textWidth + gapPx;
        float offset = (SystemClock.uptimeMillis() * speedPxPerSecond / 1000f) % cycleWidth;
        float x = -offset;
        while (x < width) {
            canvas.drawText(text, x, baseline, paint);
            x += cycleWidth;
        }
        applyEdgeFade(canvas, width);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidateDelayed(16L);
        }
    }

    private void applyEdgeFade(Canvas canvas, int width) {
        float fadeWidth = Math.min(fadeEdgePx, width / 3f);
        if (fadeWidth <= 0f) {
            return;
        }
        int transparentFade = Color.argb(0, Color.red(fadeColor), Color.green(fadeColor), Color.blue(fadeColor));

        fadePaint.setShader(new LinearGradient(
                0f,
                0f,
                fadeWidth,
                0f,
                fadeColor,
                transparentFade,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, fadeWidth, getHeight(), fadePaint);

        fadePaint.setShader(new LinearGradient(
                width - fadeWidth,
                0f,
                width,
                0f,
                transparentFade,
                fadeColor,
                Shader.TileMode.CLAMP));
        canvas.drawRect(width - fadeWidth, 0f, width, getHeight(), fadePaint);

        fadePaint.setShader(null);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
