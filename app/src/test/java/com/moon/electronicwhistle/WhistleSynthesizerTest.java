package com.moon.electronicwhistle;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public class WhistleSynthesizerTest {
    @Test public void allProfilesProduceAudibleUnclippedPcm() {
        for (SoundProfile profile : SoundProfile.values()) {
            short[] pcm = WhistleSynthesizer.render(profile);
            assertEquals(52920, pcm.length);
            long energy = 0;
            for (short value : pcm) {
                assertTrue("Must leave headroom: " + profile, Math.abs((int) value) < 24000);
                energy += (long) value * value;
            }
            assertTrue("Must contain audible signal: " + profile, energy / pcm.length > 10000000);
        }
    }

    @Test public void tonesFadeAtEdgesToAvoidLoopClicks() {
        for (SoundProfile profile : SoundProfile.values()) {
            short[] pcm = WhistleSynthesizer.render(profile);
            assertEquals(0, pcm[0]);
            assertTrue(Math.abs((int) pcm[pcm.length - 1]) < 100);
        }
    }

    @Test public void fourProfilesAreDistinct() {
        SoundProfile[] profiles = SoundProfile.values();
        assertEquals(4, profiles.length);
        for (int i = 0; i < profiles.length; i++) {
            for (int j = i + 1; j < profiles.length; j++) {
                assertFalse(Arrays.equals(WhistleSynthesizer.render(profiles[i]),
                        WhistleSynthesizer.render(profiles[j])));
            }
        }
    }

    @Test public void doubleWhistleContainsTwoBlastsSeparatedBySilence() {
        short[] pcm = WhistleSynthesizer.render(SoundProfile.DOUBLE);
        assertTrue(rms(pcm, 0.03, 0.12) > 5000);
        assertEquals(0, rms(pcm, 0.16, 0.22), 0);
        assertTrue(rms(pcm, 0.26, 0.35) > 5000);
        assertEquals(0, rms(pcm, 0.40, 0.59), 0);
    }

    @Test public void lowAndClassicHaveExpectedFundamentalPitches() {
        assertEquals(1900, positiveCrossingsPerSecond(WhistleSynthesizer.render(SoundProfile.LOW)), 45);
        assertEquals(3000, positiveCrossingsPerSecond(WhistleSynthesizer.render(SoundProfile.CLASSIC)), 45);
    }

    @Test public void trillHasMoreModulationThanClassic() {
        short[] trill = WhistleSynthesizer.render(SoundProfile.TRILL);
        short[] classic = WhistleSynthesizer.render(SoundProfile.CLASSIC);
        double trillRatio = rms(trill, 0.035, 0.042) / rms(trill, 0.020, 0.027);
        double classicRatio = rms(classic, 0.035, 0.042) / rms(classic, 0.020, 0.027);
        assertTrue(trillRatio > classicRatio * 1.3);
    }

    @Test public void unknownSavedSoundFallsBackToClassic() {
        assertEquals(SoundProfile.CLASSIC, SoundProfile.fromId("unknown"));
        assertEquals(SoundProfile.CLASSIC, SoundProfile.fromId(null));
        assertEquals(SoundProfile.LOW, SoundProfile.fromId("low"));
    }

    private double rms(short[] pcm, double start, double end) {
        int from = (int) (start * 44100);
        int to = (int) (end * 44100);
        double energy = 0;
        for (int i = from; i < to; i++) energy += (double) pcm[i] * pcm[i];
        return Math.sqrt(energy / (to - from));
    }

    private double positiveCrossingsPerSecond(short[] pcm) {
        int from = 4410;
        int to = 17640;
        int crossings = 0;
        for (int i = from + 1; i < to; i++) {
            if (pcm[i - 1] <= 0 && pcm[i] > 0) crossings++;
        }
        return crossings / ((to - from) / 44100.0);
    }
}
