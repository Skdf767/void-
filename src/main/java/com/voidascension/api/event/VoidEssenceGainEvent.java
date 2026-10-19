package com.voidascension.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired before a player gains Void Essence from gameplay. The amount can be modified, or the event cancelled.
 * Not fired for admin commands.
 */
@Cancelable
public class VoidEssenceGainEvent extends PlayerEvent {
    public enum Source { KILL, FRAGMENT, END_PASSIVE, OTHER }

    private final Source source;
    private int amount;

    public VoidEssenceGainEvent(Player player, Source source, int amount) {
        super(player);
        this.source = source;
        this.amount = amount;
    }

    public Source getSource() { return source; }

    public int getAmount() { return amount; }

    public void setAmount(int amount) { this.amount = Math.max(0, amount); }
}
