package com.voidascension.companion;

import com.voidascension.evolution.VoidTier;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanionDataTest {

    @Test
    void defaultStateInitializesCorrectly() {
        CompanionData wisp = new CompanionData(CompanionSpecies.ABYSSAL_WISP);
        assertEquals(CompanionSpecies.ABYSSAL_WISP, wisp.getSpecies());
        assertEquals(1, wisp.getLevel());
        assertEquals(0L, wisp.getXp());
        assertEquals(CompanionStage.DORMANT, wisp.getStage());
        assertEquals(24.0F, wisp.getMaxHealth(), 0.01F);
        assertEquals(24.0F, wisp.getHealth(), 0.01F);
        assertFalse(wisp.isUnlocked());
        assertFalse(wisp.isActive());
    }

    @Test
    void xpCurveAndLevelingProgresses() {
        CompanionData stalker = new CompanionData(CompanionSpecies.RIFT_STALKER);
        assertEquals(1, stalker.getLevel());

        // Level 2 requires ~25 XP
        boolean leveled = stalker.addExperience(60);
        assertTrue(leveled);
        assertTrue(stalker.getLevel() >= 2);

        // Max level capped at 30
        stalker.setLevel(30);
        assertEquals(30, stalker.getLevel());
        assertFalse(stalker.addExperience(1000));
    }

    @Test
    void stageProgressionAndAbilities() {
        CompanionData oracle = new CompanionData(CompanionSpecies.NULL_ORACLE);
        oracle.setStage(CompanionStage.DORMANT);

        // Disrupt is Dormant ability
        assertTrue(oracle.isAbilityUnlocked(CompanionAbility.DISRUPT));
        // Null Field is Resonant ability
        assertFalse(oracle.isAbilityUnlocked(CompanionAbility.NULL_FIELD));

        // Advance to Resonant
        oracle.setStage(CompanionStage.RESONANT);
        assertTrue(oracle.isAbilityUnlocked(CompanionAbility.DISRUPT));
        assertTrue(oracle.isAbilityUnlocked(CompanionAbility.VOID_SCAN));
        assertTrue(oracle.isAbilityUnlocked(CompanionAbility.NULL_FIELD));
        assertFalse(oracle.isAbilityUnlocked(CompanionAbility.RESONANCE_LINK));

        // Advance to Ascendant
        oracle.setStage(CompanionStage.ASCENDANT);
        assertTrue(oracle.isAbilityUnlocked(CompanionAbility.RESONANCE_LINK));
    }

    @Test
    void stageEvolutionRequirements() {
        CompanionStage s1 = CompanionStage.DORMANT;
        assertEquals(CompanionStage.AWAKENED, s1.next());
        assertEquals(10, s1.next().minLevel());
        assertEquals(VoidTier.ENHANCED, s1.next().requiredPlayerTier());
        assertEquals(150, s1.next().evolutionEssenceCost());

        CompanionStage s2 = CompanionStage.AWAKENED;
        assertEquals(CompanionStage.RESONANT, s2.next());
        assertEquals(20, s2.next().minLevel());
        assertEquals(VoidTier.MUTANT, s2.next().requiredPlayerTier());
        assertEquals(400, s2.next().evolutionEssenceCost());

        CompanionStage s3 = CompanionStage.RESONANT;
        assertEquals(CompanionStage.ASCENDANT, s3.next());
        assertEquals(30, s3.next().minLevel());
        assertEquals(VoidTier.ASCENDED, s3.next().requiredPlayerTier());
        assertEquals(900, s3.next().evolutionEssenceCost());

        CompanionStage s4 = CompanionStage.ASCENDANT;
        assertNull(s4.next());
        assertTrue(s4.isMax());
    }

    @Test
    void cooldownsAndRecovery() {
        CompanionData wisp = new CompanionData(CompanionSpecies.ABYSSAL_WISP);
        long now = 1000L;

        assertFalse(wisp.isRecovering(now));
        wisp.setRecoveryReadyAt(now + 200L);
        assertTrue(wisp.isRecovering(now));
        assertFalse(wisp.isRecovering(now + 201L));

        wisp.setCooldownReadyAt("abyss_shield", now + 400L);
        assertEquals(now + 400L, wisp.getCooldownReadyAt("abyss_shield"));

        wisp.pruneCooldowns(now + 500L);
        assertEquals(0L, wisp.getCooldownReadyAt("abyss_shield"));
    }

    @Test
    void roundTripNbtSerialization() {
        CompanionData in = new CompanionData(CompanionSpecies.RIFT_STALKER);
        in.setUnlocked(true);
        in.setActive(true);
        in.setLevel(15);
        in.setXp(1200L);
        in.setStage(CompanionStage.AWAKENED);
        in.setHealth(18.5F);
        in.setSelectedAbilityIndex(1);
        in.setCooldownReadyAt("rift_bolt", 5000L);
        in.setRecoveryReadyAt(6000L);

        CompoundTag tag = in.writeNbt();
        CompanionData out = new CompanionData(CompanionSpecies.RIFT_STALKER);
        out.readNbt(tag);

        assertTrue(out.isUnlocked());
        assertTrue(out.isActive());
        assertEquals(15, out.getLevel());
        assertEquals(1200L, out.getXp());
        assertEquals(CompanionStage.AWAKENED, out.getStage());
        assertEquals(18.5F, out.getHealth(), 0.01F);
        assertEquals(1, out.getSelectedAbilityIndex());
        assertEquals(5000L, out.getCooldownReadyAt("rift_bolt"));
        assertEquals(6000L, out.getRecoveryReadyAt());
    }
}
