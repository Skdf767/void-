package com.voidascension.evolution;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Pure (Minecraft-state-free) evaluation of the requirements for the next evolution.
 * Used by the server for validation and by the client screen for display, so both always agree on the rules.
 */
public final class EvolutionRequirements {

    /** Requirements in the order they are reported to the player. */
    public enum Requirement { INITIALIZED, NOT_MAX_TIER, ACTIVE_CORE, EVOLUTION_LEVEL, VOID_ESSENCE }

    /**
     * Result of an evaluation.
     *
     * @param next  the tier that would be reached, or {@code null} at max tier
     * @param unmet unmet requirements (empty = eligible)
     */
    public record Check(VoidTier current, @Nullable VoidTier next, int level, int requiredLevel,
                        int essence, int requiredEssence, List<Requirement> unmet) {
        public boolean eligible() { return unmet.isEmpty(); }

        public boolean isMet(Requirement r) { return !unmet.contains(r); }
    }

    public static Check evaluate(boolean initialized, boolean coreSatisfied, VoidTier current, int level, int essence,
                                 Function<VoidTier, TierDefinition> table) {
        List<Requirement> unmet = new ArrayList<>();
        VoidTier next = current.next();
        int requiredLevel = 0;
        int requiredEssence = 0;
        if (!initialized) unmet.add(Requirement.INITIALIZED);
        if (next == null) {
            unmet.add(Requirement.NOT_MAX_TIER);
        } else {
            TierDefinition def = table.apply(next);
            requiredLevel = def.requiredLevel();
            requiredEssence = def.essenceCost();
            if (!coreSatisfied) unmet.add(Requirement.ACTIVE_CORE);
            if (level < requiredLevel) unmet.add(Requirement.EVOLUTION_LEVEL);
            if (essence < requiredEssence) unmet.add(Requirement.VOID_ESSENCE);
        }
        return new Check(current, next, level, requiredLevel, essence, requiredEssence, Collections.unmodifiableList(unmet));
    }

    private EvolutionRequirements() {}
}
