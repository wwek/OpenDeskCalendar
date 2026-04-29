package org.opendeskcalendar.app.ui;

import android.content.Context;
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
import android.widget.TextView;

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
    private MonthGrid month;
    private NetworkState networkState;
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

    public void update(AppSettings settings, WeatherSnapshot weather, MonthGrid month, NetworkState networkState) {
        this.settings = settings;
        this.palette = ThemePalette.from(settings);
        this.weather = weather;
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
        if (landscape) {
            addView(buildLandscape(), matchFrame());
        } else {
            addView(buildPortrait(), matchFrame());
        }
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
        root.addView(buildTopBar(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(46)));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout body = horizontal();
        LinearLayout left = vertical();
        left.addView(buildTimeSection(false), weighted(1.35f));
        left.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        left.addView(buildWeatherSection(), weighted(1.05f));
        left.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        left.addView(buildForecastSection(false), weighted(1.20f));
        body.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.40f));
        body.addView(divider(), new LinearLayout.LayoutParams(dp(1), LayoutParams.MATCH_PARENT));
        body.addView(buildCalendarSection(false), new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.60f));

        root.addView(body, weighted(1f));
        root.addView(divider(), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(buildBottomSection(false), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(56)));
        return root;
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

        TextView settingsButton = label(getResources().getString(R.string.settings_title), 16, palette.secondary, false);
        settingsButton.setSingleLine(true);
        settingsButton.setGravity(Gravity.CENTER);
        settingsButton.setPadding(dp(8), dp(4), dp(8), dp(4));
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onOpenSettings();
                }
            }
        });
        bar.addView(settingsButton, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        TextView state = label(settings.showWifi ? networkState.label : "", 17, palette.secondary, false);
        state.setSingleLine(true);
        state.setGravity(Gravity.RIGHT);
        state.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (listener != null) {
                    listener.onOpenSystemMenu();
                }
                return true;
            }
        });
        bar.addView(state, new LinearLayout.LayoutParams(dp(112), LayoutParams.WRAP_CONTENT));
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
        TextView title = label(weatherSymbol(weather.condition) + "  " + localize(weather.city), 18, palette.primary, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        left.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView temp = label(weather.temperatureCelsius + "°C", 34, palette.primary, true);
        temp.setIncludeFontPadding(false);
        left.addView(temp, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        section.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = vertical();
        right.setGravity(Gravity.CENTER_VERTICAL);
        TextView cache = label(weather.fromCache ? getResources().getString(R.string.dashboard_cache) : "", 14, palette.warning, true);
        cache.setGravity(Gravity.RIGHT);
        right.addView(cache, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView humidity = label(getResources().getString(R.string.dashboard_humidity, weather.humidityPercent), 14, palette.secondary, false);
        humidity.setGravity(Gravity.RIGHT);
        humidity.setSingleLine(true);
        humidity.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(humidity, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView wind = label(localize(weather.condition + " | " + weather.wind), 14, palette.secondary, false);
        wind.setGravity(Gravity.RIGHT);
        wind.setSingleLine(true);
        wind.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(wind, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
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

    private View buildForecastDay(ForecastDay day, boolean compact) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), 0, dp(2), 0);

        TextView label = label(localize(day.label), compact ? 13 : 16, palette.primary, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView symbol = label(weatherSymbol(day.condition), compact ? 22 : 29, palette.accent, true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setIncludeFontPadding(false);
        item.addView(symbol, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

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
        LinearLayout section = vertical();
        section.setPadding(dp(12), compact ? dp(7) : dp(12), dp(12), compact ? dp(7) : dp(12));

        TextView title = label(getResources().getString(R.string.format_month_title, month.year, month.month),
                compact ? 19 : 22,
                palette.primary,
                true);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onGoToToday();
                }
            }
        });
        section.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        Calendar now = Calendar.getInstance();
        ChineseLunarCalendar.LunarDate lunar = ChineseLunarCalendar.fromSolar(
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        TextView lunarTitle = label(localize(lunar.monthName + lunar.dayName + "  " + ChineseLunarCalendar.ganzhiAnimal(lunar)),
                compact ? 12 : 14,
                palette.secondary,
                false);
        lunarTitle.setGravity(Gravity.CENTER);
        lunarTitle.setSingleLine(true);
        lunarTitle.setEllipsize(TextUtils.TruncateAt.END);
        section.addView(lunarTitle, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

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
        int padding = compact ? dp(1) : dp(3);
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
        TextView number = label(String.valueOf(day.day), compact ? 14 : 14, dayColor(day), true);
        number.setGravity(Gravity.CENTER);
        number.setIncludeFontPadding(false);
        texts.addView(number, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        TextView lower = label(settings.showLunar && (!compact || day.inMonth) ? localize(day.lowerLabel()) : "",
                compact ? 8 : 9,
                day.inMonth ? palette.secondary : palette.muted,
                false);
        lower.setGravity(Gravity.CENTER);
        lower.setSingleLine(true);
        lower.setEllipsize(TextUtils.TruncateAt.END);
        lower.setIncludeFontPadding(false);
        texts.addView(lower, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        cell.addView(texts, matchFrame());

        if (day.holiday != null) {
            TextView badge = label(localize(day.holiday.badge), compact ? 8 : 9, badgeColor(day), true);
            badge.setGravity(Gravity.RIGHT | Gravity.TOP);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP);
            cell.addView(badge, params);
        }
        return cell;
    }

    private View buildBottomSection(boolean compact) {
        LinearLayout section = compact ? vertical() : horizontal();
        section.setGravity(compact ? Gravity.CENTER_VERTICAL : Gravity.CENTER_VERTICAL);
        section.setPadding(dp(16), dp(5), dp(16), dp(5));

        Date updated = new Date(weather.updatedAtMillis == 0L ? System.currentTimeMillis() : weather.updatedAtMillis);
        String updateText = weather.fromCache
                ? getResources().getString(R.string.dashboard_cached_at, updateFormat.format(updated))
                : getResources().getString(R.string.dashboard_updated_at, updateFormat.format(updated));
        TextView updatedAt = label(updateText, compact ? 11 : 15, palette.muted, false);
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
            text.setGravity(compact ? Gravity.LEFT : Gravity.RIGHT);
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
