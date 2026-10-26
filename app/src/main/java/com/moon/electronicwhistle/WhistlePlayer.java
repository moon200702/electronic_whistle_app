package com.moon.electronicwhistle;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import java.util.EnumMap;

/** Owns audio focus and releases every track on stop, backgrounding, and focus loss. */
final class WhistlePlayer {
    private final AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final EnumMap<SoundProfile, short[]> sounds = new EnumMap<>(SoundProfile.class);
    private final Runnable onStopped;
    private final Runnable finish = this::stop;
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    private final AudioFocusRequest focusRequest;
    private AudioTrack track;
    private boolean hasFocus;

    WhistlePlayer(Context context, Runnable onStopped) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.onStopped = onStopped;
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(change -> {
                    if (change != AudioManager.AUDIOFOCUS_GAIN) stop();
                }, handler).build();
        for (SoundProfile profile : SoundProfile.values()) {
            sounds.put(profile, WhistleSynthesizer.render(profile));
        }
    }

    boolean play(SoundProfile profile, boolean loop, float volume) {
        stop();
        hasFocus = audioManager.requestAudioFocus(focusRequest)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (!hasFocus) return false;
        short[] pcm = sounds.get(profile);
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(WhistleSynthesizer.SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2).build();
            // Static tracks start in STATE_NO_STATIC_DATA and become initialized after write().
            if (track.getState() == AudioTrack.STATE_UNINITIALIZED
                    || track.write(pcm, 0, pcm.length) != pcm.length
                    || track.getState() != AudioTrack.STATE_INITIALIZED) {
                stop();
                return false;
            }
            if (loop && track.setLoopPoints(0, pcm.length, -1) != AudioTrack.SUCCESS) {
                stop();
                return false;
            }
            track.setVolume(Math.max(0, Math.min(1, volume)));
            track.play();
            if (!loop) handler.postDelayed(finish, WhistleSynthesizer.DURATION_MS + 50);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            stop();
            return false;
        }
    }

    boolean isPlaying() {
        return track != null && track.getState() == AudioTrack.STATE_INITIALIZED
                && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    void setVolume(float value) {
        if (track != null) track.setVolume(Math.max(0, Math.min(1, value)));
    }

    void stop() {
        handler.removeCallbacks(finish);
        AudioTrack current = track;
        track = null;
        if (current != null) {
            try {
                if (current.getState() == AudioTrack.STATE_INITIALIZED) current.pause();
            } catch (IllegalStateException ignored) {
                // An audio-server reset must not prevent resource release or activity pausing.
            } finally {
                current.release();
            }
        }
        if (hasFocus) {
            hasFocus = false;
            audioManager.abandonAudioFocusRequest(focusRequest);
        }
        onStopped.run();
    }
}
