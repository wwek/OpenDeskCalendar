package org.opendeskcalendar.app.data;

public final class CalendarDay {
    public final int year;
    public final int month;
    public final int day;
    public final boolean inMonth;
    public final boolean today;
    public final boolean weekend;
    public final String dateKey;
    public final String lunarLabel;
    public final String festivalLabel;
    public final String solarTerm;
    public final HolidayInfo holiday;

    public CalendarDay(
            int year,
            int month,
            int day,
            boolean inMonth,
            boolean today,
            boolean weekend,
            String dateKey,
            String lunarLabel,
            String festivalLabel,
            String solarTerm,
            HolidayInfo holiday) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.inMonth = inMonth;
        this.today = today;
        this.weekend = weekend;
        this.dateKey = dateKey;
        this.lunarLabel = lunarLabel;
        this.festivalLabel = festivalLabel;
        this.solarTerm = solarTerm;
        this.holiday = holiday;
    }

    public String lowerLabel() {
        if (holiday != null && holiday.label != null && holiday.label.length() > 0 && holiday.isHoliday()) {
            return holiday.label;
        }
        if (festivalLabel != null && festivalLabel.length() > 0) {
            return festivalLabel;
        }
        if (solarTerm != null && solarTerm.length() > 0) {
            return solarTerm;
        }
        return lunarLabel;
    }
}
