package com.voidascension.progression;

/**
 * Pure Evolution Level curve. Total Void Experience needed to reach level L is
 * {@code floor(base * L^exponent)}; level 0 needs 0 XP. With the defaults (base 30, exponent 1.5):
 * L1 = 30, L3 = 155, L8 = 678, L15 = 1742, L25 = 3750.
 */
public final class LevelCurve {

    public static long totalXpForLevel(int level, double base, double exponent) {
        if (level <= 0) return 0L;
        double v = base * Math.pow(level, exponent);
        return v >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(v);
    }

    public static int levelForXp(long xp, double base, double exponent, int maxLevel) {
        if (xp <= 0 || maxLevel <= 0) return 0;
        int level = 0;
        while (level < maxLevel && xp >= totalXpForLevel(level + 1, base, exponent)) level++;
        return level;
    }

    private LevelCurve() {}
}
