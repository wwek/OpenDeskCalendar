package org.opendeskcalendar.app.calendar;

import java.util.Calendar;

public final class AlmanacProvider {
    private static final String[] GOOD = {
            "出行", "打扫", "会友", "签约", "纳财", "修整", "学习", "安床"
    };
    private static final String[] AVOID = {
            "动土", "远行", "争执", "破土", "搬迁", "熬夜", "冒进", "诉讼"
    };

    private AlmanacProvider() {
    }

    public static String good(Calendar calendar) {
        return pick(calendar, GOOD, 3);
    }

    public static String avoid(Calendar calendar) {
        return pick(calendar, AVOID, 2);
    }

    private static String pick(Calendar calendar, String[] source, int count) {
        int seed = calendar.get(Calendar.YEAR) * 31
                + (calendar.get(Calendar.MONTH) + 1) * 17
                + calendar.get(Calendar.DAY_OF_MONTH);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(source[Math.abs(seed + i * 5) % source.length]);
        }
        return builder.toString();
    }
}
