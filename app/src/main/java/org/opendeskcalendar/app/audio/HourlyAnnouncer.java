package org.opendeskcalendar.app.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.PreferencesStore;
import org.opendeskcalendar.app.ui.ChineseText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public final class HourlyAnnouncer implements TextToSpeech.OnInitListener {
    public interface Listener {
        void onAnnouncementStatus(String message);
    }

    private static final String UTTERANCE_ID = "hourly-announcement";
    private static final String PROMPT_GAP_ID = "hourly-announcement-prompt-gap";
    private static final int CHIME_SAMPLE_RATE = 22050;
    private static final int DING_MILLIS = 260;
    private static final int CHIME_GAP_MILLIS = 90;
    private static final int DONG_MILLIS = 320;
    private static final double DING_HZ = 1046.5d;
    private static final double DONG_HZ = 784.0d;
    private static final double CHIME_GAIN = 0.72d;
    private static final long SPEECH_DELAY_MILLIS = 820L;
    private static final long CHIME_PLAYBACK_WAIT_MILLIS = DING_MILLIS + CHIME_GAP_MILLIS + DONG_MILLIS + 80L;

    private final Context context;
    private final PreferencesStore store;
    private final AudioManager audioManager;
    private final short[] promptSamples;
    private TextToSpeech textToSpeech;
    private Listener listener;
    private boolean ready;
    private boolean unavailableLogged;
    private boolean promptToneUnavailableLogged;
    private long lastAnnouncedKey = -1L;
    private String pendingText = "";

    public HourlyAnnouncer(Context context, PreferencesStore store) {
        this.context = context.getApplicationContext();
        this.store = store;
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        promptSamples = buildChimeSamples();
        textToSpeech = new TextToSpeech(this.context, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
            logUnavailable(context.getString(R.string.hourly_announcement_tts_unavailable));
            notifyStatus(R.string.settings_hourly_test_failed);
            return;
        }
        configureSpeechEngine();
        Locale locale = ChineseText.isTraditional(context) ? Locale.TAIWAN : Locale.CHINA;
        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            logUnavailable(context.getString(R.string.hourly_announcement_language_unavailable));
            notifyStatus(R.string.settings_hourly_test_failed);
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
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (settings.hourlyAnnouncementQuietNight && (hour >= 22 || hour < 6)) {
            return;
        }
        int minute = now.get(Calendar.MINUTE);
        if (minute == 0 && !settings.hourlyAnnouncementEnabled) {
            return;
        }
        if (minute == 30 && !settings.halfHourlyAnnouncementEnabled) {
            return;
        }
        if (minute != 0 && minute != 30) {
            return;
        }
        long key = now.get(Calendar.YEAR) * 100000000L
                + (now.get(Calendar.MONTH) + 1) * 1000000L
                + now.get(Calendar.DAY_OF_MONTH) * 10000L
                + hour * 100L
                + minute;
        if (key == lastAnnouncedKey) {
            return;
        }
        String text = minute == 30
                ? context.getString(R.string.half_hourly_announcement_text, hour)
                : context.getString(R.string.hourly_announcement_text, hour);
        if (speakWhenReady(text) && ready) {
            lastAnnouncedKey = key;
        }
    }

    public void announceNow(int hour) {
        speakWhenReady(context.getString(R.string.hourly_announcement_text, hour));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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

    private boolean speakWhenReady(String text) {
        warnIfVolumeZero();
        if (textToSpeech == null) {
            logUnavailable(context.getString(R.string.hourly_announcement_tts_unavailable));
            notifyStatus(R.string.settings_hourly_test_failed);
            return false;
        }
        if (!ready) {
            pendingText = text;
            notifyStatus(R.string.settings_hourly_test_queued);
            return true;
        }
        return speak(text);
    }

    private boolean speak(String text) {
        playPromptTone();
        int queueMode = queueSpeechDelay() ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH;
        int result;
        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            result = textToSpeech.speak(text, queueMode, params, UTTERANCE_ID);
        } else {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
            result = textToSpeech.speak(text, queueMode, params);
        }
        if (result == TextToSpeech.ERROR) {
            store.recordError("HourlyAnnouncement", context.getString(R.string.hourly_announcement_tts_unavailable));
            notifyStatus(R.string.settings_hourly_test_failed);
            return false;
        }
        return true;
    }

    private void configureSpeechEngine() {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            textToSpeech.setAudioAttributes(attributes);
        }
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (!UTTERANCE_ID.equals(utteranceId)) {
                    return;
                }
                notifyStatus(R.string.settings_hourly_test_started);
            }

            @Override
            public void onDone(String utteranceId) {
                if (!UTTERANCE_ID.equals(utteranceId)) {
                    return;
                }
                notifyStatus(R.string.settings_hourly_test_done);
            }

            @Override
            public void onError(String utteranceId) {
                if (!UTTERANCE_ID.equals(utteranceId)) {
                    return;
                }
                store.recordError("HourlyAnnouncement", context.getString(R.string.hourly_announcement_tts_unavailable));
                notifyStatus(R.string.settings_hourly_test_failed);
            }
        });
    }

    private AudioTrack createStreamingAudioTrack(int bufferSizeBytes) {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(CHIME_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();
            return new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        }
        return new AudioTrack(
                AudioManager.STREAM_MUSIC,
                CHIME_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeBytes,
                AudioTrack.MODE_STREAM);
    }

    private void playPromptTone() {
        Thread thread = new Thread(this::playPromptSamples, "hourly-announcement-chime");
        thread.start();
    }

    private static short[] buildChimeSamples() {
        int dingSamples = CHIME_SAMPLE_RATE * DING_MILLIS / 1000;
        int gapSamples = CHIME_SAMPLE_RATE * CHIME_GAP_MILLIS / 1000;
        int dongSamples = CHIME_SAMPLE_RATE * DONG_MILLIS / 1000;
        short[] samples = new short[dingSamples + gapSamples + dongSamples];
        writeTone(samples, 0, dingSamples, DING_HZ);
        writeTone(samples, dingSamples + gapSamples, dongSamples, DONG_HZ);
        return samples;
    }

    private void playPromptSamples() {
        AudioTrack track = null;
        try {
            int minBuffer = AudioTrack.getMinBufferSize(
                    CHIME_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(promptSamples.length * 2, minBuffer);
            track = createStreamingAudioTrack(bufferSize);
            if (track.getState() != AudioTrack.STATE_INITIALIZED) {
                logPromptToneUnavailable(null);
                return;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                track.setVolume(1.0f);
            } else {
                track.setStereoVolume(1.0f, 1.0f);
            }
            track.play();
            int written = track.write(promptSamples, 0, promptSamples.length);
            if (written != promptSamples.length) {
                logPromptToneUnavailable(null);
                return;
            }
            Thread.sleep(CHIME_PLAYBACK_WAIT_MILLIS);
            track.stop();
        } catch (RuntimeException exception) {
            logPromptToneUnavailable(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            if (track != null) {
                track.release();
            }
        }
    }

    private static void writeTone(short[] samples, int offset, int length, double frequencyHz) {
        for (int i = 0; i < length; i++) {
            double progress = (double) i / Math.max(1, length - 1);
            double attack = Math.min(1.0d, progress / 0.08d);
            double decay = Math.pow(1.0d - progress, 1.15d);
            double envelope = attack * decay;
            double angle = 2.0d * Math.PI * frequencyHz * i / CHIME_SAMPLE_RATE;
            double harmonic = Math.sin(angle) + Math.sin(angle * 2.0d) * 0.18d;
            samples[offset + i] = (short) (harmonic * envelope * CHIME_GAIN * Short.MAX_VALUE);
        }
    }

    private boolean queueSpeechDelay() {
        if (Build.VERSION.SDK_INT >= 21) {
            return textToSpeech.playSilentUtterance(
                    SPEECH_DELAY_MILLIS,
                    TextToSpeech.QUEUE_FLUSH,
                    PROMPT_GAP_ID) != TextToSpeech.ERROR;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PROMPT_GAP_ID);
        return textToSpeech.playSilence(
                SPEECH_DELAY_MILLIS,
                TextToSpeech.QUEUE_FLUSH,
                params) != TextToSpeech.ERROR;
    }

    private void warnIfVolumeZero() {
        if (audioManager == null) {
            return;
        }
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (max > 0 && current == 0) {
            notifyStatus(R.string.settings_hourly_test_volume_zero);
        }
    }

    private void notifyStatus(int messageResId) {
        Listener current = listener;
        if (current != null) {
            current.onAnnouncementStatus(context.getString(messageResId));
        }
    }

    private void logUnavailable(String message) {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        store.recordError("HourlyAnnouncement", message);
    }

    private void logPromptToneUnavailable(RuntimeException exception) {
        if (promptToneUnavailableLogged) {
            return;
        }
        promptToneUnavailableLogged = true;
        String message = context.getString(R.string.hourly_announcement_prompt_tone_unavailable);
        if (exception != null && exception.getMessage() != null) {
            message += ": " + exception.getMessage();
        }
        store.recordError("HourlyAnnouncement", message);
    }
}
