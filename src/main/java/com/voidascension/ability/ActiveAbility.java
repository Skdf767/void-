package com.voidascension.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side behaviour of an active ability. {@link AbilityService} handles unlock, cooldown and cost;
 * implementations only validate world state and perform the effect.
 */
public interface ActiveAbility {
    AbilityDefinition definition();

    int essenceCost();

    int cooldownTicks();

    /** @return a reason the ability cannot be used right now, or {@code null} if the state is valid. */
    @Nullable
    Component validateState(ServerPlayer player);

    /** Performs the effect. Called only after validation; returns false if it could not be applied. */
    boolean execute(ServerPlayer player);
}
