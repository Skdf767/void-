package com.voidascension.evolution;

import com.voidascension.VoidAscension;
import com.voidascension.config.VoidConfig;

/**
 * Single source of truth for tier balance values. Reads the server config; HUMAN always has no requirements
 * and no bonuses. Gameplay code, commands, the GUI and tests all resolve tiers through this class.
 */
public final class EvolutionTable {

    public static TierDefinition get(VoidTier tier) {
        VoidConfig.TierValues v = VoidConfig.TIERS.get(tier);
        if (v == null) return TierDefinition.none(tier);
        return new TierDefinition(tier,
                VoidConfig.get(v.requiredLevel),
                VoidConfig.get(v.essenceCost),
                VoidConfig.get(v.bonusHealth),
                VoidConfig.get(v.bonusSpeedPercent) / 100.0,
                VoidConfig.get(v.bonusAttackDamage),
                VoidConfig.get(v.bonusArmor),
                VoidConfig.get(v.bonusArmorToughness),
                VoidConfig.get(v.bonusKnockbackResistance));
    }

    /**
     * Logs a warning if the configured table is not monotonic (a higher tier requiring a lower level or giving
     * smaller bonuses). Such configs are allowed - they may be intentional - but are usually a mistake.
     */
    public static void validateConfiguredTable() {
        TierDefinition prev = get(VoidTier.HUMAN);
        for (VoidTier tier : VoidTier.values()) {
            if (tier == VoidTier.HUMAN) continue;
            TierDefinition cur = get(tier);
            if (cur.requiredLevel() < prev.requiredLevel()) {
                VoidAscension.LOGGER.warn("[config] Tier {} requires a lower Evolution Level ({}) than {} ({}).",
                        tier, cur.requiredLevel(), prev.tier(), prev.requiredLevel());
            }
            if (cur.bonusHealth() < prev.bonusHealth() || cur.bonusSpeed() < prev.bonusSpeed()
                    || cur.bonusDamage() < prev.bonusDamage() || cur.bonusArmor() < prev.bonusArmor()
                    || cur.bonusToughness() < prev.bonusToughness() || cur.bonusKnockbackResist() < prev.bonusKnockbackResist()) {
                VoidAscension.LOGGER.warn("[config] Tier {} grants smaller bonuses than {} - bonuses are totals, not increments.",
                        tier, prev.tier());
            }
            prev = cur;
        }
    }

    private EvolutionTable() {}
}
