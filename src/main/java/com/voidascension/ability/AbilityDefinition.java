package com.voidascension.ability;

import com.voidascension.evolution.VoidTier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Static metadata of an ability. Behaviour lives elsewhere: active abilities implement {@link ActiveAbility};
 * passive abilities are implemented by event handlers that query {@link AbilityService#hasPassive}.
 *
 * @param id         stable id, e.g. {@code void_dash} (stored in player data)
 * @param type       ACTIVE (triggered by key/command) or PASSIVE (always on once unlocked)
 * @param unlockTier evolution tier that unlocks the ability
 */
public record AbilityDefinition(String id, Type type, VoidTier unlockTier) {
    public enum Type { ACTIVE, PASSIVE }

    public String translationKey() { return "ability.void_ascension." + id; }

    public MutableComponent displayName() { return Component.translatable(translationKey()); }

    public MutableComponent description() { return Component.translatable(translationKey() + ".desc"); }

    public boolean isActive() { return type == Type.ACTIVE; }
}
