package org.opendeskcalendar.app.calendar;

import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.CalendarDay;
import org.opendeskcalendar.app.data.HolidayInfo;
import org.opendeskcalendar.app.data.MonthGrid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public final class CalendarRepository {
    private final HolidayRepository holidayRepository;
    private final SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public CalendarRepository(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public MonthGrid buildMonth(Calendar visibleMonth, AppSettings settings) {
        Calendar first = copyDate(visibleMonth);
        first.set(Calendar.DAY_OF_MONTH, 1);

        int firstDay = first.get(Calendar.DAY_OF_WEEK);
        int weekStart = settings.weekStartsMonday ? Calendar.MONDAY : Calendar.SUNDAY;
        int leading = (firstDay - weekStart + 7) % 7;

        Calendar cursor = copyDate(first);
        cursor.add(Calendar.DAY_OF_MONTH, -leading);

        Calendar today = Calendar.getInstance();
        ArrayList<CalendarDay> days = new ArrayList<CalendarDay>();
        for (int i = 0; i < 42; i++) {
            int year = cursor.get(Calendar.YEAR);
            int month = cursor.get(Calendar.MONTH) + 1;
            int day = cursor.get(Calendar.DAY_OF_MONTH);
            String key = keyFormat.format(cursor.getTime());
            ChineseLunarCalendar.LunarDate lunarDate = ChineseLunarCalendar.fromSolar(year, month, day);
            HolidayInfo holiday = holidayRepository.get(key);
            String solarTerm = ChineseLunarCalendar.solarTerm(year, month, day);
            String festival = ChineseLunarCalendar.festival(lunarDate, month, day);
            boolean weekend = cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    || cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            days.add(new CalendarDay(
                    year,
                    month,
                    day,
                    cursor.get(Calendar.MONTH) == first.get(Calendar.MONTH),
                    isSameDay(cursor, today),
                    weekend,
                    key,
                    lunarDate.dayName,
                    festival,
                    solarTerm,
                    holiday));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return new MonthGrid(first.get(Calendar.YEAR), first.get(Calendar.MONTH) + 1, days);
    }

    private static Calendar copyDate(Calendar source) {
        Calendar copy = Calendar.getInstance();
        copy.set(Calendar.YEAR, source.get(Calendar.YEAR));
        copy.set(Calendar.MONTH, source.get(Calendar.MONTH));
        copy.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy;
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }
}
