package com.voidascension.evolution;

import com.voidascension.VoidAscension;
import com.voidascension.ability.AbilityDefinition;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.api.event.VoidEvolutionEvent;
import com.voidascension.config.VoidConfig;
import com.voidascension.core.CoreService;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.network.VoidNetwork;
import com.voidascension.progression.ProgressionService;
import com.voidascension.util.VoidAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

/**
 * Server-authoritative evolution. {@link #tryAdvance} is transactional: every requirement is validated first,
 * then Essence is spent, the tier set, abilities unlocked and attributes applied in one server-thread call.
 * If any precondition fails nothing changes. Because all calls run on the server thread and the tier is
 * re-read each time, repeated clicks/packets cannot evolve twice or spend Essence twice.
 */
public final class EvolutionService {

    public enum Result {
        SUCCESS, NOT_INITIALIZED, MAX_TIER, NO_ACTIVE_CORE, LEVEL_TOO_LOW, NOT_ENOUGH_ESSENCE, CANCELLED, NO_DATA;

        public boolean success() { return this == SUCCESS; }
    }

    public static boolean coreRequirementSatisfied(ServerPlayer player) {
        return !VoidConfig.get(VoidConfig.EVOLUTION_REQUIRES_CARRIED_CORE) || CoreService.hasActiveCore(player);
    }

    public static EvolutionRequirements.Check check(ServerPlayer player) {
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        return EvolutionRequirements.evaluate(data.isInitialized(), coreRequirementSatisfied(player),
                data.getTier(), data.getEvolutionLevel(), data.getEssence(), EvolutionTable::get);
    }

    public static Result tryAdvance(ServerPlayer player) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return Result.NO_DATA;
        VoidPlayerData data = opt.get();
        EvolutionRequirements.Check check = check(player);
        if (!check.eligible()) return map(check.unmet().get(0));

        VoidTier from = data.getTier();
        VoidTier to = check.next();
        if (MinecraftForge.EVENT_BUS.post(new VoidEvolutionEvent.Pre(player, from, to))) return Result.CANCELLED;

        // Commit - nothing below can fail validation.
        data.setEssence(data.getEssence() - check.requiredEssence());
        applyTier(player, data, to);
        MinecraftForge.EVENT_BUS.post(new VoidEvolutionEvent.Post(player, from, to));
        if (VoidConfig.get(VoidConfig.DEBUG_LOGGING)) {
            VoidAscension.LOGGER.info("{} evolved {} -> {}", player.getGameProfile().getName(), from, to);
        }
        return Result.SUCCESS;
    }

    /** Uses {@link #tryAdvance} and tells the player the outcome. */
    public static Result tryAdvanceWithFeedback(ServerPlayer player) {
        EvolutionRequirements.Check before = check(player);
        Result result = tryAdvance(player);
        if (result == Result.SUCCESS) {
            VoidTier tier = VoidCapabilities.forPlayer(player).getTier();
            player.sendSystemMessage(Component.translatable("message.void_ascension.evolve.success", tier.displayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            for (AbilityDefinition def : VoidAbilities.unlockedBy(tier)) {
                player.sendSystemMessage(Component.translatable("message.void_ascension.ability.unlocked", def.displayName())
                        .withStyle(ChatFormatting.AQUA));
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.serverLevel().sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0,
                    player.getZ(), 80, 0.5, 1.0, 0.5, 0.02);
        } else {
            player.displayClientMessage(failureMessage(result, before).withStyle(ChatFormatting.RED), false);
        }
        return result;
    }

    public static net.minecraft.network.chat.MutableComponent failureMessage(Result result, EvolutionRequirements.Check c) {
        return switch (result) {
            case NOT_INITIALIZED -> Component.translatable("message.void_ascension.not_initialized");
            case MAX_TIER -> Component.translatable("message.void_ascension.evolve.max");
            case NO_ACTIVE_CORE -> Component.translatable("message.void_ascension.evolve.no_core");
            case LEVEL_TOO_LOW -> Component.translatable("message.void_ascension.evolve.level", c.requiredLevel(), c.level());
            case NOT_ENOUGH_ESSENCE -> Component.translatable("message.void_ascension.evolve.essence", c.requiredEssence(), c.essence());
            case CANCELLED -> Component.translatable("message.void_ascension.evolve.blocked");
            default -> Component.translatable("message.void_ascension.error");
        };
    }

    /** Admin: sets the tier directly (no cost, no requirement checks). Initializes progression if needed. */
    public static void setTier(ServerPlayer player, VoidTier tier) {
        VoidCapabilities.get(player).ifPresent(data -> {
            ProgressionService.initialize(player, data);
            applyTier(player, data, tier);
        });
    }

    private static void applyTier(ServerPlayer player, VoidPlayerData data, VoidTier tier) {
        data.setTier(tier);
        ProgressionService.recalculateUnlocks(data);
        TierAttributes.apply(player, tier);
        if (tier != VoidTier.HUMAN) VoidAdvancements.award(player, VoidAdvancements.EVOLVE);
        if (tier.isMax()) VoidAdvancements.award(player, VoidAdvancements.ASCEND);
        VoidNetwork.sync(player);
    }

    private static Result map(EvolutionRequirements.Requirement r) {
        return switch (r) {
            case INITIALIZED -> Result.NOT_INITIALIZED;
            case NOT_MAX_TIER -> Result.MAX_TIER;
            case ACTIVE_CORE -> Result.NO_ACTIVE_CORE;
            case EVOLUTION_LEVEL -> Result.LEVEL_TOO_LOW;
            case VOID_ESSENCE -> Result.NOT_ENOUGH_ESSENCE;
        };
    }

    private EvolutionService() {}
}
