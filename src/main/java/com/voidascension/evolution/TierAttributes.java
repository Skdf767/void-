package com.voidascension.evolution;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Applies tier stat bonuses as real attribute modifiers.
 * <ul>
 *     <li>Each bonus uses a fixed, VOID: ASCENSION-owned UUID. {@link #apply} removes exactly that modifier and re-adds
 *     it with the current value, so re-applying never stacks and modifiers of vanilla or other mods are untouched.</li>
 *     <li>Modifiers are <b>transient</b> (not written to the player's NBT): uninstalling the mod leaves no stale bonus
 *     behind. They are re-created from stored progression whenever the player entity is created:
 *     during capability deserialization (before vanilla reads the saved Health, so health above 20 is not clamped),
 *     on clone (death / End return), login, respawn and dimension change.</li>
 *     <li>Raising max health never heals; lowering it clamps current health to the new maximum.</li>
 * </ul>
 */
public final class TierAttributes {

    /** One VOID-owned modifier slot. */
    public record Slot(Attribute attribute, UUID id, String name, AttributeModifier.Operation op,
                       ToDoubleFunction<TierDefinition> value) {}

    public static final UUID HEALTH_ID = UUID.fromString("5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d01");
    public static final UUID SPEED_ID = UUID.fromString("5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d02");
    public static final UUID DAMAGE_ID = UUID.fromString("5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d03");
    public static final UUID ARMOR_ID = UUID.fromString("5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d04");
    public static final UUID TOUGHNESS_ID = UUID.fromString("5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d05");
    public static final UUID KNOCKBACK_ID = UUID.fromString("5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d06");

    public static final List<Slot> SLOTS = List.of(
            new Slot(Attributes.MAX_HEALTH, HEALTH_ID, "void_ascension.tier_health",
                    AttributeModifier.Operation.ADDITION, TierDefinition::bonusHealth),
            new Slot(Attributes.MOVEMENT_SPEED, SPEED_ID, "void_ascension.tier_speed",
                    AttributeModifier.Operation.MULTIPLY_BASE, TierDefinition::bonusSpeed),
            new Slot(Attributes.ATTACK_DAMAGE, DAMAGE_ID, "void_ascension.tier_damage",
                    AttributeModifier.Operation.ADDITION, TierDefinition::bonusDamage),
            new Slot(Attributes.ARMOR, ARMOR_ID, "void_ascension.tier_armor",
                    AttributeModifier.Operation.ADDITION, TierDefinition::bonusArmor),
            new Slot(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "void_ascension.tier_armor_toughness",
                    AttributeModifier.Operation.ADDITION, TierDefinition::bonusToughness),
            new Slot(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "void_ascension.tier_knockback_resistance",
                    AttributeModifier.Operation.ADDITION, TierDefinition::bonusKnockbackResist));

    public static void apply(Player player, VoidTier tier) {
        TierDefinition def = EvolutionTable.get(tier);
        for (Slot slot : SLOTS) {
            AttributeInstance inst = player.getAttribute(slot.attribute());
            if (inst == null) continue;
            double amount = slot.value().applyAsDouble(def);
            AttributeModifier current = inst.getModifier(slot.id());
            if (current != null && current.getAmount() == amount && current.getOperation() == slot.op()) continue;
            inst.removeModifier(slot.id());
            if (amount != 0.0) inst.addTransientModifier(new AttributeModifier(slot.id(), slot.name(), amount, slot.op()));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /** Removes every VOID-owned modifier (and nothing else). */
    public static void clear(Player player) {
        for (Slot slot : SLOTS) {
            AttributeInstance inst = player.getAttribute(slot.attribute());
            if (inst != null) inst.removeModifier(slot.id());
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /** Number of modifiers with our UUID on the attribute (0 or 1). Used by tests to prove no stacking. */
    public static int count(Player player, Attribute attribute, UUID id) {
        AttributeInstance inst = player.getAttribute(attribute);
        return inst == null || inst.getModifier(id) == null ? 0 : 1;
    }

    private TierAttributes() {}
}
