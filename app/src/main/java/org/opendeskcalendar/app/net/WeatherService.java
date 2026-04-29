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

public final class WeatherService {
    public WeatherSnapshot fetch(AppSettings settings) throws Exception {
        String endpoint = buildForecastUrl(settings);
        HttpURLConnection connection = open(endpoint);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("天气接口失败，HTTP " + code);
        }
        String raw = readAll(connection.getInputStream());
        return parseForecast(raw, settings);
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
        HttpURLConnection connection = open(endpoint);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("城市搜索失败，HTTP " + code);
        }
        JSONObject root = new JSONObject(readAll(connection.getInputStream()));
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
    }

    private String buildForecastUrl(AppSettings settings) {
        String host = settings.weatherHost == null || settings.weatherHost.length() == 0
                ? "https://api.open-meteo.com/v1/forecast"
                : settings.weatherHost;
        Uri.Builder builder = Uri.parse(host).buildUpon()
                .appendQueryParameter("latitude", String.format(Locale.US, "%.5f", settings.latitude))
                .appendQueryParameter("longitude", String.format(Locale.US, "%.5f", settings.longitude))
                .appendQueryParameter("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m")
                .appendQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .appendQueryParameter("forecast_days", "3")
                .appendQueryParameter("timezone", "auto");
        if (settings.weatherKey != null && settings.weatherKey.length() > 0) {
            builder.appendQueryParameter("key", settings.weatherKey);
        }
        return builder.build().toString();
    }

    private HttpURLConnection open(String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent", "OpenDeskCalendar/0.1");
        return connection;
    }

    private WeatherSnapshot parseForecast(String raw, AppSettings settings) throws Exception {
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
        return new WeatherSnapshot(
                settings.displayCity(),
                condition(weatherCode),
                (int) Math.round(current.optDouble("temperature_2m")),
                current.optInt("relative_humidity_2m"),
                windDirection(windDirection) + " " + windLevel(windSpeed) + "级",
                System.currentTimeMillis(),
                false,
                forecast);
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
