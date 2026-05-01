package org.opendeskcalendar.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.audio.HourlyAnnouncer;
import org.opendeskcalendar.app.calendar.CalendarRepository;
import org.opendeskcalendar.app.calendar.HolidayRepository;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.CalendarDay;
import org.opendeskcalendar.app.data.IndoorSnapshot;
import org.opendeskcalendar.app.data.MonthGrid;
import org.opendeskcalendar.app.data.PreferencesStore;
import org.opendeskcalendar.app.data.WeatherSnapshot;
import org.opendeskcalendar.app.net.IndoorSensorService;
import org.opendeskcalendar.app.net.NetworkState;
import org.opendeskcalendar.app.net.WeatherService;
import org.opendeskcalendar.app.ui.ChineseText;
import org.opendeskcalendar.app.ui.DashboardView;

import java.lang.ref.WeakReference;
import java.util.Calendar;

public class MainActivity extends Activity implements DashboardView.Listener {
    private static final int DASHBOARD_REFRESH_BUCKET_MINUTES = 5;

    private final Handler handler = new Handler();
    private PreferencesStore store;
    private CalendarRepository calendarRepository;
    private DashboardView dashboardView;
    private HourlyAnnouncer hourlyAnnouncer;
    private AppSettings settings;
    private WeatherSnapshot weather;
    private IndoorSnapshot indoor;
    private Calendar visibleMonth;
    private Calendar selectedDate;
    private boolean launchedBackupByTap;
    private int lastDashboardRefreshDay = -1;
    private long lastDashboardRefreshBucket = -1L;
    private float lastScreenBrightness = Float.NaN;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (settings == null) {
                handler.postDelayed(this, 60000L);
                return;
            }
            applyNightDim();
            if (shouldRefreshDashboard()) {
                refreshDashboard();
            } else {
                dashboardView.refreshClock();
            }
            long delay = settings.showSeconds ? 1000L : 60000L;
            handler.postDelayed(this, delay);
        }
    };
    private final Runnable deferredDashboardRefresh = new Runnable() {
        @Override
        public void run() {
            refreshDashboard();
        }
    };
    private final Runnable hourlyAnnouncement = new Runnable() {
        @Override
        public void run() {
            if (settings != null && hourlyAnnouncer != null) {
                hourlyAnnouncer.announceIfNeeded(settings, Calendar.getInstance());
            }
            scheduleHourlyAnnouncement();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new PreferencesStore(this);
        calendarRepository = new CalendarRepository(new HolidayRepository(this));
        dashboardView = new DashboardView(this);
        dashboardView.setListener(this);
        setContentView(dashboardView);
        visibleMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        loadState();
        refreshDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppSettings previous = settings;
        loadState();
        refreshDashboard();
        refreshWeatherIfNeeded(weatherSettingsChanged(previous, settings, weather));
        refreshIndoorIfNeeded(indoorSettingsChanged(previous, settings, indoor));
        handler.removeCallbacks(tick);
        handler.post(tick);
        scheduleHourlyAnnouncement();
        handler.postDelayed(deferredDashboardRefresh, 600L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(hourlyAnnouncement);
        handler.removeCallbacks(deferredDashboardRefresh);
        if (hourlyAnnouncer != null) {
            hourlyAnnouncer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hourlyAnnouncer != null) {
            hourlyAnnouncer.shutdown();
            hourlyAnnouncer = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (settings != null && !settings.confirmExit) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.exit_confirm, (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onOpenSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onOpenCitySettings() {
        startActivity(new Intent(this, CitySettingsActivity.class));
    }

    @Override
    public void onOpenSystemMenu() {
        final String[] items = getResources().getStringArray(R.array.system_menu_items);
        new AlertDialog.Builder(this)
                .setTitle(R.string.system_entry)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) openSystemSettings(Settings.ACTION_WIFI_SETTINGS);
                    if (which == 1) openHomeChooser();
                    if (which == 2) openBackupLauncher();
                    if (which == 3) refreshWeatherIfNeeded(true);
                })
                .show();
    }

    @Override
    public void onGoToToday() {
        visibleMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        refreshDashboard();
    }

    @Override
    public void onPreviousMonth() {
        visibleMonth.add(Calendar.MONTH, -1);
        refreshDashboard();
    }

    @Override
    public void onNextMonth() {
        visibleMonth.add(Calendar.MONTH, 1);
        refreshDashboard();
    }

    @Override
    public void onDateSelected(CalendarDay day) {
        selectedDate = dateFrom(day);
        String message = day.dateKey + "  " + ChineseText.display(this, day.lunarLabel);
        if (day.holiday != null) {
            message += "  " + ChineseText.display(this, day.holiday.badge + day.holiday.label);
        }
        refreshDashboard();
        dashboardView.showMessage(message);
    }

    private void loadState() {
        settings = store.getSettings();
        applyOrientation();
        if (settings.keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        applyNightDim();
        weather = store.readWeather(settings);
        indoor = store.readIndoor();
    }

    private void applyOrientation() {
        if (AppSettings.ORIENTATION_LANDSCAPE.equals(settings.orientationMode)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            return;
        }
        if (AppSettings.ORIENTATION_PORTRAIT.equals(settings.orientationMode)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            return;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    private void refreshDashboard() {
        if (settings == null) {
            return;
        }
        MonthGrid month = calendarRepository.buildMonth(visibleMonth, settings);
        dashboardView.update(settings, weather, indoor, month, NetworkState.from(this), selectedDate);
        lastDashboardRefreshDay = dashboardDayKey();
        lastDashboardRefreshBucket = dashboardRefreshBucket();
    }

    private boolean shouldRefreshDashboard() {
        return dashboardDayKey() != lastDashboardRefreshDay
                || dashboardRefreshBucket() != lastDashboardRefreshBucket;
    }

    private int dashboardDayKey() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR);
    }

    private long dashboardRefreshBucket() {
        Calendar now = Calendar.getInstance();
        int dayKey = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR);
        int minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return (long) dayKey * 1000L + minuteOfDay / DASHBOARD_REFRESH_BUCKET_MINUTES;
    }

    private void scheduleHourlyAnnouncement() {
        handler.removeCallbacks(hourlyAnnouncement);
        if (settings == null || (!settings.hourlyAnnouncementEnabled && !settings.halfHourlyAnnouncementEnabled)) {
            shutdownHourlyAnnouncer();
            return;
        }
        if (hourlyAnnouncer == null) {
            hourlyAnnouncer = new HourlyAnnouncer(this, store);
        }
        handler.postDelayed(hourlyAnnouncement, nextAnnouncementDelayMillis(settings));
    }

    private void shutdownHourlyAnnouncer() {
        if (hourlyAnnouncer == null) {
            return;
        }
        hourlyAnnouncer.shutdown();
        hourlyAnnouncer = null;
    }

    private static long nextAnnouncementDelayMillis(AppSettings settings) {
        long nowMillis = System.currentTimeMillis();
        long nextMillis = Long.MAX_VALUE;
        if (settings.hourlyAnnouncementEnabled) {
            nextMillis = Math.min(nextMillis, nextAnnouncementTimeMillis(0, nowMillis));
        }
        if (settings.halfHourlyAnnouncementEnabled) {
            nextMillis = Math.min(nextMillis, nextAnnouncementTimeMillis(30, nowMillis));
        }
        return Math.max(1000L, nextMillis - nowMillis);
    }

    private static long nextAnnouncementTimeMillis(int minute, long nowMillis) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= nowMillis + 1000L) {
            next.add(Calendar.HOUR_OF_DAY, 1);
        }
        return next.getTimeInMillis();
    }

    private static Calendar dateFrom(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, day.year);
        calendar.set(Calendar.MONTH, day.month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day.day);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private void refreshWeatherIfNeeded(boolean force) {
        long refreshMillis = settings.weatherRefreshMinutes * 60000L;
        boolean stale = weather == null || weather.updatedAtMillis == 0L
                || System.currentTimeMillis() - weather.updatedAtMillis > refreshMillis;
        if (!force && !stale) {
            return;
        }
        new WeatherTask(this, force).execute(settings);
    }

    private void applyWeather(WeatherSnapshot snapshot, boolean forced) {
        weather = snapshot;
        store.saveWeather(snapshot);
        refreshDashboard();
        if (forced) {
            dashboardView.showMessage(getString(R.string.weather_updated));
        }
    }

    private void failWeather(String message) {
        store.recordError("Weather", message);
        weather = store.readWeather(settings);
        refreshDashboard();
        dashboardView.showMessage(getString(R.string.weather_failed_cache));
    }

    private boolean weatherSettingsChanged(AppSettings previous, AppSettings next, WeatherSnapshot snapshot) {
        if (snapshot == null || snapshot.updatedAtMillis == 0L) {
            return false;
        }
        if (!same(snapshot.city, next.displayCity())) {
            return true;
        }
        if (previous == null) {
            return false;
        }
        return !same(previous.cityName, next.cityName)
                || !same(previous.districtName, next.districtName)
                || previous.latitude != next.latitude
                || previous.longitude != next.longitude
                || !same(previous.weatherProvider, next.weatherProvider)
                || !same(previous.weatherHost, next.weatherHost)
                || !same(previous.weatherKey, next.weatherKey);
    }

    private void refreshIndoorIfNeeded(boolean force) {
        if (!settings.indoorEnabled) {
            indoor = IndoorSnapshot.empty();
            store.saveIndoor(indoor);
            refreshDashboard();
            return;
        }
        boolean stale = indoor == null || indoor.updatedAtMillis == 0L
                || System.currentTimeMillis() - indoor.updatedAtMillis > settings.weatherRefreshMinutes * 60000L;
        if (!force && !stale) {
            return;
        }
        new IndoorTask(this).execute(settings);
    }

    private void applyIndoor(IndoorSnapshot snapshot) {
        indoor = snapshot;
        store.saveIndoor(snapshot);
        refreshDashboard();
    }

    private void failIndoor(String message) {
        store.recordError("Sensor", message);
        indoor = store.readIndoor();
        refreshDashboard();
    }

    private boolean indoorSettingsChanged(AppSettings previous, AppSettings next, IndoorSnapshot snapshot) {
        if (snapshot == null || snapshot.updatedAtMillis == 0L) {
            return false;
        }
        if (previous == null) {
            return false;
        }
        return previous.indoorEnabled != next.indoorEnabled
                || !same(previous.indoorEndpoint, next.indoorEndpoint)
                || !same(previous.indoorToken, next.indoorToken);
    }

    private static boolean same(String left, String right) {
        if (left == null) {
            return right == null || right.length() == 0;
        }
        if (right == null) {
            return left.length() == 0;
        }
        return left.equals(right);
    }

    private void applyNightDim() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        float brightness;
        if (!settings.nightDimEnabled) {
            brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            brightness = hour >= 22 || hour < 6 ? 0.12f : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        if (Float.compare(lastScreenBrightness, brightness) == 0) {
            return;
        }
        params.screenBrightness = brightness;
        getWindow().setAttributes(params);
        lastScreenBrightness = brightness;
    }

    private void openSystemSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (ActivityNotFoundException e) {
            store.recordError("System", getString(R.string.system_settings_unavailable) + ": " + e.getMessage());
            dashboardView.showMessage(getString(R.string.system_settings_unavailable));
        }
    }

    private void openHomeChooser() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(Intent.createChooser(intent, getString(R.string.choose_default_home)));
    }

    private void openBackupLauncher() {
        if (settings.backupLauncherPackage == null || settings.backupLauncherPackage.length() == 0) {
            openHomeChooser();
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(settings.backupLauncherPackage);
        if (launch == null) {
            store.recordError("Launcher", getString(R.string.backup_launcher_unavailable) + ": " + settings.backupLauncherPackage);
            dashboardView.showMessage(getString(R.string.backup_launcher_unavailable));
            return;
        }
        launchedBackupByTap = true;
        startActivity(launch);
    }

    static final class WeatherTask extends AsyncTask<AppSettings, Void, WeatherSnapshot> {
        private final WeakReference<MainActivity> activityRef;
        private final boolean forced;
        private Exception error;

        WeatherTask(MainActivity activity, boolean forced) {
            this.activityRef = new WeakReference<MainActivity>(activity);
            this.forced = forced;
        }

        @Override
        protected WeatherSnapshot doInBackground(AppSettings... params) {
            try {
                return new WeatherService().fetch(params[0]);
            } catch (Exception e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(WeatherSnapshot snapshot) {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            if (activity.isFinishing()) {
                return;
            }
            if (snapshot != null) {
                activity.applyWeather(snapshot, forced);
            } else {
                activity.failWeather(error == null ? activity.getString(R.string.unknown_error) : error.getMessage());
            }
        }
    }

    static final class IndoorTask extends AsyncTask<AppSettings, Void, IndoorSnapshot> {
        private final WeakReference<MainActivity> activityRef;
        private Exception error;

        IndoorTask(MainActivity activity) {
            this.activityRef = new WeakReference<MainActivity>(activity);
        }

        @Override
        protected IndoorSnapshot doInBackground(AppSettings... params) {
            try {
                return new IndoorSensorService().fetch(params[0]);
            } catch (Exception e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(IndoorSnapshot snapshot) {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (snapshot != null) {
                activity.applyIndoor(snapshot);
            } else {
                activity.failIndoor(error == null ? activity.getString(R.string.unknown_error) : error.getMessage());
            }
        }
    }
}
