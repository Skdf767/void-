package com.voidascension.entity.companion;

import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.companion.CompanionStage;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Base server-authoritative entity class for all Void Companions.
 * Features stable orbital flight, persistent link to player capability,
 * and safe recovery upon defeat.
 */
public abstract class AbstractVoidCompanionEntity extends PathfinderMob implements OwnableEntity {

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(AbstractVoidCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ORBIT_SLOT =
            SynchedEntityData.defineId(AbstractVoidCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(AbstractVoidCompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(AbstractVoidCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIM_STATE =
            SynchedEntityData.defineId(AbstractVoidCompanionEntity.class, EntityDataSerializers.INT);

    protected final CompanionSpecies species;

    protected AbstractVoidCompanionEntity(EntityType<? extends AbstractVoidCompanionEntity> type, Level level, CompanionSpecies species) {
        super(type, level);
        this.species = species;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.noPhysics = false;
    }

    public static AttributeSupplier.Builder createCompanionAttributes(float baseHealth) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, baseHealth)
                .add(Attributes.FLYING_SPEED, 0.65D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STAGE, 1);
        this.entityData.define(DATA_ORBIT_SLOT, 0);
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_LEVEL, 1);
        this.entityData.define(DATA_ANIM_STATE, 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    public CompanionSpecies getSpecies() { return species; }

    public CompanionStage getStage() {
        return CompanionStage.forLevel(this.entityData.get(DATA_LEVEL));
    }

    public void setStage(CompanionStage stage) {
        this.entityData.set(DATA_STAGE, stage.index());
    }

    public int getOrbitSlot() {
        return this.entityData.get(DATA_ORBIT_SLOT);
    }

    public void setOrbitSlot(int slot) {
        this.entityData.set(DATA_ORBIT_SLOT, Math.max(0, slot));
    }

    public int getCompanionLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public void setCompanionLevel(int level) {
        this.entityData.set(DATA_LEVEL, Math.max(1, Math.min(30, level)));
    }

    public int getAnimState() {
        return this.entityData.get(DATA_ANIM_STATE);
    }

    public void setAnimState(int state) {
        this.entityData.set(DATA_ANIM_STATE, state);
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Override
    @Nullable
    public LivingEntity getOwner() {
        UUID uuid = getOwnerUUID();
        return uuid == null ? null : this.level().getPlayerByUUID(uuid);
    }

    public void syncWithData(CompanionData data, int slotIndex) {
        this.setCompanionLevel(data.getLevel());
        this.setStage(data.getStage());
        this.setOrbitSlot(slotIndex);
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getMaxHealth());
        }
        this.setHealth(Math.min(data.getHealth(), data.getMaxHealth()));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);

        if (!this.level().isClientSide) {
            serverTick();
        } else {
            clientTick();
        }
    }

    protected void serverTick() {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            // Check if owner is offline or unloaded: despawn without corrupting saved data
            if (this.tickCount % 40 == 0) {
                UUID uuid = getOwnerUUID();
                if (uuid != null && this.getServer() != null && this.getServer().getPlayerList().getPlayer(uuid) == null) {
                    this.discard();
                    return;
                }
            }
            return;
        }

        // Keep health synchronized back to CompanionData periodically
        if (this.tickCount % 20 == 0 && owner instanceof ServerPlayer sp) {
            VoidCapabilities.get(sp).ifPresent(d -> {
                CompanionData cd = d.getCompanion(this.species);
                if (cd != null) {
                    cd.setHealth(this.getHealth());
                    cd.setActiveEntityUUID(this.getUUID());
                }
            });
        }

        // Orbit and follow physics
        double distSq = this.distanceToSqr(owner);
        if (distSq > 1600.0D || owner.level() != this.level()) { // > 40 blocks or different dim
            teleportToOwner(owner);
        } else {
            updateOrbitMovement(owner);
        }
    }

    protected void updateOrbitMovement(LivingEntity owner) {
        // Compute desired orbit position
        int slot = getOrbitSlot();
        double radius = VoidConfig.get(VoidConfig.COMPANION_ORBIT_RADIUS);
        double baseHeight = VoidConfig.get(VoidConfig.COMPANION_ORBIT_HEIGHT);

        // Calculate orbit angle: smoothly rotates around owner
        double orbitSpeed = 0.04D;
        double slotOffset = slot * (2.0D * Math.PI / 3.0D);
        double angle = (owner.tickCount * orbitSpeed) + slotOffset;

        // Bobbing motion
        double bob = Math.sin((owner.tickCount + slot * 15) * 0.1D) * 0.2D;

        double targetX = owner.getX() + Math.cos(angle) * radius;
        double targetY = owner.getY() + baseHeight + bob;
        double targetZ = owner.getZ() + Math.sin(angle) * radius;

        Vec3 currentPos = this.position();
        Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
        Vec3 toTarget = targetPos.subtract(currentPos);

        double dist = toTarget.length();
        if (dist > 8.0D) {
            // Speed up if falling behind
            Vec3 motion = toTarget.normalize().scale(0.8D);
            this.setDeltaMovement(motion);
        } else if (dist > 0.1D) {
            // Smooth approach
            Vec3 motion = toTarget.scale(0.2D);
            this.setDeltaMovement(motion);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // Look in the direction owner is facing or look around gently
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    public void teleportToOwner(LivingEntity owner) {
        int slot = getOrbitSlot();
        double slotOffset = slot * (2.0D * Math.PI / 3.0D);
        double x = owner.getX() + Math.cos(slotOffset) * 2.0D;
        double y = owner.getY() + 1.5D;
        double z = owner.getZ() + Math.sin(slotOffset) * 2.0D;

        this.moveTo(x, y, z, owner.getYRot(), owner.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 15, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.5F);
        }
    }

    protected void clientTick() {
        // Spawn species-specific subtle energy particles
        spawnAmbientParticles();
    }

    protected abstract void spawnAmbientParticles();

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        // Friendly fire protection: owner or other players cannot hurt companion
        if (source.getEntity() != null) {
            UUID ownerUuid = getOwnerUUID();
            if (source.getEntity().getUUID().equals(ownerUuid)) return false;
            if (source.getEntity() instanceof Player) return false;
            if (source.getEntity() instanceof OwnableEntity ownable && ownerUuid != null && ownerUuid.equals(ownable.getOwnerUUID())) {
                return false;
            }
        }

        // Suffocation, drowning, and fall damage are negated for flying energy entities
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) ||
                source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING) ||
                source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result && this.getHealth() <= 0.0F) {
            handleDefeat();
        }
        return result;
    }

    protected void handleDefeat() {
        LivingEntity owner = getOwner();
        if (owner instanceof ServerPlayer sp) {
            VoidCapabilities.get(sp).ifPresent(d -> {
                CompanionData cd = d.getCompanion(this.species);
                if (cd != null) {
                    long now = sp.server.overworld().getGameTime();
                    cd.setHealth(0.0F);
                    cd.setRecoveryReadyAt(now + VoidConfig.get(VoidConfig.COMPANION_RECOVERY_COOLDOWN_TICKS));
                    cd.setActiveEntityUUID(null);
                }
            });
            sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.void_ascension.companion.defeated", this.species.displayName()));
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        this.discard();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        UUID ownerUuid = getOwnerUUID();
        if (target.getUUID().equals(ownerUuid)) return false;
        if (target instanceof Player) return false;
        if (target instanceof OwnableEntity ownable && ownerUuid != null && ownerUuid.equals(ownable.getOwnerUUID())) {
            return false;
        }
        return target instanceof Enemy || super.canAttack(target);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Avoid colliding with owner
        if (entity != getOwner()) {
            super.doPush(entity);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", this.entityData.get(DATA_STAGE));
        tag.putInt("OrbitSlot", this.entityData.get(DATA_ORBIT_SLOT));
        tag.putInt("Level", this.entityData.get(DATA_LEVEL));
        tag.putString("Species", this.species.id());
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_STAGE, tag.getInt("Stage"));
        this.entityData.set(DATA_ORBIT_SLOT, tag.getInt("OrbitSlot"));
        this.entityData.set(DATA_LEVEL, Math.max(1, tag.getInt("Level")));
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
    }
}
