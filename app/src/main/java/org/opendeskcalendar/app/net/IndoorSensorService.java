package org.opendeskcalendar.app.net;

import org.json.JSONObject;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.IndoorSnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class IndoorSensorService {
    public IndoorSnapshot fetch(AppSettings settings) throws Exception {
        if (!settings.indoorEnabled || settings.indoorEndpoint == null || settings.indoorEndpoint.length() == 0) {
            return IndoorSnapshot.empty();
        }
        HttpURLConnection connection = open(settings);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("室内传感器接口失败，HTTP " + code);
            }
            JSONObject root = new JSONObject(readAll(connection.getInputStream()));
            double temperature = firstNumber(root, "temperature", "temperatureCelsius", "temp", "state");
            double humidity = firstNumber(root, "humidity", "humidityPercent", "relativeHumidity");
            JSONObject attributes = root.optJSONObject("attributes");
            if (attributes != null) {
                if (Double.isNaN(temperature)) {
                    temperature = firstNumber(attributes, "temperature", "temperatureCelsius", "temp");
                }
                if (Double.isNaN(humidity)) {
                    humidity = firstNumber(attributes, "humidity", "humidityPercent", "relativeHumidity");
                }
            }
            IndoorSnapshot snapshot = new IndoorSnapshot(temperature, humidity, System.currentTimeMillis(), false);
            if (!snapshot.hasData()) {
                throw new IOException("室内传感器响应缺少 temperature/humidity 字段");
            }
            return snapshot;
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(AppSettings settings) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(settings.indoorEndpoint).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "OpenDeskCalendar/0.1");
        if (settings.indoorToken != null && settings.indoorToken.length() > 0) {
            connection.setRequestProperty("Authorization", "Bearer " + settings.indoorToken);
        }
        return connection;
    }

    private static double firstNumber(JSONObject object, String... names) {
        for (String name : names) {
            if (!object.has(name)) {
                continue;
            }
            String raw = object.optString(name, "");
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
            }
            double value = object.optDouble(name, Double.NaN);
            if (!Double.isNaN(value)) {
                return value;
            }
        }
        return Double.NaN;
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
}
