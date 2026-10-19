package com.voidascension.network;

import com.voidascension.ability.AbilityService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: request to use an active ability. The id is length-limited and validated server-side. */
public record UseAbilityPacket(String abilityId) {
    private static final int MAX_ID_LENGTH = 64;

    public static void encode(UseAbilityPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.abilityId, MAX_ID_LENGTH);
    }

    public static UseAbilityPacket decode(FriendlyByteBuf buf) {
        return new UseAbilityPacket(buf.readUtf(MAX_ID_LENGTH));
    }

    public static void handle(UseAbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        AbilityService.tryUse(player, msg.abilityId);
    }
}
