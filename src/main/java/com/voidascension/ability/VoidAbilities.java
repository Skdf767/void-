package com.voidascension.ability;

import com.voidascension.evolution.VoidTier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of the abilities implemented in this milestone. Only abilities listed here exist; the roadmap
 * abilities (Void Phase, Gravity Pulse, ...) are intentionally NOT registered - see docs/ROADMAP.md.
 */
public final class VoidAbilities {
    private static final Map<String, AbilityDefinition> BY_ID = new LinkedHashMap<>();

    /** ENHANCED - active: a short physics-based burst of movement in the look direction. */
    public static final AbilityDefinition VOID_DASH = register("void_dash", AbilityDefinition.Type.ACTIVE, VoidTier.ENHANCED);
    /** MUTANT - passive: reduces fall damage. */
    public static final AbilityDefinition VOID_RESILIENCE = register("void_resilience", AbilityDefinition.Type.PASSIVE, VoidTier.MUTANT);
    /** VOIDBORN - passive: falling into the void teleports you to safety (cooldown + Essence cost). */
    public static final AbilityDefinition VOID_RESCUE = register("void_rescue", AbilityDefinition.Type.PASSIVE, VoidTier.VOIDBORN);
    /** ASCENDED - passive: more Essence from gameplay. */
    public static final AbilityDefinition VOID_ASCENDANCE = register("void_ascendance", AbilityDefinition.Type.PASSIVE, VoidTier.ASCENDED);

    private static AbilityDefinition register(String id, AbilityDefinition.Type type, VoidTier tier) {
        AbilityDefinition def = new AbilityDefinition(id, type, tier);
        if (BY_ID.put(id, def) != null) throw new IllegalStateException("Duplicate ability id " + id);
        return def;
    }

    @Nullable
    public static AbilityDefinition get(String id) { return BY_ID.get(id); }

    public static boolean isKnown(String id) { return BY_ID.containsKey(id); }

    public static List<AbilityDefinition> all() { return List.copyOf(BY_ID.values()); }

    public static Set<String> ids() { return Collections.unmodifiableSet(BY_ID.keySet()); }

    /** Ids of every ability unlocked at {@code tier} (including those of lower tiers). */
    public static Set<String> unlockedAt(VoidTier tier) {
        Set<String> out = new LinkedHashSet<>();
        for (AbilityDefinition def : BY_ID.values()) {
            if (tier.isAtLeast(def.unlockTier())) out.add(def.id());
        }
        return out;
    }

    /** Abilities whose unlock tier is exactly {@code tier}. */
    public static List<AbilityDefinition> unlockedBy(VoidTier tier) {
        return BY_ID.values().stream().filter(d -> d.unlockTier() == tier).toList();
    }

    private VoidAbilities() {}
}
