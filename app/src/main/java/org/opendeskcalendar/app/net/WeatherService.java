package org.opendeskcalendar.app.net;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.ForecastDay;
import org.opendeskcalendar.app.data.WeatherSnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public final class WeatherService {
    public WeatherSnapshot fetch(AppSettings settings) throws Exception {
        if (AppSettings.WEATHER_PROVIDER_QWEATHER.equals(settings.weatherProvider)) {
            return fetchQweather(settings);
        }
        String endpoint = buildForecastUrl(settings);
        HttpURLConnection connection = open(endpoint, settings);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("天气接口失败，HTTP " + code);
            }
            String raw = readResponse(connection);
            return parseOpenMeteoForecast(raw, settings);
        } finally {
            connection.disconnect();
        }
    }

    public List<CityResult> searchCities(String query) throws Exception {
        String endpoint = Uri.parse("https://geocoding-api.open-meteo.com/v1/search")
                .buildUpon()
                .appendQueryParameter("name", query)
                .appendQueryParameter("count", "8")
                .appendQueryParameter("language", "zh")
                .appendQueryParameter("format", "json")
                .build()
                .toString();
        HttpURLConnection connection = open(endpoint, null);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("城市搜索失败，HTTP " + code);
            }
            JSONObject root = new JSONObject(readResponse(connection));
            JSONArray results = root.optJSONArray("results");
            ArrayList<CityResult> cities = new ArrayList<CityResult>();
            if (results == null) {
                return cities;
            }
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                cities.add(new CityResult(
                        item.optString("name"),
                        item.optString("admin1"),
                        item.optString("country"),
                        item.optDouble("latitude"),
                        item.optDouble("longitude")));
            }
            return cities;
        } finally {
            connection.disconnect();
        }
    }

    private WeatherSnapshot fetchQweather(AppSettings settings) throws Exception {
        String nowRaw = fetchRaw(buildQweatherWeatherUrl(settings, "now"), settings);
        String dailyRaw = fetchRaw(buildQweatherWeatherUrl(settings, "3d"), settings);
        return parseQweatherForecast(nowRaw, dailyRaw, settings);
    }

    private String fetchRaw(String endpoint, AppSettings settings) throws IOException {
        HttpURLConnection connection = open(endpoint, settings);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("天气接口失败，HTTP " + code);
            }
            return readResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    private String buildForecastUrl(AppSettings settings) throws IOException {
        String host = settings.weatherHost == null || settings.weatherHost.length() == 0
                ? "https://api.open-meteo.com/v1/forecast"
                : settings.weatherHost;
        Uri.Builder builder = Uri.parse(host).buildUpon()
                .appendQueryParameter("latitude", String.format(Locale.US, "%.5f", settings.latitude))
                .appendQueryParameter("longitude", String.format(Locale.US, "%.5f", settings.longitude))
                .appendQueryParameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m")
                .appendQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .appendQueryParameter("forecast_days", "3")
                .appendQueryParameter("timezone", "auto");
        if (settings.weatherKey != null && settings.weatherKey.length() > 0) {
            builder.appendQueryParameter("key", settings.weatherKey);
        }
        return builder.build().toString();
    }

    private String buildQweatherWeatherUrl(AppSettings settings, String days) throws IOException {
        String host = settings.weatherHost == null ? "" : settings.weatherHost.trim();
        if (host.length() == 0) {
            throw new IOException("和风天气 Base API 未设置");
        }
        String key = settings.weatherKey == null ? "" : settings.weatherKey.trim();
        if (key.length() == 0) {
            throw new IOException("和风天气 Key 未设置");
        }
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "https://" + host;
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return Uri.parse(host + "/v7/weather/" + days)
                .buildUpon()
                .appendQueryParameter("location", String.format(Locale.US, "%.2f,%.2f", settings.longitude, settings.latitude))
                .appendQueryParameter("lang", "zh")
                .appendQueryParameter("unit", "m")
                .build()
                .toString();
    }

    private HttpURLConnection open(String endpoint, AppSettings settings) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent", "OpenDeskCalendar/0.1");
        if (settings != null && AppSettings.WEATHER_PROVIDER_QWEATHER.equals(settings.weatherProvider)) {
            String key = settings.weatherKey == null ? "" : settings.weatherKey.trim();
            if (key.length() > 0) {
                if (key.indexOf('.') >= 0) {
                    connection.setRequestProperty("Authorization", "Bearer " + key);
                } else {
                    connection.setRequestProperty("X-QW-Api-Key", key);
                }
            }
        }
        return connection;
    }

    private WeatherSnapshot parseOpenMeteoForecast(String raw, AppSettings settings) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject current = root.getJSONObject("current");
        JSONObject daily = root.getJSONObject("daily");
        JSONArray times = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray highs = daily.getJSONArray("temperature_2m_max");
        JSONArray lows = daily.getJSONArray("temperature_2m_min");

        ArrayList<ForecastDay> forecast = new ArrayList<ForecastDay>();
        for (int i = 0; i < Math.min(3, times.length()); i++) {
            int weatherCode = codes.optInt(i);
            forecast.add(new ForecastDay(
                    i == 0 ? "今天" : (i == 1 ? "明天" : "后天"),
                    times.optString(i),
                    condition(weatherCode),
                    (int) Math.round(lows.optDouble(i)),
                    (int) Math.round(highs.optDouble(i)),
                    weatherCode));
        }

        int weatherCode = current.optInt("weather_code");
        double windSpeed = current.optDouble("wind_speed_10m", 0d);
        double windDirection = current.optDouble("wind_direction_10m", 0d);
        Integer apparentTemperature = current.has("apparent_temperature") && !current.isNull("apparent_temperature")
                ? Integer.valueOf((int) Math.round(current.optDouble("apparent_temperature")))
                : null;
        return new WeatherSnapshot(
                settings.displayCity(),
                condition(weatherCode),
                (int) Math.round(current.optDouble("temperature_2m")),
                apparentTemperature,
                current.optInt("relative_humidity_2m"),
                windDirection(windDirection) + " " + windLevel(windSpeed) + "级",
                System.currentTimeMillis(),
                false,
                forecast);
    }

    private WeatherSnapshot parseQweatherForecast(String nowRaw, String dailyRaw, AppSettings settings) throws Exception {
        JSONObject nowRoot = new JSONObject(nowRaw);
        String resultCode = nowRoot.optString("code");
        if (!"200".equals(resultCode)) {
            throw new IOException("和风天气实时接口失败，code " + resultCode);
        }
        JSONObject dailyRoot = new JSONObject(dailyRaw);
        resultCode = dailyRoot.optString("code");
        if (!"200".equals(resultCode)) {
            throw new IOException("和风天气预报接口失败，code " + resultCode);
        }
        JSONObject now = nowRoot.getJSONObject("now");
        JSONArray daily = dailyRoot.getJSONArray("daily");
        if (daily.length() == 0) {
            throw new IOException("和风天气接口未返回逐日预报");
        }
        ArrayList<ForecastDay> forecast = new ArrayList<ForecastDay>();
        for (int i = 0; i < Math.min(3, daily.length()); i++) {
            JSONObject day = daily.getJSONObject(i);
            String condition = day.optString("textDay", day.optString("textNight", "阴"));
            int low = parseInt(day.optString("tempMin"), 0);
            int high = parseInt(day.optString("tempMax"), low);
            forecast.add(new ForecastDay(
                    i == 0 ? "今天" : (i == 1 ? "明天" : "后天"),
                    day.optString("fxDate"),
                    condition,
                    low,
                    high,
                    parseInt(day.optString("iconDay"), -1)));
        }

        return new WeatherSnapshot(
                settings.displayCity(),
                now.optString("text", "阴"),
                parseInt(now.optString("temp"), 0),
                parseOptionalInt(now.optString("feelsLike")),
                parseInt(now.optString("humidity"), 0),
                qweatherWind(now.optString("windDir"), now.optString("windScale")),
                System.currentTimeMillis(),
                false,
                forecast);
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getInputStream();
        String encoding = connection.getHeaderField("Content-Encoding");
        if (encoding != null && encoding.toLowerCase(Locale.US).contains("gzip")) {
            stream = new GZIPInputStream(stream);
        }
        return readAll(stream);
    }

    private static String readAll(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private static String qweatherWind(String direction, String scale) {
        if (direction.length() == 0) {
            direction = "风";
        }
        if (scale.length() == 0) {
            return direction;
        }
        return direction + " " + scale + "级";
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Integer parseOptionalInt(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String condition(int code) {
        if (code == 0) return "晴";
        if (code == 1 || code == 2) return "少云";
        if (code == 3) return "多云";
        if (code == 45 || code == 48) return "雾";
        if (code >= 51 && code <= 57) return "毛毛雨";
        if (code >= 61 && code <= 67) return "小雨";
        if (code >= 71 && code <= 77) return "雪";
        if (code >= 80 && code <= 82) return "阵雨";
        if (code >= 95) return "雷雨";
        return "阴";
    }

    private static String windDirection(double degrees) {
        String[] names = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        int index = (int) Math.round(((degrees % 360d) / 45d)) % 8;
        return names[index];
    }

    private static int windLevel(double kmh) {
        if (kmh < 6) return 1;
        if (kmh < 12) return 2;
        if (kmh < 20) return 3;
        if (kmh < 29) return 4;
        if (kmh < 39) return 5;
        return 6;
    }

    public static final class CityResult {
        public final String name;
        public final String admin;
        public final String country;
        public final double latitude;
        public final double longitude;

        public CityResult(String name, String admin, String country, double latitude, double longitude) {
            this.name = name;
            this.admin = admin;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String title() {
            if (admin == null || admin.length() == 0 || admin.equals(name)) {
                return name;
            }
            return name + " · " + admin;
        }

        public String subtitle() {
            return country + "  " + latitude + ", " + longitude;
        }
    }
}
