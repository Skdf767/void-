package com.voidascension.gametest;

import com.voidascension.VoidAscension;
import com.voidascension.companion.CompanionAbility;
import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionService;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.companion.CompanionStage;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.entity.companion.AbyssalWispEntity;
import com.voidascension.evolution.VoidTier;
import com.voidascension.registry.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.voidascension.gametest.TestSupport.activatedPlayer;
import static com.voidascension.gametest.TestSupport.data;

/**
 * In-engine GameTests verifying Void Companions systems:
 * spawning, ownership, active slot limits, evolution, and abilities.
 */
@GameTestHolder(VoidAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompanionGameTests {

    @GameTest(template = TestSupport.TEMPLATE)
    public static void defaultCompanionUnlockedOnActivation(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        VoidPlayerData d = data(player);

        CompanionData wisp = d.getCompanion(CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(wisp != null && wisp.isUnlocked(), "Abyssal Wisp should be unlocked by default");
        helper.assertTrue(wisp.getLevel() == 1, "Initial companion level must be 1");
        helper.assertTrue(wisp.getStage() == CompanionStage.DORMANT, "Initial stage must be Dormant");
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void companionSpawnsAndTracksOwner(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        VoidPlayerData d = data(player);

        // Summon Abyssal Wisp
        CompanionService.ActionResult res = CompanionService.toggleActive(player, CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(res.isSuccess(), "Summoning unlocked companion should succeed");

        CompanionData cd = d.getCompanion(CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(cd.isActive(), "Companion should be marked active");
        helper.assertTrue(cd.getActiveEntityUUID() != null, "Companion should have an active entity UUID");

        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void activeSlotLimitsFollowTier(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        VoidPlayerData d = data(player);

        // HUMAN tier allows 1 companion
        helper.assertTrue(d.getMaxActiveCompanions() == 1, "HUMAN should have 1 active slot");

        // Unlock Rift Stalker
        d.setEssence(500);
        CompanionService.ActionResult unlockRes = CompanionService.unlockCompanion(player, CompanionSpecies.RIFT_STALKER);
        helper.assertTrue(unlockRes.isSuccess(), "Unlocking Rift Stalker should succeed");

        // Summon Wisp
        CompanionService.toggleActive(player, CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(d.getActiveCompanionCount() == 1, "Active count should be 1");

        // Attempting to summon second companion at HUMAN tier should fail
        CompanionService.ActionResult secondSummon = CompanionService.toggleActive(player, CompanionSpecies.RIFT_STALKER);
        helper.assertTrue(secondSummon == CompanionService.ActionResult.MAX_ACTIVE_REACHED,
                "Exceeding active limit should return MAX_ACTIVE_REACHED");

        // Advance player tier to MUTANT -> now allows 2 companions
        d.setTier(VoidTier.MUTANT);
        helper.assertTrue(d.getMaxActiveCompanions() == 2, "MUTANT should have 2 active slots");

        CompanionService.ActionResult thirdSummon = CompanionService.toggleActive(player, CompanionSpecies.RIFT_STALKER);
        helper.assertTrue(thirdSummon.isSuccess(), "Summoning 2nd companion should now succeed");
        helper.assertTrue(d.getActiveCompanionCount() == 2, "Active count should now be 2");

        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE)
    public static void companionEvolutionRequiresLevelAndEssence(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        VoidPlayerData d = data(player);
        CompanionData wisp = d.getCompanion(CompanionSpecies.ABYSSAL_WISP);

        // Try evolve at level 1 -> fails
        CompanionService.ActionResult r1 = CompanionService.evolveCompanion(player, CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(r1 == CompanionService.ActionResult.LEVEL_TOO_LOW, "Level 1 cannot evolve to Awakened");

        // Raise level to 10, but player tier is still HUMAN -> fails
        wisp.setLevel(10);
        d.setEssence(200);
        CompanionService.ActionResult r2 = CompanionService.evolveCompanion(player, CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(r2 == CompanionService.ActionResult.PLAYER_TIER_TOO_LOW, "HUMAN player cannot evolve companion to Awakened");

        // Set player tier to ENHANCED -> succeeds
        d.setTier(VoidTier.ENHANCED);
        CompanionService.ActionResult r3 = CompanionService.evolveCompanion(player, CompanionSpecies.ABYSSAL_WISP);
        helper.assertTrue(r3.isSuccess(), "Evolution to Awakened should succeed");
        helper.assertTrue(wisp.getStage() == CompanionStage.AWAKENED, "Stage should now be Awakened");
        helper.assertTrue(wisp.isAbilityUnlocked(CompanionAbility.ABYSS_SHIELD), "Abyss Shield should now be unlocked");
        helper.assertTrue(d.getEssence() == 50, "150 Essence should have been deducted (200 - 150 = 50)");

        helper.succeed();
    }
}
