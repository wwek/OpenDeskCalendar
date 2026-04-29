package org.opendeskcalendar.app.ui;

import android.content.Context;
import android.content.res.Configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ChineseText {
    private static final Map<Character, Character> TRADITIONAL = new HashMap<Character, Character>();

    static {
        String simplified = "云东北风级湿阵雾缓存于未来天气宜忌阴少毛预报开关设置默认选择电纸书显示农历节气传统假日数据启动备用启器设备屏状态诊断错误暂无写经纬输县搜寻纬经区国庆春节劳动重阳腊惊蛰满种闰龙马鸡猪签纳财学习动争执迁诉讼远扫修整闭线离";
        String traditional = "雲東北風級濕陣霧快取於未來天氣宜忌陰少毛預報開關設定預設選擇電子紙顯示農曆節氣傳統假日資料啟動備用啟器裝置屏狀態診斷錯誤暫無寫經緯輸縣搜尋緯經區國慶春節勞動重陽臘驚蟄滿種閏龍馬雞豬簽納財學習動爭執遷訴訟遠掃修整閉線離";
        int count = Math.min(simplified.length(), traditional.length());
        for (int i = 0; i < count; i++) {
            TRADITIONAL.put(simplified.charAt(i), traditional.charAt(i));
        }
    }

    private ChineseText() {
    }

    public static String display(Context context, String text) {
        if (text == null || text.length() == 0 || !isTraditional(context)) {
            return text == null ? "" : text;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            Character mapped = TRADITIONAL.get(value);
            builder.append(mapped == null ? value : mapped.charValue());
        }
        return builder.toString();
    }

    public static Locale locale(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            return configuration.getLocales().get(0);
        }
        return configuration.locale;
    }

    public static boolean isTraditional(Context context) {
        Locale locale = locale(context);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        return "zh".equals(language)
                && ("TW".equalsIgnoreCase(country)
                || "HK".equalsIgnoreCase(country)
                || "MO".equalsIgnoreCase(country));
    }
}
