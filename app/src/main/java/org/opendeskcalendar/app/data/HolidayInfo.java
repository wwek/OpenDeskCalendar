package org.opendeskcalendar.app.data;

public final class HolidayInfo {
    public final String date;
    public final String type;
    public final String label;
    public final String badge;

    public HolidayInfo(String date, String type, String label, String badge) {
        this.date = date;
        this.type = type;
        this.label = label;
        this.badge = badge;
    }

    public boolean isHoliday() {
        return "holiday".equals(type);
    }

    public boolean isWorkday() {
        return "workday".equals(type);
    }
}
