package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public final class DashboardIconButton extends View {
    public static final int TYPE_SETTINGS = 1;
    public static final int TYPE_WIFI = 2;

    private final int type;
    private final int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public DashboardIconButton(Context context, int type, int color) {
        super(context);
        this.type = type;
        this.color = color;
        setFocusable(true);
        setClickable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(color);
        paint.setStrokeWidth(dp(2.2f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight()) * 0.46f;
        if (type == TYPE_SETTINGS) {
            drawSettingsIcon(canvas, cx, cy, size);
        } else {
            drawWifiIcon(canvas, cx, cy, size);
        }
    }

    private void drawSettingsIcon(Canvas canvas, float cx, float cy, float size) {
        float outer = size * 0.44f;
        float ring = size * 0.31f;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4d;
            float innerX = cx + (float) Math.cos(angle) * ring;
            float innerY = cy + (float) Math.sin(angle) * ring;
            float outerX = cx + (float) Math.cos(angle) * outer;
            float outerY = cy + (float) Math.sin(angle) * outer;
            canvas.drawLine(innerX, innerY, outerX, outerY, paint);
        }
        canvas.drawCircle(cx, cy, size * 0.28f, paint);
        canvas.drawCircle(cx, cy, size * 0.10f, paint);
    }

    private void drawWifiIcon(Canvas canvas, float cx, float cy, float size) {
        float dotY = cy + size * 0.26f;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, dotY, size * 0.06f, paint);
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            float radius = size * (0.24f + i * 0.18f);
            rect.set(cx - radius, dotY - radius, cx + radius, dotY + radius);
            canvas.drawArc(rect, 225f, 90f, false, paint);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
