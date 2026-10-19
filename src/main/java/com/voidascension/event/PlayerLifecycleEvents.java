package com.voidascension.event;

import com.voidascension.VoidAscension;
import com.voidascension.command.VoidCommand;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.evolution.TierAttributes;
import com.voidascension.evolution.VoidTier;
import com.voidascension.progression.ProgressionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Persistence and lifecycle: attaches the data capability, copies it across death/End-return cloning,
 * and revalidates + re-syncs on login, respawn and dimension change.
 */
@Mod.EventBusSubscriber(modid = VoidAscension.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerLifecycleEvents {

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            // Note: no invalidate listener - the old player's caps must remain revivable for Clone.
            event.addCapability(VoidCapabilities.KEY, new VoidCapabilities.Provider(player));
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        Player fresh = event.getEntity();
        VoidCapabilities.get(original).ifPresent(old ->
                VoidCapabilities.get(fresh).ifPresent(d -> {
                    d.copyFrom(old);
                    // Transient tier modifiers are not copied by vanilla; re-apply so max health is correct at once.
                    TierAttributes.apply(fresh, d.isInitialized() ? d.getTier() : VoidTier.HUMAN);
                    if (event.isWasDeath()) {
                        fresh.setHealth(fresh.getMaxHealth()); // respawn at full (boosted) health, like vanilla
                    } else {
                        // End return: vanilla copied health before our bonus existed and clamped it to 20 (defect D2).
                        fresh.setHealth(Math.min(original.getHealth(), fresh.getMaxHealth()));
                    }
                }));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ProgressionService.revalidate(sp);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ProgressionService.revalidate(sp);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ProgressionService.revalidate(sp);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        VoidCommand.register(event.getDispatcher());
    }

    private PlayerLifecycleEvents() {}
}
