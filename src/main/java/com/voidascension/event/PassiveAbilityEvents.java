package com.voidascension.event;

import com.voidascension.VoidAscension;
import com.voidascension.ability.AbilityService;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.util.VoidAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Event-driven passive abilities: Void Resilience and Void Rescue. (Void Ascendance is in VoidEssenceService.) */
@Mod.EventBusSubscriber(modid = VoidAscension.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PassiveAbilityEvents {

    /** Void Resilience: reduces fall damage by {@code abilities.void_resilience.fallDamageReduction}. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) return;
        if (!AbilityService.hasPassive(player, VoidAbilities.VOID_RESILIENCE)) return;
        double reduction = VoidConfig.get(VoidConfig.RESILIENCE_FALL_REDUCTION);
        event.setAmount((float) (event.getAmount() * (1.0 - reduction)));
    }

    /**
     * Void Rescue: when an unlocked player takes out-of-world damage below the world, they are returned to their
     * respawn point (or world spawn) instead. Uses the shared ability cooldown/cost infrastructure.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;
        if (player.getY() >= player.level().getMinBuildHeight()) return; // /kill etc. are not rescued
        if (!AbilityService.tryConsumePassive(player, VoidAbilities.VOID_RESCUE,
                VoidConfig.get(VoidConfig.RESCUE_ESSENCE_COST), VoidConfig.get(VoidConfig.RESCUE_COOLDOWN_TICKS))) return;
        event.setCanceled(true);
        rescue(player);
    }

    private static void rescue(ServerPlayer player) {
        ServerLevel target = player.server.getLevel(player.getRespawnDimension());
        BlockPos pos = player.getRespawnPosition();
        if (target == null || pos == null) {
            target = player.server.overworld();
            pos = target.getSharedSpawnPos();
        }
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        if (target == player.level()) player.moveTo(x, y, z); // keeps position authoritative for fake players too
        player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
        VoidCapabilities.get(player).ifPresent(d -> d.setVoidRescues(d.getVoidRescues() + 1));
        player.sendSystemMessage(Component.translatable("message.void_ascension.rescue").withStyle(ChatFormatting.DARK_PURPLE));
        target.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
        target.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 60, 0.4, 0.8, 0.4, 0.1);
        VoidAdvancements.award(player, VoidAdvancements.VOID_RESCUE);
    }

    private PassiveAbilityEvents() {}
}
