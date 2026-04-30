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
        Map<String, AlmanacDay> year = YearCache.get(context, calendar.get(Calendar.YEAR));
        if (year == null) {
            return null;
        }
        String key = calendar.get(Calendar.YEAR)
                + "-" + two(calendar.get(Calendar.MONTH) + 1)
                + "-" + two(calendar.get(Calendar.DAY_OF_MONTH));
        return year.get(key);
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static final class YearCache {
        private static final Map<Integer, Map<String, AlmanacDay>> YEARS = new HashMap<Integer, Map<String, AlmanacDay>>();
        private static final Map<Integer, Boolean> MISSING = new HashMap<Integer, Boolean>();

        private YearCache() {
        }

        static synchronized Map<String, AlmanacDay> get(Context context, int year) {
            if (YEARS.containsKey(year)) {
                return YEARS.get(year);
            }
            if (MISSING.containsKey(year)) {
                return null;
            }
            try {
                Map<String, AlmanacDay> data = load(context, year);
                YEARS.put(year, data);
                return data;
            } catch (Exception e) {
                MISSING.put(year, true);
                return null;
            }
        }

        private static Map<String, AlmanacDay> load(Context context, int year) throws Exception {
            String raw = readAll(context.getAssets().open("almanac/cn-" + year + ".json"));
            JSONObject root = new JSONObject(raw);
            JSONObject items = root.getJSONObject("items");
            HashMap<String, AlmanacDay> data = new HashMap<String, AlmanacDay>();
            JSONArray names = items.names();
            if (names == null) {
                return data;
            }
            for (int i = 0; i < names.length(); i++) {
                String date = names.getString(i);
                JSONObject item = items.getJSONObject(date);
                data.put(date, new AlmanacDay(readArray(item.getJSONArray("good")), readArray(item.getJSONArray("avoid"))));
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

        private static List<String> readArray(JSONArray array) throws Exception {
            ArrayList<String> values = new ArrayList<String>();
            for (int i = 0; i < array.length(); i++) {
                values.add(array.getString(i));
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
