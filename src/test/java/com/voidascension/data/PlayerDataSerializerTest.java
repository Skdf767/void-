package com.voidascension.data;

import com.voidascension.evolution.VoidTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataSerializerTest {
    private static final Predicate<String> KNOWN = Set.of("void_dash", "void_resilience", "void_rescue", "void_ascendance")::contains;

    private static VoidPlayerData sample() {
        VoidPlayerData d = new VoidPlayerData();
        d.setOwner(UUID.randomUUID());
        d.setInitialized(true);
        d.setCoreId(UUID.randomUUID());
        d.setTier(VoidTier.MUTANT);
        d.setEssence(4321);
        d.setVoidExperience(98765L);
        d.setEvolutionLevel(12);
        d.setUnlockedAbilities(Set.of("void_dash", "void_resilience"));
        d.setCooldownReadyAt("void_dash", 123456L);
        d.setVoidRescues(3);
        return d;
    }

    @Test
    void roundTripPreservesEverything() {
        VoidPlayerData in = sample();
        CompoundTag tag = PlayerDataSerializer.write(in);
        assertEquals(PlayerDataSerializer.CURRENT_VERSION, tag.getInt("DataVersion"));
        VoidPlayerData out = new VoidPlayerData();
        var report = PlayerDataSerializer.read(tag, out, KNOWN);
        assertTrue(report.clean(), report.issues().toString());
        assertFalse(report.migrated());
        assertEquals(in.getOwner(), out.getOwner());
        assertTrue(out.isInitialized());
        assertEquals(in.getCoreId(), out.getCoreId());
        assertEquals(VoidTier.MUTANT, out.getTier());
        assertEquals(4321, out.getEssence());
        assertEquals(98765L, out.getVoidExperience());
        assertEquals(12, out.getEvolutionLevel());
        assertEquals(Set.of("void_dash", "void_resilience"), out.getUnlockedAbilities());
        assertEquals(123456L, out.getCooldownReadyAt("void_dash"));
        assertEquals(3, out.getVoidRescues());
    }

    @Test
    void roundTripPreservesCompanions() {
        VoidPlayerData in = sample();
        in.getCompanion(com.voidascension.companion.CompanionSpecies.RIFT_STALKER).setUnlocked(true);
        in.getCompanion(com.voidascension.companion.CompanionSpecies.RIFT_STALKER).setActive(true);
        in.getCompanion(com.voidascension.companion.CompanionSpecies.RIFT_STALKER).setLevel(12);
        CompoundTag tag = PlayerDataSerializer.write(in);
        assertEquals(PlayerDataSerializer.CURRENT_VERSION, tag.getInt("DataVersion"));
        VoidPlayerData out = new VoidPlayerData();
        var report = PlayerDataSerializer.read(tag, out, KNOWN);
        assertTrue(report.clean(), report.issues().toString());
        assertTrue(out.getCompanion(com.voidascension.companion.CompanionSpecies.RIFT_STALKER).isUnlocked());
        assertTrue(out.getCompanion(com.voidascension.companion.CompanionSpecies.RIFT_STALKER).isActive());
        assertEquals(12, out.getCompanion(com.voidascension.companion.CompanionSpecies.RIFT_STALKER).getLevel());
    }

    @Test
    void emptyTagGivesFreshUninitializedData() {
        VoidPlayerData out = sample();
        var report = PlayerDataSerializer.read(new CompoundTag(), out, KNOWN);
        assertTrue(report.clean());
        assertFalse(out.isInitialized());
        assertEquals(VoidTier.HUMAN, out.getTier());
        assertEquals(0, out.getEssence());
        assertNull(out.getCoreId());
        assertTrue(out.getUnlockedAbilities().isEmpty());
    }

    @Test
    void malformedValuesAreSanitized() {
        CompoundTag tag = PlayerDataSerializer.write(sample());
        tag.putInt("Tier", 99);
        tag.putInt("Essence", -50);
        tag.putLong("VoidXp", -1);
        tag.putInt("EvolutionLevel", -3);
        ListTag abilities = new ListTag();
        abilities.add(StringTag.valueOf("void_dash"));
        abilities.add(StringTag.valueOf("unlimited_power"));
        tag.put("Abilities", abilities);
        tag.getCompound("Cooldowns").putLong("unlimited_power", 5L);

        VoidPlayerData out = new VoidPlayerData();
        var report = PlayerDataSerializer.read(tag, out, KNOWN);
        assertFalse(report.clean());
        assertEquals(VoidTier.HUMAN, out.getTier(), "invalid tier must never grant a higher tier");
        assertEquals(0, out.getEssence());
        assertEquals(0L, out.getVoidExperience());
        assertEquals(0, out.getEvolutionLevel());
        assertEquals(Set.of("void_dash"), out.getUnlockedAbilities());
        assertFalse(out.getCooldowns().containsKey("unlimited_power"));
    }

    @Test
    void wrongTypesDoNotThrow() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 2);
        tag.putString("Tier", "ASCENDED");
        tag.putString("Essence", "lots");
        tag.putString("Abilities", "void_dash");
        VoidPlayerData out = new VoidPlayerData();
        var report = assertDoesNotThrow(() -> PlayerDataSerializer.read(tag, out, KNOWN));
        assertEquals(3, report.issues().size(), report.issues().toString());
        assertEquals(VoidTier.HUMAN, out.getTier());
        assertEquals(0, out.getEssence());
    }

    @Test
    void migratesVersion1Saves() {
        UUID core = UUID.randomUUID();
        CompoundTag v1 = new CompoundTag();
        v1.putInt("Essence", 600);
        v1.putInt("LifetimeEssence", 2400);
        v1.putInt("Tier", 3); // Voidtouched
        v1.putUUID("CoreId", core);
        v1.putInt("VoidRescues", 2);

        VoidPlayerData out = new VoidPlayerData();
        var report = PlayerDataSerializer.read(v1, out, KNOWN);
        assertTrue(report.migrated());
        assertEquals(1, report.sourceVersion());
        assertTrue(out.isInitialized());
        assertEquals(VoidTier.MUTANT, out.getTier());
        assertEquals(600, out.getEssence());
        assertEquals(2400L, out.getVoidExperience());
        assertEquals(core, out.getCoreId());
        assertEquals(2, out.getVoidRescues());

        CompoundTag rewritten = PlayerDataSerializer.write(out);
        assertEquals(2, rewritten.getInt("DataVersion"));
    }

    @Test
    void version1TierMapping() {
        assertEquals(VoidTier.HUMAN, PlayerDataSerializer.mapV1Tier(0));
        assertEquals(VoidTier.HUMAN, PlayerDataSerializer.mapV1Tier(1));
        assertEquals(VoidTier.ENHANCED, PlayerDataSerializer.mapV1Tier(2));
        assertEquals(VoidTier.MUTANT, PlayerDataSerializer.mapV1Tier(3));
        assertEquals(VoidTier.VOIDBORN, PlayerDataSerializer.mapV1Tier(4));
        assertEquals(VoidTier.ASCENDED, PlayerDataSerializer.mapV1Tier(5));
        assertNull(PlayerDataSerializer.mapV1Tier(6));
        assertNull(PlayerDataSerializer.mapV1Tier(-1));
    }

    @Test
    void untouchedV1PlayerStaysUninitialized() {
        CompoundTag v1 = new CompoundTag();
        v1.putInt("Essence", 0);
        v1.putInt("Tier", 0);
        VoidPlayerData out = new VoidPlayerData();
        PlayerDataSerializer.read(v1, out, KNOWN);
        assertFalse(out.isInitialized());
    }

    @Test
    void futureVersionLoadsKnownFieldsWithWarning() {
        CompoundTag tag = PlayerDataSerializer.write(sample());
        tag.putInt("DataVersion", 7);
        tag.putString("SomeFutureField", "x");
        VoidPlayerData out = new VoidPlayerData();
        var report = PlayerDataSerializer.read(tag, out, KNOWN);
        assertFalse(report.clean());
        assertEquals(VoidTier.MUTANT, out.getTier());
        assertEquals(4321, out.getEssence());
    }

    @Test
    void negativeSettersClamp() {
        VoidPlayerData d = new VoidPlayerData();
        d.setEssence(-1);
        d.setVoidExperience(-1);
        d.setTier(null);
        assertEquals(0, d.getEssence());
        assertEquals(0L, d.getVoidExperience());
        assertEquals(VoidTier.HUMAN, d.getTier());
    }
}
