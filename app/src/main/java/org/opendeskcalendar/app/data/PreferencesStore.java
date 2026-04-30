package org.opendeskcalendar.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.ui.ChineseText;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PreferencesStore {
    private static final String NAME = "open_desk_calendar";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SHOW_SECONDS = "show_seconds";
    private static final String KEY_USE_24H = "use_24h";
    private static final String KEY_WEEK_MONDAY = "week_monday";
    private static final String KEY_KEEP_ON = "keep_screen_on";
    private static final String KEY_BOOT = "boot_autostart";
    private static final String KEY_SHOW_LUNAR = "show_lunar";
    private static final String KEY_SHOW_ALMANAC = "show_almanac";
    private static final String KEY_SHOW_WIFI = "show_wifi";
    private static final String KEY_ORIENTATION_MODE = "orientation_mode";
    private static final String KEY_FONT_SCALE = "font_scale";
    private static final String KEY_REFRESH = "weather_refresh";
    private static final String KEY_CITY = "city";
    private static final String KEY_DISTRICT = "district";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_PROVIDER = "weather_provider";
    private static final String KEY_HOST = "host";
    private static final String KEY_KEY = "api_key";
    private static final String KEY_BACKUP = "backup_launcher";
    private static final String KEY_CONFIRM_EXIT = "confirm_exit";
    private static final String KEY_HOURLY_ANNOUNCEMENT = "hourly_announcement";
    private static final String KEY_HALF_HOURLY_ANNOUNCEMENT = "half_hourly_announcement";
    private static final String KEY_HOURLY_QUIET_NIGHT = "hourly_quiet_night";
    private static final String KEY_NIGHT_DIM = "night_dim";
    private static final String KEY_BURN_IN = "burn_in_protection";
    private static final String KEY_INDOOR_ENABLED = "indoor_enabled";
    private static final String KEY_INDOOR_ENDPOINT = "indoor_endpoint";
    private static final String KEY_INDOOR_TOKEN = "indoor_token";
    private static final String KEY_WEATHER = "weather_snapshot";
    private static final String KEY_INDOOR = "indoor_snapshot";
    private static final String KEY_ERRORS = "error_log";

    private final Context context;
    private final SharedPreferences preferences;

    public PreferencesStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public AppSettings getSettings() {
        String theme = preferences.getString(KEY_THEME, AppSettings.THEME_DARK);
        boolean eink = AppSettings.THEME_EINK.equals(theme);
        return new AppSettings(
                theme,
                preferences.getBoolean(KEY_SHOW_SECONDS, !eink),
                preferences.getBoolean(KEY_USE_24H, true),
                preferences.getBoolean(KEY_WEEK_MONDAY, false),
                preferences.getBoolean(KEY_KEEP_ON, true),
                preferences.getBoolean(KEY_BOOT, false),
                preferences.getBoolean(KEY_SHOW_LUNAR, true),
                preferences.getBoolean(KEY_SHOW_ALMANAC, true),
                preferences.getBoolean(KEY_SHOW_WIFI, true),
                preferences.getString(KEY_ORIENTATION_MODE, AppSettings.ORIENTATION_SYSTEM),
                preferences.getInt(KEY_FONT_SCALE, 2),
                preferences.getInt(KEY_REFRESH, eink ? 120 : 60),
                preferences.getString(KEY_CITY, context.getString(R.string.default_city)),
                preferences.getString(KEY_DISTRICT, context.getString(R.string.default_district)),
                Double.longBitsToDouble(preferences.getLong(KEY_LAT, Double.doubleToLongBits(39.9593d))),
                Double.longBitsToDouble(preferences.getLong(KEY_LON, Double.doubleToLongBits(116.2985d))),
                preferences.getString(KEY_PROVIDER, AppSettings.WEATHER_PROVIDER_OPEN_METEO),
                preferences.getString(KEY_HOST, ""),
                preferences.getString(KEY_KEY, ""),
                preferences.getString(KEY_BACKUP, ""),
                preferences.getBoolean(KEY_CONFIRM_EXIT, true),
                preferences.getBoolean(KEY_HOURLY_ANNOUNCEMENT, false),
                preferences.getBoolean(KEY_HALF_HOURLY_ANNOUNCEMENT, false),
                preferences.getBoolean(KEY_HOURLY_QUIET_NIGHT, true),
                preferences.getBoolean(KEY_NIGHT_DIM, false),
                preferences.getBoolean(KEY_BURN_IN, true),
                preferences.getBoolean(KEY_INDOOR_ENABLED, false),
                preferences.getString(KEY_INDOOR_ENDPOINT, ""),
                preferences.getString(KEY_INDOOR_TOKEN, ""));
    }

    public void saveSettings(AppSettings settings) {
        preferences.edit()
                .putString(KEY_THEME, settings.theme)
                .putBoolean(KEY_SHOW_SECONDS, settings.showSeconds)
                .putBoolean(KEY_USE_24H, settings.use24Hour)
                .putBoolean(KEY_WEEK_MONDAY, settings.weekStartsMonday)
                .putBoolean(KEY_KEEP_ON, settings.keepScreenOn)
                .putBoolean(KEY_BOOT, settings.bootAutostart)
                .putBoolean(KEY_SHOW_LUNAR, settings.showLunar)
                .putBoolean(KEY_SHOW_ALMANAC, settings.showAlmanac)
                .putBoolean(KEY_SHOW_WIFI, settings.showWifi)
                .putString(KEY_ORIENTATION_MODE, safe(settings.orientationMode))
                .putInt(KEY_FONT_SCALE, settings.fontScale)
                .putInt(KEY_REFRESH, settings.weatherRefreshMinutes)
                .putString(KEY_CITY, safe(settings.cityName))
                .putString(KEY_DISTRICT, safe(settings.districtName))
                .putLong(KEY_LAT, Double.doubleToLongBits(settings.latitude))
                .putLong(KEY_LON, Double.doubleToLongBits(settings.longitude))
                .putString(KEY_PROVIDER, safe(settings.weatherProvider))
                .putString(KEY_HOST, safe(settings.weatherHost))
                .putString(KEY_KEY, safe(settings.weatherKey))
                .putString(KEY_BACKUP, safe(settings.backupLauncherPackage))
                .putBoolean(KEY_CONFIRM_EXIT, settings.confirmExit)
                .putBoolean(KEY_HOURLY_ANNOUNCEMENT, settings.hourlyAnnouncementEnabled)
                .putBoolean(KEY_HALF_HOURLY_ANNOUNCEMENT, settings.halfHourlyAnnouncementEnabled)
                .putBoolean(KEY_HOURLY_QUIET_NIGHT, settings.hourlyAnnouncementQuietNight)
                .putBoolean(KEY_NIGHT_DIM, settings.nightDimEnabled)
                .putBoolean(KEY_BURN_IN, settings.burnInProtectionEnabled)
                .putBoolean(KEY_INDOOR_ENABLED, settings.indoorEnabled)
                .putString(KEY_INDOOR_ENDPOINT, safe(settings.indoorEndpoint))
                .putString(KEY_INDOOR_TOKEN, safe(settings.indoorToken))
                .apply();
    }

    public IndoorSnapshot readIndoor() {
        String raw = preferences.getString(KEY_INDOOR, "");
        if (raw.length() == 0) {
            return IndoorSnapshot.empty();
        }
        try {
            JSONObject json = new JSONObject(raw);
            return new IndoorSnapshot(
                    json.optDouble("temperature", Double.NaN),
                    json.optDouble("humidity", Double.NaN),
                    json.optLong("updatedAt", 0L),
                    true);
        } catch (JSONException e) {
            recordError("Sensor", context.getString(R.string.indoor_cache_parse_failed, e.getMessage()));
            return IndoorSnapshot.empty();
        }
    }

    public void saveIndoor(IndoorSnapshot snapshot) {
        if (snapshot == null || !snapshot.hasData()) {
            preferences.edit().remove(KEY_INDOOR).apply();
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("temperature", snapshot.temperatureCelsius);
            json.put("humidity", snapshot.humidityPercent);
            json.put("updatedAt", snapshot.updatedAtMillis);
            preferences.edit().putString(KEY_INDOOR, json.toString()).apply();
        } catch (JSONException e) {
            recordError("Sensor", context.getString(R.string.indoor_cache_write_failed, e.getMessage()));
        }
    }

    public WeatherSnapshot readWeather(AppSettings settings) {
        String raw = preferences.getString(KEY_WEATHER, "");
        if (raw.length() == 0) {
            return WeatherSnapshot.fallback(settings.displayCity());
        }
        try {
            JSONObject json = new JSONObject(raw);
            JSONArray forecastJson = json.optJSONArray("forecast");
            ArrayList<ForecastDay> forecast = new ArrayList<ForecastDay>();
            if (forecastJson != null) {
                for (int i = 0; i < forecastJson.length(); i++) {
                    JSONObject day = forecastJson.getJSONObject(i);
                    forecast.add(new ForecastDay(
                            day.optString("label"),
                            day.optString("date"),
                            day.optString("condition"),
                            day.optInt("low"),
                            day.optInt("high"),
                            day.optInt("code")));
                }
            }
            if (forecast.isEmpty()) {
                return WeatherSnapshot.fallback(settings.displayCity());
            }
            String city = json.optString("city", settings.displayCity());
            if (!city.equals(settings.displayCity())) {
                return WeatherSnapshot.fallback(settings.displayCity());
            }
            return new WeatherSnapshot(
                    city,
                    json.optString("condition", "多云"),
                    json.optInt("temperature", 26),
                    optionalInt(json, "apparentTemperature"),
                    json.optInt("humidity", 64),
                    json.optString("wind", "东北风 1级"),
                    json.optLong("updatedAt", 0L),
                    true,
                    forecast);
        } catch (JSONException e) {
            recordError("WeatherCache", context.getString(R.string.weather_cache_parse_failed, e.getMessage()));
            return WeatherSnapshot.fallback(settings.displayCity());
        }
    }

    public void saveWeather(WeatherSnapshot snapshot) {
        try {
            JSONObject json = new JSONObject();
            json.put("city", snapshot.city);
            json.put("condition", snapshot.condition);
            json.put("temperature", snapshot.temperatureCelsius);
            if (snapshot.apparentTemperatureCelsius != null) {
                json.put("apparentTemperature", snapshot.apparentTemperatureCelsius.intValue());
            }
            json.put("humidity", snapshot.humidityPercent);
            json.put("wind", snapshot.wind);
            json.put("updatedAt", snapshot.updatedAtMillis);
            JSONArray forecast = new JSONArray();
            for (ForecastDay day : snapshot.forecast) {
                JSONObject item = new JSONObject();
                item.put("label", day.label);
                item.put("date", day.date);
                item.put("condition", day.condition);
                item.put("low", day.lowCelsius);
                item.put("high", day.highCelsius);
                item.put("code", day.weatherCode);
                forecast.put(item);
            }
            json.put("forecast", forecast);
            preferences.edit().putString(KEY_WEATHER, json.toString()).apply();
        } catch (JSONException e) {
            recordError("WeatherCache", context.getString(R.string.weather_cache_write_failed, e.getMessage()));
        }
    }

    private static Integer optionalInt(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return Integer.valueOf(json.optInt(key));
    }

    public void recordError(String module, String message) {
        String sanitized = ChineseText.display(context, sanitize(message));
        ArrayList<ErrorLogEntry> entries = new ArrayList<ErrorLogEntry>(readErrors());
        entries.add(0, new ErrorLogEntry(System.currentTimeMillis(), module, sanitized));
        while (entries.size() > 12) {
            entries.remove(entries.size() - 1);
        }
        JSONArray array = new JSONArray();
        for (ErrorLogEntry entry : entries) {
            JSONObject item = new JSONObject();
            try {
                item.put("time", entry.timeMillis);
                item.put("module", entry.module);
                item.put("message", entry.message);
                array.put(item);
            } catch (JSONException ignored) {
            }
        }
        preferences.edit().putString(KEY_ERRORS, array.toString()).apply();
    }

    public List<ErrorLogEntry> readErrors() {
        ArrayList<ErrorLogEntry> entries = new ArrayList<ErrorLogEntry>();
        String raw = preferences.getString(KEY_ERRORS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                entries.add(new ErrorLogEntry(
                        item.optLong("time"),
                        item.optString("module"),
                        item.optString("message")));
            }
        } catch (JSONException ignored) {
        }
        return entries;
    }

    public String errorSummary() {
        StringBuilder builder = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        for (ErrorLogEntry entry : readErrors()) {
            builder.append(format.format(new Date(entry.timeMillis)))
                    .append("  ")
                    .append(entry.module)
                    .append("  ")
                    .append(entry.message)
                    .append('\n');
        }
        if (builder.length() == 0) {
            return context.getString(R.string.no_errors);
        }
        return ChineseText.display(context, builder.toString());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("(?i)(key=)[^&\\s]+", "$1***")
                .replaceAll("(?i)(apikey=)[^&\\s]+", "$1***")
                .replaceAll("(?i)(api_key=)[^&\\s]+", "$1***")
                .replaceAll("(?i)(token=)[^&\\s]+", "$1***")
                .replaceAll("(?i)(Authorization:\\s*Bearer\\s+)[^\\s]+", "$1***")
                .replaceAll("(?i)(Bearer\\s+)[^\\s]+", "$1***");
    }
}
