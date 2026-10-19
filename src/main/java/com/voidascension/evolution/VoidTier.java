package com.voidascension.evolution;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Identity of the evolution tiers (VoidEvolutionTier). Balance numbers live in {@link EvolutionTable}
 * (configurable); this enum only defines the stable id, numeric level, ordering and colour.
 * <p>
 * Saved data stores {@link #level()}. Unknown levels resolve to {@code null} via {@link #byLevel(int)} so callers
 * must decide explicitly - an invalid value can never silently grant a high tier.
 */
public enum VoidTier {
    HUMAN(0, ChatFormatting.GRAY),
    ENHANCED(1, ChatFormatting.LIGHT_PURPLE),
    MUTANT(2, ChatFormatting.DARK_PURPLE),
    VOIDBORN(3, ChatFormatting.BLUE),
    ASCENDED(4, ChatFormatting.GOLD);

    private static final VoidTier[] BY_LEVEL = values();

    private final int level;
    private final ChatFormatting color;

    VoidTier(int level, ChatFormatting color) {
        this.level = level;
        this.color = color;
    }

    public int level() { return level; }

    public ChatFormatting color() { return color; }

    /** Lower-case stable identifier used in commands and config ("human", "enhanced", ...). */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String translationKey() { return "tier.void_ascension." + id(); }

    public MutableComponent displayName() { return Component.translatable(translationKey()).withStyle(color); }

    public boolean isMax() { return level == BY_LEVEL.length - 1; }

    @Nullable
    public VoidTier next() { return isMax() ? null : BY_LEVEL[level + 1]; }

    @Nullable
    public VoidTier previous() { return level == 0 ? null : BY_LEVEL[level - 1]; }

    public boolean isAtLeast(VoidTier other) { return level >= other.level; }

    /** @return the tier with this level, or {@code null} if the level is invalid. */
    @Nullable
    public static VoidTier byLevel(int level) {
        return level >= 0 && level < BY_LEVEL.length ? BY_LEVEL[level] : null;
    }

    /** Accepts an id ("mutant") or a numeric level ("2"); returns {@code null} if unknown. */
    @Nullable
    public static VoidTier parse(String value) {
        for (VoidTier t : BY_LEVEL) {
            if (t.id().equalsIgnoreCase(value)) return t;
        }
        try {
            return byLevel(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
