package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

public final class BatteryStatusView extends View {
    private final int percent;
    private final int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final RectF cap = new RectF();

    public BatteryStatusView(Context context, int percent, int color) {
        super(context);
        this.percent = percent;
        this.color = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = Math.min(getWidth() - dp(10), dp(46));
        float height = Math.min(getHeight() * 0.42f, dp(22));
        float left = (getWidth() - width) / 2f - dp(1);
        float top = (getHeight() - height) / 2f;
        float capWidth = dp(3);
        body.set(left, top, left + width - capWidth, top + height);
        cap.set(body.right + dp(1), top + height * 0.32f, body.right + capWidth, top + height * 0.68f);

        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        canvas.drawRoundRect(body, dp(3.5f), dp(3.5f), paint);
        canvas.drawRoundRect(cap, dp(1.5f), dp(1.5f), paint);

        String text = percent >= 0 ? percent + "%" : "--";
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(9));
        paint.setFakeBoldText(true);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = body.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(text, body.centerX(), baseline, paint);
        paint.setFakeBoldText(false);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
