package com.voidascension.evolution;

import com.voidascension.evolution.EvolutionRequirements.Requirement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionRequirementsTest {
    /** Mirrors the shipped defaults so the test does not depend on a loaded config. */
    private static final Function<VoidTier, TierDefinition> TABLE = t -> switch (t) {
        case HUMAN -> TierDefinition.none(t);
        case ENHANCED -> new TierDefinition(t, 3, 100, 2, 0.05, 0.5, 1, 0, 0);
        case MUTANT -> new TierDefinition(t, 8, 300, 4, 0.08, 1, 2, 0, 0.05);
        case VOIDBORN -> new TierDefinition(t, 15, 750, 6, 0.12, 2, 4, 1, 0.10);
        case ASCENDED -> new TierDefinition(t, 25, 1500, 10, 0.15, 3, 6, 2, 0.15);
    };

    @Test
    void eligibleWhenAllMet() {
        var c = EvolutionRequirements.evaluate(true, true, VoidTier.HUMAN, 3, 100, TABLE);
        assertTrue(c.eligible());
        assertEquals(VoidTier.ENHANCED, c.next());
        assertEquals(3, c.requiredLevel());
        assertEquals(100, c.requiredEssence());
    }

    @Test
    void reportsEachUnmetRequirement() {
        var c = EvolutionRequirements.evaluate(true, false, VoidTier.MUTANT, 14, 749, TABLE);
        assertFalse(c.eligible());
        assertEquals(List.of(Requirement.ACTIVE_CORE, Requirement.EVOLUTION_LEVEL, Requirement.VOID_ESSENCE), c.unmet());
        assertEquals(VoidTier.VOIDBORN, c.next());
    }

    @Test
    void levelBoundary() {
        assertTrue(EvolutionRequirements.evaluate(true, true, VoidTier.ENHANCED, 8, 300, TABLE).eligible());
        assertEquals(List.of(Requirement.EVOLUTION_LEVEL),
                EvolutionRequirements.evaluate(true, true, VoidTier.ENHANCED, 7, 300, TABLE).unmet());
        assertEquals(List.of(Requirement.VOID_ESSENCE),
                EvolutionRequirements.evaluate(true, true, VoidTier.ENHANCED, 8, 299, TABLE).unmet());
    }

    @Test
    void maxTierCannotEvolve() {
        var c = EvolutionRequirements.evaluate(true, true, VoidTier.ASCENDED, 100, 1_000_000, TABLE);
        assertFalse(c.eligible());
        assertNull(c.next());
        assertTrue(c.unmet().contains(Requirement.NOT_MAX_TIER));
    }

    @Test
    void uninitializedIsReportedFirst() {
        var c = EvolutionRequirements.evaluate(false, true, VoidTier.HUMAN, 50, 5000, TABLE);
        assertEquals(Requirement.INITIALIZED, c.unmet().get(0));
    }

    @Test
    void tiersAreOrderedAndParseable() {
        VoidTier[] all = VoidTier.values();
        for (int i = 0; i < all.length; i++) {
            assertEquals(i, all[i].level());
            assertSame(all[i], VoidTier.byLevel(i));
            assertSame(all[i], VoidTier.parse(all[i].id()));
            assertSame(all[i], VoidTier.parse(String.valueOf(i)));
        }
        assertNull(VoidTier.byLevel(-1));
        assertNull(VoidTier.byLevel(all.length));
        assertNull(VoidTier.parse("godlike"));
        assertTrue(VoidTier.ASCENDED.isMax());
        assertNull(VoidTier.ASCENDED.next());
        assertSame(VoidTier.ENHANCED, VoidTier.HUMAN.next());
    }
}
