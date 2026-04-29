package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.calendar.AlmanacProvider;
import org.opendeskcalendar.app.calendar.ChineseLunarCalendar;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.CalendarDay;
import org.opendeskcalendar.app.data.ForecastDay;
import org.opendeskcalendar.app.data.MonthGrid;
import org.opendeskcalendar.app.data.WeatherSnapshot;
import org.opendeskcalendar.app.net.NetworkState;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DashboardView extends View {
    public interface Listener {
        void onOpenSettings();
        void onOpenCitySettings();
        void onOpenSystemMenu();
        void onGoToToday();
        void onPreviousMonth();
        void onNextMonth();
        void onDateSelected(CalendarDay day);
    }

    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final GestureDetector gestures;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat time24Format = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private final SimpleDateFormat time12Format = new SimpleDateFormat("hh:mm", Locale.CHINA);
    private final SimpleDateFormat secondFormat = new SimpleDateFormat(":ss", Locale.CHINA);
    private final SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.CHINA);
    private final SimpleDateFormat updateFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private Listener listener;
    private AppSettings settings;
    private ThemePalette palette;
    private WeatherSnapshot weather;
    private MonthGrid month;
    private NetworkState networkState;
    private String transientMessage = "";
    private long transientUntil = 0L;
    private RectF timeHit = new RectF();
    private RectF weatherHit = new RectF();
    private RectF wifiHit = new RectF();
    private RectF monthTitleHit = new RectF();
    private RectF[] dayHits = new RectF[42];

    public DashboardView(Context context) {
        super(context);
        setFocusable(true);
        dateFormat = new SimpleDateFormat(
                getResources().getString(R.string.format_full_date),
                ChineseText.locale(context));
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                if (listener == null) return;
                if (timeHit.contains(event.getX(), event.getY())) {
                    listener.onOpenSettings();
                } else if (weatherHit.contains(event.getX(), event.getY())) {
                    listener.onOpenCitySettings();
                } else if (wifiHit.contains(event.getX(), event.getY())) {
                    listener.onOpenSystemMenu();
                }
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                if (listener == null) return true;
                if (monthTitleHit.contains(event.getX(), event.getY())) {
                    listener.onGoToToday();
                    return true;
                }
                for (int i = 0; i < dayHits.length && month != null; i++) {
                    if (dayHits[i] != null && dayHits[i].contains(event.getX(), event.getY())) {
                        listener.onDateSelected(month.days.get(i));
                        return true;
                    }
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (listener == null || Math.abs(velocityX) < Math.abs(velocityY)) return false;
                if (velocityX < -300) {
                    listener.onNextMonth();
                    return true;
                }
                if (velocityX > 300) {
                    listener.onPreviousMonth();
                    return true;
                }
                return false;
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void update(AppSettings settings, WeatherSnapshot weather, MonthGrid month, NetworkState networkState) {
        this.settings = settings;
        this.palette = ThemePalette.from(settings);
        this.weather = weather;
        this.month = month;
        this.networkState = networkState;
        invalidate();
    }

    public void showMessage(String message) {
        transientMessage = message == null ? "" : message;
        transientUntil = System.currentTimeMillis() + 4200L;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return gestures.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (settings == null || palette == null || weather == null || month == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(palette.background);
        canvas.drawRect(0, 0, width, height, paint);
        if (width >= height) {
            drawLandscape(canvas, width, height);
        } else {
            drawPortrait(canvas, width, height);
        }
        drawTransient(canvas, width, height);
    }

    private void drawLandscape(Canvas canvas, int width, int height) {
        float pad = dp(14);
        float topHeight = height * 0.075f;
        float bottomHeight = height * 0.09f;
        float leftWidth = width * 0.40f;
        drawTopBar(canvas, pad, 0, width - pad, topHeight);

        drawLine(canvas, 0, topHeight, width, topHeight);
        drawLine(canvas, leftWidth, topHeight, leftWidth, height);
        drawLine(canvas, 0, height - bottomHeight, width, height - bottomHeight);

        float leftBottom = height - bottomHeight;
        float timeBottom = topHeight + (leftBottom - topHeight) * 0.36f;
        float weatherBottom = topHeight + (leftBottom - topHeight) * 0.64f;
        drawLine(canvas, 0, timeBottom, leftWidth, timeBottom);
        drawLine(canvas, 0, weatherBottom, leftWidth, weatherBottom);

        timeHit.set(0, topHeight, leftWidth, timeBottom);
        weatherHit.set(0, timeBottom, leftWidth, weatherBottom);

        drawTime(canvas, pad, topHeight + pad, leftWidth - pad, timeBottom - pad, false);
        drawWeather(canvas, pad, timeBottom + pad, leftWidth - pad, weatherBottom - pad);
        drawForecast(canvas, pad, weatherBottom + pad, leftWidth - pad, leftBottom - pad, false);
        drawCalendar(canvas, leftWidth + pad, topHeight + pad, width - pad, height - bottomHeight - pad);
        drawBottom(canvas, pad, height - bottomHeight, width - pad, height);
    }

    private void drawPortrait(Canvas canvas, int width, int height) {
        float pad = dp(10);
        float topHeight = height * 0.065f;
        float timeBottom = topHeight + height * 0.205f;
        float weatherBottom = timeBottom + height * 0.125f;
        float forecastBottom = weatherBottom + height * 0.145f;
        float bottomHeight = height * 0.075f;
        drawTopBar(canvas, pad, 0, width - pad, topHeight);
        drawLine(canvas, 0, topHeight, width, topHeight);
        drawLine(canvas, 0, timeBottom, width, timeBottom);
        drawLine(canvas, 0, weatherBottom, width, weatherBottom);
        drawLine(canvas, 0, forecastBottom, width, forecastBottom);
        drawLine(canvas, 0, height - bottomHeight, width, height - bottomHeight);
        timeHit.set(0, topHeight, width, timeBottom);
        weatherHit.set(0, timeBottom, width, weatherBottom);

        drawTime(canvas, pad, topHeight + pad, width - pad, timeBottom - pad, true);
        drawWeather(canvas, pad, timeBottom + pad, width - pad, weatherBottom - pad);
        drawForecast(canvas, pad, weatherBottom + pad, width - pad, forecastBottom - pad, true);
        drawCalendar(canvas, pad, forecastBottom + pad, width - pad, height - bottomHeight - pad);
        drawBottom(canvas, pad, height - bottomHeight, width - pad, height);
    }

    private void drawTopBar(Canvas canvas, float left, float top, float right, float bottom) {
        float centerY = (top + bottom) / 2f + dp(5);
        float titleSize = Math.min(scaled(20), (bottom - top) * 0.36f);
        drawFittedText(canvas, localize(settings.displayCity()), left, centerY, titleSize, palette.primary, Paint.Align.LEFT, true, (right - left) * 0.62f);
        String state = settings.showWifi ? networkState.label : "";
        drawFittedText(canvas, state, right, centerY, Math.min(scaled(18), (bottom - top) * 0.32f), palette.secondary, Paint.Align.RIGHT, false, (right - left) * 0.34f);
        wifiHit.set(right - dp(140), top, right, bottom);
    }

    private void drawTime(Canvas canvas, float left, float top, float right, float bottom, boolean compact) {
        Calendar now = Calendar.getInstance();
        float width = right - left;
        float height = bottom - top;
        float dateSize = compact ? Math.min(scaled(16), height * 0.14f) : scaled(20);
        drawFittedText(canvas, dateFormat.format(now.getTime()), left, top + dateSize, dateSize, palette.secondary, Paint.Align.LEFT, false, width);

        ChineseLunarCalendar.LunarDate lunar = ChineseLunarCalendar.fromSolar(
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String lunarText = lunar.monthName + lunar.dayName + "  " + ChineseLunarCalendar.ganzhiAnimal(lunar);
        if (!compact) {
            drawText(canvas, localize(lunarText), right, top + scaled(24), scaled(18), palette.secondary, Paint.Align.RIGHT, false);
        }

        String main = settings.use24Hour ? time24Format.format(now.getTime()) : time12Format.format(now.getTime());
        float timeSize = Math.min(width * (compact ? 0.23f : 0.34f), height * (compact ? 0.48f : 0.54f)) * fontMultiplier();
        float secondsSize = timeSize * (compact ? 0.30f : 0.34f);
        float gap = dp(compact ? 5 : 8);
        boolean drawSeconds = settings.showSeconds && !settings.isEink();
        String seconds = secondFormat.format(now.getTime());
        if (drawSeconds) {
            while (timeSize > scaled(34)) {
                secondsSize = timeSize * (compact ? 0.30f : 0.34f);
                float groupWidth = measureText(main, timeSize, true) + gap + measureText(seconds, secondsSize, false);
                if (groupWidth <= width * 0.90f) {
                    break;
                }
                timeSize *= 0.94f;
            }
        }
        float baseY = top + height * 0.66f;
        float mainWidth = measureText(main, timeSize, true);
        if (drawSeconds) {
            float secondsWidth = measureText(seconds, secondsSize, false);
            float groupLeft = left + (width - mainWidth - gap - secondsWidth) / 2f;
            drawText(canvas, main, groupLeft + mainWidth / 2f, baseY, timeSize, palette.primary, Paint.Align.CENTER, true);
            drawText(canvas, seconds, groupLeft + mainWidth + gap, baseY, secondsSize, palette.secondary, Paint.Align.LEFT, false);
        } else {
            drawText(canvas, main, left + width * 0.50f, baseY, timeSize, palette.primary, Paint.Align.CENTER, true);
        }
        if (!settings.use24Hour) {
            float suffixX = left + width * 0.76f;
            drawText(canvas, amPmFormat.format(now.getTime()), suffixX, baseY + timeSize * 0.26f, timeSize * 0.20f, palette.secondary, Paint.Align.LEFT, false);
        }
    }

    private void drawWeather(Canvas canvas, float left, float top, float right, float bottom) {
        float width = right - left;
        float height = bottom - top;
        boolean compact = getWidth() < getHeight();
        float titleSize = compact ? Math.min(scaled(18), height * 0.20f) : scaled(22);
        float tempSize = compact ? Math.min(scaled(36), height * 0.42f) : scaled(42);
        float infoSize = compact ? Math.min(scaled(14), height * 0.16f) : scaled(18);
        drawFittedText(canvas, weatherSymbol(weather.condition) + "  " + localize(weather.city), left, top + titleSize, titleSize, palette.primary, Paint.Align.LEFT, true, width * 0.72f);
        drawText(canvas, weather.temperatureCelsius + "°C", left, top + height * 0.62f, tempSize, palette.primary, Paint.Align.LEFT, true);
        drawFittedText(canvas, getResources().getString(R.string.dashboard_humidity, weather.humidityPercent), left + width * 0.47f, top + height * 0.50f, infoSize, palette.secondary, Paint.Align.LEFT, false, width * 0.50f);
        drawFittedText(canvas, localize(weather.condition + " | " + weather.wind), left + width * 0.47f, top + height * 0.73f, infoSize, palette.secondary, Paint.Align.LEFT, false, width * 0.50f);
        if (weather.fromCache) {
            drawText(canvas, getResources().getString(R.string.dashboard_cache), right, top + titleSize, Math.min(scaled(16), titleSize), palette.warning, Paint.Align.RIGHT, true);
        }
    }

    private void drawForecast(Canvas canvas, float left, float top, float right, float bottom, boolean compact) {
        float height = bottom - top;
        float titleSize = compact ? Math.min(scaled(15), height * 0.16f) : scaled(19);
        drawText(canvas, getResources().getString(R.string.dashboard_forecast_title), left, top + titleSize, titleSize, palette.secondary, Paint.Align.LEFT, true);
        float width = right - left;
        float cardTop = top + titleSize + dp(compact ? 6 : 14);
        float cardHeight = bottom - cardTop;
        float labelSize = compact ? Math.min(scaled(13), cardHeight * 0.18f) : scaled(17);
        float symbolSize = compact ? Math.min(scaled(20), cardHeight * 0.26f) : scaled(28);
        float infoSize = compact ? Math.min(scaled(12), cardHeight * 0.16f) : scaled(16);
        for (int i = 0; i < weather.forecast.size() && i < 3; i++) {
            ForecastDay day = weather.forecast.get(i);
            float x = left + width * (i + 0.5f) / 3f;
            drawText(canvas, localize(day.label), x, cardTop + labelSize, labelSize, palette.primary, Paint.Align.CENTER, true);
            drawText(canvas, weatherSymbol(day.condition), x, cardTop + cardHeight * 0.42f, symbolSize, palette.accent, Paint.Align.CENTER, true);
            drawFittedText(canvas, localize(day.condition), x, cardTop + cardHeight * 0.66f, infoSize, palette.secondary, Paint.Align.CENTER, false, width / 3f - dp(6));
            drawFittedText(canvas, day.tempRange(), x, cardTop + cardHeight * 0.90f, infoSize, palette.secondary, Paint.Align.CENTER, false, width / 3f - dp(6));
        }
    }

    private void drawCalendar(Canvas canvas, float left, float top, float right, float bottom) {
        float width = right - left;
        float height = bottom - top;
        boolean compact = getWidth() < getHeight();
        float titleSize = compact ? Math.min(scaled(18), height * 0.07f) : scaled(24);
        float lunarSize = compact ? Math.min(scaled(12), height * 0.045f) : scaled(17);
        String title = getResources().getString(R.string.format_month_title, month.year, month.month);
        monthTitleHit.set(left, top, right, top + titleSize + dp(12));
        drawText(canvas, title, left + width / 2f, top + titleSize, titleSize, palette.primary, Paint.Align.CENTER, true);

        Calendar now = Calendar.getInstance();
        ChineseLunarCalendar.LunarDate lunar = ChineseLunarCalendar.fromSolar(
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String lunarTitle = localize(lunar.monthName + lunar.dayName + "  " + ChineseLunarCalendar.ganzhiAnimal(lunar));
        if (compact) {
            drawFittedText(canvas, lunarTitle, left + width / 2f, top + titleSize + lunarSize + dp(4), lunarSize, palette.secondary, Paint.Align.CENTER, false, width);
        } else {
            drawText(canvas, lunarTitle, right, top + scaled(22), scaled(17), palette.secondary, Paint.Align.RIGHT, false);
        }

        String[] names = settings.weekStartsMonday
                ? new String[]{"一", "二", "三", "四", "五", "六", "日"}
                : new String[]{"日", "一", "二", "三", "四", "五", "六"};
        float headerTop = compact ? top + titleSize + lunarSize + dp(12) : top + titleSize + dp(20);
        float cellW = width / 7f;
        float cellH = (bottom - headerTop) / 7f;
        float headerSize = compact ? Math.min(scaled(11), cellH * 0.34f) : scaled(16);
        for (int i = 0; i < 7; i++) {
            drawText(canvas, localize(names[i]), left + cellW * (i + 0.5f), headerTop + headerSize, headerSize, palette.muted, Paint.Align.CENTER, true);
        }
        float gridTop = headerTop + cellH;
        for (int i = 0; i < month.days.size() && i < 42; i++) {
            CalendarDay day = month.days.get(i);
            int col = i % 7;
            int row = i / 7;
            float cellLeft = left + col * cellW;
            float cellTop = gridTop + row * cellH;
            RectF hit = dayHits[i];
            if (hit == null) {
                hit = new RectF();
                dayHits[i] = hit;
            }
            hit.set(cellLeft, cellTop, cellLeft + cellW, cellTop + cellH);
            drawCalendarDay(canvas, day, hit);
        }
    }

    private void drawCalendarDay(Canvas canvas, CalendarDay day, RectF cell) {
        boolean compact = getWidth() < getHeight();
        float inset = compact ? dp(1.5f) : dp(3);
        rect.set(cell.left + inset, cell.top + inset, cell.right - inset, cell.bottom - inset);
        if (day.today) {
            paint.setStyle(settings.isMonochrome() ? Paint.Style.STROKE : Paint.Style.FILL);
            paint.setStrokeWidth(dp(settings.isMonochrome() ? 2 : 1));
            paint.setColor(settings.isMonochrome() ? palette.primary : palette.todayFill);
            canvas.drawRoundRect(rect, dp(3), dp(3), paint);
        }
        int mainColor = day.inMonth ? palette.primary : palette.muted;
        if (day.holiday != null && day.holiday.isHoliday() && !settings.isMonochrome()) mainColor = palette.holiday;
        if (day.holiday != null && day.holiday.isWorkday() && !settings.isMonochrome()) mainColor = palette.workday;
        float daySize = compact ? Math.min(scaled(13), Math.min(cell.height() * 0.33f, cell.width() * 0.30f)) : scaled(20);
        float lowerSize = compact ? Math.min(scaled(8), Math.min(cell.height() * 0.18f, cell.width() * 0.18f)) : scaled(12);
        drawText(canvas, String.valueOf(day.day), cell.centerX(), cell.top + cell.height() * 0.36f, daySize, mainColor, Paint.Align.CENTER, true);
        if (settings.showLunar && (!compact || day.inMonth)) {
            drawFittedText(canvas, localize(day.lowerLabel()), cell.centerX(), cell.top + cell.height() * 0.70f, lowerSize, day.inMonth ? palette.secondary : palette.muted, Paint.Align.CENTER, false, cell.width() - dp(2));
        }
        if (day.holiday != null) {
            float badgeSize = compact ? Math.min(scaled(8), cell.height() * 0.18f) : scaled(13);
            drawText(canvas, localize(day.holiday.badge), cell.right - dp(10), cell.top + badgeSize + dp(3), badgeSize,
                    day.holiday.isWorkday() && !settings.isMonochrome() ? palette.workday : palette.holiday,
                    Paint.Align.CENTER, true);
        }
    }

    private void drawBottom(Canvas canvas, float left, float top, float right, float bottom) {
        Date updated = new Date(weather.updatedAtMillis == 0L ? System.currentTimeMillis() : weather.updatedAtMillis);
        String updateText = weather.fromCache
                ? getResources().getString(R.string.dashboard_cached_at, updateFormat.format(updated))
                : getResources().getString(R.string.dashboard_updated_at, updateFormat.format(updated));
        boolean compact = getWidth() < getHeight();
        float height = bottom - top;
        float width = right - left;
        float updateSize = compact ? Math.min(scaled(11), height * 0.30f) : scaled(16);
        drawFittedText(canvas, updateText, left, top + height * (compact ? 0.36f : 0.58f), updateSize, palette.muted, Paint.Align.LEFT, false, compact ? width : width * 0.34f);
        if (settings.showAlmanac) {
            Calendar now = Calendar.getInstance();
            String textValue = getResources().getString(
                    R.string.dashboard_good_avoid,
                    localize(AlmanacProvider.good(now)),
                    localize(AlmanacProvider.avoid(now)));
            if (compact) {
                drawFittedText(canvas, textValue, left, top + height * 0.78f, Math.min(scaled(11), height * 0.30f), palette.secondary, Paint.Align.LEFT, false, width);
            } else {
                drawText(canvas, textValue, right, top + height * 0.58f, scaled(17), palette.secondary, Paint.Align.RIGHT, false);
            }
        }
    }

    private void drawTransient(Canvas canvas, int width, int height) {
        if (transientMessage.length() == 0 || System.currentTimeMillis() > transientUntil) {
            return;
        }
        float padding = dp(12);
        text.setTextSize(scaled(16));
        float textWidth = text.measureText(transientMessage);
        float boxWidth = Math.min(width - dp(32), textWidth + padding * 2);
        rect.set((width - boxWidth) / 2f, height - dp(72), (width + boxWidth) / 2f, height - dp(28));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(palette.dark ? 0xEEFFFFFF : 0xEE111827);
        canvas.drawRoundRect(rect, dp(4), dp(4), paint);
        drawText(canvas, transientMessage, width / 2f, rect.centerY() + scaled(6), scaled(16),
                palette.dark ? 0xFF111827 : 0xFFFFFFFF, Paint.Align.CENTER, false);
    }

    private void drawLine(Canvas canvas, float startX, float startY, float stopX, float stopY) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(palette.divider);
        canvas.drawLine(startX, startY, stopX, stopY, paint);
    }

    private void drawText(Canvas canvas, String value, float x, float y, float size, int color, Paint.Align align, boolean bold) {
        text.setColor(color);
        text.setTextSize(size);
        text.setTextAlign(align);
        text.setFakeBoldText(bold);
        canvas.drawText(value == null ? "" : value, x, y, text);
        text.setFakeBoldText(false);
    }

    private void drawFittedText(Canvas canvas, String value, float x, float y, float size, int color, Paint.Align align, boolean bold, float maxWidth) {
        String safeValue = value == null ? "" : value;
        float fittedSize = fittedSize(safeValue, size, bold, maxWidth);
        drawText(canvas, ellipsize(safeValue, fittedSize, bold, maxWidth), x, y, fittedSize, color, align, bold);
    }

    private float fittedSize(String value, float size, boolean bold, float maxWidth) {
        if (maxWidth <= 0) {
            return size;
        }
        float width = measureText(value, size, bold);
        if (width <= maxWidth) {
            return size;
        }
        return Math.max(dp(6), size * maxWidth / width);
    }

    private String ellipsize(String value, float size, boolean bold, float maxWidth) {
        if (measureText(value, size, bold) <= maxWidth) {
            return value;
        }
        String suffix = "…";
        int end = value.length();
        while (end > 0 && measureText(value.substring(0, end) + suffix, size, bold) > maxWidth) {
            end--;
        }
        return end <= 0 ? "" : value.substring(0, end) + suffix;
    }

    private float measureText(String value, float size, boolean bold) {
        text.setTextSize(size);
        text.setFakeBoldText(bold);
        float width = text.measureText(value == null ? "" : value);
        text.setFakeBoldText(false);
        return width;
    }

    private String weatherSymbol(String condition) {
        if (settings.isMonochrome()) {
            return localize(condition);
        }
        if ("晴".equals(condition)) return "☀";
        if ("少云".equals(condition) || "多云".equals(condition)) return "☁";
        if (condition != null && condition.indexOf("雨") >= 0) return localize("雨");
        if (condition != null && condition.indexOf("雪") >= 0) return localize("雪");
        return "☁";
    }

    private String localize(String value) {
        return ChineseText.display(getContext(), value);
    }

    private float scaled(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity * fontMultiplier();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float fontMultiplier() {
        if (settings == null) return 1f;
        if (settings.fontScale <= 0) return 0.86f;
        if (settings.fontScale == 1) return 0.95f;
        if (settings.fontScale == 3) return 1.16f;
        return 1.04f;
    }
}
