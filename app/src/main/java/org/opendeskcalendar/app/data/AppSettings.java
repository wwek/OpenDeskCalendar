package org.opendeskcalendar.app.data;

public final class AppSettings {
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_MONO = "mono";
    public static final String THEME_EINK = "eink";
    public static final String WEATHER_PROVIDER_OPEN_METEO = "open_meteo";
    public static final String WEATHER_PROVIDER_QWEATHER = "qweather";
    public static final String ORIENTATION_SYSTEM = "system";
    public static final String ORIENTATION_PORTRAIT = "portrait";
    public static final String ORIENTATION_LANDSCAPE = "landscape";

    public final String theme;
    public final boolean showSeconds;
    public final boolean use24Hour;
    public final boolean weekStartsMonday;
    public final boolean keepScreenOn;
    public final boolean bootAutostart;
    public final boolean showLunar;
    public final boolean showAlmanac;
    public final boolean showWifi;
    public final String orientationMode;
    public final int fontScale;
    public final int weatherRefreshMinutes;
    public final String cityName;
    public final String districtName;
    public final double latitude;
    public final double longitude;
    public final String weatherProvider;
    public final String weatherHost;
    public final String weatherKey;
    public final String backupLauncherPackage;
    public final boolean confirmExit;
    public final boolean hourlyAnnouncementEnabled;
    public final boolean halfHourlyAnnouncementEnabled;
    public final boolean hourlyAnnouncementQuietNight;
    public final boolean nightDimEnabled;
    public final boolean burnInProtectionEnabled;
    public final boolean indoorEnabled;
    public final String indoorEndpoint;
    public final String indoorToken;

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
            String orientationMode,
            int fontScale,
            int weatherRefreshMinutes,
            String cityName,
            String districtName,
            double latitude,
            double longitude,
            String weatherProvider,
            String weatherHost,
            String weatherKey,
            String backupLauncherPackage,
            boolean confirmExit,
            boolean hourlyAnnouncementEnabled,
            boolean halfHourlyAnnouncementEnabled,
            boolean hourlyAnnouncementQuietNight,
            boolean nightDimEnabled,
            boolean burnInProtectionEnabled,
            boolean indoorEnabled,
            String indoorEndpoint,
            String indoorToken) {
        this.theme = theme;
        this.showSeconds = showSeconds;
        this.use24Hour = use24Hour;
        this.weekStartsMonday = weekStartsMonday;
        this.keepScreenOn = keepScreenOn;
        this.bootAutostart = bootAutostart;
        this.showLunar = showLunar;
        this.showAlmanac = showAlmanac;
        this.showWifi = showWifi;
        this.orientationMode = orientationMode;
        this.fontScale = fontScale;
        this.weatherRefreshMinutes = weatherRefreshMinutes;
        this.cityName = cityName;
        this.districtName = districtName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.weatherProvider = weatherProvider;
        this.weatherHost = weatherHost;
        this.weatherKey = weatherKey;
        this.backupLauncherPackage = backupLauncherPackage;
        this.confirmExit = confirmExit;
        this.hourlyAnnouncementEnabled = hourlyAnnouncementEnabled;
        this.halfHourlyAnnouncementEnabled = halfHourlyAnnouncementEnabled;
        this.hourlyAnnouncementQuietNight = hourlyAnnouncementQuietNight;
        this.nightDimEnabled = nightDimEnabled;
        this.burnInProtectionEnabled = burnInProtectionEnabled;
        this.indoorEnabled = indoorEnabled;
        this.indoorEndpoint = indoorEndpoint;
        this.indoorToken = indoorToken;
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
