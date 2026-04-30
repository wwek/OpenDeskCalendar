package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
    private final SimpleDateFormat time12Format = new SimpleDateFormat("hh:mm", Locale.CHINA);
    private final SimpleDateFormat secondFormat = new SimpleDateFormat(":ss", Locale.CHINA);
    private final SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.CHINA);
    private final SimpleDateFormat updateFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private Listener listener;
    private AppSettings settings;
    private ThemePalette palette;
    private WeatherSnapshot weather;
    private IndoorSnapshot indoor;
    private MonthGrid month;
    private NetworkState networkState;
    private TextView messageView;
    private String transientMessage = "";
    private long transientUntil = 0L;
    private int lastOrientation = 0;
    private static final int ICON_SETTINGS = 1;
    private static final int ICON_WIFI = 2;

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

    public void update(AppSettings settings, WeatherSnapshot weather, IndoorSnapshot indoor, MonthGrid month, NetworkState networkState) {
        this.settings = settings;
        this.palette = ThemePalette.from(settings);
        this.weather = weather;
        this.indoor = indoor;
        this.month = month;
        this.networkState = networkState;
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
        root.addView(buildTopBar(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(buildTimeSection(true), weighted(1.55f));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(buildWeatherSection(), weighted(1.05f));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(buildForecastSection(true), weighted(1.15f));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(buildCalendarSection(true), weighted(3.15f));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(buildBottomSection(true), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(56)));
        return root;
    }

    private View buildLandscape() {
        LinearLayout root = vertical();
        LinearLayout body = horizontal();
        LinearLayout left = vertical();
        left.addView(buildTimeSection(false), weighted(1.35f));
        left.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        left.addView(buildWeatherSection(), weighted(1.05f));
        left.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        left.addView(buildLandscapeForecastSection(), weighted(1.20f));
        body.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.38f));
        body.addView(divider(), new LinearLayout.LayoutParams(dp(1), LayoutParams.MATCH_PARENT));
        body.addView(buildLandscapeCalendarColumn(), new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.62f));

        root.addView(body, weighted(1f));
        return root;
    }

    private View buildLandscapeCalendarColumn() {
        LinearLayout section = vertical();
        section.addView(buildCalendarSection(true, true), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        if (settings.showAlmanac) {
            section.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
            section.addView(buildAlmanacSection(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(52)));
        }
        return section;
    }

    private View buildTopBar() {
        LinearLayout bar = horizontal();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), 0, dp(14), 0);

        TextView city = label(localize(settings.displayCity()), 19, palette.primary, true);
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

        IconButton settingsButton = new IconButton(getContext(), ICON_SETTINGS, palette.secondary);
        settingsButton.setContentDescription(getResources().getString(R.string.settings_title));
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onOpenSettings();
                }
            }
        });
        bar.addView(settingsButton, new LinearLayout.LayoutParams(dp(56), LayoutParams.MATCH_PARENT));

        if (settings.showWifi) {
            int wifiColor = networkState.connected ? palette.secondary : palette.warning;
            IconButton state = new IconButton(getContext(), ICON_WIFI, wifiColor);
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
            bar.addView(state, new LinearLayout.LayoutParams(dp(56), LayoutParams.MATCH_PARENT));
        }
        return bar;
    }

    private View buildTimeSection(boolean compact) {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), compact ? dp(8) : dp(12), dp(16), compact ? dp(8) : dp(12));
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
        date.setSingleLine(true);
        date.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(date, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        String main = settings.use24Hour ? time24Format.format(now.getTime()) : time12Format.format(now.getTime());
        TextView time = label(main, compact ? 68 : 54, palette.primary, true);
        time.setIncludeFontPadding(false);
        time.setSingleLine(true);
        row.addView(time, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        if (settings.showSeconds && !settings.isEink()) {
            TextView seconds = label(secondFormat.format(now.getTime()), compact ? 23 : 20, palette.secondary, false);
            seconds.setIncludeFontPadding(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.leftMargin = dp(6);
            params.bottomMargin = compact ? dp(5) : dp(8);
            row.addView(seconds, params);
        }
        if (!settings.use24Hour) {
            TextView suffix = label(amPmFormat.format(now.getTime()), compact ? 16 : 16, palette.secondary, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.leftMargin = dp(8);
            params.bottomMargin = compact ? dp(8) : dp(12);
            row.addView(suffix, params);
        }
        section.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private View buildWeatherSection() {
        LinearLayout section = horizontal();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), dp(8), dp(16), dp(8));
        section.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenCitySettings();
                }
                return true;
            }
        });

        LinearLayout left = vertical();
        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        WeatherIconView currentIcon = new WeatherIconView(getContext(), weather.condition, weatherIconColor(weather.condition));
        LinearLayout.LayoutParams currentIconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        currentIconParams.rightMargin = dp(8);
        titleRow.addView(currentIcon, currentIconParams);
        TextView title = label(localize(weather.city), 18, palette.primary, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        left.addView(titleRow, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView temp = label(weather.temperatureCelsius + "°C", 34, palette.primary, true);
        temp.setIncludeFontPadding(false);
        left.addView(temp, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        section.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = vertical();
        right.setGravity(Gravity.CENTER_VERTICAL);
        TextView humidity = label(getResources().getString(R.string.dashboard_humidity, weather.humidityPercent), 14, palette.secondary, false);
        humidity.setGravity(Gravity.END);
        humidity.setSingleLine(true);
        humidity.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(humidity, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView wind = label(localize(weather.condition + " | " + weather.wind), 14, palette.secondary, false);
        wind.setGravity(Gravity.END);
        wind.setSingleLine(true);
        wind.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(wind, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (settings.indoorEnabled && indoor != null && indoor.hasData()) {
            TextView indoorView = label(indoorText(), 14, palette.secondary, false);
            indoorView.setGravity(Gravity.END);
            indoorView.setSingleLine(true);
            indoorView.setEllipsize(TextUtils.TruncateAt.END);
            right.addView(indoorView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        section.addView(right, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return section;
    }

    private View buildForecastSection(boolean compact) {
        LinearLayout section = vertical();
        section.setPadding(dp(16), compact ? dp(7) : dp(10), dp(16), compact ? dp(7) : dp(10));
        TextView title = label(getResources().getString(R.string.dashboard_forecast_title), compact ? 15 : 18, palette.secondary, true);
        title.setSingleLine(true);
        section.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER);
        int count = Math.min(3, weather.forecast.size());
        for (int i = 0; i < count; i++) {
            row.addView(buildForecastDay(weather.forecast.get(i), compact), new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        }
        section.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private View buildLandscapeForecastSection() {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), dp(4), dp(16), dp(4));
        TextView title = label(getResources().getString(R.string.dashboard_forecast_title), 15, palette.secondary, true);
        title.setSingleLine(true);
        section.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER);
        int count = Math.min(3, weather.forecast.size());
        for (int i = 0; i < count; i++) {
            row.addView(buildLandscapeForecastDay(weather.forecast.get(i)), new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        }
        section.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private View buildLandscapeForecastDay(ForecastDay day) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), 0, dp(2), 0);

        TextView label = label(localize(day.label), 13, palette.primary, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout detail = horizontal();
        detail.setGravity(Gravity.CENTER);
        WeatherIconView symbol = new WeatherIconView(getContext(), day.condition, weatherIconColor(day.condition));
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(dp(24), dp(22));
        symbolParams.rightMargin = dp(5);
        detail.addView(symbol, symbolParams);

        TextView condition = label(localize(day.condition), 11, palette.secondary, false);
        condition.setSingleLine(true);
        condition.setEllipsize(TextUtils.TruncateAt.END);
        detail.addView(condition, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        item.addView(detail, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView temp = label(day.tempRange(), 11, palette.secondary, false);
        temp.setGravity(Gravity.CENTER);
        temp.setSingleLine(true);
        item.addView(temp, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View buildForecastDay(ForecastDay day, boolean compact) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), 0, dp(2), 0);

        TextView label = label(localize(day.label), compact ? 13 : 16, palette.primary, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        WeatherIconView symbol = new WeatherIconView(getContext(), day.condition, weatherIconColor(day.condition));
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(compact ? dp(30) : dp(38), compact ? dp(26) : dp(34));
        symbolParams.topMargin = compact ? dp(1) : dp(2);
        symbolParams.bottomMargin = compact ? dp(1) : dp(2);
        item.addView(symbol, symbolParams);

        TextView condition = label(localize(day.condition), compact ? 12 : 15, palette.secondary, false);
        condition.setGravity(Gravity.CENTER);
        condition.setSingleLine(true);
        condition.setEllipsize(TextUtils.TruncateAt.END);
        item.addView(condition, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView temp = label(day.tempRange(), compact ? 12 : 15, palette.secondary, false);
        temp.setGravity(Gravity.CENTER);
        temp.setSingleLine(true);
        item.addView(temp, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View buildCalendarSection(boolean compact) {
        return buildCalendarSection(compact, false);
    }

    private View buildCalendarSection(boolean compact, boolean inlineHeader) {
        LinearLayout section = vertical();
        section.setPadding(dp(12), compact ? dp(7) : dp(12), dp(12), compact ? dp(7) : dp(12));

        Calendar now = Calendar.getInstance();
        ChineseLunarCalendar.LunarDate lunar = ChineseLunarCalendar.fromSolar(
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String lunarHeader = localize(lunar.monthName + lunar.dayName + "  " + ChineseLunarCalendar.ganzhiAnimal(lunar));
        TextView title = label(getResources().getString(R.string.format_month_title, month.year, month.month),
                compact ? 19 : 22,
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
            TextView lunarTitle = label(lunarHeader, 11, palette.secondary, false);
            lunarTitle.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            lunarTitle.setSingleLine(true);
            lunarTitle.setEllipsize(TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams lunarParams = new FrameLayout.LayoutParams(dp(210), LayoutParams.MATCH_PARENT, Gravity.END | Gravity.CENTER_VERTICAL);
            header.addView(lunarTitle, lunarParams);
            section.addView(header, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(30)));
        } else {
            section.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            TextView lunarTitle = label(lunarHeader, compact ? 12 : 14, palette.secondary, false);
            lunarTitle.setGravity(Gravity.CENTER);
            lunarTitle.setSingleLine(true);
            lunarTitle.setEllipsize(TextUtils.TruncateAt.END);
            section.addView(lunarTitle, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        LinearLayout grid = vertical();
        grid.addView(buildWeekHeader(compact), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, compact ? dp(22) : dp(28)));
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
            TextView item = label(localize(name), compact ? 11 : 15, palette.muted, true);
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
        TextView number = label(String.valueOf(day.day), compact ? 13 : 15, dayColor(day), true);
        number.setGravity(Gravity.CENTER);
        number.setIncludeFontPadding(false);
        texts.addView(number, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        boolean showLower = settings.showLunar;
        TextView lower = label(showLower ? localize(day.lowerLabel()) : "",
                compact ? 8 : 10,
                day.inMonth ? palette.secondary : palette.muted,
                false);
        lower.setGravity(Gravity.CENTER);
        lower.setSingleLine(true);
        lower.setEllipsize(TextUtils.TruncateAt.END);
        lower.setIncludeFontPadding(false);
        texts.addView(lower, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        cell.addView(texts, matchFrame());

        if (day.holiday != null) {
            TextView badge = label(localize(day.holiday.badge), compact ? 7 : 8, badgeColor(day), true);
            badge.setGravity(Gravity.END | Gravity.TOP);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP);
            cell.addView(badge, params);
        }
        return cell;
    }

    private View buildAlmanacSection() {
        LinearLayout section = vertical();
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(14), 0, dp(14), dp(6));
        Calendar now = Calendar.getInstance();
        TextView good = label(localize("宜：") + localize(AlmanacProvider.good(now)), 11, palette.secondary, false);
        good.setGravity(Gravity.CENTER_VERTICAL);
        good.setSingleLine(true);
        good.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(good, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        TextView avoid = label(localize("忌：") + localize(AlmanacProvider.avoid(now)), 11, palette.secondary, false);
        avoid.setGravity(Gravity.CENTER_VERTICAL);
        avoid.setSingleLine(true);
        avoid.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(avoid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return section;
    }

    private View buildBottomSection(boolean compact) {
        LinearLayout section = compact ? vertical() : horizontal();
        section.setGravity(compact ? Gravity.CENTER_VERTICAL : Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), dp(5), dp(16), dp(5));

        TextView updatedAt = label(updateText(), compact ? 11 : 15, palette.muted, false);
        updatedAt.setSingleLine(true);
        updatedAt.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(updatedAt, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, compact ? 0f : 0.35f));

        if (settings.showAlmanac) {
            Calendar now = Calendar.getInstance();
            String almanac = getResources().getString(
                    R.string.dashboard_good_avoid,
                    localize(AlmanacProvider.good(now)),
                    localize(AlmanacProvider.avoid(now)));
            TextView text = label(almanac, compact ? 11 : 16, palette.secondary, false);
            text.setSingleLine(true);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setGravity(compact ? Gravity.START : Gravity.END);
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

    private String updateText() {
        Date updated = new Date(weather.updatedAtMillis == 0L ? System.currentTimeMillis() : weather.updatedAtMillis);
        return weather.fromCache
                ? getResources().getString(R.string.dashboard_cached_at, updateFormat.format(updated))
                : getResources().getString(R.string.dashboard_updated_at, updateFormat.format(updated));
    }

    private final class IconButton extends View {
        private final int type;
        private final int color;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        IconButton(Context context, int type, int color) {
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
            if (type == ICON_SETTINGS) {
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
    }

    private final class WeatherIconView extends View {
        private final String condition;
        private final int color;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        WeatherIconView(Context context, String condition, int color) {
            super(context);
            this.condition = condition == null ? "" : condition;
            this.color = color;
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
        background.setColor(day.today && !settings.isMonochrome() ? palette.todayFill : Color.TRANSPARENT);
        background.setCornerRadius(dp(4));
        if (day.today && settings.isMonochrome()) {
            background.setStroke(dp(1), palette.primary);
        }
        return background;
    }

    private int dayColor(CalendarDay day) {
        if (!day.inMonth) {
            return palette.muted;
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
        view.setBackgroundColor(palette.divider);
        return view;
    }

    private int weatherIconColor(String condition) {
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

    private boolean isSunny(String condition) {
        return "晴".equals(condition);
    }

    private boolean isRain(String condition) {
        return condition != null && condition.indexOf("雨") >= 0;
    }

    private boolean isSnow(String condition) {
        return condition != null && condition.indexOf("雪") >= 0;
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
