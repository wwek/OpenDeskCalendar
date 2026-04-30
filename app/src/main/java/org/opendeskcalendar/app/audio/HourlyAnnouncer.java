package org.opendeskcalendar.app.audio;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.PreferencesStore;
import org.opendeskcalendar.app.ui.ChineseText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public final class HourlyAnnouncer implements TextToSpeech.OnInitListener {
    private final Context context;
    private final PreferencesStore store;
    private TextToSpeech textToSpeech;
    private boolean ready;
    private boolean unavailableLogged;
    private int lastAnnouncedKey = -1;
    private String pendingText = "";

    public HourlyAnnouncer(Context context, PreferencesStore store) {
        this.context = context.getApplicationContext();
        this.store = store;
        textToSpeech = new TextToSpeech(this.context, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
            logUnavailable(context.getString(R.string.hourly_announcement_tts_unavailable));
            return;
        }
        Locale locale = ChineseText.isTraditional(context) ? Locale.TAIWAN : Locale.CHINA;
        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            logUnavailable(context.getString(R.string.hourly_announcement_language_unavailable));
            return;
        }
        ready = true;
        if (pendingText.length() > 0) {
            String text = pendingText;
            pendingText = "";
            speak(text);
        }
    }

    public void announceIfNeeded(AppSettings settings, Calendar now) {
        if (!settings.hourlyAnnouncementEnabled) {
            return;
        }
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (settings.hourlyAnnouncementQuietNight && (hour >= 22 || hour < 6)) {
            return;
        }
        int key = now.get(Calendar.YEAR) * 1000000
                + (now.get(Calendar.MONTH) + 1) * 10000
                + now.get(Calendar.DAY_OF_MONTH) * 100
                + hour;
        if (key == lastAnnouncedKey) {
            return;
        }
        lastAnnouncedKey = key;
        String text = context.getString(R.string.hourly_announcement_text, hour);
        speakWhenReady(text);
    }

    public void announceNow(int hour) {
        speakWhenReady(context.getString(R.string.hourly_announcement_text, hour));
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        pendingText = "";
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        ready = false;
        pendingText = "";
    }

    private void speakWhenReady(String text) {
        if (!ready || textToSpeech == null) {
            pendingText = text;
            return;
        }
        speak(text);
    }

    private void speak(String text) {
        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "hourly-announcement");
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "hourly-announcement");
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void logUnavailable(String message) {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        store.recordError("HourlyAnnouncement", message);
    }
}
