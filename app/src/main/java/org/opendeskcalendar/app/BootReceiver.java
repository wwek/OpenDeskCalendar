package org.opendeskcalendar.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.opendeskcalendar.app.data.PreferencesStore;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (!new PreferencesStore(context).getSettings().bootAutostart) {
            return;
        }
        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(launch);
    }
}
