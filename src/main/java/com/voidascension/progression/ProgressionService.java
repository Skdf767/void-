package com.voidascension.progression;

import com.voidascension.VoidAscension;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.evolution.TierAttributes;
import com.voidascension.evolution.VoidTier;
import com.voidascension.network.VoidNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Lifecycle of a player's progression: initialization on first Core activation, Evolution Level from
 * Void Experience, revalidation on login/respawn and admin reset.
 */
public final class ProgressionService {

    public static long totalXpForLevel(int level) {
        return LevelCurve.totalXpForLevel(level, VoidConfig.get(VoidConfig.XP_BASE), VoidConfig.get(VoidConfig.XP_EXPONENT));
    }

    public static int levelForXp(long xp) {
        return LevelCurve.levelForXp(xp, VoidConfig.get(VoidConfig.XP_BASE), VoidConfig.get(VoidConfig.XP_EXPONENT),
                VoidConfig.get(VoidConfig.MAX_LEVEL));
    }

    public static int maxLevel() { return VoidConfig.get(VoidConfig.MAX_LEVEL); }

    /**
     * Initializes progression the first time a player activates a Core: VoidEvolutionTier = HUMAN,
     * VoidEssence = 0, EvolutionLevel = 0. Does nothing if already initialized (re-activation never resets).
     *
     * @return true if the player was initialized by this call
     */
    public static boolean initialize(ServerPlayer player, VoidPlayerData data) {
        if (data.isInitialized()) return false;
        data.setOwner(player.getUUID());
        data.setInitialized(true);
        data.setTier(VoidTier.HUMAN);
        data.setEssence(0);
        data.setVoidExperience(0);
        data.setEvolutionLevel(0);
        data.clearCooldowns();
        recalculateUnlocks(data);
        TierAttributes.apply(player, VoidTier.HUMAN);
        debug("Initialized progression for {}", player);
        return true;
    }

    /** Ability unlocks derived from the tier. Locked-in abilities above the tier are removed. */
    public static void recalculateUnlocks(VoidPlayerData data) {
        data.setUnlockedAbilities(data.isInitialized() ? VoidAbilities.unlockedAt(data.getTier()) : java.util.Set.of());
    }

    /**
     * Adds Void Experience and updates the Evolution Level.
     *
     * @return number of levels gained
     */
    public static int addExperience(ServerPlayer player, VoidPlayerData data, long xp) {
        if (xp <= 0) return 0;
        int before = data.getEvolutionLevel();
        long total = data.getVoidExperience() + xp;
        data.setVoidExperience(total < 0 ? Long.MAX_VALUE : total);
        data.setEvolutionLevel(levelForXp(data.getVoidExperience()));
        int gained = data.getEvolutionLevel() - before;
        if (gained > 0) {
            player.sendSystemMessage(Component.translatable("message.void_ascension.level_up", data.getEvolutionLevel())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return gained;
    }

    /** Admin: sets the Evolution Level by setting Void Experience to the exact threshold of that level. */
    public static int setLevel(ServerPlayer player, int level) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return -1;
        VoidPlayerData data = opt.get();
        int clamped = Math.max(0, Math.min(level, maxLevel()));
        data.setVoidExperience(totalXpForLevel(clamped));
        data.setEvolutionLevel(levelForXp(data.getVoidExperience()));
        VoidNetwork.sync(player);
        return data.getEvolutionLevel();
    }

    /**
     * Re-derives every computed value from stored state. Called on login, respawn and dimension change; repairs
     * data edited externally or produced by older versions, then re-applies attributes and syncs the client.
     */
    public static void revalidate(ServerPlayer player) {
        VoidCapabilities.get(player).ifPresent(data -> {
            if (data.getOwner() == null) {
                data.setOwner(player.getUUID());
            } else if (!data.getOwner().equals(player.getUUID())) {
                VoidAscension.LOGGER.warn("Void data owner mismatch for {} (stored {}), correcting owner.",
                        player.getGameProfile().getName(), data.getOwner());
                data.setOwner(player.getUUID());
            }
            data.setEvolutionLevel(levelForXp(data.getVoidExperience()));
            recalculateUnlocks(data);
            data.pruneCooldowns(com.voidascension.ability.AbilityService.now(player));
            TierAttributes.apply(player, data.isInitialized() ? data.getTier() : VoidTier.HUMAN);
            com.voidascension.companion.CompanionService.reconcileCompanions(player);
            VoidNetwork.sync(player);
        });
    }

    /** Admin: wipes all progression (the player must activate a Core again). */
    public static void reset(ServerPlayer player) {
        VoidCapabilities.get(player).ifPresent(data -> {
            data.reset();
            data.setOwner(player.getUUID());
            TierAttributes.apply(player, VoidTier.HUMAN);
            VoidNetwork.sync(player);
        });
    }

    static void debug(String msg, ServerPlayer player) {
        if (VoidConfig.get(VoidConfig.DEBUG_LOGGING)) VoidAscension.LOGGER.info(msg, player.getGameProfile().getName());
    }

    private ProgressionService() {}
}
