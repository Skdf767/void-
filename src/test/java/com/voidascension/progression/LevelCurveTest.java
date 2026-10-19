package com.voidascension.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelCurveTest {
    private static final double BASE = 30.0, EXP = 1.5;
    private static final int MAX = 100;

    @Test
    void levelZeroNeedsNoXp() {
        assertEquals(0L, LevelCurve.totalXpForLevel(0, BASE, EXP));
        assertEquals(0L, LevelCurve.totalXpForLevel(-5, BASE, EXP));
        assertEquals(0, LevelCurve.levelForXp(0, BASE, EXP, MAX));
        assertEquals(0, LevelCurve.levelForXp(-100, BASE, EXP, MAX));
    }

    @Test
    void documentedDefaultThresholds() {
        assertEquals(30L, LevelCurve.totalXpForLevel(1, BASE, EXP));
        assertEquals(155L, LevelCurve.totalXpForLevel(3, BASE, EXP));
        assertEquals(678L, LevelCurve.totalXpForLevel(8, BASE, EXP));
        assertEquals(1742L, LevelCurve.totalXpForLevel(15, BASE, EXP));
        assertEquals(3750L, LevelCurve.totalXpForLevel(25, BASE, EXP));
    }

    @Test
    void thresholdBoundaries() {
        for (int level = 1; level <= 50; level++) {
            long t = LevelCurve.totalXpForLevel(level, BASE, EXP);
            assertEquals(level, LevelCurve.levelForXp(t, BASE, EXP, MAX), "exact threshold of " + level);
            assertEquals(level - 1, LevelCurve.levelForXp(t - 1, BASE, EXP, MAX), "one below threshold of " + level);
        }
    }

    @Test
    void strictlyIncreasing() {
        long prev = -1;
        for (int level = 0; level <= 1000; level++) {
            long t = LevelCurve.totalXpForLevel(level, BASE, EXP);
            assertTrue(t > prev, "curve must increase at level " + level);
            prev = t;
        }
    }

    @Test
    void cappedAtMaxLevel() {
        assertEquals(MAX, LevelCurve.levelForXp(Long.MAX_VALUE, BASE, EXP, MAX));
        assertEquals(10, LevelCurve.levelForXp(Long.MAX_VALUE, BASE, EXP, 10));
        assertEquals(0, LevelCurve.levelForXp(1_000_000, BASE, EXP, 0));
    }
}
