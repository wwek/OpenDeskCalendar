package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

public final class BatteryStatusView extends View {
    private final int percent;
    private final boolean charging;
    private final int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final RectF cap = new RectF();
    private final Path bolt = new Path();

    public BatteryStatusView(Context context, int percent, boolean charging, int color) {
        super(context);
        this.percent = percent;
        this.charging = charging;
        this.color = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = Math.min(getWidth() - (charging ? dp(12) : dp(8)), dp(38));
        float height = Math.min(getHeight() * 0.38f, dp(18));
        float left = (getWidth() - width) / 2f - dp(1) + (charging ? dp(3) : 0);
        float top = (getHeight() - height) / 2f;
        float capWidth = dp(3);
        body.set(left, top, left + width - capWidth, top + height);
        cap.set(body.right + dp(1), top + height * 0.32f, body.right + capWidth, top + height * 0.68f);

        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.3f));
        canvas.drawRoundRect(body, dp(3f), dp(3f), paint);
        canvas.drawRoundRect(cap, dp(1.5f), dp(1.5f), paint);

        if (charging) {
            drawChargingContent(canvas);
        } else {
            drawPercentText(canvas, body.centerX());
        }
    }

    private void drawChargingContent(Canvas canvas) {
        float boltWidth = dp(6);
        float boltLeft = body.left - dp(7);
        float centerY = body.centerY();
        bolt.reset();
        bolt.moveTo(boltLeft + boltWidth * 0.55f, centerY - dp(6));
        bolt.lineTo(boltLeft + boltWidth * 0.10f, centerY);
        bolt.lineTo(boltLeft + boltWidth * 0.48f, centerY);
        bolt.lineTo(boltLeft + boltWidth * 0.22f, centerY + dp(6));
        bolt.lineTo(boltLeft + boltWidth * 0.92f, centerY - dp(1));
        bolt.lineTo(boltLeft + boltWidth * 0.55f, centerY - dp(1));
        bolt.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(bolt, paint);
        drawPercentText(canvas, body.centerX());
    }

    private void drawPercentText(Canvas canvas, float centerX) {
        String text = percent >= 0 ? percent + "%" : "--";
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(8));
        paint.setFakeBoldText(true);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = body.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(text, centerX, baseline, paint);
        paint.setFakeBoldText(false);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
