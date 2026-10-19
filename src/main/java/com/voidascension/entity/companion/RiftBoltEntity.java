package com.voidascension.entity.companion;

import com.voidascension.config.VoidConfig;
import com.voidascension.registry.ModEntities;
import com.voidascension.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Server-authoritative projectile fired by the Rift Stalker companion.
 */
public class RiftBoltEntity extends ThrowableItemProjectile {

    public RiftBoltEntity(EntityType<? extends RiftBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public RiftBoltEntity(Level level, LivingEntity shooter) {
        super(ModEntities.RIFT_BOLT.get(), shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.VOID_FRAGMENT.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.2,
                    this.getY() + (this.random.nextDouble() - 0.5) * 0.2,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.2,
                    0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.DRAGON_BREATH,
                    this.getX(), this.getY(), this.getZ(),
                    0.0, 0.0, 0.0);
        } else if (this.tickCount > 80) { // Despawn after 4 seconds
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            float damage = (float) (double) VoidConfig.get(VoidConfig.STALKER_BOLT_DAMAGE);
            target.hurt(this.damageSources().magic(), damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_HURT, SoundSource.PLAYERS, 0.8F, 1.4F);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.6F, 1.8F);
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
