package org.opendeskcalendar.app.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.opendeskcalendar.app.R;

public final class NetworkState {
    public final boolean connected;
    public final boolean wifiConnected;
    public final int wifiLevel;
    public final String label;

    public NetworkState(boolean connected, boolean wifiConnected, int wifiLevel, String label) {
        this.connected = connected;
        this.wifiConnected = wifiConnected;
        this.wifiLevel = wifiLevel;
        this.label = label;
    }

    public static NetworkState from(Context context) {
        boolean connected = false;
        boolean wifi = false;
        int level = 0;
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo active = connectivityManager.getActiveNetworkInfo();
            connected = active != null && active.isConnected();
            wifi = connected && active.getType() == ConnectivityManager.TYPE_WIFI;
        }
        if (wifi) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    level = WifiManager.calculateSignalLevel(info.getRssi(), 4);
                }
            }
        }
        String label = wifi ? "Wi-Fi " + bars(level)
                : (connected ? context.getString(R.string.wifi_online) : context.getString(R.string.wifi_offline));
        return new NetworkState(connected, wifi, level, label);
    }

    private static String bars(int level) {
        if (level <= 0) return "▮";
        if (level == 1) return "▮▮";
        if (level == 2) return "▮▮▮";
        return "▮▮▮▮";
    }
}
