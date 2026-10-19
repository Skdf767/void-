package com.voidascension.config;

import com.voidascension.evolution.VoidTier;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * All tunable values of VOID: ASCENSION.
 * <ul>
 *     <li><b>SERVER</b> spec - gameplay and balance. Stored per world in {@code <world>/serverconfig/void_ascension-server.toml}
 *     and synchronised by Forge to every connecting client, so client screens always show the server's real
 *     requirements. Pack makers can ship defaults in {@code defaultconfigs/}.</li>
 *     <li><b>CLIENT</b> spec - purely cosmetic options in {@code config/void_ascension-client.toml}.</li>
 * </ul>
 * Every numeric value is range-checked by Forge; out-of-range values are corrected to the default with a log message.
 * Read values through {@link #get(ForgeConfigSpec.ConfigValue)} which falls back to defaults before the
 * server config is loaded (e.g. tooltips in the main menu).
 */
public final class VoidConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // ---------------------------------------------------------------- Default progression table (index = tier level)
    public static final int[] DEFAULT_REQUIRED_LEVEL = {0, 3, 8, 15, 25};
    public static final int[] DEFAULT_ESSENCE_COST = {0, 100, 300, 750, 1500};
    public static final double[] DEFAULT_BONUS_HEALTH = {0.0, 2.0, 4.0, 6.0, 10.0};
    public static final double[] DEFAULT_BONUS_SPEED_PERCENT = {0.0, 5.0, 8.0, 12.0, 15.0};
    public static final double[] DEFAULT_BONUS_DAMAGE = {0.0, 0.5, 1.0, 2.0, 3.0};
    public static final double[] DEFAULT_BONUS_ARMOR = {0.0, 1.0, 2.0, 4.0, 6.0};
    public static final double[] DEFAULT_BONUS_TOUGHNESS = {0.0, 0.0, 0.0, 1.0, 2.0};
    public static final double[] DEFAULT_BONUS_KNOCKBACK_RESIST = {0.0, 0.0, 0.05, 0.10, 0.15};

    // ---------------------------------------------------------------- Essence
    public static final ForgeConfigSpec.IntValue FRAGMENT_ESSENCE;
    public static final ForgeConfigSpec.DoubleValue HEALTH_PER_ESSENCE;
    public static final ForgeConfigSpec.DoubleValue VOID_CREATURE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue END_DIMENSION_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue END_PASSIVE_INTERVAL;
    public static final ForgeConfigSpec.IntValue END_PASSIVE_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue ESSENCE_REQUIRES_CARRIED_CORE;
    public static final ForgeConfigSpec.IntValue MAX_ESSENCE;

    // ---------------------------------------------------------------- Evolution Level
    public static final ForgeConfigSpec.DoubleValue XP_BASE;
    public static final ForgeConfigSpec.DoubleValue XP_EXPONENT;
    public static final ForgeConfigSpec.IntValue MAX_LEVEL;

    // ---------------------------------------------------------------- Core
    public static final ForgeConfigSpec.IntValue BIND_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue REBIND_PENALTY_PERCENT;

    // ---------------------------------------------------------------- Evolution
    public static final ForgeConfigSpec.BooleanValue EVOLUTION_REQUIRES_CARRIED_CORE;
    public static final Map<VoidTier, TierValues> TIERS = new EnumMap<>(VoidTier.class);

    // ---------------------------------------------------------------- Abilities
    public static final ForgeConfigSpec.IntValue DASH_ESSENCE_COST;
    public static final ForgeConfigSpec.IntValue DASH_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue DASH_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue DASH_LIFT;
    public static final ForgeConfigSpec.DoubleValue RESILIENCE_FALL_REDUCTION;
    public static final ForgeConfigSpec.IntValue RESCUE_ESSENCE_COST;
    public static final ForgeConfigSpec.IntValue RESCUE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue ASCENDANCE_ESSENCE_BONUS;

    // ---------------------------------------------------------------- Companions (0.2.0)
    public static final ForgeConfigSpec.IntValue COMPANIONS_MAX_ACTIVE_HUMAN;
    public static final ForgeConfigSpec.IntValue COMPANIONS_MAX_ACTIVE_MUTANT;
    public static final ForgeConfigSpec.IntValue COMPANIONS_MAX_ACTIVE_ASCENDED;
    public static final ForgeConfigSpec.IntValue COMPANION_EVOLUTION_COST_AWAKENED;
    public static final ForgeConfigSpec.IntValue COMPANION_EVOLUTION_COST_RESONANT;
    public static final ForgeConfigSpec.IntValue COMPANION_EVOLUTION_COST_ASCENDANT;
    public static final ForgeConfigSpec.DoubleValue COMPANION_ORBIT_RADIUS;
    public static final ForgeConfigSpec.DoubleValue COMPANION_ORBIT_HEIGHT;
    public static final ForgeConfigSpec.IntValue COMPANION_RECOVERY_COOLDOWN_TICKS;

    public static final ForgeConfigSpec.DoubleValue WISP_SIPHON_BONUS_PERCENT;
    public static final ForgeConfigSpec.DoubleValue WISP_SHIELD_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue WISP_PULSE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STALKER_BOLT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STALKER_PHASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ORACLE_NULL_FIELD_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue ORACLE_RESONANCE_CD_REDUCTION;

    // ---------------------------------------------------------------- Loot
    public static final ForgeConfigSpec.BooleanValue NATURAL_FRAGMENT_SOURCES;
    public static final ForgeConfigSpec.DoubleValue FRAGMENT_CHANCE_MULTIPLIER;

    // ---------------------------------------------------------------- Debug
    public static final ForgeConfigSpec.BooleanValue DEBUG_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    // ---------------------------------------------------------------- Client
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ForgeConfigSpec.BooleanValue HUD_SHOW_DORMANT;
    public static final ForgeConfigSpec.BooleanValue HUD_COMPANION_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUD_COMPANION_COMPACT;

    /** Per-tier balance values. */
    public static final class TierValues {
        public final ForgeConfigSpec.IntValue requiredLevel;
        public final ForgeConfigSpec.IntValue essenceCost;
        public final ForgeConfigSpec.DoubleValue bonusHealth;
        public final ForgeConfigSpec.DoubleValue bonusSpeedPercent;
        public final ForgeConfigSpec.DoubleValue bonusAttackDamage;
        public final ForgeConfigSpec.DoubleValue bonusArmor;
        public final ForgeConfigSpec.DoubleValue bonusArmorToughness;
        public final ForgeConfigSpec.DoubleValue bonusKnockbackResistance;

        private TierValues(ForgeConfigSpec.Builder b, VoidTier tier) {
            int i = tier.level();
            b.push(tier.id());
            requiredLevel = b.comment("Minimum Evolution Level required to evolve into " + tier.name() + ".")
                    .defineInRange("requiredLevel", DEFAULT_REQUIRED_LEVEL[i], 0, 1000);
            essenceCost = b.comment("Void Essence consumed when evolving into " + tier.name() + ".")
                    .defineInRange("essenceCost", DEFAULT_ESSENCE_COST[i], 0, 1_000_000_000);
            bonusHealth = b.comment("Total extra max health (half-hearts) while at this tier. Not cumulative with lower tiers.")
                    .defineInRange("bonusHealth", DEFAULT_BONUS_HEALTH[i], 0.0, 40.0);
            bonusSpeedPercent = b.comment("Total movement speed bonus in percent of base speed while at this tier.")
                    .defineInRange("bonusSpeedPercent", DEFAULT_BONUS_SPEED_PERCENT[i], 0.0, 50.0);
            bonusAttackDamage = b.comment("Total flat attack damage bonus while at this tier.")
                    .defineInRange("bonusAttackDamage", DEFAULT_BONUS_DAMAGE[i], 0.0, 20.0);
            bonusArmor = b.comment("Total armor points while at this tier (a full diamond set is 20).")
                    .defineInRange("bonusArmor", DEFAULT_BONUS_ARMOR[i], 0.0, 20.0);
            bonusArmorToughness = b.comment("Total armor toughness while at this tier (a full netherite set is 12).")
                    .defineInRange("bonusArmorToughness", DEFAULT_BONUS_TOUGHNESS[i], 0.0, 12.0);
            bonusKnockbackResistance = b.comment("Total knockback resistance while at this tier (1.0 = immune; netherite set = 0.4).")
                    .defineInRange("bonusKnockbackResistance", DEFAULT_BONUS_KNOCKBACK_RESIST[i], 0.0, 1.0);
            b.pop();
        }
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Void Essence: the spendable progression resource.").push("essence");
        FRAGMENT_ESSENCE = b.comment("Essence gained when absorbing one Void Fragment.")
                .defineInRange("fragmentEssence", 25, 0, 100_000);
        HEALTH_PER_ESSENCE = b.comment("Kills grant 1 Essence per this many points of the victim's max health (minimum 1).")
                .defineInRange("healthPerEssence", 5.0, 0.1, 10_000.0);
        VOID_CREATURE_MULTIPLIER = b.comment("Kill reward multiplier for #void_ascension:void_creatures.")
                .defineInRange("voidCreatureMultiplier", 3.0, 0.0, 100.0);
        END_DIMENSION_MULTIPLIER = b.comment("Kill reward multiplier inside The End.")
                .defineInRange("endDimensionMultiplier", 2.0, 0.0, 100.0);
        END_PASSIVE_INTERVAL = b.comment("Ticks between passive Essence trickles while in The End (0 disables).")
                .defineInRange("endPassiveInterval", 600, 0, 1_000_000);
        END_PASSIVE_AMOUNT = b.comment("Essence per passive trickle in The End.")
                .defineInRange("endPassiveAmount", 1, 0, 10_000);
        ESSENCE_REQUIRES_CARRIED_CORE = b.comment("If true, gameplay Essence (kills, fragments, End trickle) is only gained",
                        "while carrying your active bound Void Core. Progression itself never depends on the item.")
                .define("requireCarriedCore", true);
        MAX_ESSENCE = b.comment("Upper bound for stored Essence.")
                .defineInRange("maxEssence", 1_000_000_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.comment("Evolution Level: grows with lifetime Void Experience (1 XP per Essence gained through gameplay).",
                "Total XP for level L = floor(xpBase * L ^ xpExponent).").push("level");
        XP_BASE = b.defineInRange("xpBase", 30.0, 1.0, 100_000.0);
        XP_EXPONENT = b.defineInRange("xpExponent", 1.5, 1.0, 4.0);
        MAX_LEVEL = b.defineInRange("maxLevel", 100, 1, 1000);
        b.pop();

        b.comment("Void Core binding.").push("core");
        BIND_DURATION_TICKS = b.comment("Ticks right-click must be held to bind a Core.")
                .defineInRange("bindDurationTicks", 60, 1, 1200);
        REBIND_PENALTY_PERCENT = b.comment("Percent of stored Essence lost when transferring your bond to a new Core.")
                .defineInRange("rebindPenaltyPercent", 25, 0, 100);
        b.pop();

        b.comment("Evolution tiers. HUMAN is the starting tier and has no values.").push("evolution");
        EVOLUTION_REQUIRES_CARRIED_CORE = b.comment("If true, evolving requires carrying your active bound Void Core.")
                .define("requireCarriedCore", true);
        for (VoidTier tier : VoidTier.values()) {
            if (tier != VoidTier.HUMAN) TIERS.put(tier, new TierValues(b, tier));
        }
        b.pop();

        b.comment("Abilities.").push("abilities");
        b.push("void_dash");
        DASH_ESSENCE_COST = b.defineInRange("essenceCost", 10, 0, 100_000);
        DASH_COOLDOWN_TICKS = b.defineInRange("cooldownTicks", 100, 1, 72_000);
        DASH_STRENGTH = b.comment("Horizontal launch velocity (blocks/tick). Movement is physics-based and collides with blocks.")
                .defineInRange("strength", 1.4, 0.1, 3.0);
        DASH_LIFT = b.comment("Upward velocity added by the dash.").defineInRange("lift", 0.2, 0.0, 1.0);
        b.pop();
        b.push("void_resilience");
        RESILIENCE_FALL_REDUCTION = b.comment("Fraction of fall damage negated.")
                .defineInRange("fallDamageReduction", 0.3, 0.0, 0.9);
        b.pop();
        b.push("void_rescue");
        RESCUE_ESSENCE_COST = b.defineInRange("essenceCost", 100, 0, 100_000);
        RESCUE_COOLDOWN_TICKS = b.defineInRange("cooldownTicks", 1200, 0, 72_000);
        b.pop();
        b.push("void_ascendance");
        ASCENDANCE_ESSENCE_BONUS = b.comment("Extra fraction of gameplay Essence gained.")
                .defineInRange("essenceGainBonus", 0.25, 0.0, 1.0);
        b.pop();
        b.pop();

        b.comment("Void Companions (0.2.0): supernatural floating energy entities bound to the player.").push("companions");
        COMPANIONS_MAX_ACTIVE_HUMAN = b.comment("Active companion limit for HUMAN and ENHANCED tiers.")
                .defineInRange("maxActiveHuman", 1, 1, 3);
        COMPANIONS_MAX_ACTIVE_MUTANT = b.comment("Active companion limit for MUTANT and VOIDBORN tiers.")
                .defineInRange("maxActiveMutant", 2, 1, 3);
        COMPANIONS_MAX_ACTIVE_ASCENDED = b.comment("Active companion limit for ASCENDED tier.")
                .defineInRange("maxActiveAscended", 3, 1, 3);

        COMPANION_EVOLUTION_COST_AWAKENED = b.comment("Essence required to evolve a companion from Dormant to Awakened.")
                .defineInRange("evolutionCostAwakened", 150, 0, 100_000);
        COMPANION_EVOLUTION_COST_RESONANT = b.comment("Essence required to evolve a companion from Awakened to Resonant.")
                .defineInRange("evolutionCostResonant", 400, 0, 100_000);
        COMPANION_EVOLUTION_COST_ASCENDANT = b.comment("Essence required to evolve a companion from Resonant to Ascendant.")
                .defineInRange("evolutionCostAscendant", 900, 0, 100_000);

        COMPANION_ORBIT_RADIUS = b.comment("Horizontal distance (blocks) at which companions orbit their owner.")
                .defineInRange("orbitRadius", 2.2, 1.0, 5.0);
        COMPANION_ORBIT_HEIGHT = b.comment("Height offset (blocks above owner feet) for companion orbit.")
                .defineInRange("orbitHeight", 1.4, 0.5, 3.0);
        COMPANION_RECOVERY_COOLDOWN_TICKS = b.comment("Ticks a defeated companion stays in recovery before returning.")
                .defineInRange("recoveryCooldownTicks", 600, 20, 72_000);

        WISP_SIPHON_BONUS_PERCENT = b.comment("Percent bonus Essence granted by Abyssal Wisp's Void Siphon on eligible kills.")
                .defineInRange("wispSiphonBonusPercent", 25.0, 0.0, 200.0);
        WISP_SHIELD_AMOUNT = b.comment("Damage absorption granted by Abyssal Wisp's Abyss Shield.")
                .defineInRange("wispShieldAmount", 8.0, 1.0, 40.0);
        WISP_PULSE_DAMAGE = b.comment("Direct void damage dealt by Abyssal Wisp's Abyss Pulse.")
                .defineInRange("wispPulseDamage", 8.0, 1.0, 50.0);

        STALKER_BOLT_DAMAGE = b.comment("Damage dealt by Rift Stalker's Rift Bolt projectile.")
                .defineInRange("stalkerBoltDamage", 7.0, 1.0, 50.0);
        STALKER_PHASE_DAMAGE = b.comment("Damage dealt by Rift Stalker's Phase Strike.")
                .defineInRange("stalkerPhaseDamage", 14.0, 1.0, 100.0);

        ORACLE_NULL_FIELD_REDUCTION = b.comment("Damage reduction fraction provided by Null Oracle's Null Field.")
                .defineInRange("oracleNullFieldReduction", 0.40, 0.05, 0.85);
        ORACLE_RESONANCE_CD_REDUCTION = b.comment("Cooldown reduction fraction granted by Null Oracle's Resonance Link.")
                .defineInRange("oracleResonanceCdReduction", 0.35, 0.05, 0.75);
        b.pop();

        b.comment("Natural Void Fragment sources (loot modifiers). Ore generation is configured via data pack.").push("loot");
        NATURAL_FRAGMENT_SOURCES = b.define("naturalFragmentSources", true);
        FRAGMENT_CHANCE_MULTIPLIER = b.comment("Multiplies every fragment drop chance (result capped at 100%).")
                .defineInRange("fragmentChanceMultiplier", 1.0, 0.0, 10.0);
        b.pop();

        b.push("debug");
        DEBUG_COMMANDS = b.comment("Enables /void debug (still requires permission level 2).")
                .define("enableDebugCommands", false);
        DEBUG_LOGGING = b.comment("Logs progression changes at INFO level.").define("debugLogging", false);
        b.pop();
        SERVER_SPEC = b.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.push("hud");
        HUD_ENABLED = c.comment("Show the permanent Void HUD panel (tier, level, Essence, next evolution, cooldowns).")
                .define("enabled", true);
        HUD_ANCHOR = c.comment("Screen corner the HUD is anchored to.").defineEnum("anchor", HudAnchor.BOTTOM_RIGHT);
        HUD_OFFSET_X = c.comment("Horizontal distance (GUI pixels) from the anchored screen edge.")
                .defineInRange("offsetX", 4, 0, 4000);
        HUD_OFFSET_Y = c.comment("Vertical distance (GUI pixels) from the anchored screen edge. When anchored to the bottom and the",
                        "panel would overlap the hotbar / health / food bars (narrow windows), it is lifted above them.")
                .defineInRange("offsetY", 4, 0, 4000);
        HUD_SHOW_DORMANT = c.comment("Before your Void Core is activated, show a small 'Core dormant' hint instead of nothing.")
                .define("showDormantHint", true);
        HUD_COMPANION_ENABLED = c.comment("Show the companion HUD info panel.")
                .define("showCompanionInfo", true);
        HUD_COMPANION_COMPACT = c.comment("Use a more compact companion HUD display.")
                .define("compactCompanionInfo", false);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    /** Server value, or its default if the server config is not loaded (main menu, early init). */
    public static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
        return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** Client value, or its default if the client config is not loaded. */
    public static <T> T client(ForgeConfigSpec.ConfigValue<T> value) {
        return CLIENT_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private VoidConfig() {}
}
