package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.opendeskcalendar.app.data.AppSettings;

public final class WeatherIconView extends View {
    private final String condition;
    private final int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public WeatherIconView(Context context, String condition, int color) {
        super(context);
        this.condition = condition == null ? "" : condition;
        this.color = color;
    }

    public static int colorFor(String condition, AppSettings settings, ThemePalette palette) {
        if (settings.isMonochrome()) {
            return palette.primary;
        }
        if (isSunny(condition)) {
            return 0xFFFFB020;
        }
        if (isRain(condition)) {
            return palette.accent;
        }
        if (isSnow(condition)) {
            return palette.primary;
        }
        return palette.secondary;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(color);
        paint.setStrokeWidth(dp(2.1f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight()) * 0.78f;
        if (isRain(condition)) {
            drawCloud(canvas, cx, cy - size * 0.06f, size);
            drawRain(canvas, cx, cy, size);
        } else if (isSnow(condition)) {
            drawCloud(canvas, cx, cy - size * 0.06f, size);
            drawSnow(canvas, cx, cy, size);
        } else if (isSunny(condition)) {
            drawSun(canvas, cx, cy, size);
        } else {
            drawCloud(canvas, cx, cy, size);
        }
    }

    private void drawSun(Canvas canvas, float cx, float cy, float size) {
        float radius = size * 0.24f;
        canvas.drawCircle(cx, cy, radius, paint);
        float inner = size * 0.36f;
        float outer = size * 0.50f;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4d;
            float x1 = cx + (float) Math.cos(angle) * inner;
            float y1 = cy + (float) Math.sin(angle) * inner;
            float x2 = cx + (float) Math.cos(angle) * outer;
            float y2 = cy + (float) Math.sin(angle) * outer;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float size) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx - size * 0.21f, cy + size * 0.10f, size * 0.17f, paint);
        canvas.drawCircle(cx - size * 0.02f, cy - size * 0.03f, size * 0.25f, paint);
        canvas.drawCircle(cx + size * 0.22f, cy + size * 0.09f, size * 0.19f, paint);
        rect.set(cx - size * 0.38f, cy + size * 0.05f, cx + size * 0.40f, cy + size * 0.27f);
        canvas.drawRoundRect(rect, size * 0.11f, size * 0.11f, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawRain(Canvas canvas, float cx, float cy, float size) {
        float top = cy + size * 0.34f;
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * size * 0.20f;
            canvas.drawLine(x + size * 0.04f, top, x - size * 0.04f, top + size * 0.20f, paint);
        }
    }

    private void drawSnow(Canvas canvas, float cx, float cy, float size) {
        float y = cy + size * 0.42f;
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * size * 0.20f;
            canvas.drawLine(x - size * 0.05f, y, x + size * 0.05f, y, paint);
            canvas.drawLine(x, y - size * 0.05f, x, y + size * 0.05f, paint);
        }
    }

    private static boolean isSunny(String condition) {
        return "晴".equals(condition);
    }

    private static boolean isRain(String condition) {
        return condition != null && condition.indexOf("雨") >= 0;
    }

    private static boolean isSnow(String condition) {
        return condition != null && condition.indexOf("雪") >= 0;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
