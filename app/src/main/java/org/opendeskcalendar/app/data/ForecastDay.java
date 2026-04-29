package org.opendeskcalendar.app.data;

public final class ForecastDay {
    public final String label;
    public final String date;
    public final String condition;
    public final int lowCelsius;
    public final int highCelsius;
    public final int weatherCode;

    public ForecastDay(String label, String date, String condition, int lowCelsius, int highCelsius, int weatherCode) {
        this.label = label;
        this.date = date;
        this.condition = condition;
        this.lowCelsius = lowCelsius;
        this.highCelsius = highCelsius;
        this.weatherCode = weatherCode;
    }

    public String tempRange() {
        return lowCelsius + "~" + highCelsius + "°C";
    }
}
