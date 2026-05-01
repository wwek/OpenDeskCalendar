package org.opendeskcalendar.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.calendar.AlmanacProvider;
import org.opendeskcalendar.app.calendar.ChineseLunarCalendar;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.CalendarDay;
import org.opendeskcalendar.app.data.ForecastDay;
import org.opendeskcalendar.app.data.IndoorSnapshot;
import org.opendeskcalendar.app.data.MonthGrid;
import org.opendeskcalendar.app.data.WeatherSnapshot;
import org.opendeskcalendar.app.net.NetworkState;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DashboardView extends FrameLayout {
    public interface Listener {
        void onOpenSettings();
        void onOpenCitySettings();
        void onOpenSystemMenu();
        void onGoToToday();
        void onPreviousMonth();
        void onNextMonth();
        void onDateSelected(CalendarDay day);
    }

    private final Handler handler = new Handler();
    private final GestureDetector gestures;
    private final SimpleDateFormat time24Format = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private final SimpleDateFormat time24WithSecondsFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private final SimpleDateFormat time12Format = new SimpleDateFormat("hh:mm", Locale.CHINA);
    private final SimpleDateFormat time12WithSecondsFormat = new SimpleDateFormat("hh:mm:ss", Locale.CHINA);
    private final SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.CHINA);
    private final SimpleDateFormat updateFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private Listener listener;
    private AppSettings settings;
    private ThemePalette palette;
    private WeatherSnapshot weather;
    private IndoorSnapshot indoor;
    private MonthGrid month;
    private NetworkState networkState;
    private Calendar selectedDate;
    private TextView messageView;
    private String transientMessage = "";
    private long transientUntil = 0L;
    private int lastOrientation = 0;

    public DashboardView(Context context) {
        super(context);
        setFocusable(true);
        setClickable(true);
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (listener == null || Math.abs(velocityX) < Math.abs(velocityY)) {
                    return false;
                }
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

    public void update(AppSettings settings, WeatherSnapshot weather, IndoorSnapshot indoor, MonthGrid month, NetworkState networkState, Calendar selectedDate) {
        this.settings = settings;
        this.palette = ThemePalette.from(settings);
        this.weather = weather;
        this.indoor = indoor;
        this.month = month;
        this.networkState = networkState;
        this.selectedDate = copyDate(selectedDate == null ? Calendar.getInstance() : selectedDate);
        buildLayout();
    }

    public void showMessage(String message) {
        transientMessage = message == null ? "" : message;
        transientUntil = System.currentTimeMillis() + 4200L;
        bindMessageVisibility();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bindMessageVisibility();
            }
        }, 4300L);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestures.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int orientation = w >= h ? 1 : 2;
        if (lastOrientation != orientation) {
            lastOrientation = orientation;
            buildLayout();
        }
    }

    private void buildLayout() {
        if (settings == null || palette == null || weather == null || month == null) {
            return;
        }
        removeAllViews();
        setBackgroundColor(palette.background);
        boolean landscape = getWidth() > 0 && getWidth() >= getHeight();
        View content;
        if (landscape) {
            content = buildLandscape();
        } else {
            content = buildPortrait();
        }
        addContentView(content);
        messageView = buildMessageView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.leftMargin = dp(16);
        params.rightMargin = dp(16);
        params.bottomMargin = dp(24);
        addView(messageView, params);
        bindMessageVisibility();
    }

    private void addContentView(View content) {
        FrameLayout.LayoutParams params = matchFrame();
        if (settings.burnInProtectionEnabled) {
            int[] offset = burnInOffset(burnInRange());
            content.setTranslationX(offset[0]);
            content.setTranslationY(offset[1]);
        }
        addView(content, params);
    }

    private int burnInRange() {
        return Math.max(2, dp(3));
    }

    private int[] burnInOffset(int range) {
        int[][] pattern = {
                {0, 0},
                {range, 0},
                {range, range},
                {0, range},
                {-range, range},
                {-range, 0},
                {-range, -range},
                {0, -range},
                {range, -range}
        };
        int index = (int) ((System.currentTimeMillis() / 60000L) % pattern.length);
        return pattern[index];
    }

    private View buildPortrait() {
        LinearLayout root = vertical();
        root.addView(buildTopBar(true), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38)));
        root.addView(softDivider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));

        View timeCard = wrapInCard(buildTimeSection(true), dp(12));
        LinearLayout.LayoutParams timeParams = weighted(1.60f);
        timeParams.setMargins(dp(10), dp(4), dp(10), dp(4));
        root.addView(timeCard, timeParams);

        View weatherCard = wrapInCard(buildPortraitWeatherSection(), dp(10));
        LinearLayout.LayoutParams weatherParams = weighted(0.92f);
        weatherParams.setMargins(dp(10), dp(4), dp(10), dp(4));
        root.addView(weatherCard, weatherParams);

        View forecastCard = wrapInCard(buildForecastSection(true), dp(10));
        LinearLayout.LayoutParams forecastParams = weighted(0.92f);
        forecastParams.setMargins(dp(10), dp(4), dp(10), dp(4));
        root.addView(forecastCard, forecastParams);

        View calendarCard = wrapInCard(constrainPortraitCalendarWidth(buildCalendarSection(true, false)), dp(12));
        LinearLayout.LayoutParams calendarParams = weighted(3.30f);
        calendarParams.setMargins(dp(10), dp(4), dp(10), dp(4));
        root.addView(calendarCard, calendarParams);

        root.addView(buildBottomSection(true), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(52)));
        return root;
    }

    private View wrapInCard(View content, int cornerRadiusPx) {
        FrameLayout card = new FrameLayout(getContext());
        if (!settings.isMonochrome() && !settings.isEink()) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(palette.panel);
            bg.setCornerRadius(cornerRadiusPx);
            bg.setStroke(dp(1), cardBorderColor());
            card.setBackground(bg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                card.setElevation(dp(2));
            }
        }
        card.addView(content, matchFrame());
        return card;
    }

    private View buildLandscape() {
        LinearLayout root = vertical();
        root.addView(buildTopBar(true), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));

        LinearLayout body = horizontal();
        body.setPadding(dp(10), dp(6), dp(10), dp(8));

        LinearLayout left = vertical();
        addLandscapeCard(left, buildTimeSection(false), 1.40f, 0);
        addLandscapeCard(left, buildLandscapeWeatherSection(), 1.05f, dp(6));
        addLandscapeCard(left, buildLandscapeForecastSection(), 0.95f, dp(6));
        body.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.50f));

        View calendarCard = wrapInCard(buildLandscapeCalendarColumn(), dp(12));
        LinearLayout.LayoutParams calendarParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.50f);
        calendarParams.leftMargin = dp(8);
        body.addView(calendarCard, calendarParams);

        root.addView(body, weighted(1f));
        return root;
    }

    private void addLandscapeCard(LinearLayout parent, View content, float weight, int topMargin) {
        View card = wrapInCard(content, dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, weight);
        params.topMargin = topMargin;
        parent.addView(card, params);
    }

    private View buildLandscapeCalendarColumn() {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_HORIZONTAL);
        section.setPadding(dp(8), 0, dp(8), 0);
        section.addView(constrainLandscapeCalendarWidth(buildCalendarSection(true, true)), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        if (settings.showAlmanac) {
            section.addView(constrainLandscapeCalendarWidth(softDivider()), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
            section.addView(buildAlmanacSection(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        }
        return section;
    }

    private View constrainLandscapeCalendarWidth(View content) {
        FrameLayout frame = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(landscapeCalendarContentWidth(), LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        frame.addView(content, params);
        return frame;
    }

    private View constrainPortraitCalendarWidth(View content) {
        FrameLayout frame = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(portraitCalendarContentWidth(), LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        frame.addView(content, params);
        return frame;
    }

    private int landscapeCalendarContentWidth() {
        int columnWidth = Math.max(0, (getWidth() - dp(1)) / 2 - dp(16));
        if (columnWidth == 0) {
            return dp(390);
        }
        return Math.min(dp(390), columnWidth);
    }

    private int portraitCalendarContentWidth() {
        int availableWidth = Math.max(0, getWidth() - dp(20));
        if (availableWidth == 0) {
            return dp(350);
        }
        return Math.min(dp(350), availableWidth);
    }

    private int landscapeCalendarSideInset() {
        int columnWidth = Math.max(0, (getWidth() - dp(1)) / 2 - dp(16));
        if (columnWidth == 0) {
            return 0;
        }
        return Math.max(0, (columnWidth - landscapeCalendarContentWidth()) / 2);
    }

    private View buildTopBar(boolean compact) {
        LinearLayout bar = horizontal();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), 0, compact ? dp(10) : dp(16), 0);

        TextView city = label(localize(settings.displayCity()), compact ? 16 : 19, palette.primary, true);
        city.setSingleLine(true);
        city.setEllipsize(TextUtils.TruncateAt.END);
        city.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenCitySettings();
                }
                return true;
            }
        });
        bar.addView(city, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        DashboardIconButton settingsButton = new DashboardIconButton(
                getContext(), DashboardIconButton.TYPE_SETTINGS, palette.muted);
        settingsButton.setContentDescription(getResources().getString(R.string.settings_title));
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onOpenSettings();
                }
            }
        });
        bar.addView(settingsButton, new LinearLayout.LayoutParams(compact ? dp(34) : dp(46), LayoutParams.MATCH_PARENT));

        if (settings.showWifi) {
            int wifiColor = networkState.connected ? palette.muted : palette.warning;
            DashboardIconButton state = new DashboardIconButton(
                    getContext(), DashboardIconButton.TYPE_WIFI, wifiColor);
            state.setContentDescription(networkState.label);
            state.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMessage(networkState.label);
                }
            });
            state.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (listener != null) {
                        listener.onOpenSystemMenu();
                    }
                    return true;
                }
            });
            bar.addView(state, new LinearLayout.LayoutParams(compact ? dp(34) : dp(46), LayoutParams.MATCH_PARENT));
        }
        BatteryState battery = batteryState();
        bar.addView(new BatteryStatusView(getContext(), battery.percent, battery.charging, palette.muted), new LinearLayout.LayoutParams(compact ? dp(38) : dp(46), LayoutParams.MATCH_PARENT));
        return bar;
    }

    private View buildTimeSection(boolean compact) {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), compact ? dp(9) : dp(14), dp(16), compact ? dp(9) : dp(14));
        section.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenSettings();
                }
                return true;
            }
        });

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                getResources().getString(R.string.format_full_date),
                ChineseText.isTraditional(getContext()) ? Locale.TAIWAN : Locale.CHINA);
        TextView date = label(dateFormat.format(now.getTime()), compact ? 15 : 18, palette.secondary, false);
        date.setGravity(Gravity.CENTER);
        date.setSingleLine(true);
        date.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(date, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout row = horizontal();
        row.setGravity(compact ? Gravity.CENTER : Gravity.CENTER | Gravity.BOTTOM);
        String main;
        if (settings.use24Hour) {
            main = settings.showSeconds ? time24WithSecondsFormat.format(now.getTime()) : time24Format.format(now.getTime());
        } else {
            main = settings.showSeconds ? time12WithSecondsFormat.format(now.getTime()) : time12Format.format(now.getTime());
        }
        TextView time = label(main, compact ? 58 : 64, palette.primary, true);
        time.setIncludeFontPadding(false);
        time.setSingleLine(true);
        row.addView(time, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        if (!settings.use24Hour) {
            TextView suffix = label(amPmFormat.format(now.getTime()), compact ? 16 : 16, palette.secondary, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.leftMargin = dp(8);
            params.bottomMargin = compact ? dp(8) : dp(12);
            row.addView(suffix, params);
        }
        if (compact) {
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(8);
            section.addView(row, rowParams);
        } else {
            section.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        }
        return section;
    }

    private View buildPortraitWeatherSection() {
        LinearLayout section = horizontal();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), dp(6), dp(16), dp(6));
        section.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenCitySettings();
                }
                return true;
            }
        });

        LinearLayout primary = horizontal();
        primary.setGravity(Gravity.CENTER_VERTICAL);
        WeatherIconView currentIcon = new WeatherIconView(getContext(), weather.condition, WeatherIconView.colorFor(weather.condition, settings, palette));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        iconParams.rightMargin = dp(8);
        primary.addView(currentIcon, iconParams);

        TextView temp = label(weather.temperatureCelsius + "°C", 34, palette.primary, true);
        temp.setIncludeFontPadding(false);
        temp.setSingleLine(true);
        primary.addView(temp, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        section.addView(primary, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        LinearLayout details = vertical();
        details.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TextView comfort = label(currentComfortText(), 13, palette.secondary, false);
        comfort.setGravity(Gravity.START);
        comfort.setSingleLine(true);
        comfort.setEllipsize(TextUtils.TruncateAt.END);
        comfort.setIncludeFontPadding(false);
        details.addView(comfort, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView detail = label(localize(secondaryWeatherDetailText()), 12, palette.secondary, false);
        detail.setGravity(Gravity.START);
        detail.setIncludeFontPadding(false);
        detail.setSingleLine(true);
        detail.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(5);
        details.addView(detail, detailParams);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        detailsParams.leftMargin = dp(16);
        section.addView(details, detailsParams);
        return section;
    }

    private View buildLandscapeWeatherSection() {
        LinearLayout section = horizontal();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(18), dp(8), dp(18), dp(8));
        section.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenCitySettings();
                }
                return true;
            }
        });

        LinearLayout primary = horizontal();
        primary.setGravity(Gravity.CENTER_VERTICAL);
        WeatherIconView currentIcon = new WeatherIconView(getContext(), weather.condition, WeatherIconView.colorFor(weather.condition, settings, palette));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        iconParams.rightMargin = dp(10);
        primary.addView(currentIcon, iconParams);

        TextView temp = label(weather.temperatureCelsius + "°C", 42, palette.primary, true);
        temp.setIncludeFontPadding(false);
        temp.setSingleLine(true);
        primary.addView(temp, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        section.addView(primary, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.05f));

        LinearLayout details = vertical();
        details.setGravity(Gravity.CENTER_VERTICAL);

        TextView humidity = label(currentComfortText(), 15, palette.secondary, false);
        humidity.setSingleLine(true);
        humidity.setEllipsize(TextUtils.TruncateAt.END);
        humidity.setIncludeFontPadding(false);
        details.addView(humidity, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView detail = label(localize(secondaryWeatherDetailText()), 13, palette.secondary, false);
        detail.setIncludeFontPadding(false);
        detail.setSingleLine(true);
        detail.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(5);
        details.addView(detail, detailParams);

        if (settings.indoorEnabled && indoor != null && indoor.hasData()) {
            TextView indoorView = label(indoorText(), 12, palette.muted, false);
            indoorView.setSingleLine(true);
            indoorView.setEllipsize(TextUtils.TruncateAt.END);
            indoorView.setIncludeFontPadding(false);
            LinearLayout.LayoutParams indoorParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            indoorParams.topMargin = dp(4);
            details.addView(indoorView, indoorParams);
        }
        section.addView(details, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.10f));
        return section;
    }

    private View buildWeatherMetric(String name, String value, int valueSp) {
        LinearLayout metric = horizontal();
        metric.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = label(name, 11, palette.muted, true);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setLineSpacing(0f, 0.88f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(dp(16), LayoutParams.WRAP_CONTENT);
        labelParams.rightMargin = dp(4);
        metric.addView(label, labelParams);

        TextView text = label(value, valueSp, palette.primary, true);
        text.setIncludeFontPadding(false);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        metric.addView(text, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return metric;
    }

    private String verticalWeatherLabel(String simplified, String traditional) {
        return ChineseText.isTraditional(getContext()) ? traditional : simplified;
    }

    private String currentWeatherDetailText() {
        String text = weather.condition + " | " + weather.wind;
        if (!weather.forecast.isEmpty()) {
            text += " | " + weather.forecast.get(0).tempRange();
        }
        if (weather.apparentTemperatureCelsius != null) {
            text += " | " + getResources().getString(R.string.dashboard_apparent_temperature, weather.apparentTemperatureCelsius.intValue());
        }
        return text;
    }

    private String secondaryWeatherDetailText() {
        String text = weather.condition + " | " + weather.wind;
        if (!weather.forecast.isEmpty()) {
            text += " | " + weather.forecast.get(0).tempRange();
        }
        return text;
    }

    private String currentComfortText() {
        String text = getResources().getString(R.string.dashboard_humidity, weather.humidityPercent);
        if (weather.apparentTemperatureCelsius != null) {
            text += "   " + getResources().getString(R.string.dashboard_apparent_temperature, weather.apparentTemperatureCelsius.intValue());
        }
        return text;
    }

    private View buildForecastSection(boolean compact) {
        LinearLayout section = vertical();
        section.setPadding(dp(14), compact ? dp(2) : dp(10), dp(14), compact ? dp(2) : dp(12));

        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER);
        int count = Math.min(3, weather.forecast.size());
        for (int i = 0; i < count; i++) {
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            fp.leftMargin = dp(3);
            fp.rightMargin = dp(3);
            row.addView(buildForecastDay(weather.forecast.get(i), compact), fp);
        }
        section.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private View buildLandscapeForecastSection() {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(20), dp(4), dp(20), dp(4));

        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER);
        int count = Math.min(3, weather.forecast.size());
        for (int i = 0; i < count; i++) {
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            fp.leftMargin = dp(2);
            fp.rightMargin = dp(2);
            row.addView(buildLandscapeForecastDay(weather.forecast.get(i)), fp);
        }
        section.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private View buildLandscapeForecastDay(ForecastDay day) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), 0, dp(2), 0);

        TextView label = label(localize(day.label), 17, palette.primary, true);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout detail = horizontal();
        detail.setGravity(Gravity.CENTER);
        WeatherIconView symbol = new WeatherIconView(getContext(), day.condition, WeatherIconView.colorFor(day.condition, settings, palette));
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(dp(30), dp(28));
        symbolParams.rightMargin = dp(6);
        detail.addView(symbol, symbolParams);

        TextView condition = label(localize(day.condition), 14, palette.secondary, false);
        condition.setIncludeFontPadding(false);
        condition.setSingleLine(true);
        condition.setEllipsize(TextUtils.TruncateAt.END);
        detail.addView(condition, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        item.addView(detail, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView temp = label(day.tempRange(), 14, palette.secondary, false);
        temp.setGravity(Gravity.CENTER);
        temp.setIncludeFontPadding(false);
        temp.setSingleLine(true);
        item.addView(temp, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View buildForecastDay(ForecastDay day, boolean compact) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        int hPad = compact ? dp(2) : dp(6);
        int vPad = compact ? 0 : dp(4);
        item.setPadding(hPad, vPad, hPad, vPad);
        if (!compact) {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(palette.dark ? Color.argb(25, 255, 255, 255) : Color.argb(20, 0, 0, 0));
            cardBg.setCornerRadius(dp(8));
            item.setBackground(cardBg);
        }

        TextView label = label(localize(day.label), compact ? 12 : 18, palette.primary, true);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        WeatherIconView symbol = new WeatherIconView(getContext(), day.condition, WeatherIconView.colorFor(day.condition, settings, palette));
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(compact ? dp(18) : dp(42), compact ? dp(16) : dp(38));
        symbolParams.topMargin = compact ? 0 : dp(2);
        symbolParams.bottomMargin = compact ? 0 : dp(2);
        item.addView(symbol, symbolParams);

        TextView condition = label(localize(day.condition), compact ? 11 : 17, palette.secondary, false);
        condition.setGravity(Gravity.CENTER);
        condition.setIncludeFontPadding(false);
        condition.setSingleLine(true);
        condition.setEllipsize(TextUtils.TruncateAt.END);
        item.addView(condition, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView temp = label(day.tempRange(), compact ? 10 : 17, palette.secondary, false);
        temp.setGravity(Gravity.CENTER);
        temp.setIncludeFontPadding(false);
        temp.setSingleLine(true);
        item.addView(temp, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View buildCalendarSection(boolean compact) {
        return buildCalendarSection(compact, false);
    }

    private View buildCalendarSection(boolean compact, boolean inlineHeader) {
        LinearLayout section = vertical();
        section.setPadding(compact ? dp(6) : dp(14), compact ? dp(4) : dp(12), compact ? dp(6) : dp(14), compact ? dp(4) : dp(12));

        Calendar now = almanacDate();
        ChineseLunarCalendar.LunarDate lunar = ChineseLunarCalendar.fromSolar(
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String lunarHeader = localize(lunar.monthName + lunar.dayName + "  " + ChineseLunarCalendar.ganzhiAnimal(lunar));
        TextView title = label(getResources().getString(R.string.format_month_title, month.year, month.month),
                compact ? 18 : 23,
                palette.primary,
                true);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        bindCalendarTitleActions(title);
        if (inlineHeader) {
            FrameLayout header = new FrameLayout(getContext());
            header.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onGoToToday();
                    }
                }
            });
            header.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (listener != null) {
                        listener.onOpenSystemMenu();
                    }
                    return true;
                }
            });
            header.addView(title, matchFrame());
            TextView lunarTitle = label(lunarHeader, compact ? 10 : 12, palette.secondary, false);
            lunarTitle.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            lunarTitle.setSingleLine(true);
            lunarTitle.setEllipsize(TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams lunarParams = new FrameLayout.LayoutParams(compact ? dp(128) : dp(210), LayoutParams.MATCH_PARENT, Gravity.END | Gravity.CENTER_VERTICAL);
            header.addView(lunarTitle, lunarParams);
            section.addView(header, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, compact ? dp(22) : dp(30)));
        } else {
            section.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            TextView lunarTitle = label(lunarHeader, compact ? 11 : 15, palette.secondary, false);
            lunarTitle.setGravity(Gravity.CENTER);
            lunarTitle.setSingleLine(true);
            lunarTitle.setEllipsize(TextUtils.TruncateAt.END);
            section.addView(lunarTitle, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        LinearLayout grid = vertical();
        grid.addView(buildWeekHeader(compact), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, compact ? dp(18) : dp(28)));
        int index = 0;
        for (int row = 0; row < 6; row++) {
            LinearLayout week = horizontal();
            for (int col = 0; col < 7; col++) {
                CalendarDay day = month.days.get(index);
                week.addView(buildDayCell(day, compact), new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
                index++;
            }
            grid.addView(week, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        }
        section.addView(grid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private void bindCalendarTitleActions(View title) {
        title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onGoToToday();
                }
            }
        });
        title.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenSystemMenu();
                }
                return true;
            }
        });
    }

    private View buildWeekHeader(boolean compact) {
        String[] names = settings.weekStartsMonday
                ? new String[]{"一", "二", "三", "四", "五", "六", "日"}
                : new String[]{"日", "一", "二", "三", "四", "五", "六"};
        LinearLayout row = horizontal();
        for (String name : names) {
            TextView item = label(localize(name), compact ? 12 : 14, palette.muted, true);
            item.setGravity(Gravity.CENTER);
            row.addView(item, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        }
        return row;
    }

    private View buildDayCell(final CalendarDay day, boolean compact) {
        FrameLayout cell = new FrameLayout(getContext());
        int padding = compact ? dp(1) : dp(4);
        cell.setPadding(padding, padding, padding, padding);
        cell.setBackground(dayBackground(day));
        cell.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onDateSelected(day);
                }
            }
        });

        LinearLayout texts = vertical();
        texts.setGravity(Gravity.CENTER);
        TextView number = label(String.valueOf(day.day), compact ? 13 : 17, dayColor(day), true);
        number.setGravity(Gravity.CENTER);
        number.setIncludeFontPadding(false);
        texts.addView(number, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        boolean showLower = settings.showLunar;
        TextView lower = label(showLower ? localize(day.lowerLabel()) : "",
                compact ? 8 : 11,
                day.inMonth ? palette.secondary : palette.muted,
                false);
        lower.setGravity(Gravity.CENTER);
        lower.setSingleLine(true);
        lower.setEllipsize(TextUtils.TruncateAt.END);
        lower.setIncludeFontPadding(false);
        texts.addView(lower, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (isSelected(day)) {
            texts.setPadding(dp(3), dp(1), dp(3), dp(1));
            texts.setBackground(selectedDayBackground());
            FrameLayout.LayoutParams selectedParams = compact
                    ? new FrameLayout.LayoutParams(dp(45), dp(34), Gravity.CENTER)
                    : new FrameLayout.LayoutParams(dp(60), dp(46), Gravity.CENTER);
            cell.addView(texts, selectedParams);
        } else {
            cell.addView(texts, matchFrame());
        }

        if (day.holiday != null) {
            TextView badge = label(localize(day.holiday.badge), compact ? 8 : 9, badgeColor(day), true);
            badge.setGravity(Gravity.END | Gravity.TOP);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP);
            cell.addView(badge, params);
        }
        return cell;
    }

    private View buildAlmanacSection() {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(landscapeCalendarSideInset() + dp(10), 0, dp(14), dp(6));
        bindAlmanacDetails(section);
        Calendar now = almanacDate();
        section.addView(buildLandscapeAlmanacRow(localize("宜："), localize(AlmanacProvider.fullGood(getContext(), now)), 1),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        section.addView(buildLandscapeAlmanacRow(localize("忌："), localize(AlmanacProvider.fullAvoid(getContext(), now)), 1),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return section;
    }

    private View buildLandscapeAlmanacRow(String title, String text, int maxLines) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.TOP);
        TextView titleView = label(title, 11, palette.secondary, false);
        titleView.setGravity(Gravity.TOP);
        row.addView(titleView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        TextView content = label(text, 11, palette.secondary, false);
        content.setMaxLines(maxLines);
        content.setEllipsize(TextUtils.TruncateAt.END);
        content.setLineSpacing(dp(1), 1.0f);
        if (maxLines == 1) {
            content.setSingleLine(true);
        }
        row.addView(content, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View buildBottomSection(boolean compact) {
        LinearLayout section = compact ? vertical() : horizontal();
        section.setGravity(compact ? Gravity.CENTER_VERTICAL : Gravity.CENTER_VERTICAL);
        section.setPadding(compact ? dp(26) : dp(16), dp(8), dp(16), dp(8));

        if (compact && settings.showAlmanac) {
            bindAlmanacDetails(section);
            Calendar now = almanacDate();
            TextView good = label(localize("宜：") + localize(AlmanacProvider.good(getContext(), now)), 11, palette.secondary, false);
            good.setSingleLine(true);
            good.setEllipsize(TextUtils.TruncateAt.END);
            section.addView(good, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

            TextView avoid = label(localize("忌：") + localize(AlmanacProvider.avoid(getContext(), now)), 11, palette.secondary, false);
            avoid.setSingleLine(true);
            avoid.setEllipsize(TextUtils.TruncateAt.END);
            section.addView(avoid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
            return section;
        }

        TextView updatedAt = label(updateText(), compact ? 11 : 15, palette.muted, false);
        updatedAt.setSingleLine(true);
        updatedAt.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(updatedAt, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, compact ? 0f : 0.35f));

        if (settings.showAlmanac) {
            Calendar now = almanacDate();
            String almanac = getResources().getString(
                    R.string.dashboard_good_avoid,
                    localize(AlmanacProvider.good(getContext(), now)),
                    localize(AlmanacProvider.avoid(getContext(), now)));
            TextView text = label(almanac, compact ? 11 : 16, palette.secondary, false);
            text.setSingleLine(true);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setGravity(compact ? Gravity.START : Gravity.END);
            bindAlmanacDetails(text);
            section.addView(text, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, compact ? 0f : 0.65f));
        }
        if (compact) {
            updatedAt.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            if (section.getChildCount() > 1) {
                section.getChildAt(1).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
        }
        return section;
    }

    private void bindAlmanacDetails(View view) {
        view.setClickable(true);
        view.setContentDescription(getResources().getString(R.string.almanac_title));
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View clicked) {
                showAlmanacDetails();
            }
        });
    }

    private void showAlmanacDetails() {
        Calendar now = almanacDate();
        LinearLayout content = vertical();
        content.setPadding(dp(18), dp(14), dp(18), dp(14));
        content.setBackgroundColor(palette.background);

        TextView good = label(
                localize("宜：") + localize(AlmanacProvider.fullGood(getContext(), now)),
                16,
                palette.primary,
                false);
        good.setLineSpacing(dp(2), 1.0f);
        content.addView(good, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView avoid = label(
                localize("忌：") + localize(AlmanacProvider.fullAvoid(getContext(), now)),
                16,
                palette.primary,
                false);
        avoid.setPadding(0, dp(12), 0, 0);
        avoid.setLineSpacing(dp(2), 1.0f);
        content.addView(avoid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(getContext());
        scroll.setBackgroundColor(palette.background);
        scroll.addView(content);
        new AlertDialog.Builder(getContext())
                .setTitle(getResources().getString(R.string.almanac_selected_title, selectedDateKey()))
                .setView(scroll)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private Calendar almanacDate() {
        return selectedDate == null ? Calendar.getInstance() : copyDate(selectedDate);
    }

    private Calendar copyDate(Calendar source) {
        Calendar copy = Calendar.getInstance();
        copy.set(Calendar.YEAR, source.get(Calendar.YEAR));
        copy.set(Calendar.MONTH, source.get(Calendar.MONTH));
        copy.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
        copy.set(Calendar.HOUR_OF_DAY, 12);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy;
    }

    private String selectedDateKey() {
        Calendar date = almanacDate();
        return date.get(Calendar.YEAR)
                + "-" + two(date.get(Calendar.MONTH) + 1)
                + "-" + two(date.get(Calendar.DAY_OF_MONTH));
    }

    private boolean isSelected(CalendarDay day) {
        if (selectedDate == null) {
            return day.today;
        }
        return day.year == selectedDate.get(Calendar.YEAR)
                && day.month == selectedDate.get(Calendar.MONTH) + 1
                && day.day == selectedDate.get(Calendar.DAY_OF_MONTH);
    }

    private String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private String updateText() {
        Date updated = new Date(weather.updatedAtMillis == 0L ? System.currentTimeMillis() : weather.updatedAtMillis);
        return weather.fromCache
                ? getResources().getString(R.string.dashboard_cached_at, updateFormat.format(updated))
                : getResources().getString(R.string.dashboard_updated_at, updateFormat.format(updated));
    }

    private BatteryState batteryState() {
        Intent intent = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return new BatteryState(-1, false);
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
                || plugged != 0;
        if (level < 0 || scale <= 0) {
            return new BatteryState(-1, charging);
        }
        return new BatteryState(Math.round(level * 100f / scale), charging);
    }

    private static final class BatteryState {
        final int percent;
        final boolean charging;

        BatteryState(int percent, boolean charging) {
            this.percent = percent;
            this.charging = charging;
        }
    }

    private TextView buildMessageView() {
        TextView view = label("", 15, palette.dark ? 0xFF111827 : Color.WHITE, false);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(dp(14), dp(9), dp(14), dp(9));
        GradientDrawable background = new GradientDrawable();
        background.setColor(palette.dark ? 0xEEFFFFFF : 0xEE111827);
        background.setCornerRadius(dp(4));
        view.setBackground(background);
        return view;
    }

    private void bindMessageVisibility() {
        if (messageView == null) {
            return;
        }
        boolean visible = transientMessage.length() > 0 && System.currentTimeMillis() <= transientUntil;
        messageView.setText(visible ? transientMessage : "");
        messageView.setVisibility(visible ? VISIBLE : GONE);
    }

    private GradientDrawable dayBackground(CalendarDay day) {
        GradientDrawable background = new GradientDrawable();
        boolean selected = isSelected(day);
        background.setColor(Color.TRANSPARENT);
        background.setCornerRadius(dp(8));
        if (day.today && !selected) {
            int borderColor = settings.isMonochrome()
                    ? palette.secondary
                    : Color.argb(130, Color.red(palette.muted), Color.green(palette.muted), Color.blue(palette.muted));
            background.setStroke(dp(1), borderColor);
        }
        return background;
    }

    private GradientDrawable selectedDayBackground() {
        GradientDrawable background = new GradientDrawable();
        if (settings.isMonochrome()) {
            background.setColor(Color.TRANSPARENT);
            background.setStroke(dp(2), palette.primary);
        } else {
            background.setColor(Color.argb(185, Color.red(palette.todayFill), Color.green(palette.todayFill), Color.blue(palette.todayFill)));
        }
        background.setCornerRadius(dp(7));
        return background;
    }

    private int dayColor(CalendarDay day) {
        if (!day.inMonth) {
            return palette.muted;
        }
        if ((day.today || isSelected(day)) && !settings.isMonochrome()) {
            return palette.accentSecondary;
        }
        if (day.holiday != null && day.holiday.isHoliday() && !settings.isMonochrome()) {
            return palette.holiday;
        }
        if (day.holiday != null && day.holiday.isWorkday() && !settings.isMonochrome()) {
            return palette.workday;
        }
        return palette.primary;
    }

    private int badgeColor(CalendarDay day) {
        if (day.holiday != null && day.holiday.isWorkday() && !settings.isMonochrome()) {
            return palette.workday;
        }
        return palette.holiday;
    }

    private TextView label(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value == null ? "" : value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * fontMultiplier());
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout.LayoutParams weighted(float weight) {
        return new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, weight);
    }

    private FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    private View divider() {
        View view = new View(getContext());
        view.setBackgroundColor(Color.argb(130, Color.red(palette.divider), Color.green(palette.divider), Color.blue(palette.divider)));
        return view;
    }

    private int cardBorderColor() {
        int alpha = palette.dark ? 36 : 70;
        return Color.argb(alpha, Color.red(palette.divider), Color.green(palette.divider), Color.blue(palette.divider));
    }

    private View softDivider() {
        View view = new View(getContext());
        view.setBackgroundColor(Color.argb(64, Color.red(palette.divider), Color.green(palette.divider), Color.blue(palette.divider)));
        return view;
    }

    private View spacer(int heightPx) {
        View view = new View(getContext());
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private String localize(String value) {
        return ChineseText.display(getContext(), value);
    }

    private String indoorText() {
        StringBuilder builder = new StringBuilder(getResources().getString(R.string.dashboard_indoor));
        if (!Double.isNaN(indoor.temperatureCelsius)) {
            builder.append(' ').append(Math.round(indoor.temperatureCelsius)).append("°C");
        }
        if (!Double.isNaN(indoor.humidityPercent)) {
            builder.append(' ').append(getResources().getString(R.string.dashboard_humidity, Math.round(indoor.humidityPercent)));
        }
        return builder.toString();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float fontMultiplier() {
        if (settings == null) return 1f;
        if (settings.fontScale <= 0) return 0.86f;
        if (settings.fontScale == 1) return 0.95f;
        if (settings.fontScale == 3) return 1.16f;
        return 1.04f;
    }
}
