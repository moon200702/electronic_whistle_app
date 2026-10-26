package com.moon.electronicwhistle;

/** Stable IDs are persisted in preferences, so enum order is not used as identity. */
public enum SoundProfile {
    CLASSIC("classic", 3000),
    DOUBLE("double", 2800),
    TRILL("trill", 3200),
    LOW("low", 1900);

    public final String id;
    public final double frequency;

    SoundProfile(String id, double frequency) {
        this.id = id;
        this.frequency = frequency;
    }

    public static SoundProfile fromId(String id) {
        for (SoundProfile profile : values()) {
            if (profile.id.equals(id)) return profile;
        }
        return CLASSIC;
    }
}
