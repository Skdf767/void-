package com.voidascension.network;

import com.voidascension.evolution.EvolutionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the player pressed "Evolve". Carries no data; the server decides everything. */
public record RequestEvolutionPacket() {

    public static void encode(RequestEvolutionPacket msg, FriendlyByteBuf buf) {}

    public static RequestEvolutionPacket decode(FriendlyByteBuf buf) { return new RequestEvolutionPacket(); }

    public static void handle(RequestEvolutionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        EvolutionService.tryAdvanceWithFeedback(player);
    }
}
