package com.moon.electronicwhistle;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WhistleInteractionTest {
    @Before public void resetPreferences() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue(context.getSharedPreferences("whistle", Context.MODE_PRIVATE).edit().clear().commit());
    }

    @Test public void launchHasFourSoundsAndSafeDefaults() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertFalse(activity.isSounding());
                assertTrue(activity.findViewById(R.id.sound_classic).isSelected());
                assertNotNull(activity.findViewById(R.id.sound_double));
                assertNotNull(activity.findViewById(R.id.sound_trill));
                assertNotNull(activity.findViewById(R.id.sound_low));
                assertTrue(activity.findViewById(R.id.hold_mode).isSelected());
                assertEquals(45, ((SeekBar) activity.findViewById(R.id.volume)).getProgress());
            });
        }
    }

    @Test public void holdStartsAndReleaseStopsAudio() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View button = activity.findViewById(R.id.whistle_button);
                touch(button, MotionEvent.ACTION_DOWN, 100, 100);
                assertTrue(activity.isSounding());
                touch(button, MotionEvent.ACTION_UP, 100, 100);
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void cancelAndDraggingOutsideStopAudio() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View button = activity.findViewById(R.id.whistle_button);
                touch(button, MotionEvent.ACTION_DOWN, 100, 100);
                assertTrue(activity.isSounding());
                touch(button, MotionEvent.ACTION_CANCEL, 100, 100);
                assertFalse(activity.isSounding());
                touch(button, MotionEvent.ACTION_DOWN, 100, 100);
                assertTrue(activity.isSounding());
                touch(button, MotionEvent.ACTION_MOVE, -20, 100);
                assertFalse(activity.isSounding());
                touch(button, MotionEvent.ACTION_UP, -20, 100);
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void tapStartsAndSecondTapStops() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.tap_mode).performClick();
                View button = activity.findViewById(R.id.whistle_button);
                button.performClick();
                assertTrue(activity.isSounding());
                button.performClick();
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void switchingSoundStopsPlaybackAndPersistsAcrossRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.tap_mode).performClick();
                activity.findViewById(R.id.whistle_button).performClick();
                assertTrue(activity.isSounding());
                activity.findViewById(R.id.sound_trill).performClick();
                assertFalse(activity.isSounding());
                assertTrue(activity.findViewById(R.id.sound_trill).isSelected());
                ((SeekBar) activity.findViewById(R.id.volume)).setProgress(27);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.sound_trill).isSelected());
                assertTrue(activity.findViewById(R.id.tap_mode).isSelected());
                assertEquals(27, ((SeekBar) activity.findViewById(R.id.volume)).getProgress());
                assertFalse(activity.isSounding());
            });
        }
    }

    @Test public void backgroundingStopsSound() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.whistle_button).performClick();
                assertTrue(activity.isSounding());
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertFalse(activity.isSounding()));
        }
    }

    private void touch(View view, int action, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }
}
