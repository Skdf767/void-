package com.voidascension.event;

import com.voidascension.VoidAscension;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.entity.companion.AbstractVoidCompanionEntity;
import com.voidascension.evolution.EvolutionTable;
import com.voidascension.registry.ModEntities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Mod-bus registration: capability type, entity attributes and config validation. */
@Mod.EventBusSubscriber(modid = VoidAscension.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(VoidPlayerData.class);
    }

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ABYSSAL_WISP.get(), AbstractVoidCompanionEntity.createCompanionAttributes(CompanionSpecies.ABYSSAL_WISP.baseMaxHealth()).build());
        event.put(ModEntities.RIFT_STALKER.get(), AbstractVoidCompanionEntity.createCompanionAttributes(CompanionSpecies.RIFT_STALKER.baseMaxHealth()).build());
        event.put(ModEntities.NULL_ORACLE.get(), AbstractVoidCompanionEntity.createCompanionAttributes(CompanionSpecies.NULL_ORACLE.baseMaxHealth()).build());
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) { validate(event.getConfig()); }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) { validate(event.getConfig()); }

    private static void validate(ModConfig config) {
        if (config.getSpec() == VoidConfig.SERVER_SPEC) EvolutionTable.validateConfiguredTable();
    }

    private ModBusEvents() {}
}
