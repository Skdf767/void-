package com.voidascension.gametest;

import com.voidascension.VoidAscension;
import com.voidascension.evolution.EvolutionService;
import com.voidascension.evolution.EvolutionTable;
import com.voidascension.evolution.TierAttributes;
import com.voidascension.evolution.TierDefinition;
import com.voidascension.evolution.VoidTier;
import com.voidascension.progression.ProgressionService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.voidascension.gametest.TestSupport.*;

/** P0 regression tests for the 0.1.0 attribute defects (bonus health lost on relog / End return). */
@GameTestHolder(VoidAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttributeRegressionGameTests {

    /** Defect D1: player saved at 30/30 must load at 30/30, not be clamped to vanilla 20 before bonuses exist. */
    @GameTest(template = TEMPLATE)
    public static void bonusHealthSurvivesRelog(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.ASCENDED);
        float max = player.getMaxHealth();
        helper.assertTrue(max > 20.0F, "ASCENDED raises max health, got " + max);
        player.setHealth(max);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());

        ServerPlayer reloaded = newPlayer(helper);
        reloaded.load(saved);                       // what PlayerList does when the player joins
        ProgressionService.revalidate(reloaded);    // what our PlayerLoggedInEvent handler does
        helper.assertTrue(reloaded.getMaxHealth() == max, "max health restored, got " + reloaded.getMaxHealth());
        helper.assertTrue(reloaded.getHealth() == max,
                "current health must not be clamped to 20 on relog, got " + reloaded.getHealth() + "/" + reloaded.getMaxHealth());
        ProgressionService.revalidate(reloaded);    // a second revalidate (e.g. respawn) must not stack anything
        for (TierAttributes.Slot slot : TierAttributes.SLOTS) {
            AttributeInstance a = reloaded.getAttribute(slot.attribute());
            long ours = a.getModifiers().stream().filter(m -> m.getId().equals(slot.id())).count();
            helper.assertTrue(ours <= 1, slot.name() + " duplicated after relog: " + ours);
            helper.assertTrue(Math.abs(a.getValue() - player.getAttribute(slot.attribute()).getValue()) < 1e-9,
                    slot.name() + " differs after relog");
        }
        cleanup(player, reloaded);
        helper.succeed();
    }

    /** Defect D2: returning from The End (Clone, wasDeath=false) must keep current health above 20. */
    @GameTest(template = TEMPLATE)
    public static void bonusHealthSurvivesEndReturn(GameTestHelper helper) {
        ServerPlayer original = activatedPlayer(helper);
        EvolutionService.setTier(original, VoidTier.ASCENDED);
        original.setHealth(27.0F);
        ServerPlayer fresh = newPlayer(helper);
        fresh.setHealth(original.getHealth());      // vanilla ServerPlayer.restoreFrom(keepEverything=true)
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(fresh, original, false));
        helper.assertTrue(fresh.getHealth() == 27.0F, "health kept on End return, got " + fresh.getHealth());
        cleanup(original, fresh);
        helper.succeed();
    }

    /** Requirement 2.1: all six configured attributes match every tier; lowering removes; HUMAN has none of ours. */
    @GameTest(template = TEMPLATE)
    public static void allSixAttributesFollowTier(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        VoidTier[] order = {VoidTier.ENHANCED, VoidTier.MUTANT, VoidTier.VOIDBORN, VoidTier.ASCENDED,
                VoidTier.ASCENDED, VoidTier.MUTANT, VoidTier.HUMAN, VoidTier.VOIDBORN};
        for (VoidTier t : order) {
            EvolutionService.setTier(player, t);
            TierDefinition def = EvolutionTable.get(t);
            for (TierAttributes.Slot slot : TierAttributes.SLOTS) {
                AttributeInstance a = player.getAttribute(slot.attribute());
                double bonus = slot.value().applyAsDouble(def);
                double expected = slot.op() == AttributeModifier.Operation.MULTIPLY_BASE
                        ? a.getBaseValue() * (1 + bonus) : a.getBaseValue() + bonus;
                expected = slot.attribute().sanitizeValue(expected);
                helper.assertTrue(Math.abs(a.getValue() - expected) < 1e-9,
                        t + " " + slot.name() + ": expected " + expected + " got " + a.getValue());
                helper.assertTrue(TierAttributes.count(player, slot.attribute(), slot.id()) == (bonus == 0 ? 0 : 1),
                        t + " " + slot.name() + " modifier presence");
            }
        }
        helper.assertTrue(EvolutionTable.get(VoidTier.ASCENDED).bonusKnockbackResist() < 1.0,
                "ASCENDED must not be knockback-immune by default");
        ProgressionService.reset(player);
        for (TierAttributes.Slot slot : TierAttributes.SLOTS) {
            helper.assertTrue(TierAttributes.count(player, slot.attribute(), slot.id()) == 0, slot.name() + " removed on reset");
        }
        cleanup(player);
        helper.succeed();
    }

    /** Requirement 2.1.11: unrelated modifiers (vanilla / other mods) must survive every tier change. */
    @GameTest(template = TEMPLATE)
    public static void unrelatedModifiersAreNeverRemoved(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        UUID foreign = UUID.fromString("0f0f0f0f-1111-2222-3333-444455556666");
        player.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                new AttributeModifier(foreign, "other_mod_health", 4.0, AttributeModifier.Operation.ADDITION));
        player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
                new AttributeModifier(foreign, "other_mod_speed", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
        for (VoidTier t : new VoidTier[]{VoidTier.ASCENDED, VoidTier.HUMAN, VoidTier.MUTANT, VoidTier.MUTANT}) {
            EvolutionService.setTier(player, t);
        }
        ProgressionService.reset(player);
        helper.assertTrue(player.getAttribute(Attributes.MAX_HEALTH).getModifier(foreign) != null, "foreign health modifier kept");
        helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(foreign) != null, "foreign speed modifier kept");
        helper.assertTrue(player.getMaxHealth() == 24.0F, "20 base + 4 foreign after reset, got " + player.getMaxHealth());
        cleanup(player);
        helper.succeed();
    }

    /** Requirement 2.1: evolving must not heal the player to full. */
    @GameTest(template = TEMPLATE)
    public static void evolvingDoesNotHealToFull(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        player.setHealth(10.0F);
        EvolutionService.setTier(player, VoidTier.VOIDBORN);
        ProgressionService.revalidate(player);
        helper.assertTrue(player.getHealth() == 10.0F, "health unchanged by evolution, got " + player.getHealth());
        EvolutionService.setTier(player, VoidTier.ASCENDED);
        player.setHealth(player.getMaxHealth());
        EvolutionService.setTier(player, VoidTier.HUMAN);
        helper.assertTrue(player.getHealth() == 20.0F, "lowering the tier clamps health to the new max, got " + player.getHealth());
        cleanup(player);
        helper.succeed();
    }

    private AttributeRegressionGameTests() {}
}
