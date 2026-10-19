package com.voidascension.progression;

import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.api.event.VoidEssenceGainEvent;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.network.VoidNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

/**
 * Central Void Essence API. All Essence changes go through this class; every mutation validates its input,
 * reports a {@link Result}, never produces negative values and syncs the client.
 * <ul>
 *     <li>{@code add/remove/setVoidEssence} - direct changes (admin, costs). No Void Experience is granted.</li>
 *     <li>{@link #grantFromGameplay} - gameplay income (kills, fragments, End). Fires {@link VoidEssenceGainEvent},
 *     applies Void Ascendance and grants the same amount as Void Experience.</li>
 * </ul>
 */
public final class VoidEssenceService {

    public enum Result {
        SUCCESS, INVALID_AMOUNT, INSUFFICIENT_ESSENCE, NOT_INITIALIZED, NO_DATA;

        public boolean success() { return this == SUCCESS; }
    }

    public static int maxEssence() { return VoidConfig.get(VoidConfig.MAX_ESSENCE); }

    public static int getVoidEssence(Player player) {
        return VoidCapabilities.forPlayer(player).getEssence();
    }

    public static boolean hasVoidEssence(Player player, int amount) {
        return amount >= 0 && getVoidEssence(player) >= amount;
    }

    public static Result addVoidEssence(ServerPlayer player, int amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        return mutate(player, data -> {
            long v = (long) data.getEssence() + amount;
            data.setEssence((int) Math.min(v, maxEssence()));
            return Result.SUCCESS;
        });
    }

    public static Result removeVoidEssence(ServerPlayer player, int amount) {
        if (amount <= 0) return Result.INVALID_AMOUNT;
        return mutate(player, data -> {
            if (data.getEssence() < amount) return Result.INSUFFICIENT_ESSENCE;
            data.setEssence(data.getEssence() - amount);
            return Result.SUCCESS;
        });
    }

    public static Result setVoidEssence(ServerPlayer player, int amount) {
        if (amount < 0) return Result.INVALID_AMOUNT;
        return mutate(player, data -> {
            data.setEssence(Math.min(amount, maxEssence()));
            return Result.SUCCESS;
        });
    }

    /** True if gameplay Essence can currently be earned (initialized and, if configured, carrying the active Core). */
    public static boolean canEarn(ServerPlayer player) {
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        if (!data.isInitialized()) return false;
        return !VoidConfig.get(VoidConfig.ESSENCE_REQUIRES_CARRIED_CORE) || CoreService.hasActiveCore(player);
    }

    /**
     * Grants gameplay Essence and the same amount of Void Experience.
     *
     * @return the Essence actually gained (0 if not eligible or cancelled)
     */
    public static int grantFromGameplay(ServerPlayer player, int baseAmount, VoidEssenceGainEvent.Source source) {
        if (baseAmount <= 0 || !canEarn(player)) return 0;
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return 0;
        VoidPlayerData data = opt.get();

        int amount = baseAmount;
        if (AbilityService.isUnlocked(data, VoidAbilities.VOID_ASCENDANCE)) {
            amount = (int) Math.round(amount * (1.0 + VoidConfig.get(VoidConfig.ASCENDANCE_ESSENCE_BONUS)));
        }
        VoidEssenceGainEvent event = new VoidEssenceGainEvent(player, source, amount);
        if (MinecraftForge.EVENT_BUS.post(event) || event.getAmount() <= 0) return 0;
        amount = event.getAmount();

        int before = data.getEssence();
        data.setEssence((int) Math.min((long) before + amount, maxEssence()));
        int gained = data.getEssence() - before;
        ProgressionService.addExperience(player, data, amount);
        VoidNetwork.sync(player);
        return gained;
    }

    private interface Mutation { Result apply(VoidPlayerData data); }

    private static Result mutate(ServerPlayer player, Mutation mutation) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return Result.NO_DATA;
        VoidPlayerData data = opt.get();
        if (!data.isInitialized()) return Result.NOT_INITIALIZED;
        Result r = mutation.apply(data);
        if (r == Result.SUCCESS) VoidNetwork.sync(player);
        return r;
    }

    private VoidEssenceService() {}
}
