package com.voidascension.ability;

import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.network.AbilityFeedbackPacket;
import com.voidascension.network.VoidNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;

/**
 * Server-authoritative ability lifecycle:
 * <ol>
 *     <li>resolve the ability id</li>
 *     <li>check the player has initialized progression and the ability is unlocked (tier)</li>
 *     <li>check the cooldown</li>
 *     <li>check Essence</li>
 *     <li>validate world state</li>
 *     <li>spend Essence, execute (refund if execution fails), start cooldown, sync</li>
 * </ol>
 * Cooldowns are stored as the server overworld game time at which the ability is ready again, so they survive
 * logout/restart and cannot be reset by relogging or changing dimension.
 */
public final class AbilityService {

    public enum UseResult {
        SUCCESS, UNKNOWN_ABILITY, NOT_INITIALIZED, PASSIVE, LOCKED, ON_COOLDOWN, NOT_ENOUGH_ESSENCE, INVALID_STATE, FAILED;

        public boolean success() { return this == SUCCESS; }
    }

    private static final Map<String, ActiveAbility> ACTIVE = Map.of(
            VoidAbilities.VOID_DASH.id(), new VoidDashAbility());

    public static Optional<ActiveAbility> activeImpl(String id) { return Optional.ofNullable(ACTIVE.get(id)); }

    /** Server game time used for every cooldown (overworld time - identical across dimensions). */
    public static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    public static boolean isUnlocked(VoidPlayerData data, AbilityDefinition def) {
        return data.isInitialized() && data.getTier().isAtLeast(def.unlockTier()) && data.isAbilityUnlocked(def.id());
    }

    /** True if the player has this passive ability unlocked (does not check cooldown). */
    public static boolean hasPassive(Player player, AbilityDefinition def) {
        return isUnlocked(VoidCapabilities.forPlayer(player), def);
    }

    public static long cooldownRemaining(VoidPlayerData data, String id, long now) {
        return Math.max(0L, data.getCooldownReadyAt(id) - now);
    }

    /** Uses an active ability and tells the player the outcome. */
    public static UseResult tryUse(ServerPlayer player, String id) {
        UseResult result = use(player, id);
        if (result != UseResult.SUCCESS) {
            player.displayClientMessage(failureMessage(player, id, result).withStyle(ChatFormatting.RED), true);
            int required = ACTIVE.containsKey(id) ? ACTIVE.get(id).essenceCost() : 0;
            int available = VoidCapabilities.get(player).map(VoidPlayerData::getEssence).orElse(0);
            int cd = (int) Math.min(Integer.MAX_VALUE, VoidCapabilities.get(player)
                    .map(d -> cooldownRemaining(d, id, now(player))).orElse(0L));
            String safeId = VoidAbilities.isKnown(id) ? id : "unknown";
            VoidNetwork.sendFeedback(player, new AbilityFeedbackPacket(safeId, result, required, available, cd));
        }
        return result;
    }

    /** Uses an active ability without player-facing messages (used by tests and by {@link #tryUse}). */
    public static UseResult use(ServerPlayer player, String id) {
        AbilityDefinition def = VoidAbilities.get(id);
        if (def == null) return UseResult.UNKNOWN_ABILITY;
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return UseResult.FAILED;
        VoidPlayerData data = opt.get();
        if (!data.isInitialized()) return UseResult.NOT_INITIALIZED;
        if (!def.isActive()) return UseResult.PASSIVE;
        if (!isUnlocked(data, def)) return UseResult.LOCKED;
        ActiveAbility impl = ACTIVE.get(id);
        if (impl == null) return UseResult.UNKNOWN_ABILITY;

        long now = now(player);
        if (cooldownRemaining(data, id, now) > 0) return UseResult.ON_COOLDOWN;
        int cost = impl.essenceCost();
        if (data.getEssence() < cost) return UseResult.NOT_ENOUGH_ESSENCE;
        if (impl.validateState(player) != null) return UseResult.INVALID_STATE;

        data.setEssence(data.getEssence() - cost);
        boolean ok;
        try {
            ok = impl.execute(player);
        } catch (RuntimeException e) {
            com.voidascension.VoidAscension.LOGGER.error("Ability {} threw for {}", id, player.getGameProfile().getName(), e);
            ok = false;
        }
        if (!ok) {
            data.setEssence(data.getEssence() + cost); // refund
            VoidNetwork.sync(player);
            return UseResult.FAILED;
        }
        data.setCooldownReadyAt(id, now + impl.cooldownTicks());
        VoidNetwork.sync(player);
        return UseResult.SUCCESS;
    }

    /**
     * Shared cooldown/cost handling for event-driven passive abilities (e.g. Void Rescue): if the passive is
     * unlocked, off cooldown and affordable, spends the cost and starts the cooldown.
     *
     * @return true if the passive may fire now
     */
    public static boolean tryConsumePassive(ServerPlayer player, AbilityDefinition def, int cost, int cooldown) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return false;
        VoidPlayerData data = opt.get();
        if (!isUnlocked(data, def)) return false;
        long now = now(player);
        if (cooldownRemaining(data, def.id(), now) > 0 || data.getEssence() < cost) return false;
        data.setEssence(data.getEssence() - cost);
        if (cooldown > 0) data.setCooldownReadyAt(def.id(), now + cooldown);
        VoidNetwork.sync(player);
        return true;
    }

    public static Component failureMessageFor(ServerPlayer player, String id, UseResult result) {
        return failureMessage(player, id, result);
    }

    private static net.minecraft.network.chat.MutableComponent failureMessage(ServerPlayer player, String id, UseResult result) {
        AbilityDefinition def = VoidAbilities.get(id);
        Component name = def == null ? Component.literal(id) : def.displayName();
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        return switch (result) {
            case UNKNOWN_ABILITY -> Component.translatable("message.void_ascension.ability.unknown", id);
            case NOT_INITIALIZED -> Component.translatable("message.void_ascension.not_initialized");
            case PASSIVE -> Component.translatable("message.void_ascension.ability.passive", name);
            case LOCKED -> Component.translatable("message.void_ascension.ability.locked", name,
                    def == null ? "?" : def.unlockTier().displayName());
            case ON_COOLDOWN -> Component.translatable("message.void_ascension.ability.cooldown", name,
                    String.format(java.util.Locale.ROOT, "%.1f", cooldownRemaining(data, id, now(player)) / 20.0));
            case NOT_ENOUGH_ESSENCE -> Component.translatable("message.void_ascension.ability.no_essence", name,
                    ACTIVE.containsKey(id) ? ACTIVE.get(id).essenceCost() : 0, data.getEssence());
            case INVALID_STATE -> Component.translatable("message.void_ascension.ability.invalid_state");
            case FAILED -> Component.translatable("message.void_ascension.ability.failed", name);
            case SUCCESS -> Component.empty();
        };
    }

    private AbilityService() {}
}
