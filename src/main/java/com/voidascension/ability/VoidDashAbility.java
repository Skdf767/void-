package com.voidascension.ability;

import com.voidascension.config.VoidConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Void Dash: launches the player along their horizontal look direction. This is a velocity impulse, not a
 * teleport, so normal collision applies and the player cannot pass through blocks. Fall distance is reset so the
 * dash cannot be used to amplify fall damage.
 */
public final class VoidDashAbility implements ActiveAbility {

    @Override
    public AbilityDefinition definition() { return VoidAbilities.VOID_DASH; }

    @Override
    public int essenceCost() { return VoidConfig.get(VoidConfig.DASH_ESSENCE_COST); }

    @Override
    public int cooldownTicks() { return VoidConfig.get(VoidConfig.DASH_COOLDOWN_TICKS); }

    @Override
    @Nullable
    public Component validateState(ServerPlayer player) {
        if (player.isSpectator() || player.isSleeping() || player.isPassenger() || !player.isAlive()) {
            return Component.translatable("message.void_ascension.ability.invalid_state");
        }
        return null;
    }

    @Override
    public boolean execute(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            // Looking straight up/down: dash in the facing direction.
            float yaw = player.getYRot() * ((float) Math.PI / 180F);
            horizontal = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        }
        Vec3 impulse = horizontal.normalize().scale(VoidConfig.get(VoidConfig.DASH_STRENGTH));
        double lift = VoidConfig.get(VoidConfig.DASH_LIFT);
        player.setDeltaMovement(impulse.x, Math.max(player.getDeltaMovement().y, 0.0) + lift, impulse.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        if (player.connection != null) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                24, 0.3, 0.5, 0.3, 0.05);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
        com.voidascension.companion.CompanionService.onPlayerDash(player);
        return true;
    }
}
