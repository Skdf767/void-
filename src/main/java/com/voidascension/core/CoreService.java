package com.voidascension.core;

import com.voidascension.api.event.VoidCoreBoundEvent;
import com.voidascension.config.VoidConfig;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.item.VoidCoreItem;
import com.voidascension.network.VoidNetwork;
import com.voidascension.progression.ProgressionService;
import com.voidascension.util.VoidAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;
import java.util.UUID;

/**
 * Void Core activation and bond rules (server-side only).
 * <ul>
 *     <li>A Core can only be bound by the player holding it; a Core bound to someone else is rejected.</li>
 *     <li>First activation initializes progression (HUMAN / 0 Essence / Level 0).</li>
 *     <li>A player has at most one active Core. Binding a new Core while another is active requires sneaking and
 *     costs {@code core.rebindPenaltyPercent} of Essence; the old Core becomes severed (inert).</li>
 *     <li>If the player has no active Core (lost it, or an admin unbound it), binding a Core is free.</li>
 *     <li>Re-activating never resets progression; progression is stored on the player, not on the item.</li>
 * </ul>
 */
public final class CoreService {

    public enum BindResult {
        INITIALIZED, BOUND, TRANSFERRED, NEEDS_SNEAK_TO_TRANSFER, ALREADY_ACTIVE, OWNED_BY_OTHER, NOT_A_CORE, NO_DATA;

        public boolean success() { return this == INITIALIZED || this == BOUND || this == TRANSFERRED; }
    }

    public static BindResult bind(ServerPlayer player, ItemStack stack, boolean allowTransfer) {
        if (!(stack.getItem() instanceof VoidCoreItem)) return BindResult.NOT_A_CORE;
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty()) return BindResult.NO_DATA;
        VoidPlayerData data = opt.get();

        if (VoidCoreItem.isBound(stack) && !VoidCoreItem.isOwnedBy(stack, player)) return BindResult.OWNED_BY_OTHER;
        if (VoidCoreItem.isActiveFor(stack, player, data)) return BindResult.ALREADY_ACTIVE;

        boolean hadActiveCore = data.hasBoundCore();
        if (hadActiveCore && !allowTransfer) return BindResult.NEEDS_SNEAK_TO_TRANSFER;

        BindResult result;
        if (ProgressionService.initialize(player, data)) {
            result = BindResult.INITIALIZED;
        } else if (hadActiveCore) {
            int penalty = data.getEssence() * VoidConfig.get(VoidConfig.REBIND_PENALTY_PERCENT) / 100;
            data.setEssence(data.getEssence() - penalty);
            result = BindResult.TRANSFERRED;
        } else {
            result = BindResult.BOUND;
        }

        UUID coreId = UUID.randomUUID();
        VoidCoreItem.writeBond(stack, player, coreId);
        data.setCoreId(coreId);
        data.setOwner(player.getUUID());
        VoidNetwork.sync(player);

        MinecraftForge.EVENT_BUS.post(new VoidCoreBoundEvent(player, stack, coreId, result == BindResult.TRANSFERRED));
        VoidAdvancements.award(player, VoidAdvancements.BIND_CORE);

        String key = switch (result) {
            case INITIALIZED -> "message.void_ascension.core.initialized";
            case TRANSFERRED -> "message.void_ascension.core.transferred";
            default -> "message.void_ascension.core.bound";
        };
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.4F, 1.4F);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                60, 0.4, 0.8, 0.4, 0.1);
        return result;
    }

    /** Admin: severs the player's bond. Progression is kept; any Core they bind next is free and becomes active. */
    public static boolean unbind(ServerPlayer player) {
        Optional<VoidPlayerData> opt = VoidCapabilities.get(player);
        if (opt.isEmpty() || !opt.get().hasBoundCore()) return false;
        opt.get().setCoreId(null);
        VoidNetwork.sync(player);
        return true;
    }

    /** True if the player's inventory (incl. offhand) contains their currently active Core. */
    public static boolean hasActiveCore(Player player) {
        return !findActiveCore(player).isEmpty();
    }

    public static ItemStack findActiveCore(Player player) {
        VoidPlayerData data = VoidCapabilities.forPlayer(player);
        if (!data.hasBoundCore()) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (VoidCoreItem.isActiveFor(stack, player, data)) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (VoidCoreItem.isActiveFor(stack, player, data)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private CoreService() {}
}
