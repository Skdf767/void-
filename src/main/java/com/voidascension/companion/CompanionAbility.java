package com.voidascension.companion;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * All 12 companion abilities spanning the three species and their evolution stages.
 */
public enum CompanionAbility {
    // Abyssal Wisp (Support & Sustain)
    VOID_SIPHON("void_siphon", CompanionSpecies.ABYSSAL_WISP, CompanionStage.DORMANT, false, 0, 0),
    ABYSS_SHIELD("abyss_shield", CompanionSpecies.ABYSSAL_WISP, CompanionStage.AWAKENED, true, 25, 400),
    THREAT_SENSE("threat_sense", CompanionSpecies.ABYSSAL_WISP, CompanionStage.RESONANT, true, 10, 300),
    ABYSS_PULSE("abyss_pulse", CompanionSpecies.ABYSSAL_WISP, CompanionStage.ASCENDANT, true, 15, 160),

    // Rift Stalker (Offensive Hunter)
    RIFT_BOLT("rift_bolt", CompanionSpecies.RIFT_STALKER, CompanionStage.DORMANT, true, 8, 60),
    MARK_PREY("mark_preY", CompanionSpecies.RIFT_STALKER, CompanionStage.AWAKENED, true, 12, 240),
    PHASE_STRIKE("phase_strike", CompanionSpecies.RIFT_STALKER, CompanionStage.RESONANT, true, 18, 200),
    EXECUTION_SURGE("execution_surge", CompanionSpecies.RIFT_STALKER, CompanionStage.ASCENDANT, true, 20, 300),

    // Null Oracle (Tactical Defender)
    DISRUPT("disrupt", CompanionSpecies.NULL_ORACLE, CompanionStage.DORMANT, true, 14, 240),
    VOID_SCAN("void_scan", CompanionSpecies.NULL_ORACLE, CompanionStage.AWAKENED, true, 15, 400),
    NULL_FIELD("null_field", CompanionSpecies.NULL_ORACLE, CompanionStage.RESONANT, true, 30, 500),
    RESONANCE_LINK("resonance_link", CompanionSpecies.NULL_ORACLE, CompanionStage.ASCENDANT, true, 20, 600);

    private final String id;
    private final CompanionSpecies species;
    private final CompanionStage requiredStage;
    private final boolean active;
    private final int defaultEssenceCost;
    private final int defaultCooldownTicks;

    CompanionAbility(String id, CompanionSpecies species, CompanionStage requiredStage,
                     boolean active, int defaultEssenceCost, int defaultCooldownTicks) {
        this.id = id.toLowerCase();
        this.species = species;
        this.requiredStage = requiredStage;
        this.active = active;
        this.defaultEssenceCost = defaultEssenceCost;
        this.defaultCooldownTicks = defaultCooldownTicks;
    }

    public String id() { return id; }
    public CompanionSpecies species() { return species; }
    public CompanionStage requiredStage() { return requiredStage; }
    public boolean isActive() { return active; }
    public int essenceCost() { return defaultEssenceCost; }
    public int cooldownTicks() { return defaultCooldownTicks; }

    public String translationKey() {
        return "companion_ability.void_ascension." + id;
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey()).withStyle(species.color());
    }

    public MutableComponent description() {
        return Component.translatable(translationKey() + ".desc").withStyle(ChatFormatting.GRAY);
    }

    public static List<CompanionAbility> getForSpecies(CompanionSpecies species) {
        List<CompanionAbility> list = new ArrayList<>();
        for (CompanionAbility a : values()) {
            if (a.species == species) list.add(a);
        }
        return list;
    }

    @Nullable
    public static CompanionAbility byId(String id) {
        if (id == null) return null;
        for (CompanionAbility a : values()) {
            if (a.id.equalsIgnoreCase(id) || a.name().equalsIgnoreCase(id)) return a;
        }
        return null;
    }
}
