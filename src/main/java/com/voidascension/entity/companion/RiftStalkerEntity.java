package com.voidascension.entity.companion;

import com.voidascension.companion.CompanionSpecies;
import com.voidascension.config.VoidConfig;
import com.voidascension.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Rift Stalker — Offensive Hunter Companion.
 * Aggressive hovering crimson-black entity with jagged energy fragments.
 */
public class RiftStalkerEntity extends AbstractVoidCompanionEntity {

    public RiftStalkerEntity(EntityType<? extends RiftStalkerEntity> type, Level level) {
        super(type, level, CompanionSpecies.RIFT_STALKER);
    }

    public RiftStalkerEntity(Level level) {
        this(ModEntities.RIFT_STALKER.get(), level);
    }

    @Override
    protected void spawnAmbientParticles() {
        if (this.random.nextFloat() < 0.45F) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.4D;
            double oy = (this.random.nextDouble() - 0.5D) * 0.4D + 0.3D;
            double oz = (this.random.nextDouble() - 0.5D) * 0.4D;
            this.level().addParticle(ParticleTypes.CRIMSON_SPORE,
                    this.getX() + ox, this.getY() + oy, this.getZ() + oz,
                    0.0D, 0.03D, 0.0D);
        }
    }

    /**
     * Rift Bolt: launches a projectile at the specified target.
     */
    public boolean fireRiftBolt(LivingEntity target) {
        if (this.level().isClientSide || target == null) return false;

        RiftBoltEntity bolt = new RiftBoltEntity(this.level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - this.getY(0.5D);
        double dz = target.getZ() - this.getZ();
        bolt.shoot(dx, dy, dz, 1.4F, 1.0F);
        this.level().addFreshEntity(bolt);

        this.level().playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.6F);
        return true;
    }

    /**
     * Mark Prey: marks a hostile target, weakening it and highlighting it for allies.
     */
    public boolean executeMarkPrey(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || target == null) return false;

        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true, true));

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0D, target.getZ(),
                15, 0.4D, 0.6D, 0.4D, 0.05D);
        sl.playSound(null, target.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    /**
     * Phase Strike: instantaneously teleports behind target and executes a critical strike.
     */
    public boolean executePhaseStrike(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl) || target == null) return false;

        float yaw = target.getYRot() * ((float) Math.PI / 180F);
        double behindX = target.getX() + Math.sin(yaw) * 1.5D;
        double behindZ = target.getZ() - Math.cos(yaw) * 1.5D;
        double behindY = target.getY() + 0.5D;

        // Visual vanish
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.5D, this.getZ(),
                12, 0.2, 0.2, 0.2, 0.05);

        // Move to target
        this.moveTo(behindX, behindY, behindZ, target.getYRot(), 0.0F);

        float damage = (float) (double) VoidConfig.get(VoidConfig.STALKER_PHASE_DAMAGE);
        target.hurt(this.damageSources().mobAttack(this), damage);
        target.setDeltaMovement(target.getDeltaMovement().add(0, 0.3D, 0));

        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                20, 0.4, 0.4, 0.4, 0.1);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);

        LivingEntity owner = getOwner();
        if (owner != null) {
            teleportToOwner(owner);
        }
        return true;
    }

    /**
     * Execution Surge: devastating attack against enemies below 35% health.
     */
    public boolean executeExecutionSurge(LivingEntity target, LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel sl) || target == null) return false;

        float healthFrac = target.getHealth() / target.getMaxHealth();
        float baseDamage = (float) (double) VoidConfig.get(VoidConfig.STALKER_PHASE_DAMAGE);
        float multiplier = healthFrac < 0.35F ? 1.5F : 1.0F;

        target.hurt(this.damageSources().mobAttack(this), baseDamage * multiplier);

        if (healthFrac < 0.35F && owner != null) {
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, true));
        }

        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.8D, target.getZ(),
                1, 0, 0, 0, 0);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }
}
