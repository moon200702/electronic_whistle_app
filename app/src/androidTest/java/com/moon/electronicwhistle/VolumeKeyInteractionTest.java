package com.moon.electronicwhistle;

import static org.junit.Assert.*;
import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Switch;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VolumeKeyInteractionTest {
    @Before public void resetPreferences() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue(context.getSharedPreferences("whistle", Context.MODE_PRIVATE).edit().clear().commit());
    }

    @Test public void eitherKeyHoldsBeyondShortWhistleEvenInTapMode() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(((Switch) activity.findViewById(R.id.volume_keys)).isChecked());
                activity.findViewById(R.id.tap_mode).performClick();
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertTrue(activity.isSounding());
            });
            // Hardware keys must keep playing beyond the 1.2-second touchscreen short whistle.
            SystemClock.sleep(1500);
            scenario.onActivity(activity -> {
                assertTrue(activity.isSounding());
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 3, 0);
                assertTrue(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertFalse(activity.isSounding());
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
                assertTrue(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void bothKeysMustBeReleasedAndCanceledReleaseStops() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertTrue(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 0, KeyEvent.FLAG_CANCELED);
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void releasingTouchDoesNotCutHeldKeyAndReleasingKeyDoesNotCutTouch() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View button = activity.findViewById(R.id.whistle_button);
                touch(button, MotionEvent.ACTION_DOWN);
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                touch(button, MotionEvent.ACTION_UP);
                assertTrue(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertFalse(activity.isSounding());
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
                touch(button, MotionEvent.ACTION_DOWN);
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
                assertTrue(activity.isSounding());
                touch(button, MotionEvent.ACTION_UP);
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void disablingStopsHeldKeyAndPersistsAcrossRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertTrue(activity.isSounding());
                activity.findViewById(R.id.volume_keys).performClick();
                assertFalse(activity.isSounding());
                assertFalse(((Switch) activity.findViewById(R.id.volume_keys)).isChecked());
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertFalse(((Switch) activity.findViewById(R.id.volume_keys)).isChecked());
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void backgroundAndFocusLossClearHeldKeysWithoutRepeatRestart() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertTrue(activity.isSounding());
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                assertFalse(activity.isSounding());
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 2, 0);
                assertFalse(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
                assertTrue(activity.isSounding());
                activity.onWindowFocusChanged(false);
                assertFalse(activity.isSounding());
                activity.onWindowFocusChanged(true);
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 2, 0);
                assertFalse(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 0, 0);
            });
        }
    }

    @Test public void changingSoundStopsAndStaleKeyReleaseDoesNotStopNewShortWhistle() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertTrue(activity.isSounding());
                activity.findViewById(R.id.sound_low).performClick();
                assertFalse(activity.isSounding());
                key(activity, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 1, 0);
                assertFalse(activity.isSounding());
                activity.findViewById(R.id.whistle_button).performClick();
                assertTrue(activity.isSounding());
                key(activity, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 0, 0);
                assertTrue(activity.isSounding());
                activity.findViewById(R.id.whistle_button).performClick();
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void androidKeyRoutingPreservesVolumeWhileEnabledAndRestoresVolumeWhenDisabled() {
        Context context = ApplicationProvider.getApplicationContext();
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int original = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int direction = original == audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                ? KeyEvent.KEYCODE_VOLUME_DOWN : KeyEvent.KEYCODE_VOLUME_UP;
        int reverse = direction == KeyEvent.KEYCODE_VOLUME_UP
                ? KeyEvent.KEYCODE_VOLUME_DOWN : KeyEvent.KEYCODE_VOLUME_UP;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitWindowFocus(scenario);
            try {
                inject(KeyEvent.ACTION_DOWN, direction);
                scenario.onActivity(activity -> assertTrue(activity.isSounding()));
                assertEquals(original, audio.getStreamVolume(AudioManager.STREAM_MUSIC));
                inject(KeyEvent.ACTION_UP, direction);
                scenario.onActivity(activity -> {
                    assertFalse(activity.isSounding());
                    activity.findViewById(R.id.volume_keys).performClick();
                    assertFalse(((Switch) activity.findViewById(R.id.volume_keys)).isChecked());
                });
                inject(KeyEvent.ACTION_DOWN, direction);
                inject(KeyEvent.ACTION_UP, direction);
                long deadline = SystemClock.uptimeMillis() + 5000;
                while (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == original
                        && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50);
                assertNotEquals(original, audio.getStreamVolume(AudioManager.STREAM_MUSIC));
                scenario.onActivity(activity -> assertFalse(activity.isSounding()));
            } finally {
                // Restore the emulator's media level through the same normal key route.
                scenario.onActivity(activity -> ((Switch) activity.findViewById(R.id.volume_keys)).setChecked(false));
                if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) != original) {
                    inject(KeyEvent.ACTION_DOWN, reverse);
                    inject(KeyEvent.ACTION_UP, reverse);
                }
            }
        }
    }

    private void awaitWindowFocus(ActivityScenario<MainActivity> scenario) {
        AtomicBoolean focused = new AtomicBoolean();
        long deadline = SystemClock.uptimeMillis() + 15000;
        do {
            scenario.onActivity(activity -> focused.set(activity.hasWindowFocus()));
            if (focused.get()) return;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Activity did not receive window focus for Android key injection");
    }

    private void inject(int action, int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeySync(new KeyEvent(action, keyCode));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void key(MainActivity activity, int action, int code, int repeat, int flags) {
        long now = SystemClock.uptimeMillis();
        assertTrue(activity.dispatchKeyEvent(new KeyEvent(now, now, action, code, repeat, 0,
                KeyEvent.KEYCODE_UNKNOWN, 0, flags)));
    }

    private void touch(View view, int action) {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, 100, 100, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }
}
