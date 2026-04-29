package org.opendeskcalendar.app.calendar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendeskcalendar.app.data.HolidayInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class HolidayRepository {
    private final Map<String, HolidayInfo> holidays = new HashMap<String, HolidayInfo>();

    public HolidayRepository(Context context) {
        load(context, "holidays/cn-2026.json");
    }

    public HolidayInfo get(String dateKey) {
        return holidays.get(dateKey);
    }

    private void load(Context context, String assetPath) {
        try {
            InputStream stream = context.getAssets().open(assetPath);
            String raw = readAll(stream);
            JSONObject root = new JSONObject(raw);
            JSONArray days = root.getJSONArray("days");
            for (int i = 0; i < days.length(); i++) {
                JSONObject item = days.getJSONObject(i);
                HolidayInfo info = new HolidayInfo(
                        item.optString("date"),
                        item.optString("type"),
                        item.optString("label"),
                        item.optString("badge"));
                holidays.put(info.date, info);
            }
        } catch (IOException ignored) {
        } catch (JSONException ignored) {
        }
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
