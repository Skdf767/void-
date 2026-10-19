package com.voidascension.event;

import com.voidascension.VoidAscension;
import com.voidascension.api.event.VoidEssenceGainEvent;
import com.voidascension.config.VoidConfig;
import com.voidascension.progression.VoidEssenceService;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Gameplay sources of Void Essence: kills and the passive trickle in The End. */
@Mod.EventBusSubscriber(modid = VoidAscension.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EssenceSourceEvents {
    public static final TagKey<EntityType<?>> VOID_CREATURES = TagKey.create(Registries.ENTITY_TYPE, VoidAscension.id("void_creatures"));

    /** Essence for killing {@code victim} in {@code level}, before Ascendance/events. */
    public static int killReward(LivingEntity victim, Level level) {
        double base = Math.max(1.0, Math.floor(victim.getMaxHealth() / VoidConfig.get(VoidConfig.HEALTH_PER_ESSENCE)));
        if (victim.getType().is(VOID_CREATURES)) base *= VoidConfig.get(VoidConfig.VOID_CREATURE_MULTIPLIER);
        if (level.dimension() == Level.END) base *= VoidConfig.get(VoidConfig.END_DIMENSION_MULTIPLIER);
        return (int) Math.round(base);
    }

    /** LOWEST priority + canceled events skipped: no reward if another mod (e.g. a totem-like effect) cancels the death. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim instanceof Player) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            VoidEssenceService.grantFromGameplay(killer, killReward(victim, victim.level()), VoidEssenceGainEvent.Source.KILL);
            com.voidascension.companion.CompanionService.onKillVictim(killer, victim);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        if (sp.level().dimension() != Level.END) return;
        int interval = VoidConfig.get(VoidConfig.END_PASSIVE_INTERVAL);
        if (interval <= 0 || sp.tickCount % interval != 0) return;
        VoidEssenceService.grantFromGameplay(sp, VoidConfig.get(VoidConfig.END_PASSIVE_AMOUNT), VoidEssenceGainEvent.Source.END_PASSIVE);
    }

    private EssenceSourceEvents() {}
}
