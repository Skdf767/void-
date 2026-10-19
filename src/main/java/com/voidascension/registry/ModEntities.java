package com.voidascension.registry;

import com.voidascension.VoidAscension;
import com.voidascension.entity.companion.AbyssalWispEntity;
import com.voidascension.entity.companion.NullOracleEntity;
import com.voidascension.entity.companion.RiftBoltEntity;
import com.voidascension.entity.companion.RiftStalkerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for all Void Ascension entities, including the Void Companions.
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VoidAscension.MOD_ID);

    public static final RegistryObject<EntityType<AbyssalWispEntity>> ABYSSAL_WISP =
            ENTITIES.register("abyssal_wisp", () ->
                    EntityType.Builder.of(AbyssalWispEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.6F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("abyssal_wisp"));

    public static final RegistryObject<EntityType<RiftStalkerEntity>> RIFT_STALKER =
            ENTITIES.register("rift_stalker", () ->
                    EntityType.Builder.of(RiftStalkerEntity::new, MobCategory.CREATURE)
                            .sized(0.7F, 0.7F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("rift_stalker"));

    public static final RegistryObject<EntityType<NullOracleEntity>> NULL_ORACLE =
            ENTITIES.register("null_oracle", () ->
                    EntityType.Builder.of(NullOracleEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.8F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("null_oracle"));

    public static final RegistryObject<EntityType<RiftBoltEntity>> RIFT_BOLT =
            ENTITIES.register("rift_bolt", () ->
                    EntityType.Builder.<RiftBoltEntity>of(RiftBoltEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build("rift_bolt"));

    private ModEntities() {}
}
