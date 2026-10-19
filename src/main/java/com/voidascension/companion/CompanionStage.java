package com.voidascension.companion;

import com.voidascension.config.VoidConfig;
import com.voidascension.evolution.VoidTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Evolution stages for Void Companions.
 * <ul>
 *     <li>Stage I: Dormant (Lv 1–9)</li>
 *     <li>Stage II: Awakened (Lv 10–19) - requires player ENHANCED tier</li>
 *     <li>Stage III: Resonant (Lv 20–29) - requires player MUTANT tier</li>
 *     <li>Stage IV: Ascendant (Lv 30) - requires player ASCENDED tier</li>
 * </ul>
 */
public enum CompanionStage {
    DORMANT(1, "dormant", ChatFormatting.GRAY, 1, 9, 0.85F, 0, VoidTier.HUMAN),
    AWAKENED(2, "awakened", ChatFormatting.LIGHT_PURPLE, 10, 19, 1.0F, 150, VoidTier.ENHANCED),
    RESONANT(3, "resonant", ChatFormatting.DARK_PURPLE, 20, 29, 1.15F, 400, VoidTier.MUTANT),
    ASCENDANT(4, "ascendant", ChatFormatting.GOLD, 30, 30, 1.30F, 900, VoidTier.ASCENDED);

    private final int index;
    private final String id;
    private final ChatFormatting color;
    private final int minLevel;
    private final int maxLevel;
    private final float modelScale;
    private final int evolutionEssenceCost;
    private final VoidTier requiredPlayerTier;

    CompanionStage(int index, String id, ChatFormatting color, int minLevel, int maxLevel,
                   float modelScale, int evolutionEssenceCost, VoidTier requiredPlayerTier) {
        this.index = index;
        this.id = id;
        this.color = color;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.modelScale = modelScale;
        this.evolutionEssenceCost = evolutionEssenceCost;
        this.requiredPlayerTier = requiredPlayerTier;
    }

    public int index() { return index; }
    public String id() { return id; }
    public ChatFormatting color() { return color; }
    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }
    public float modelScale() { return modelScale; }

    /** Server-configured essence cost to reach THIS stage from the previous one (0 for Dormant). */
    public int evolutionEssenceCost() {
        return switch (this) {
            case DORMANT -> 0;
            case AWAKENED -> VoidConfig.get(VoidConfig.COMPANION_EVOLUTION_COST_AWAKENED);
            case RESONANT -> VoidConfig.get(VoidConfig.COMPANION_EVOLUTION_COST_RESONANT);
            case ASCENDANT -> VoidConfig.get(VoidConfig.COMPANION_EVOLUTION_COST_ASCENDANT);
        };
    }

    public VoidTier requiredPlayerTier() { return requiredPlayerTier; }

    public String translationKey() {
        return "companion_stage.void_ascension." + id;
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey()).withStyle(color);
    }

    public boolean isMax() { return this == ASCENDANT; }

    @Nullable
    public CompanionStage next() {
        return switch (this) {
            case DORMANT -> AWAKENED;
            case AWAKENED -> RESONANT;
            case RESONANT -> ASCENDANT;
            case ASCENDANT -> null;
        };
    }

    public boolean isAtLeast(CompanionStage other) {
        return this.index >= other.index;
    }

    public static CompanionStage forLevel(int level) {
        if (level >= 30) return ASCENDANT;
        if (level >= 20) return RESONANT;
        if (level >= 10) return AWAKENED;
        return DORMANT;
    }

    @Nullable
    public static CompanionStage byIndex(int index) {
        for (CompanionStage s : values()) {
            if (s.index == index) return s;
        }
        return null;
    }
}
