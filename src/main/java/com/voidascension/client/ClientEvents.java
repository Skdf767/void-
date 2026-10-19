package com.voidascension.client;

import com.voidascension.VoidAscension;
import com.voidascension.ability.VoidAbilities;
import com.voidascension.client.gui.VoidCompanionScreen;
import com.voidascension.client.render.companion.AbyssalWispModel;
import com.voidascension.client.render.companion.AbyssalWispRenderer;
import com.voidascension.client.render.companion.CompanionModelLayers;
import com.voidascension.client.render.companion.NullOracleModel;
import com.voidascension.client.render.companion.NullOracleRenderer;
import com.voidascension.client.render.companion.RiftBoltRenderer;
import com.voidascension.client.render.companion.RiftStalkerModel;
import com.voidascension.client.render.companion.RiftStalkerRenderer;
import com.voidascension.companion.CompanionData;
import com.voidascension.companion.CompanionSpecies;
import com.voidascension.data.ClientVoidData;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.item.VoidCoreItem;
import com.voidascension.network.CompanionActionPacket;
import com.voidascension.network.UseAbilityPacket;
import com.voidascension.network.VoidNetwork;
import com.voidascension.registry.ModEntities;
import com.voidascension.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** Client-only registration and input handling. */
public final class ClientEvents {

    @Mod.EventBusSubscriber(modid = VoidAscension.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(ModItems.VOID_CORE.get(), VoidAscension.id("bound"),
                    (stack, level, entity, seed) -> VoidCoreItem.isBound(stack) ? 1.0F : 0.0F));
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "void_status", VoidHudOverlay.INSTANCE);
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(VoidKeyMappings.OPEN_PROGRESSION);
            event.register(VoidKeyMappings.VOID_DASH);
            event.register(VoidKeyMappings.OPEN_COMPANIONS);
            event.register(VoidKeyMappings.COMPANION_ABILITY);
            event.register(VoidKeyMappings.CYCLE_COMPANION);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.ABYSSAL_WISP.get(), AbyssalWispRenderer::new);
            event.registerEntityRenderer(ModEntities.RIFT_STALKER.get(), RiftStalkerRenderer::new);
            event.registerEntityRenderer(ModEntities.NULL_ORACLE.get(), NullOracleRenderer::new);
            event.registerEntityRenderer(ModEntities.RIFT_BOLT.get(), RiftBoltRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(CompanionModelLayers.ABYSSAL_WISP, AbyssalWispModel::createBodyLayer);
            event.registerLayerDefinition(CompanionModelLayers.RIFT_STALKER, RiftStalkerModel::createBodyLayer);
            event.registerLayerDefinition(CompanionModelLayers.NULL_ORACLE, NullOracleModel::createBodyLayer);
        }

        private ModBus() {}
    }

    @Mod.EventBusSubscriber(modid = VoidAscension.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            while (VoidKeyMappings.OPEN_PROGRESSION.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new VoidProgressionScreen());
            }

            while (VoidKeyMappings.VOID_DASH.consumeClick()) {
                if (VoidNetwork.CHANNEL != null) {
                    VoidNetwork.CHANNEL.sendToServer(new UseAbilityPacket(VoidAbilities.VOID_DASH.id()));
                }
            }

            while (VoidKeyMappings.OPEN_COMPANIONS.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new VoidCompanionScreen());
            }

            while (VoidKeyMappings.COMPANION_ABILITY.consumeClick()) {
                VoidPlayerData d = ClientVoidData.get();
                CompanionSpecies sp = d.getSelectedCompanion();
                if (sp != null) {
                    CompanionData cd = d.getCompanion(sp);
                    int abilityIdx = cd != null ? cd.getSelectedAbilityIndex() : 0;
                    if (VoidNetwork.CHANNEL != null) {
                        VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                                CompanionActionPacket.Action.USE_ABILITY, sp, abilityIdx));
                    }
                }
            }

            while (VoidKeyMappings.CYCLE_COMPANION.consumeClick()) {
                VoidPlayerData d = ClientVoidData.get();
                List<CompanionData> activeList = d.getActiveCompanions();
                if (!activeList.isEmpty()) {
                    int curIdx = 0;
                    for (int i = 0; i < activeList.size(); i++) {
                        if (activeList.get(i).getSpecies() == d.getSelectedCompanion()) {
                            curIdx = i;
                            break;
                        }
                    }
                    int nextIdx = (curIdx + 1) % activeList.size();
                    CompanionSpecies nextSpecies = activeList.get(nextIdx).getSpecies();
                    if (VoidNetwork.CHANNEL != null) {
                        VoidNetwork.CHANNEL.sendToServer(new CompanionActionPacket(
                                CompanionActionPacket.Action.SELECT, nextSpecies, 0));
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientVoidData.clear();
        }

        private ForgeBus() {}
    }

    private ClientEvents() {}
}
