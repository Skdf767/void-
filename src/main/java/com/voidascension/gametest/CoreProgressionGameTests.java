package com.voidascension.gametest;

import com.voidascension.VoidAscension;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.api.event.VoidEssenceGainEvent;
import com.voidascension.core.CoreService;
import com.voidascension.core.CoreService.BindResult;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.EvolutionService;
import com.voidascension.evolution.TierAttributes;
import com.voidascension.evolution.VoidTier;
import com.voidascension.item.VoidCoreItem;
import com.voidascension.progression.ProgressionService;
import com.voidascension.progression.VoidEssenceService;
import com.voidascension.progression.VoidEssenceService.Result;
import com.voidascension.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.UUID;

import static com.voidascension.gametest.TestSupport.*;

/**
 * In-game tests for crafting, activation, essence, evolution and persistence.
 * Run headless with {@code ./gradlew runGameTestServer} (or {@code /test runall} in a dev world).
 */
@GameTestHolder(VoidAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreProgressionGameTests {
    private static final String CAP_KEY = VoidCapabilities.KEY.toString();

    // ------------------------------------------------------------------ Recipe

    @GameTest(template = TEMPLATE)
    public static void recipeRequiresExactlyFivePlusShape(GameTestHelper helper) {
        Recipe<?> raw = helper.getLevel().getRecipeManager().byKey(VoidAscension.id("void_core")).orElse(null);
        helper.assertTrue(raw instanceof CraftingRecipe, "void_core crafting recipe is not loaded");
        CraftingRecipe recipe = (CraftingRecipe) raw;
        ItemStack fragment = new ItemStack(ModItems.VOID_FRAGMENT.get());

        TransientCraftingContainer grid = new TransientCraftingContainer(dummyMenu(), 3, 3);
        for (int slot : new int[]{1, 3, 4, 5, 7}) grid.setItem(slot, fragment.copy());
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "plus shape of 5 fragments should match");
        ItemStack result = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModItems.VOID_CORE.get()) && result.getCount() == 1, "result should be 1 Void Core");
        helper.assertTrue(!VoidCoreItem.isBound(result), "a crafted Core must be unbound");

        grid.setItem(7, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "4 fragments must not match");
        grid.setItem(7, new ItemStack(Items.ENDER_PEARL));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "a wrong ingredient must not match");
        grid.setItem(7, fragment.copy());
        grid.setItem(0, fragment.copy());
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "6 fragments must not match");

        TransientCraftingContainer line = new TransientCraftingContainer(dummyMenu(), 3, 3);
        for (int slot : new int[]{0, 1, 2, 3, 4}) line.setItem(slot, fragment.copy());
        helper.assertTrue(!recipe.matches(line, helper.getLevel()), "5 fragments in the wrong shape must not match");
        helper.succeed();
    }

    // ------------------------------------------------------------------ Activation

    @GameTest(template = TEMPLATE)
    public static void firstActivationInitializesProgression(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(!data(player).isInitialized(), "players start uninitialized");
        ItemStack core = new ItemStack(ModItems.VOID_CORE.get());

        helper.assertTrue(CoreService.bind(player, core, false) == BindResult.INITIALIZED, "first activation initializes");
        VoidPlayerData d = data(player);
        helper.assertTrue(d.isInitialized(), "initialized flag set");
        helper.assertTrue(d.getTier() == VoidTier.HUMAN, "VoidEvolutionTier = HUMAN (0)");
        helper.assertTrue(d.getEssence() == 0, "VoidEssence = 0");
        helper.assertTrue(d.getEvolutionLevel() == 0, "EvolutionLevel = 0");
        helper.assertTrue(d.getUnlockedAbilities().isEmpty(), "HUMAN unlocks no abilities");
        helper.assertTrue(player.getUUID().equals(d.getOwner()), "owner recorded");
        helper.assertTrue(VoidCoreItem.isBound(core) && player.getUUID().equals(VoidCoreItem.getOwner(core)), "core carries bond");
        helper.assertTrue(VoidCoreItem.getCoreId(core).equals(d.getCoreId()), "player references the core id");
        helper.assertTrue(CoreService.bind(player, core, true) == BindResult.ALREADY_ACTIVE, "re-activating the same core is a no-op");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repeatedActivationNeverResetsProgression(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.MUTANT);
        ProgressionService.setLevel(player, 10);
        VoidEssenceService.setVoidEssence(player, 400);
        long xp = data(player).getVoidExperience();

        ItemStack second = new ItemStack(ModItems.VOID_CORE.get());
        helper.assertTrue(CoreService.bind(player, second, false) == BindResult.NEEDS_SNEAK_TO_TRANSFER, "transfer needs sneak");
        helper.assertTrue(!VoidCoreItem.isBound(second), "refused core stays unbound");
        helper.assertTrue(data(player).getEssence() == 400, "refusal costs nothing");

        helper.assertTrue(CoreService.bind(player, second, true) == BindResult.TRANSFERRED, "sneak transfer works");
        VoidPlayerData d = data(player);
        helper.assertTrue(d.getTier() == VoidTier.MUTANT, "tier kept on transfer");
        helper.assertTrue(d.getVoidExperience() == xp && d.getEvolutionLevel() == 10, "level/xp kept on transfer");
        helper.assertTrue(d.getEssence() == 300, "25% transfer penalty, got " + d.getEssence());

        // Admin unbind (e.g. lost core) -> next activation is free and keeps everything.
        helper.assertTrue(CoreService.unbind(player), "unbind succeeds");
        helper.assertTrue(d.isInitialized() && d.getTier() == VoidTier.MUTANT, "unbind keeps progression");
        ItemStack third = new ItemStack(ModItems.VOID_CORE.get());
        helper.assertTrue(CoreService.bind(player, third, false) == BindResult.BOUND, "re-activation after unbind");
        helper.assertTrue(d.getEssence() == 300 && d.getTier() == VoidTier.MUTANT, "no penalty and no reset after unbind");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unauthorizedPlayerCannotUseOthersCore(GameTestHelper helper) {
        ServerPlayer owner = newPlayer(helper);
        ServerPlayer thief = newPlayer(helper);
        ItemStack core = new ItemStack(ModItems.VOID_CORE.get());
        CoreService.bind(owner, core, false);

        helper.assertTrue(CoreService.bind(thief, core, true) == BindResult.OWNED_BY_OTHER, "other players are rejected");
        helper.assertTrue(!data(thief).isInitialized() && !data(thief).hasBoundCore(), "thief gains nothing");
        helper.assertTrue(owner.getUUID().equals(VoidCoreItem.getOwner(core)), "bond unchanged");
        thief.getInventory().add(core);
        helper.assertTrue(!CoreService.hasActiveCore(thief), "carrying someone else's core is not an active core");
        cleanup(owner, thief);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void losingCoreKeepsProgressionButGatesGains(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.ENHANCED);
        VoidEssenceService.setVoidEssence(player, 500);
        ProgressionService.setLevel(player, 20);

        player.getInventory().clearContent(); // core lost / destroyed
        VoidPlayerData d = data(player);
        helper.assertTrue(d.getTier() == VoidTier.ENHANCED && d.getEssence() == 500, "progression is stored on the player");
        helper.assertTrue(!CoreService.hasActiveCore(player), "no active core carried");
        helper.assertTrue(VoidEssenceService.grantFromGameplay(player, 10, VoidEssenceGainEvent.Source.OTHER) == 0,
                "gameplay essence requires the carried core");
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.NO_ACTIVE_CORE, "evolution requires the core");
        helper.assertTrue(d.getTier() == VoidTier.ENHANCED && d.getEssence() == 500, "failed evolution changes nothing");
        cleanup(player);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Essence

    @GameTest(template = TEMPLATE)
    public static void essenceApiValidatesInput(GameTestHelper helper) {
        ServerPlayer fresh = newPlayer(helper);
        helper.assertTrue(VoidEssenceService.addVoidEssence(fresh, 10) == Result.NOT_INITIALIZED, "uninitialized players are rejected");

        ServerPlayer player = activatedPlayer(helper);
        helper.assertTrue(VoidEssenceService.addVoidEssence(player, 0) == Result.INVALID_AMOUNT, "add 0 is invalid");
        helper.assertTrue(VoidEssenceService.addVoidEssence(player, -5) == Result.INVALID_AMOUNT, "negative add is invalid");
        helper.assertTrue(VoidEssenceService.addVoidEssence(player, 120) == Result.SUCCESS, "add works");
        helper.assertTrue(VoidEssenceService.getVoidEssence(player) == 120, "120 stored");
        helper.assertTrue(data(player).getVoidExperience() == 0, "direct add grants no Void Experience");
        helper.assertTrue(VoidEssenceService.hasVoidEssence(player, 120) && !VoidEssenceService.hasVoidEssence(player, 121), "has");
        helper.assertTrue(!VoidEssenceService.hasVoidEssence(player, -1), "has(negative) is false");
        helper.assertTrue(VoidEssenceService.removeVoidEssence(player, 200) == Result.INSUFFICIENT_ESSENCE, "cannot go negative");
        helper.assertTrue(VoidEssenceService.getVoidEssence(player) == 120, "failed remove changes nothing");
        helper.assertTrue(VoidEssenceService.removeVoidEssence(player, 20) == Result.SUCCESS, "remove works");
        helper.assertTrue(VoidEssenceService.setVoidEssence(player, -1) == Result.INVALID_AMOUNT, "negative set is invalid");
        helper.assertTrue(VoidEssenceService.setVoidEssence(player, Integer.MAX_VALUE) == Result.SUCCESS, "huge set is clamped");
        helper.assertTrue(VoidEssenceService.getVoidEssence(player) == VoidEssenceService.maxEssence(), "clamped to max");
        helper.assertTrue(VoidEssenceService.addVoidEssence(player, 1000) == Result.SUCCESS
                && VoidEssenceService.getVoidEssence(player) == VoidEssenceService.maxEssence(), "add cannot overflow");
        cleanup(fresh, player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void gameplayEssenceGrantsExperienceAndLevels(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        int gained = VoidEssenceService.grantFromGameplay(player, 30, VoidEssenceGainEvent.Source.KILL);
        VoidPlayerData d = data(player);
        helper.assertTrue(gained == 30 && d.getEssence() == 30, "30 essence granted, got " + gained);
        helper.assertTrue(d.getVoidExperience() == 30, "same amount of Void Experience");
        helper.assertTrue(d.getEvolutionLevel() == 1, "30 XP = level 1, got " + d.getEvolutionLevel());
        VoidEssenceService.grantFromGameplay(player, 125, VoidEssenceGainEvent.Source.KILL);
        helper.assertTrue(d.getEvolutionLevel() == 3, "155 XP = level 3, got " + d.getEvolutionLevel());
        VoidEssenceService.removeVoidEssence(player, 155);
        helper.assertTrue(d.getEvolutionLevel() == 3 && d.getVoidExperience() == 155, "spending never lowers level");
        helper.assertTrue(VoidEssenceService.grantFromGameplay(player, -4, VoidEssenceGainEvent.Source.KILL) == 0, "negative grant ignored");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void absorbingFragmentsGrantsEssence(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOID_FRAGMENT.get(), 3));
        player.getCooldowns().removeCooldown(ModItems.VOID_FRAGMENT.get());
        ModItems.VOID_FRAGMENT.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(data(player).getEssence() == 25, "one fragment = 25 essence, got " + data(player).getEssence());
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "one fragment consumed");

        ServerPlayer uninitialized = newPlayer(helper);
        uninitialized.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOID_FRAGMENT.get(), 3));
        ModItems.VOID_FRAGMENT.get().use(helper.getLevel(), uninitialized, InteractionHand.MAIN_HAND);
        helper.assertTrue(uninitialized.getMainHandItem().getCount() == 3, "cannot absorb before activation");
        cleanup(player, uninitialized);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Evolution

    @GameTest(template = TEMPLATE)
    public static void evolutionIsTransactional(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        VoidPlayerData d = data(player);
        VoidEssenceService.setVoidEssence(player, 1000);
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.LEVEL_TOO_LOW, "level 0 < 3");
        helper.assertTrue(d.getTier() == VoidTier.HUMAN && d.getEssence() == 1000, "nothing changed");

        ProgressionService.setLevel(player, 3);
        VoidEssenceService.setVoidEssence(player, 99);
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.NOT_ENOUGH_ESSENCE, "99 < 100");
        helper.assertTrue(d.getTier() == VoidTier.HUMAN && d.getEssence() == 99, "nothing changed");

        VoidEssenceService.setVoidEssence(player, 150);
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.SUCCESS, "evolves");
        helper.assertTrue(d.getTier() == VoidTier.ENHANCED, "now ENHANCED");
        helper.assertTrue(d.getEssence() == 50, "cost 100 spent once");
        helper.assertTrue(d.isAbilityUnlocked(VoidAbilities.VOID_DASH.id()), "void_dash unlocked");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == 22.0, "ENHANCED: +2 max health");

        // Duplicate request (double click / packet spam) cannot evolve twice or spend twice.
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.LEVEL_TOO_LOW, "MUTANT needs level 8");
        helper.assertTrue(d.getTier() == VoidTier.ENHANCED && d.getEssence() == 50, "no duplicate evolution");

        EvolutionService.setTier(player, VoidTier.ASCENDED);
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.MAX_TIER, "ASCENDED is max");
        helper.assertTrue(d.getUnlockedAbilities().equals(VoidAbilities.ids()), "all abilities unlocked at ASCENDED");

        EvolutionService.setTier(player, VoidTier.HUMAN);
        helper.assertTrue(d.getUnlockedAbilities().isEmpty(), "lowering the tier re-locks abilities");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == 20.0, "HUMAN has no bonus");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void uninitializedPlayerCannotEvolve(GameTestHelper helper) {
        ServerPlayer player = newPlayer(helper);
        helper.assertTrue(EvolutionService.tryAdvance(player) == EvolutionService.Result.NOT_INITIALIZED, "must activate a core first");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void attributeModifiersNeverStack(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        for (int i = 0; i < 5; i++) {
            EvolutionService.setTier(player, VoidTier.ASCENDED);
            TierAttributes.apply(player, VoidTier.ASCENDED);
            ProgressionService.revalidate(player);
        }
        // Expected values come from the live config, so the test does not depend on shipped defaults.
        var def = com.voidascension.evolution.EvolutionTable.get(VoidTier.ASCENDED);
        double hp = 20.0 + def.bonusHealth(), speed = 0.1 * (1 + def.bonusSpeed());
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == hp,
                "ASCENDED: exactly +" + def.bonusHealth() + " max health once, got " + player.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1e-6,
                "speed bonus applied once: expected " + speed + " got " + player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1.0 + def.bonusDamage(),
                "damage bonus applied once, got " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        for (TierAttributes.Slot slot : TierAttributes.SLOTS) {
            long ours = player.getAttribute(slot.attribute()).getModifiers().stream().filter(m -> m.getId().equals(slot.id())).count();
            helper.assertTrue(ours == 1, slot.name() + ": exactly one modifier after 5x re-apply, got " + ours);
        }

        // Save/load (relog) and re-validate (login) - still exactly one modifier.
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer reloaded = newPlayer(helper);
        reloaded.load(saved);
        ProgressionService.revalidate(reloaded);
        ProgressionService.revalidate(reloaded);
        helper.assertTrue(reloaded.getAttributeValue(Attributes.MAX_HEALTH) == 30.0,
                "after reload: 30, got " + reloaded.getAttributeValue(Attributes.MAX_HEALTH));
        cleanup(player, reloaded);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Persistence

    @GameTest(template = TEMPLATE)
    public static void progressionPersistsInPlayerSave(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.VOIDBORN);
        ProgressionService.setLevel(player, 16);
        VoidEssenceService.setVoidEssence(player, 1234);
        data(player).setCooldownReadyAt(VoidAbilities.VOID_RESCUE.id(), 999_999L);
        UUID coreId = data(player).getCoreId();

        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        CompoundTag capTag = saved.getCompound("ForgeCaps").getCompound(CAP_KEY);
        helper.assertTrue(capTag.getInt("DataVersion") == 2, "data version written");
        helper.assertTrue(capTag.getInt("Essence") == 1234, "essence written to player NBT");

        ServerPlayer reloaded = newPlayer(helper);
        reloaded.load(saved);
        VoidPlayerData d = data(reloaded);
        helper.assertTrue(d.isInitialized(), "initialized restored");
        helper.assertTrue(d.getEssence() == 1234, "essence restored");
        helper.assertTrue(d.getTier() == VoidTier.VOIDBORN, "tier restored");
        helper.assertTrue(d.getEvolutionLevel() == 16, "level restored");
        helper.assertTrue(coreId.equals(d.getCoreId()), "core bond restored");
        helper.assertTrue(d.getUnlockedAbilities().equals(Set.of("void_dash", "void_resilience", "void_rescue")), "abilities restored");
        helper.assertTrue(d.getCooldownReadyAt(VoidAbilities.VOID_RESCUE.id()) == 999_999L, "cooldown restored");
        cleanup(player, reloaded);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void progressionSurvivesDeath(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        EvolutionService.setTier(player, VoidTier.MUTANT);
        VoidEssenceService.setVoidEssence(player, 777);
        UUID coreId = data(player).getCoreId();

        // What PlayerList.respawn does on death: old entity removed (caps invalidated), new ServerPlayer
        // created, PlayerEvent.Clone(wasDeath=true) fired, then PlayerRespawnEvent.
        player.invalidateCaps();
        ServerPlayer respawned = newPlayer(helper);
        helper.assertTrue(!data(respawned).isInitialized(), "fresh entity starts empty before cloning");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(respawned, player, true));
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        VoidPlayerData after = data(respawned);
        helper.assertTrue(after.getEssence() == 777, "essence survives death, got " + after.getEssence());
        helper.assertTrue(coreId.equals(after.getCoreId()), "core bond survives death");
        helper.assertTrue(after.getTier() == VoidTier.MUTANT, "tier survives death");
        helper.assertTrue(respawned.getAttributeValue(Attributes.MAX_HEALTH) == 24.0,
                "MUTANT health bonus re-applied once, got " + respawned.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(respawned.getHealth() == 24.0F, "respawn at full (boosted) health");
        cleanup(respawned);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playersAreIsolated(GameTestHelper helper) {
        ServerPlayer a = activatedPlayer(helper);
        ServerPlayer b = activatedPlayer(helper);
        VoidEssenceService.addVoidEssence(a, 500);
        EvolutionService.setTier(a, VoidTier.VOIDBORN);
        helper.assertTrue(data(b).getEssence() == 0 && data(b).getTier() == VoidTier.HUMAN, "B unaffected by A");
        helper.assertTrue(b.getAttributeValue(Attributes.MAX_HEALTH) == 20.0, "B has no bonus");
        helper.assertTrue(!data(a).getCoreId().equals(data(b).getCoreId()), "distinct cores");
        cleanup(a, b);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void malformedSaveDataIsSanitized(GameTestHelper helper) {
        ServerPlayer donor = activatedPlayer(helper);
        CompoundTag saved = donor.saveWithoutId(new CompoundTag());
        CompoundTag cap = saved.getCompound("ForgeCaps").getCompound(CAP_KEY);
        cap.putInt("Tier", 42);
        cap.putInt("Essence", -900);
        cap.putString("EvolutionLevel", "banana");
        cap.getList("Abilities", 8).add(net.minecraft.nbt.StringTag.valueOf("infinite_power"));

        ServerPlayer reloaded = newPlayer(helper);
        reloaded.load(saved);
        VoidPlayerData d = data(reloaded);
        helper.assertTrue(d.getTier() == VoidTier.HUMAN, "invalid tier falls back to HUMAN");
        helper.assertTrue(d.getEssence() == 0, "negative essence reset");
        helper.assertTrue(d.getEvolutionLevel() == 0, "wrong-typed level reset");
        helper.assertTrue(!d.isAbilityUnlocked("infinite_power"), "unknown ability dropped");
        cleanup(donor, reloaded);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void legacyV1SaveIsMigrated(GameTestHelper helper) {
        ServerPlayer donor = newPlayer(helper);
        CompoundTag saved = donor.saveWithoutId(new CompoundTag());
        CompoundTag v1 = new CompoundTag();
        v1.putInt("Essence", 640);
        v1.putInt("LifetimeEssence", 1800);
        v1.putInt("Tier", 4); // 0.1.0 Voidborn
        v1.putUUID("CoreId", UUID.randomUUID());
        saved.getCompound("ForgeCaps").put(CAP_KEY, v1);

        ServerPlayer reloaded = newPlayer(helper);
        reloaded.load(saved);
        ProgressionService.revalidate(reloaded);
        VoidPlayerData d = data(reloaded);
        helper.assertTrue(d.isInitialized(), "migrated player initialized");
        helper.assertTrue(d.getTier() == VoidTier.VOIDBORN, "old tier 4 -> VOIDBORN, got " + d.getTier());
        helper.assertTrue(d.getEssence() == 640, "essence kept");
        helper.assertTrue(d.getVoidExperience() == 1800, "lifetime essence -> Void XP");
        helper.assertTrue(d.getEvolutionLevel() == ProgressionService.levelForXp(1800), "level derived on login");
        helper.assertTrue(d.isAbilityUnlocked(VoidAbilities.VOID_RESCUE.id()), "tier abilities unlocked after migration");
        helper.assertTrue(reloaded.getUUID().equals(d.getOwner()), "owner filled in on login");
        cleanup(donor, reloaded);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void adminResetAndLevelSet(GameTestHelper helper) {
        ServerPlayer player = activatedPlayer(helper);
        helper.assertTrue(ProgressionService.setLevel(player, 15) == 15, "level set");
        helper.assertTrue(data(player).getVoidExperience() == ProgressionService.totalXpForLevel(15), "xp = threshold");
        helper.assertTrue(ProgressionService.setLevel(player, 5000) == ProgressionService.maxLevel(), "level clamped");
        EvolutionService.setTier(player, VoidTier.ASCENDED);
        ProgressionService.reset(player);
        VoidPlayerData d = data(player);
        helper.assertTrue(!d.isInitialized() && d.getTier() == VoidTier.HUMAN && d.getEssence() == 0
                && d.getEvolutionLevel() == 0 && !d.hasBoundCore(), "reset wipes progression");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == 20.0, "reset removes bonuses");
        cleanup(player);
        helper.succeed();
    }

    private CoreProgressionGameTests() {}
}
