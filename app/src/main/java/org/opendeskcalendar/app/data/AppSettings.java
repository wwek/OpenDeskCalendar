package org.opendeskcalendar.app.data;

public final class AppSettings {
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_MONO = "mono";
    public static final String THEME_EINK = "eink";

    public final String theme;
    public final boolean showSeconds;
    public final boolean use24Hour;
    public final boolean weekStartsMonday;
    public final boolean keepScreenOn;
    public final boolean bootAutostart;
    public final boolean showLunar;
    public final boolean showAlmanac;
    public final boolean showWifi;
    public final int fontScale;
    public final int weatherRefreshMinutes;
    public final String cityName;
    public final String districtName;
    public final double latitude;
    public final double longitude;
    public final String weatherHost;
    public final String weatherKey;
    public final String backupLauncherPackage;

    public AppSettings(
            String theme,
            boolean showSeconds,
            boolean use24Hour,
            boolean weekStartsMonday,
            boolean keepScreenOn,
            boolean bootAutostart,
            boolean showLunar,
            boolean showAlmanac,
            boolean showWifi,
            int fontScale,
            int weatherRefreshMinutes,
            String cityName,
            String districtName,
            double latitude,
            double longitude,
            String weatherHost,
            String weatherKey,
            String backupLauncherPackage) {
        this.theme = theme;
        this.showSeconds = showSeconds;
        this.use24Hour = use24Hour;
        this.weekStartsMonday = weekStartsMonday;
        this.keepScreenOn = keepScreenOn;
        this.bootAutostart = bootAutostart;
        this.showLunar = showLunar;
        this.showAlmanac = showAlmanac;
        this.showWifi = showWifi;
        this.fontScale = fontScale;
        this.weatherRefreshMinutes = weatherRefreshMinutes;
        this.cityName = cityName;
        this.districtName = districtName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.weatherHost = weatherHost;
        this.weatherKey = weatherKey;
        this.backupLauncherPackage = backupLauncherPackage;
    }

    public boolean isEink() {
        return THEME_EINK.equals(theme);
    }

    public boolean isMonochrome() {
        return THEME_MONO.equals(theme) || THEME_EINK.equals(theme);
    }

    public String displayCity() {
        if (districtName == null || districtName.length() == 0 || cityName.equals(districtName)) {
            return cityName;
        }
        return cityName + "·" + districtName;
    }
}
