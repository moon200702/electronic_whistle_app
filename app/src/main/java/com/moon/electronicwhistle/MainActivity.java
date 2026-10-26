package com.moon.electronicwhistle;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.EnumMap;

public final class MainActivity extends Activity {
    static final int LIME = 0xFFD3F76B;
    private static final int BACKGROUND = 0xFF101715;
    private static final int SURFACE = 0xFF1B2420;
    private static final int MUTED = 0xFF9CAEA4;
    private static final int WHITE = 0xFFF3F6EF;
    private static final int[] TITLES = {R.string.classic, R.string.double_whistle,
            R.string.trill, R.string.low};
    private static final int[] DESCRIPTIONS = {R.string.classic_description,
            R.string.double_description, R.string.trill_description, R.string.low_description};
    private static final int[] SOUND_IDS = {R.id.sound_classic, R.id.sound_double,
            R.id.sound_trill, R.id.sound_low};

    private final EnumMap<SoundProfile, LinearLayout> soundCards = new EnumMap<>(SoundProfile.class);
    private SharedPreferences preferences;
    private WhistlePlayer player;
    private SoundProfile selected;
    private boolean holdMode;
    private boolean volumeKeysEnabled;
    private boolean resumed;
    private boolean volumeUpHeld;
    private boolean volumeDownHeld;
    private boolean touchHeld;
    private int volume;
    private int activePointer = -1;
    private boolean touchRelease;
    private WhistleButton whistle;
    private TextView status;
    private TextView hint;
    private Button hold;
    private Button tap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        preferences = getSharedPreferences("whistle", MODE_PRIVATE);
        selected = SoundProfile.fromId(preferences.getString("sound", "classic"));
        holdMode = preferences.getBoolean("hold", true);
        volumeKeysEnabled = preferences.getBoolean("volume_keys", true);
        volume = Math.max(0, Math.min(100, preferences.getInt("volume", 45)));
        createScreen();
        player = new WhistlePlayer(this, this::refreshPlayback);
        refreshSelection();
        refreshMode();
    }

    private void createScreen() {
        boolean compact = getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density < 850;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout outer = column();
        outer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout content = column();
        content.setPadding(dp(24), dp(compact ? 20 : 26), dp(24), dp(28));
        int availableWidth = getResources().getDisplayMetrics().widthPixels;
        outer.addView(content, new LinearLayout.LayoutParams(Math.min(availableWidth, dp(540)), -2));
        scroll.addView(outer);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("WHISTLE / 電子哨", 13, LIME, true);
        brand.setLetterSpacing(0.1f);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text(getString(R.string.offline), 11, MUTED, false);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7));
        badge.setBackground(shape(SURFACE, 12, 0));
        header.addView(badge);
        content.addView(header);
        if (!compact) {
            TextView title = text(getString(R.string.app_name), 36, WHITE, true);
            add(content, title, 22, 0);
        }
        add(content, text(getString(R.string.tagline), 15, MUTED, false), compact ? 16 : 5, 0);

        status = text("", 13, LIME, false);
        status.setId(R.id.status);
        status.setGravity(Gravity.CENTER);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(content, status, compact ? 20 : 26, 0);

        whistle = new WhistleButton(this);
        int buttonSize = Math.min(dp(compact ? 200 : 242), availableWidth - dp(64));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(buttonSize, buttonSize);
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.topMargin = dp(compact ? 12 : 20);
        buttonParams.bottomMargin = dp(16);
        content.addView(whistle, buttonParams);
        whistle.setOnClickListener(view -> {
            if (touchRelease) return;
            if (isSounding()) stopSound(); else play(false);
        });
        whistle.setOnTouchListener(this::handleWhistleTouch);

        hint = text("", 13, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        content.addView(hint);

        add(content, text(getString(R.string.sound_heading), 16, WHITE, true), compact ? 20 : 29, 12);
        SoundProfile[] profiles = SoundProfile.values();
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            for (int col = 0; col < 2; col++) {
                SoundProfile profile = profiles[row * 2 + col];
                LinearLayout card = soundCard(profile);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
                params.setMargins(col == 0 ? 0 : dp(6), 0, col == 0 ? dp(6) : 0, 0);
                line.addView(card, params);
            }
            add(content, line, row == 0 ? 0 : 12, 0);
        }

        add(content, text(getString(R.string.mode_heading), 16, WHITE, true), 25, 12);
        LinearLayout modes = new LinearLayout(this);
        modes.setPadding(dp(4), dp(4), dp(4), dp(4));
        modes.setBackground(shape(SURFACE, 16, 0));
        hold = modeButton(R.id.hold_mode, R.string.hold_mode, true);
        tap = modeButton(R.id.tap_mode, R.string.tap_mode, false);
        modes.addView(hold, new LinearLayout.LayoutParams(0, dp(48), 1));
        modes.addView(tap, new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(modes);

        Switch volumeKeys = new Switch(this);
        volumeKeys.setId(R.id.volume_keys);
        volumeKeys.setText(R.string.volume_keys_title);
        volumeKeys.setTextColor(WHITE);
        volumeKeys.setTextSize(16);
        volumeKeys.setMinimumHeight(dp(56));
        volumeKeys.setPadding(dp(16), dp(12), dp(16), dp(12));
        volumeKeys.setBackground(shape(SURFACE, 16, 0));
        volumeKeys.setChecked(volumeKeysEnabled);
        add(content, volumeKeys, 20, 8);
        add(content, text(getString(R.string.volume_keys_description), 12, MUTED, false), 0, 0);
        volumeKeys.setOnCheckedChangeListener((button, checked) -> {
            volumeKeysEnabled = checked;
            boolean wasHolding = volumeUpHeld || volumeDownHeld;
            volumeUpHeld = false;
            volumeDownHeld = false;
            if (wasHolding) stopIfReleased();
            preferences.edit().putBoolean("volume_keys", checked).apply();
            refreshMode();
        });

        LinearLayout volumeLabel = new LinearLayout(this);
        volumeLabel.setGravity(Gravity.CENTER_VERTICAL);
        volumeLabel.addView(text(getString(R.string.volume_heading), 16, WHITE, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView volumeValue = text(getString(R.string.volume_value, volume), 14, LIME, true);
        volumeLabel.addView(volumeValue);
        add(content, volumeLabel, 25, 5);
        SeekBar slider = new SeekBar(this);
        slider.setId(R.id.volume);
        slider.setMax(100);
        slider.setProgress(volume);
        slider.setContentDescription(getString(R.string.volume_heading));
        slider.setProgressTintList(ColorStateList.valueOf(LIME));
        slider.setThumbTintList(ColorStateList.valueOf(LIME));
        content.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                volume = value;
                volumeValue.setText(getString(R.string.volume_value, value));
                if (player != null) player.setVolume(gain());
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) {
                preferences.edit().putInt("volume", volume).apply();
            }
        });
        add(content, text(getString(R.string.volume_hint), 11, MUTED, false), 0, 0);
        TextView footer = text(getString(R.string.footer), 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        add(content, footer, 28, 0);
    }

    private LinearLayout soundCard(SoundProfile profile) {
        int index = profile.ordinal();
        LinearLayout card = column();
        card.setId(SOUND_IDS[index]);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setMinimumHeight(dp(100));
        card.setPadding(dp(16), dp(14), dp(12), dp(14));
        card.setClickable(true);
        card.setFocusable(true);
        // Group the title and description into a single accessible selection target.
        card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        card.setContentDescription(getString(TITLES[index]) + "，" + getString(DESCRIPTIONS[index]));
        TextView number = text("0" + (index + 1) + "  /  " + profile.id.toUpperCase(java.util.Locale.ROOT),
                10, MUTED, true);
        TextView title = text(getString(TITLES[index]), 18, WHITE, true);
        TextView description = text(getString(DESCRIPTIONS[index]), 11, MUTED, false);
        for (TextView child : new TextView[]{number, title, description}) {
            child.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        card.addView(number);
        add(card, title, 7, 4);
        card.addView(description);
        card.setOnClickListener(view -> {
            stopSound();
            selected = profile;
            preferences.edit().putString("sound", profile.id).apply();
            refreshSelection();
        });
        soundCards.put(profile, card);
        return card;
    }

    private Button modeButton(int id, int title, boolean isHold) {
        Button button = new Button(this);
        button.setId(id);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setStateListAnimator(null);
        button.setOnClickListener(view -> {
            stopSound();
            holdMode = isHold;
            preferences.edit().putBoolean("hold", holdMode).apply();
            refreshMode();
        });
        return button;
    }

    private boolean handleWhistleTouch(View view, MotionEvent event) {
        if (!holdMode) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointer = event.getPointerId(0);
                view.getParent().requestDisallowInterceptTouchEvent(true);
                view.setPressed(true);
                boolean alreadyHolding = anyHoldActive();
                touchHeld = true;
                if (!alreadyHolding) play(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(activePointer);
                if (pointerIndex >= 0 && (event.getX(pointerIndex) < 0
                        || event.getX(pointerIndex) >= view.getWidth()
                        || event.getY(pointerIndex) < 0 || event.getY(pointerIndex) >= view.getHeight())) {
                    touchHeld = false;
                    stopIfReleased();
                    view.setPressed(false);
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == activePointer) finishTouch(view, false);
                return true;
            case MotionEvent.ACTION_UP:
                finishTouch(view, true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishTouch(view, false);
                return true;
            default:
                return true;
        }
    }

    private void finishTouch(View view, boolean click) {
        touchHeld = false;
        stopIfReleased();
        activePointer = -1;
        view.setPressed(false);
        view.getParent().requestDisallowInterceptTouchEvent(false);
        if (click) {
            touchRelease = true;
            view.performClick();
            touchRelease = false;
        }
    }

    private void play(boolean loop) {
        if (!player.play(selected, loop, gain())) {
            Toast.makeText(this, R.string.audio_unavailable, Toast.LENGTH_SHORT).show();
        }
        refreshPlayback();
    }

    private float gain() {
        // Squared taper gives useful control over quieter listening levels.
        return (volume / 100f) * (volume / 100f);
    }

    private void stopSound() {
        touchHeld = false;
        volumeUpHeld = false;
        volumeDownHeld = false;
        if (player != null) player.stop();
    }

    private boolean anyHoldActive() {
        return touchHeld || volumeUpHeld || volumeDownHeld;
    }

    private void stopIfReleased() {
        if (!anyHoldActive() && player != null) player.stop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (!resumed || player == null
                || (key != KeyEvent.KEYCODE_VOLUME_UP && key != KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return super.dispatchKeyEvent(event);
        }
        if (!volumeKeysEnabled) {
            // Route explicitly to media volume, including when no whistle currently owns focus.
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        key == KeyEvent.KEYCODE_VOLUME_UP ? AudioManager.ADJUST_RAISE
                                : AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI);
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // Android repeats held keys. Consume repeats without restarting the whistle.
            if (event.getRepeatCount() == 0) {
                boolean alreadyHolding = anyHoldActive();
                if (key == KeyEvent.KEYCODE_VOLUME_UP) volumeUpHeld = true;
                else volumeDownHeld = true;
                if (!alreadyHolding) play(true);
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            // Ignore stale releases after changing sound/mode, backgrounding, or disabling keys.
            boolean wasHeld = key == KeyEvent.KEYCODE_VOLUME_UP ? volumeUpHeld : volumeDownHeld;
            if (key == KeyEvent.KEYCODE_VOLUME_UP) volumeUpHeld = false;
            else volumeDownHeld = false;
            if (wasHeld) stopIfReleased();
        }
        return true;
    }

    boolean isSounding() {
        return player != null && player.isPlaying();
    }

    private void refreshSelection() {
        for (SoundProfile profile : SoundProfile.values()) {
            LinearLayout card = soundCards.get(profile);
            boolean chosen = profile == selected;
            card.setSelected(chosen);
            card.setBackground(ripple(shape(chosen ? 0xFF263523 : SURFACE, 18,
                    chosen ? LIME : 0xFF2C3831)));
            ((TextView) card.getChildAt(1)).setTextColor(chosen ? LIME : WHITE);
        }
        refreshPlayback();
    }

    private void refreshMode() {
        hold.setSelected(holdMode);
        tap.setSelected(!holdMode);
        hold.setBackground(ripple(shape(holdMode ? LIME : Color.TRANSPARENT, 12, 0)));
        tap.setBackground(ripple(shape(!holdMode ? LIME : Color.TRANSPARENT, 12, 0)));
        hold.setTextColor(holdMode ? BACKGROUND : MUTED);
        tap.setTextColor(!holdMode ? BACKGROUND : MUTED);
        hint.setText(volumeKeysEnabled
                ? (holdMode ? R.string.hold_with_keys_hint : R.string.tap_with_keys_hint)
                : (holdMode ? R.string.hold_hint : R.string.tap_hint));
        refreshPlayback();
    }

    private void refreshPlayback() {
        if (whistle == null) return;
        boolean playing = isSounding();
        status.setText(getString(R.string.status_format,
                getString(playing ? R.string.playing : R.string.ready),
                getString(TITLES[selected.ordinal()])));
        String label = getString(playing ? R.string.stop_action
                : holdMode ? R.string.hold_mode : R.string.tap_action);
        whistle.update(label, playing, getString(R.string.whistle_accessibility,
                getString(TITLES[selected.ordinal()]), label));
        if (playing) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) stopSound();
    }

    @Override
    protected void onPause() {
        resumed = false;
        stopSound();
        activePointer = -1;
        whistle.setPressed(false);
        preferences.edit().putInt("volume", volume).apply();
        super.onPause();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String label, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private RippleDrawable ripple(GradientDrawable drawable) {
        return new RippleDrawable(ColorStateList.valueOf(0x28D3F76B), drawable, null);
    }

    private void add(LinearLayout parent, View child, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
