package com.voidascension.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/** Fired after a Void Core has been bound to a player. {@code transferred} is true if an older bond was severed. */
public class VoidCoreBoundEvent extends PlayerEvent {
    private final ItemStack core;
    private final UUID coreId;
    private final boolean transferred;

    public VoidCoreBoundEvent(Player player, ItemStack core, UUID coreId, boolean transferred) {
        super(player);
        this.core = core;
        this.coreId = coreId;
        this.transferred = transferred;
    }

    public ItemStack getCore() { return core; }

    public UUID getCoreId() { return coreId; }

    public boolean wasTransferred() { return transferred; }
}
