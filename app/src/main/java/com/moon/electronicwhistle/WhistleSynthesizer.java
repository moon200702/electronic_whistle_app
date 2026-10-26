package com.moon.electronicwhistle;

/** Offline PCM synthesis. No recordings, network, microphone, or audio permissions. */
public final class WhistleSynthesizer {
    public static final int SAMPLE_RATE = 44100;
    public static final int DURATION_MS = 1200;

    private WhistleSynthesizer() { }

    public static short[] render(SoundProfile profile) {
        int count = SAMPLE_RATE * DURATION_MS / 1000;
        short[] samples = new short[count];
        double phase = 0;
        for (int i = 0; i < count; i++) {
            double time = i / (double) SAMPLE_RATE;
            double envelope;
            if (profile == SoundProfile.DOUBLE) {
                double cycle = time % 0.6;
                envelope = pulse(cycle, 0, 0.15) + pulse(cycle, 0.23, 0.38);
            } else {
                envelope = pulse(time, 0, DURATION_MS / 1000.0);
            }
            double frequency = profile.frequency;
            if (profile == SoundProfile.TRILL) {
                frequency += 250 * Math.sin(2 * Math.PI * 32 * time);
                envelope *= 0.72 + 0.28 * Math.sin(2 * Math.PI * 32 * time);
            } else {
                frequency += 14 * Math.sin(2 * Math.PI * 5 * time);
            }
            double tone = Math.sin(phase) + 0.15 * Math.sin(phase * 2);
            samples[i] = (short) Math.round(Short.MAX_VALUE * 0.65 * envelope * tone / 1.15);
            phase = (phase + 2 * Math.PI * frequency / SAMPLE_RATE) % (2 * Math.PI);
        }
        return samples;
    }

    private static double pulse(double time, double start, double end) {
        if (time < start || time >= end) return 0;
        // Short fades prevent clicks at pulse boundaries and when the buffer loops.
        return Math.min(1, Math.min((time - start) / 0.008, (end - time) / 0.012));
    }
}
