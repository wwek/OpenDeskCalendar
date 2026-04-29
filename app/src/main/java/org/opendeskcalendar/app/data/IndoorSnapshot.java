package org.opendeskcalendar.app.data;

public final class IndoorSnapshot {
    public final double temperatureCelsius;
    public final double humidityPercent;
    public final long updatedAtMillis;
    public final boolean fromCache;

    public IndoorSnapshot(double temperatureCelsius, double humidityPercent, long updatedAtMillis, boolean fromCache) {
        this.temperatureCelsius = temperatureCelsius;
        this.humidityPercent = humidityPercent;
        this.updatedAtMillis = updatedAtMillis;
        this.fromCache = fromCache;
    }

    public boolean hasData() {
        return !Double.isNaN(temperatureCelsius) || !Double.isNaN(humidityPercent);
    }

    public static IndoorSnapshot empty() {
        return new IndoorSnapshot(Double.NaN, Double.NaN, 0L, true);
    }
}
