package org.opendeskcalendar.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MonthGrid {
    public final int year;
    public final int month;
    public final List<CalendarDay> days;

    public MonthGrid(int year, int month, List<CalendarDay> days) {
        this.year = year;
        this.month = month;
        this.days = Collections.unmodifiableList(new ArrayList<CalendarDay>(days));
    }
}
