package org.opendeskcalendar.app.calendar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public final class AlmanacProvider {
    private static final String[] GOOD = {
            "出行", "打扫", "会友", "签约", "纳财", "修整", "学习", "安床"
    };
    private static final String[] AVOID = {
            "动土", "远行", "争执", "破土", "搬迁", "熬夜", "冒进", "诉讼"
    };

    private AlmanacProvider() {
    }

    public static String good(Context context, Calendar calendar) {
        AlmanacDay day = findDay(context, calendar);
        if (day != null) {
            return join(day.good, 3);
        }
        return pick(calendar, GOOD, 3);
    }

    public static String avoid(Context context, Calendar calendar) {
        AlmanacDay day = findDay(context, calendar);
        if (day != null) {
            return join(day.avoid, 2);
        }
        return pick(calendar, AVOID, 2);
    }

    public static String fullGood(Context context, Calendar calendar) {
        AlmanacDay day = findDay(context, calendar);
        if (day != null) {
            return join(day.good, Integer.MAX_VALUE);
        }
        return pick(calendar, GOOD, GOOD.length);
    }

    public static String fullAvoid(Context context, Calendar calendar) {
        AlmanacDay day = findDay(context, calendar);
        if (day != null) {
            return join(day.avoid, Integer.MAX_VALUE);
        }
        return pick(calendar, AVOID, AVOID.length);
    }

    private static String pick(Calendar calendar, String[] source, int count) {
        int seed = calendar.get(Calendar.YEAR) * 31
                + (calendar.get(Calendar.MONTH) + 1) * 17
                + calendar.get(Calendar.DAY_OF_MONTH);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(source[Math.abs(seed + i * 5) % source.length]);
        }
        return builder.toString();
    }

    private static String join(List<String> source, int count) {
        StringBuilder builder = new StringBuilder();
        int added = 0;
        for (int i = 0; i < source.size() && added < count; i++) {
            String value = source.get(i);
            if (value == null || value.length() == 0) {
                continue;
            }
            if (added > 0) {
                builder.append("、");
            }
            builder.append(value);
            added++;
        }
        return builder.toString();
    }

    private static AlmanacDay findDay(Context context, Calendar calendar) {
        Map<String, AlmanacDay> days = AlmanacCache.get(context);
        if (days == null) {
            return null;
        }
        String key = calendar.get(Calendar.YEAR)
                + "-" + two(calendar.get(Calendar.MONTH) + 1)
                + "-" + two(calendar.get(Calendar.DAY_OF_MONTH));
        return days.get(key);
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static final class AlmanacCache {
        private static Map<String, AlmanacDay> days;
        private static boolean missing;

        private AlmanacCache() {
        }

        static synchronized Map<String, AlmanacDay> get(Context context) {
            if (days != null) {
                return days;
            }
            if (missing) {
                return null;
            }
            try {
                days = load(context);
                return days;
            } catch (Exception e) {
                missing = true;
                return null;
            }
        }

        private static Map<String, AlmanacDay> load(Context context) throws Exception {
            InputStream stream = new GZIPInputStream(context.getAssets().open("almanac/cn-2026-2099.dat"));
            String raw = readAll(stream);
            JSONObject root = new JSONObject(raw);
            JSONArray terms = root.getJSONArray("terms");
            JSONObject items = root.getJSONObject("items");
            HashMap<String, AlmanacDay> data = new HashMap<String, AlmanacDay>();
            JSONArray names = items.names();
            if (names == null) {
                return data;
            }
            for (int i = 0; i < names.length(); i++) {
                String date = names.getString(i);
                JSONArray item = items.getJSONArray(date);
                data.put(date, new AlmanacDay(readIndexedArray(item.getJSONArray(0), terms), readIndexedArray(item.getJSONArray(1), terms)));
            }
            return data;
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

        private static List<String> readIndexedArray(JSONArray array, JSONArray terms) throws Exception {
            ArrayList<String> values = new ArrayList<String>();
            for (int i = 0; i < array.length(); i++) {
                values.add(terms.getString(array.getInt(i)));
            }
            return values;
        }
    }

    private static final class AlmanacDay {
        final List<String> good;
        final List<String> avoid;

        AlmanacDay(List<String> good, List<String> avoid) {
            this.good = good;
            this.avoid = avoid;
        }
    }
}
