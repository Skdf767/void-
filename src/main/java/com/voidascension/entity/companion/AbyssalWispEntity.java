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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Abyssal Wisp — Support and Sustain Companion.
 * Floats as a dark-violet energy orb with subtle tendrils.
 */
public class AbyssalWispEntity extends AbstractVoidCompanionEntity {

    public AbyssalWispEntity(EntityType<? extends AbyssalWispEntity> type, Level level) {
        super(type, level, CompanionSpecies.ABYSSAL_WISP);
    }

    public AbyssalWispEntity(Level level) {
        this(ModEntities.ABYSSAL_WISP.get(), level);
    }

    @Override
    protected void spawnAmbientParticles() {
        if (this.random.nextFloat() < 0.4F) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.4D;
            double oy = (this.random.nextDouble() - 0.5D) * 0.4D + 0.3D;
            double oz = (this.random.nextDouble() - 0.5D) * 0.4D;
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + ox, this.getY() + oy, this.getZ() + oz,
                    0.0D, 0.02D, 0.0D);
        }
    }

    /**
     * Abyss Shield: grants temporary absorption hearts to the owner.
     */
    public boolean executeAbyssShield(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        float shieldAmt = (float) (double) VoidConfig.get(VoidConfig.WISP_SHIELD_AMOUNT);
        int amp = Math.max(0, (int) (shieldAmt / 4.0F) - 1);
        owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, amp, false, true, true));

        sl.sendParticles(ParticleTypes.ENCHANT, owner.getX(), owner.getY() + 1.0D, owner.getZ(),
                20, 0.5D, 0.5D, 0.5D, 0.1D);
        sl.playSound(null, owner.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.5F);
        return true;
    }

    /**
     * Threat Sense: detects and highlights nearby hostiles for 6 seconds.
     */
    public boolean executeThreatSense(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        AABB box = owner.getBoundingBox().inflate(24.0D);
        List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, box, this::canAttack);
        for (LivingEntity enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false, true));
        }

        sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5D, this.getZ(),
                15, 0.4D, 0.4D, 0.4D, 0.05D);
        sl.playSound(null, this.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    /**
     * Abyss Pulse: 360-degree radial blast knocking back and damaging nearby enemies.
     */
    public boolean executeAbyssPulse() {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        AABB box = this.getBoundingBox().inflate(6.0D);
        List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, box, this::canAttack);
        float damage = (float) (double) VoidConfig.get(VoidConfig.WISP_PULSE_DAMAGE);

        for (LivingEntity enemy : enemies) {
            enemy.hurt(this.damageSources().magic(), damage);
            Vec3 knockback = enemy.position().subtract(this.position()).normalize().scale(1.2D);
            enemy.setDeltaMovement(knockback.x, 0.4D, knockback.z);
            enemy.hurtMarked = true;
        }

        // Particle wave
        for (int i = 0; i < 36; i++) {
            double rad = Math.toRadians(i * 10);
            double px = this.getX() + Math.cos(rad) * 2.5D;
            double pz = this.getZ() + Math.sin(rad) * 2.5D;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, px, this.getY() + 0.4D, pz, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }
}
