package org.opendeskcalendar.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeatherSnapshot {
    public final String city;
    public final String condition;
    public final int temperatureCelsius;
    public final Integer apparentTemperatureCelsius;
    public final int humidityPercent;
    public final String wind;
    public final long updatedAtMillis;
    public final boolean fromCache;
    public final List<ForecastDay> forecast;

    public WeatherSnapshot(
            String city,
            String condition,
            int temperatureCelsius,
            Integer apparentTemperatureCelsius,
            int humidityPercent,
            String wind,
            long updatedAtMillis,
            boolean fromCache,
            List<ForecastDay> forecast) {
        this.city = city;
        this.condition = condition;
        this.temperatureCelsius = temperatureCelsius;
        this.apparentTemperatureCelsius = apparentTemperatureCelsius;
        this.humidityPercent = humidityPercent;
        this.wind = wind;
        this.updatedAtMillis = updatedAtMillis;
        this.fromCache = fromCache;
        this.forecast = Collections.unmodifiableList(new ArrayList<ForecastDay>(forecast));
    }

    public static WeatherSnapshot fallback(String city) {
        ArrayList<ForecastDay> days = new ArrayList<ForecastDay>();
        days.add(new ForecastDay("今天", "", "多云", 19, 23, 3));
        days.add(new ForecastDay("明天", "", "阴", 20, 26, 3));
        days.add(new ForecastDay("后天", "", "小雨", 19, 27, 61));
        return new WeatherSnapshot(city, "多云", 26, null, 64, "东北风 1级", 0L, true, days);
    }
}
