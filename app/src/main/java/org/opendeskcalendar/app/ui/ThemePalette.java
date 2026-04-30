package org.opendeskcalendar.app.ui;

import android.graphics.Color;

import org.opendeskcalendar.app.data.AppSettings;

public final class ThemePalette {
    public final int background;
    public final int panel;
    public final int divider;
    public final int primary;
    public final int secondary;
    public final int muted;
    public final int accent;
    public final int accentSecondary;
    public final int todayFill;
    public final int holiday;
    public final int workday;
    public final int warning;
    public final boolean dark;
    public final boolean monochrome;

    private ThemePalette(
            int background,
            int panel,
            int divider,
            int primary,
            int secondary,
            int muted,
            int accent,
            int accentSecondary,
            int todayFill,
            int holiday,
            int workday,
            int warning,
            boolean dark,
            boolean monochrome) {
        this.background = background;
        this.panel = panel;
        this.divider = divider;
        this.primary = primary;
        this.secondary = secondary;
        this.muted = muted;
        this.accent = accent;
        this.accentSecondary = accentSecondary;
        this.todayFill = todayFill;
        this.holiday = holiday;
        this.workday = workday;
        this.warning = warning;
        this.dark = dark;
        this.monochrome = monochrome;
    }

    public static ThemePalette from(AppSettings settings) {
        if (AppSettings.THEME_DARK.equals(settings.theme)) {
            return new ThemePalette(
                    Color.rgb(15, 17, 20),
                    Color.rgb(22, 25, 30),
                    Color.rgb(48, 52, 58),
                    Color.rgb(245, 247, 250),
                    Color.rgb(201, 209, 219),
                    Color.rgb(138, 148, 160),
                    Color.rgb(96, 165, 250),
                    Color.rgb(147, 197, 253),
                    Color.rgb(22, 75, 128),
                    Color.rgb(74, 222, 128),
                    Color.rgb(251, 146, 60),
                    Color.rgb(248, 113, 113),
                    true,
                    false);
        }
        if (AppSettings.THEME_MONO.equals(settings.theme) || AppSettings.THEME_EINK.equals(settings.theme)) {
            return new ThemePalette(
                    Color.WHITE,
                    Color.WHITE,
                    Color.rgb(46, 46, 46),
                    Color.BLACK,
                    Color.rgb(32, 32, 32),
                    Color.rgb(92, 92, 92),
                    Color.BLACK,
                    Color.BLACK,
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    Color.BLACK,
                    false,
                    true);
        }
        return new ThemePalette(
                Color.rgb(250, 251, 253),
                Color.rgb(255, 255, 255),
                Color.rgb(226, 232, 240),
                Color.rgb(15, 23, 42),
                Color.rgb(51, 65, 85),
                Color.rgb(100, 116, 139),
                Color.rgb(37, 99, 235),
                Color.rgb(30, 64, 175),
                Color.rgb(219, 234, 254),
                Color.rgb(22, 163, 74),
                Color.rgb(234, 88, 12),
                Color.rgb(220, 38, 38),
                false,
                false);
    }
}
