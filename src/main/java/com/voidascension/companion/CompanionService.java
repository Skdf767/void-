package com.voidascension.companion;

import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.entity.companion.AbstractVoidCompanionEntity;
import com.voidascension.entity.companion.AbyssalWispEntity;
import com.voidascension.entity.companion.NullOracleEntity;
import com.voidascension.entity.companion.RiftStalkerEntity;
import com.voidascension.network.CompanionFeedbackPacket;
import com.voidascension.network.VoidNetwork;
import com.voidascension.progression.VoidEssenceService;
import com.voidascension.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative management service for Void Companions.
 * Handles entity reconciliation, lifecycle, evolution, and ability execution.
 */
public final class CompanionService {

    public enum ActionResult {
        SUCCESS,
        NOT_INITIALIZED,
        LOCKED,
        ALREADY_UNLOCKED,
        MAX_ACTIVE_REACHED,
        RECOVERING,
        ON_COOLDOWN,
        NOT_ENOUGH_ESSENCE,
        PLAYER_TIER_TOO_LOW,
        LEVEL_TOO_LOW,
        MAX_STAGE_REACHED,
        NO_TARGET,
        FAILED;

        public boolean isSuccess() { return this == SUCCESS; }
    }

    public static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    /**
     * Reconciles all active companions for a player: spawns missing entities,
     * updates slots, removes duplicates or deactivated entities.
     */
    public static void reconcileCompanions(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty() || !opt.get().isInitialized()) return;
        VoidPlayerData data = opt.get();

        ServerLevel level = player.serverLevel();
        long now = now(player);

        // Auto-restore companions whose recovery cooldown has elapsed
        for (CompanionData cd : data.getAllCompanions().values()) {
            cd.tryCompleteRecovery(now);
        }

        List<CompanionData> activeList = data.getActiveCompanions();
        for (int i = 0; i < activeList.size(); i++) {
            CompanionData cd = activeList.get(i);

            // Check recovery status
            if (cd.isRecovering(now)) {
                // Remove entity if it somehow exists
                discardEntity(level, cd.getActiveEntityUUID());
                cd.setActiveEntityUUID(null);
                continue;
            }

            // Check if active entity exists and is valid
            UUID entityUuid = cd.getActiveEntityUUID();
            AbstractVoidCompanionEntity companionEntity = null;
            if (entityUuid != null) {
                Entity found = level.getEntity(entityUuid);
                if (found instanceof AbstractVoidCompanionEntity ace && ace.isAlive()) {
                    companionEntity = ace;
                }
            }

            // If missing or in a different level, spawn a fresh entity
            if (companionEntity == null) {
                companionEntity = spawnCompanionEntity(level, player, cd.getSpecies());
                if (companionEntity != null) {
                    cd.setActiveEntityUUID(companionEntity.getUUID());
                }
            }

            if (companionEntity != null) {
                companionEntity.syncWithData(cd, i);
                companionEntity.setOwnerUUID(player.getUUID());
            }
        }

        // Clean up entities for inactive companions
        for (CompanionData cd : data.getAllCompanions().values()) {
            if (!cd.isActive() && cd.getActiveEntityUUID() != null) {
                discardEntity(level, cd.getActiveEntityUUID());
                cd.setActiveEntityUUID(null);
            }
        }
    }

    private static void discardEntity(ServerLevel level, UUID uuid) {
        if (uuid == null) return;
        Entity entity = level.getEntity(uuid);
        if (entity != null) {
            entity.discard();
        }
    }

    private static AbstractVoidCompanionEntity spawnCompanionEntity(ServerLevel level, ServerPlayer owner, CompanionSpecies species) {
        AbstractVoidCompanionEntity entity = switch (species) {
            case ABYSSAL_WISP -> new AbyssalWispEntity(ModEntities.ABYSSAL_WISP.get(), level);
            case RIFT_STALKER -> new RiftStalkerEntity(ModEntities.RIFT_STALKER.get(), level);
            case NULL_ORACLE -> new NullOracleEntity(ModEntities.NULL_ORACLE.get(), level);
        };

        double x = owner.getX() + (level.random.nextDouble() - 0.5D) * 2.0D;
        double y = owner.getY() + 1.5D;
        double z = owner.getZ() + (level.random.nextDouble() - 0.5D) * 2.0D;

        entity.moveTo(x, y, z, owner.getYRot(), owner.getXRot());
        entity.setOwnerUUID(owner.getUUID());
        level.addFreshEntity(entity);

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 15, 0.2, 0.2, 0.2, 0.05);
        level.playSound(null, owner.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.6F, 1.4F);
        return entity;
    }

    /**
     * Toggles the summoned/active state of a companion species.
     */
    public static ActionResult toggleActive(ServerPlayer player, CompanionSpecies species) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty() || !opt.get().isInitialized()) return ActionResult.NOT_INITIALIZED;
        VoidPlayerData data = opt.get();
        CompanionData cd = data.getCompanion(species);
        if (cd == null) return ActionResult.FAILED;
        if (!cd.isUnlocked()) return ActionResult.LOCKED;

        long now = now(player);
        if (cd.isActive()) {
            cd.setActive(false);
            discardEntity(player.serverLevel(), cd.getActiveEntityUUID());
            cd.setActiveEntityUUID(null);
            reconcileCompanions(player);
            VoidNetwork.sync(player);
            return ActionResult.SUCCESS;
        } else {
            if (cd.isRecovering(now)) return ActionResult.RECOVERING;
            if (data.getActiveCompanionCount() >= data.getMaxActiveCompanions()) {
                return ActionResult.MAX_ACTIVE_REACHED;
            }
            cd.setActive(true);
            reconcileCompanions(player);
            VoidNetwork.sync(player);
            return ActionResult.SUCCESS;
        }
    }

    /**
     * Unlocks a companion species for the player.
     */
    public static ActionResult unlockCompanion(ServerPlayer player, CompanionSpecies species) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty() || !opt.get().isInitialized()) return ActionResult.NOT_INITIALIZED;
        VoidPlayerData data = opt.get();
        CompanionData cd = data.getCompanion(species);
        if (cd == null) return ActionResult.FAILED;
        if (cd.isUnlocked()) return ActionResult.ALREADY_UNLOCKED;

        int cost = 50; // Moderate introductory cost
        if (data.getEssence() < cost) return ActionResult.NOT_ENOUGH_ESSENCE;

        VoidEssenceService.removeVoidEssence(player, cost);
        cd.setUnlocked(true);
        player.sendSystemMessage(Component.translatable("message.void_ascension.companion.unlocked", species.displayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        VoidNetwork.sync(player);
        return ActionResult.SUCCESS;
    }

    /**
     * Evolves a companion to the next stage.
     */
    public static ActionResult evolveCompanion(ServerPlayer player, CompanionSpecies species) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty() || !opt.get().isInitialized()) return ActionResult.NOT_INITIALIZED;
        VoidPlayerData data = opt.get();
        CompanionData cd = data.getCompanion(species);
        if (cd == null) return ActionResult.FAILED;
        if (!cd.isUnlocked()) return ActionResult.LOCKED;
        if (cd.getStage().isMax()) return ActionResult.MAX_STAGE_REACHED;

        CompanionStage next = cd.getStage().next();
        if (next == null) return ActionResult.MAX_STAGE_REACHED;

        if (cd.getLevel() < next.minLevel()) return ActionResult.LEVEL_TOO_LOW;
        if (!data.getTier().isAtLeast(next.requiredPlayerTier())) return ActionResult.PLAYER_TIER_TOO_LOW;

        int cost = next.evolutionEssenceCost();
        if (data.getEssence() < cost) return ActionResult.NOT_ENOUGH_ESSENCE;

        VoidEssenceService.removeVoidEssence(player, cost);
        cd.setStage(next);
        cd.recalculateMaxHealth();

        ServerLevel sl = player.serverLevel();
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2D, player.getZ(),
                30, 0.5D, 0.5D, 0.5D, 0.1D);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);

        player.sendSystemMessage(Component.translatable("message.void_ascension.companion.evolved",
                species.displayName(), next.displayName()).withStyle(ChatFormatting.GOLD));

        reconcileCompanions(player);
        VoidNetwork.sync(player);
        return ActionResult.SUCCESS;
    }

    /**
     * Executes the selected ability of a companion.
     */
    public static ActionResult useAbility(ServerPlayer player, CompanionSpecies species, int abilityIndex) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty() || !opt.get().isInitialized()) return ActionResult.NOT_INITIALIZED;
        VoidPlayerData data = opt.get();
        CompanionData cd = data.getCompanion(species);
        if (cd == null || !cd.isUnlocked() || !cd.isActive()) return ActionResult.LOCKED;

        List<CompanionAbility> abilities = species.getAbilities();
        if (abilityIndex < 0 || abilityIndex >= abilities.size()) return ActionResult.FAILED;
        CompanionAbility ability = abilities.get(abilityIndex);

        if (!cd.isAbilityUnlocked(ability)) return ActionResult.LOCKED;

        long now = now(player);
        if (cd.getCooldownReadyAt(ability.id()) > now) return ActionResult.ON_COOLDOWN;

        int cost = ability.essenceCost();
        if (data.getEssence() < cost) return ActionResult.NOT_ENOUGH_ESSENCE;

        // Resolve active companion entity
        ServerLevel level = player.serverLevel();
        Entity entity = cd.getActiveEntityUUID() != null ? level.getEntity(cd.getActiveEntityUUID()) : null;
        if (!(entity instanceof AbstractVoidCompanionEntity ace) || !ace.isAlive()) {
            reconcileCompanions(player);
            return ActionResult.FAILED;
        }

        boolean success = false;
        LivingEntity target = findTarget(player, 24.0D);

        if (ace instanceof AbyssalWispEntity wisp) {
            success = switch (ability) {
                case ABYSS_SHIELD -> wisp.executeAbyssShield(player);
                case THREAT_SENSE -> wisp.executeThreatSense(player);
                case ABYSS_PULSE -> wisp.executeAbyssPulse();
                default -> false;
            };
        } else if (ace instanceof RiftStalkerEntity stalker) {
            if (target == null) return ActionResult.NO_TARGET;
            success = switch (ability) {
                case RIFT_BOLT -> stalker.fireRiftBolt(target);
                case MARK_PREY -> stalker.executeMarkPrey(target);
                case PHASE_STRIKE -> stalker.executePhaseStrike(target);
                case EXECUTION_SURGE -> stalker.executeExecutionSurge(target, player);
                default -> false;
            };
        } else if (ace instanceof NullOracleEntity oracle) {
            success = switch (ability) {
                case NULL_FIELD -> oracle.executeNullField(player);
                case DISRUPT -> oracle.executeDisrupt();
                case VOID_SCAN -> oracle.executeVoidScan(player);
                case RESONANCE_LINK -> oracle.executeResonanceLink(player);
                default -> false;
            };
        }

        if (success) {
            VoidEssenceService.removeVoidEssence(player, cost);
            cd.setCooldownReadyAt(ability.id(), now + ability.cooldownTicks());
            addCompanionXp(player, 15);
            VoidNetwork.sync(player);
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAILED;
    }

    private static LivingEntity findTarget(ServerPlayer player, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = player.getBoundingBox().inflate(maxDist);
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e instanceof Enemy && e.isAlive() && !e.isInvisible());

        LivingEntity closest = null;
        double closestDot = 0.5D; // Within cone of vision
        for (LivingEntity e : list) {
            Vec3 toEntity = e.position().subtract(eye).normalize();
            double dot = look.dot(toEntity);
            if (dot > closestDot) {
                closestDot = dot;
                closest = e;
            }
        }
        return closest;
    }

    /**
     * Grants companion XP to all currently active companions.
     */
    public static void addCompanionXp(ServerPlayer player, long amount) {
        if (amount <= 0 || player.level().isClientSide) return;
        VoidCapabilities.get(player).ifPresent(d -> {
            for (CompanionData cd : d.getActiveCompanions()) {
                boolean levelUp = cd.addExperience(amount);
                if (levelUp) {
                    player.sendSystemMessage(Component.translatable("message.void_ascension.companion.level_up",
                            cd.getSpecies().displayName(), cd.getLevel()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    player.serverLevel().playSound(null, player.blockPosition(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.4F);
                }
            }
        });
    }

    /**
     * Combat synergy on enemy kill: grants XP and triggers Void Siphon if active.
     */
    public static void onKillVictim(ServerPlayer player, LivingEntity victim) {
        VoidCapabilities.get(player).ifPresent(d -> {
            // Companion XP from kills
            long xpGain = Math.max(5L, (long) (victim.getMaxHealth() / 2.0));
            addCompanionXp(player, xpGain);

            // Void Siphon bonus Essence
            CompanionData wispData = d.getCompanion(CompanionSpecies.ABYSSAL_WISP);
            if (wispData != null && wispData.isActive() && wispData.isAbilityUnlocked(CompanionAbility.VOID_SIPHON)) {
                int bonusEssence = (int) Math.round(victim.getMaxHealth() * (VoidConfig.get(VoidConfig.WISP_SIPHON_BONUS_PERCENT) / 100.0) / 4.0);
                if (bonusEssence > 0) {
                    VoidEssenceService.addVoidEssence(player, Math.min(10, bonusEssence));
                }
            }
        });
    }

    /**
     * Synergy when player uses Void Dash.
     */
    public static void onPlayerDash(ServerPlayer player) {
        VoidCapabilities.get(player).ifPresent(d -> {
            for (CompanionData cd : d.getActiveCompanions()) {
                if (cd.getActiveEntityUUID() != null) {
                    Entity e = player.serverLevel().getEntity(cd.getActiveEntityUUID());
                    if (e instanceof AbstractVoidCompanionEntity ace) {
                        Vec3 dashMotion = player.getDeltaMovement().scale(0.8D);
                        ace.setDeltaMovement(dashMotion);
                    }
                }
            }
        });
    }

    private CompanionService() {}
}
