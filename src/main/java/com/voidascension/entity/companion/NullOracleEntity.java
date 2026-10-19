package com.voidascension.entity.companion;

import com.voidascension.companion.CompanionSpecies;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidCapabilities;
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

import java.util.List;

/**
 * Null Oracle — Tactical Defender Companion.
 * Floating dark energy core with concentric geometric rings and cyan energy fragments.
 */
public class NullOracleEntity extends AbstractVoidCompanionEntity {

    public NullOracleEntity(EntityType<? extends NullOracleEntity> type, Level level) {
        super(type, level, CompanionSpecies.NULL_ORACLE);
    }

    public NullOracleEntity(Level level) {
        this(ModEntities.NULL_ORACLE.get(), level);
    }

    @Override
    protected void spawnAmbientParticles() {
        if (this.random.nextFloat() < 0.4F) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.5D;
            double oy = (this.random.nextDouble() - 0.5D) * 0.5D + 0.3D;
            double oz = (this.random.nextDouble() - 0.5D) * 0.5D;
            this.level().addParticle(ParticleTypes.ENCHANT,
                    this.getX() + ox, this.getY() + oy, this.getZ() + oz,
                    (this.random.nextDouble() - 0.5D) * 0.1D, 0.05D, (this.random.nextDouble() - 0.5D) * 0.1D);
        }
    }

    /**
     * Null Field: deploys a protective field reducing incoming damage to the owner.
     */
    public boolean executeNullField(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, true, true));

        sl.sendParticles(ParticleTypes.GLOW, owner.getX(), owner.getY() + 1.0D, owner.getZ(),
                25, 0.6D, 0.6D, 0.6D, 0.05D);
        sl.playSound(null, owner.blockPosition(), SoundEvents.BEACON_POWER, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    /**
     * Disrupt: suppresses and slows enemies within 8 blocks.
     */
    public boolean executeDisrupt() {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        AABB box = this.getBoundingBox().inflate(8.0D);
        List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, box, this::canAttack);
        for (LivingEntity enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true, true));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true, true));
        }

        sl.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.4D, this.getZ(),
                1, 0, 0, 0, 0);
        sl.playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8F, 1.8F);
        return true;
    }

    /**
     * Void Scan: scans a 32-block radius, highlighting all hostiles.
     */
    public boolean executeVoidScan(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        AABB box = owner.getBoundingBox().inflate(32.0D);
        List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, box, this::canAttack);
        for (LivingEntity enemy : enemies) {
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
        }

        sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 0.5D, this.getZ(),
                1, 0, 0, 0, 0);
        sl.playSound(null, this.blockPosition(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /**
     * Resonance Link: provides cooldown acceleration and ability efficiency to the owner.
     * Reduces remaining cooldowns of player active abilities by configured fraction and grants Haste.
     */
    public boolean executeResonanceLink(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        // Visual + haste buff (tactical efficiency)
        owner.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 240, 1, false, false, true));

        // Actual cooldown acceleration: reduce remaining Dash cooldown and companion ability cooldowns
        if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
            VoidCapabilities.get(sp).ifPresent(data -> {
                double reduction = VoidConfig.get(VoidConfig.ORACLE_RESONANCE_CD_REDUCTION);
                long now = sp.server.overworld().getGameTime();
                // Player ability cooldowns
                for (String id : new java.util.HashSet<>(data.getCooldowns().keySet())) {
                    long readyAt = data.getCooldownReadyAt(id);
                    if (readyAt > now) {
                        long remaining = readyAt - now;
                        long reduced = (long) (remaining * (1.0 - reduction));
                        data.setCooldownReadyAt(id, now + Math.max(1, reduced));
                    }
                }
                // Companion ability cooldowns for all active companions
                for (com.voidascension.companion.CompanionData cd : data.getAllCompanions().values()) {
                    if (cd.isUnlocked() && cd.isActive()) {
                        for (String abilId : new java.util.HashSet<>(cd.getCooldowns().keySet())) {
                            long rAt = cd.getCooldownReadyAt(abilId);
                            if (rAt > now) {
                                long rem = rAt - now;
                                long red = (long) (rem * (1.0 - reduction));
                                cd.setCooldownReadyAt(abilId, now + Math.max(1, red));
                            }
                        }
                    }
                }
            });
            // Sync to client for HUD update
            com.voidascension.network.VoidNetwork.sync(sp);
        }

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, owner.getX(), owner.getY() + 1.2D, owner.getZ(),
                18, 0.4D, 0.4D, 0.4D, 0.05D);
        sl.playSound(null, owner.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);
        return true;
    }
}
