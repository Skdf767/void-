package com.voidascension.api.event;

import com.voidascension.evolution.VoidTier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} when a player evolves to a new tier
 * through their Void Core. Add-ons can veto ({@link Pre}) or react to ({@link Post}) evolutions.
 */
public abstract class VoidEvolutionEvent extends PlayerEvent {
    private final VoidTier from;
    private final VoidTier to;

    protected VoidEvolutionEvent(Player player, VoidTier from, VoidTier to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public VoidTier getFrom() { return from; }

    public VoidTier getTo() { return to; }

    /** Cancel to block the evolution. No Essence is spent when cancelled. */
    @Cancelable
    public static class Pre extends VoidEvolutionEvent {
        public Pre(Player player, VoidTier from, VoidTier to) { super(player, from, to); }
    }

    /** Fired after the tier has been applied and Essence spent. */
    public static class Post extends VoidEvolutionEvent {
        public Post(Player player, VoidTier from, VoidTier to) { super(player, from, to); }
    }
}
