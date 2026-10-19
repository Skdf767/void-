package com.voidascension.evolution;

/**
 * Resolved balance values of one tier. All bonuses are TOTALS for that tier (not added to lower tiers).
 *
 * @param requiredLevel        minimum Evolution Level needed to evolve INTO this tier
 * @param essenceCost          Void Essence consumed when evolving INTO this tier
 * @param bonusHealth          max-health bonus (half-hearts), ADDITION
 * @param bonusSpeed           movement-speed bonus as a fraction of base speed (0.05 = +5%), MULTIPLY_BASE
 * @param bonusDamage          attack-damage bonus, ADDITION
 * @param bonusArmor           armor points, ADDITION
 * @param bonusToughness       armor toughness, ADDITION
 * @param bonusKnockbackResist knockback resistance (0..1), ADDITION
 */
public record TierDefinition(VoidTier tier, int requiredLevel, int essenceCost,
                             double bonusHealth, double bonusSpeed, double bonusDamage,
                             double bonusArmor, double bonusToughness, double bonusKnockbackResist) {

    public static TierDefinition none(VoidTier tier) {
        return new TierDefinition(tier, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
