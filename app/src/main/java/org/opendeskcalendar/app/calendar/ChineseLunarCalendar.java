package org.opendeskcalendar.app.calendar;

import java.util.Calendar;
import java.util.Date;

public final class ChineseLunarCalendar {
    private static final int[] LUNAR_INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
    };
    private static final int[] SOLAR_TERM_INFO = {
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921, 173149, 195551, 218072, 240693,
            263343, 285989, 308563, 331033, 353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    };
    private static final String[] SOLAR_TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    private static final String[] MONTH_NAMES = {
            "", "正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"
    };
    private static final String[] DAY_PREFIX = {"初", "十", "廿", "三"};
    private static final String[] DAY_NAMES = {"十", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] ANIMALS = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};

    private ChineseLunarCalendar() {
    }

    public static LunarDate fromSolar(int year, int month, int day) {
        Calendar base = Calendar.getInstance();
        base.set(1900, Calendar.JANUARY, 31, 0, 0, 0);
        base.set(Calendar.MILLISECOND, 0);
        Calendar target = Calendar.getInstance();
        target.set(year, month - 1, day, 0, 0, 0);
        target.set(Calendar.MILLISECOND, 0);
        int offset = (int) ((target.getTimeInMillis() - base.getTimeInMillis()) / 86400000L);

        int lunarYear;
        int daysOfYear = 0;
        for (lunarYear = 1900; lunarYear < 2050 && offset > 0; lunarYear++) {
            daysOfYear = yearDays(lunarYear);
            offset -= daysOfYear;
        }
        if (offset < 0) {
            offset += daysOfYear;
            lunarYear--;
        }

        int leapMonth = leapMonth(lunarYear);
        boolean leap = false;
        int lunarMonth;
        int daysOfMonth = 0;
        for (lunarMonth = 1; lunarMonth <= 12 && offset > 0; lunarMonth++) {
            if (leapMonth > 0 && lunarMonth == leapMonth + 1 && !leap) {
                --lunarMonth;
                leap = true;
                daysOfMonth = leapDays(lunarYear);
            } else {
                daysOfMonth = monthDays(lunarYear, lunarMonth);
            }
            offset -= daysOfMonth;
            if (leap && lunarMonth == leapMonth + 1) {
                leap = false;
            }
        }
        if (offset == 0 && leapMonth > 0 && lunarMonth == leapMonth + 1) {
            if (leap) {
                leap = false;
            } else {
                leap = true;
                --lunarMonth;
            }
        }
        if (offset < 0) {
            offset += daysOfMonth;
            --lunarMonth;
        }
        int lunarDay = offset + 1;
        return new LunarDate(
                lunarYear,
                lunarMonth,
                lunarDay,
                leap,
                (leap ? "闰" : "") + MONTH_NAMES[lunarMonth],
                dayName(lunarDay),
                ganzhiYear(lunarYear),
                ANIMALS[(lunarYear - 4) % 12]);
    }

    public static String solarTerm(int year, int month, int day) {
        int index = (month - 1) * 2;
        int first = termDay(year, index);
        if (day == first) {
            return SOLAR_TERMS[index];
        }
        int second = termDay(year, index + 1);
        if (day == second) {
            return SOLAR_TERMS[index + 1];
        }
        return "";
    }

    public static String festival(LunarDate lunar, int solarMonth, int solarDay) {
        if (solarMonth == 1 && solarDay == 1) return "元旦";
        if (solarMonth == 4 && solarDay == 5) return "清明";
        if (solarMonth == 5 && solarDay == 1) return "劳动";
        if (solarMonth == 10 && solarDay == 1) return "国庆";
        if (lunar.month == 1 && lunar.day == 1) return "春节";
        if (lunar.month == 1 && lunar.day == 15) return "元宵";
        if (lunar.month == 5 && lunar.day == 5) return "端午";
        if (lunar.month == 7 && lunar.day == 7) return "七夕";
        if (lunar.month == 8 && lunar.day == 15) return "中秋";
        if (lunar.month == 9 && lunar.day == 9) return "重阳";
        if (lunar.month == 12 && lunar.day == 8) return "腊八";
        return "";
    }

    public static String ganzhiAnimal(LunarDate lunar) {
        return lunar.ganzhiYear + "年【" + lunar.animal + "】";
    }

    private static int termDay(int year, int index) {
        long base = Date.UTC(0, 0, 6, 2, 5, 0);
        long offset = (long) (31556925974.7d * (year - 1900)) + SOLAR_TERM_INFO[index] * 60000L;
        Date date = new Date(base + offset);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private static int yearDays(int year) {
        int sum = 348;
        for (int mask = 0x8000; mask > 0x8; mask >>= 1) {
            if ((LUNAR_INFO[year - 1900] & mask) != 0) {
                sum++;
            }
        }
        return sum + leapDays(year);
    }

    private static int leapDays(int year) {
        if (leapMonth(year) != 0) {
            return (LUNAR_INFO[year - 1900] & 0x10000) != 0 ? 30 : 29;
        }
        return 0;
    }

    private static int leapMonth(int year) {
        return LUNAR_INFO[year - 1900] & 0xf;
    }

    private static int monthDays(int year, int month) {
        return (LUNAR_INFO[year - 1900] & (0x10000 >> month)) == 0 ? 29 : 30;
    }

    private static String dayName(int day) {
        if (day == 10) return "初十";
        if (day == 20) return "二十";
        if (day == 30) return "三十";
        int prefix = day / 10;
        int value = day % 10;
        return DAY_PREFIX[prefix] + DAY_NAMES[value];
    }

    private static String ganzhiYear(int year) {
        return STEMS[(year - 4) % 10] + BRANCHES[(year - 4) % 12];
    }

    public static final class LunarDate {
        public final int year;
        public final int month;
        public final int day;
        public final boolean leap;
        public final String monthName;
        public final String dayName;
        public final String ganzhiYear;
        public final String animal;

        private LunarDate(
                int year,
                int month,
                int day,
                boolean leap,
                String monthName,
                String dayName,
                String ganzhiYear,
                String animal) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.leap = leap;
            this.monthName = monthName;
            this.dayName = dayName;
            this.ganzhiYear = ganzhiYear;
            this.animal = animal;
        }
    }
}
