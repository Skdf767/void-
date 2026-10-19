package com.voidascension;

import com.mojang.logging.LogUtils;
import com.voidascension.config.VoidConfig;
import com.voidascension.network.VoidNetwork;
import com.voidascension.registry.ModBlocks;
import com.voidascension.registry.ModCreativeTabs;
import com.voidascension.registry.ModItems;
import com.voidascension.registry.ModLootModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Void Ascension - main mod entry point.
 * <p>
 * Milestone 1 (0.1.0-alpha). Module layout - see docs/ARCHITECTURE.md:
 * <ul>
 *     <li>{@code data} - versioned player data model, serializer/migration, capability</li>
 *     <li>{@code progression} - Void Essence API, Evolution Level curve, initialization</li>
 *     <li>{@code evolution} - tiers, configurable requirements, transactional advancement, attributes</li>
 *     <li>{@code ability} - ability metadata, lifecycle (unlock/cooldown/cost) and implementations</li>
 *     <li>{@code core} - Void Core activation and bond rules</li>
 *     <li>{@code network}, {@code command}, {@code client}, {@code event}, {@code config}, {@code loot}</li>
 * </ul>
 */
@Mod(VoidAscension.MOD_ID)
public class VoidAscension {
    public static final String MOD_ID = "void_ascension";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidAscension() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModLootModifiers.SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VoidConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, VoidConfig.CLIENT_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(VoidNetwork::register);
        LOGGER.info("Void Ascension initialised - the Void is listening.");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
