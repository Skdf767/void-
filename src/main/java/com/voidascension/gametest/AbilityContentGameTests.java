package com.voidascension.gametest;

import com.voidascension.VoidAscension;
import com.voidascension.ability.AbilityService;
import com.voidascension.ability.AbilityService.UseResult;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.api.event.VoidEssenceGainEvent;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.event.EssenceSourceEvents;
import com.voidascension.evolution.EvolutionService;
import com.voidascension.evolution.VoidTier;
import com.voidascension.progression.VoidEssenceService;
import com.voidascension.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.voidascension.gametest.TestSupport.*;

/** In-game tests for abilities, commands, essence sources and data-driven content. */
@GameTestHolder(VoidAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AbilityContentGameTests {

    // ------------------------------------------------------------------ Void Dash (active)

    @GameTest(template = TEMPLATE)
    public static void voidDashLifecycle(GameTestHelper helper) {
        ServerPlayer fresh = newPlayer(helper);
        helper.assertTrue(AbilityService.use(fresh, "void_dash") == UseResult.NOT_INITIALIZED, "uninitialized");

        ServerPlayer player = activatedPlayer(helper);
        VoidPlayerData d = data(player);
        helper.assertTrue(AbilityService.use(player, "void_dash") == UseResult.LOCKED, "HUMAN cannot dash");
        helper.assertTrue(AbilityService.use(player, "nonexistent") == UseResult.UNKNOWN_ABILITY, "unknown id");

        EvolutionService.setTier(player, VoidTier.ENHANCED);
        VoidEssenceService.setVoidEssence(player, 5);
        helper.assertTrue(AbilityService.use(player, "void_dash") == UseResult.NOT_ENOUGH_ESSENCE, "5 < 10");
        helper.assertTrue(d.getEssence() == 5 && d.getCooldownReadyAt("void_dash") == 0, "failed use changes nothing");

        VoidEssenceService.setVoidEssence(player, 50);
        player.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(AbilityService.use(player, "void_dash") == UseResult.INVALID_STATE, "spectators cannot dash");
        helper.assertTrue(d.getEssence() == 50, "no cost on invalid state");
        player.setGameMode(GameType.SURVIVAL);

        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(0F);
        player.setXRot(0F);
        player.fallDistance = 12F;
        long now = AbilityService.now(player);
        helper.assertTrue(AbilityService.use(player, "void_dash") == UseResult.SUCCESS, "dash succeeds");
        Vec3 v = player.getDeltaMovement();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        helper.assertTrue(Math.abs(horizontal - 1.4) < 1e-3, "horizontal impulse = strength, got " + horizontal);
        helper.assertTrue(v.z > 1.0, "yaw 0 dashes towards +Z, got " + v);
        helper.assertTrue(v.y > 0.0, "dash adds lift");
        helper.assertTrue(player.fallDistance == 0F, "dash resets fall distance");
        helper.assertTrue(d.getEssence() == 40, "cost 10 spent, got " + d.getEssence());
        helper.assertTrue(d.getCooldownReadyAt("void_dash") == now + 100, "100 tick cooldown recorded");

        helper.assertTrue(AbilityService.use(player, "void_dash") == UseResult.ON_COOLDOWN, "cooldown enforced");
        helper.assertTrue(d.getEssence() == 40, "no cost while on cooldown");
        helper.assertTrue(AbilityService.use(player, "void_resilience") == UseResult.PASSIVE, "passives cannot be activated");
        cleanup(fresh, player);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Void Resilience (passive)

    @GameTest(template = TEMPLATE)
    public static void voidResilienceReducesFallDamage(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.ENHANCED);
        helper.assertTrue(postHurt(player, true, 10F) == 10F, "ENHANCED: no reduction");

        EvolutionService.setTier(player, VoidTier.MUTANT);
        float reduced = postHurt(player, true, 10F);
        helper.assertTrue(Math.abs(reduced - 7F) < 1e-4, "MUTANT: 30% fall reduction, got " + reduced);
        helper.assertTrue(postHurt(player, false, 10F) == 10F, "non-fall damage unaffected");
        cleanup(player);
        helper.succeed();
    }

    private static float postHurt(ServerPlayer player, boolean fall, float amount) {
        var src = fall ? player.damageSources().fall() : player.damageSources().generic();
        LivingHurtEvent event = new LivingHurtEvent(player, src, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    // ------------------------------------------------------------------ Void Rescue (passive)

    @GameTest(template = TEMPLATE)
    public static void voidRescueUsesCooldownAndCost(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        int minY = helper.getLevel().getMinBuildHeight();
        VoidEssenceService.setVoidEssence(player, 250);

        EvolutionService.setTier(player, VoidTier.MUTANT);
        player.moveTo(0.5, minY - 10, 0.5);
        helper.assertTrue(!postVoidDamage(player), "MUTANT is not rescued");

        EvolutionService.setTier(player, VoidTier.VOIDBORN);
        player.moveTo(0.5, minY + 50, 0.5);
        helper.assertTrue(!postVoidDamage(player), "above the world (e.g. /kill) is not rescued");

        player.moveTo(0.5, minY - 10, 0.5);
        helper.assertTrue(postVoidDamage(player), "VOIDBORN is rescued");
        VoidPlayerData d = data(player);
        helper.assertTrue(player.getY() >= minY, "teleported back into the world, y=" + player.getY());
        helper.assertTrue(d.getEssence() == 150, "rescue costs 100, got " + d.getEssence());
        helper.assertTrue(d.getVoidRescues() == 1, "rescue counted");
        helper.assertTrue(AbilityService.cooldownRemaining(d, "void_rescue", AbilityService.now(player)) == 1200, "cooldown started");

        player.moveTo(0.5, minY - 10, 0.5);
        helper.assertTrue(!postVoidDamage(player), "second fall during cooldown is not rescued");
        helper.assertTrue(d.getEssence() == 150, "no cost during cooldown");
        cleanup(player);
        helper.succeed();
    }

    private static boolean postVoidDamage(ServerPlayer player) {
        LivingAttackEvent event = new LivingAttackEvent(player, player.damageSources().fellOutOfWorld(), 4F);
        return MinecraftForge.EVENT_BUS.post(event);
    }

    // ------------------------------------------------------------------ Void Ascendance (passive)

    @GameTest(template = TEMPLATE)
    public static void voidAscendanceBoostsGameplayEssence(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.VOIDBORN);
        helper.assertTrue(VoidEssenceService.grantFromGameplay(player, 100, VoidEssenceGainEvent.Source.KILL) == 100, "no bonus");
        EvolutionService.setTier(player, VoidTier.ASCENDED);
        int gained = VoidEssenceService.grantFromGameplay(player, 100, VoidEssenceGainEvent.Source.KILL);
        helper.assertTrue(gained == 125, "ASCENDED +25%, got " + gained);
        cleanup(player);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Commands

    @GameTest(template = TEMPLATE)
    public static void commandsEnforcePermissions(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = activatedPlayer(helper);
        CommandSourceStack asPlayer = player.createCommandSourceStack().withPermission(0).withSuppressedOutput();
        CommandSourceStack asOp = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();

        helper.assertTrue(run(server, asPlayer, "void status") == 1, "/void status is available to everyone");
        helper.assertTrue(run(server, asPlayer, "voidascension status") == 1, "/voidascension alias works");
        helper.assertTrue(run(server, asPlayer, "void essence set @s 500") == 0, "non-op cannot set essence");
        helper.assertTrue(data(player).getEssence() == 0, "essence unchanged by denied command");
        helper.assertTrue(run(server, asPlayer, "void evolution set @s ascended") == 0, "non-op cannot set tier");
        helper.assertTrue(data(player).getTier() == VoidTier.HUMAN, "tier unchanged by denied command");
        helper.assertTrue(run(server, asPlayer, "void reset @s") == 0, "non-op cannot reset");

        helper.assertTrue(run(server, asOp, "void essence set @s 500") == 500, "op can set essence");
        helper.assertTrue(data(player).getEssence() == 500, "essence set by op");
        helper.assertTrue(run(server, asOp, "void essence remove @s 900") == 0, "cannot remove more than available");
        helper.assertTrue(data(player).getEssence() == 500, "failed remove changes nothing");
        helper.assertTrue(run(server, asOp, "void evolution set @s mutant") == 2, "op can set tier");
        helper.assertTrue(data(player).getTier() == VoidTier.MUTANT, "tier set by op");
        helper.assertTrue(run(server, asOp, "void evolution set @s godlike") == 0, "unknown tier rejected");
        helper.assertTrue(run(server, asOp, "void debug @s") == 0, "debug disabled by default config");
        helper.assertTrue(run(server, asOp, "void level set @s 8") == 8, "op can set level");
        cleanup(player);
        helper.succeed();
    }

    private static int run(MinecraftServer server, CommandSourceStack source, String command) {
        return server.getCommands().performPrefixedCommand(source, command);
    }

    // ------------------------------------------------------------------ Essence sources

    @GameTest(template = TEMPLATE)
    public static void killRewardsScaleWithHealthAndVoidTag(GameTestHelper helper) {
        LivingEntity zombie = EntityType.ZOMBIE.create(helper.getLevel());
        LivingEntity enderman = EntityType.ENDERMAN.create(helper.getLevel());
        helper.assertTrue(zombie != null && enderman != null, "entities created");
        helper.assertTrue(EssenceSourceEvents.killReward(zombie, helper.getLevel()) == 4, "20 hp / 5 = 4");
        helper.assertTrue(EssenceSourceEvents.killReward(enderman, helper.getLevel()) == 24, "40 hp / 5 * 3 (void creature) = 24");
        zombie.discard();
        enderman.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ Data-driven content

    @GameTest(template = TEMPLATE)
    public static void endCityChestsContainFragmentsAndRespectConfig(GameTestHelper helper) {
        helper.assertTrue(rollEndCityFragments(helper, 60) > 0, "End City chests should contain fragments (60% each)");
        boolean before = VoidConfig.NATURAL_FRAGMENT_SOURCES.get();
        try {
            VoidConfig.NATURAL_FRAGMENT_SOURCES.set(false);
            helper.assertTrue(rollEndCityFragments(helper, 60) == 0, "naturalFragmentSources=false disables loot injection");
        } finally {
            VoidConfig.NATURAL_FRAGMENT_SOURCES.set(before);
        }
        helper.succeed();
    }

    private static int rollEndCityFragments(GameTestHelper helper, int chests) {
        LootTable table = helper.getLevel().getServer().getLootData().getLootTable(BuiltInLootTables.END_CITY_TREASURE);
        int fragments = 0;
        for (int i = 0; i < chests; i++) {
            LootParams params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            for (ItemStack s : table.getRandomItems(params)) {
                if (s.is(ModItems.VOID_FRAGMENT.get())) fragments += s.getCount();
            }
        }
        return fragments;
    }

    @GameTest(template = TEMPLATE)
    public static void voidOreInjectedIntoOverworldOnly(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE)
                .getHolder(ResourceKey.create(Registries.PLACED_FEATURE, VoidAscension.id("void_fragment_ore")));
        helper.assertTrue(placed.isPresent(), "placed feature void_fragment_ore not registered");
        int step = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
        Biome plains = access.registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        var steps = plains.getGenerationSettings().features();
        helper.assertTrue(steps.size() > step && steps.get(step).contains(placed.get()), "ore missing from plains");
        Biome theEnd = access.registryOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_END);
        var endSteps = theEnd.getGenerationSettings().features();
        helper.assertTrue(endSteps.size() <= step || !endSteps.get(step).contains(placed.get()), "ore must not generate in The End");
        helper.succeed();
    }

    private AbilityContentGameTests() {}
}
