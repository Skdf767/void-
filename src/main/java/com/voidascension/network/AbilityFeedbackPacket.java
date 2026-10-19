package com.voidascension.network;

import com.voidascension.ability.AbilityService;
import com.voidascension.data.ClientVoidData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: an ability request was denied. Purely informational - lets the HUD flash a warning
 * (e.g. "Not enough Essence 4/10"). The server has already decided; nothing here is trusted for gameplay.
 */
public record AbilityFeedbackPacket(String abilityId, AbilityService.UseResult result, int required, int available,
                                    int cooldownTicks) {
    private static final int MAX_ID_LENGTH = 64;

    public static void encode(AbilityFeedbackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.abilityId, MAX_ID_LENGTH);
        buf.writeEnum(msg.result);
        buf.writeVarInt(Math.max(0, msg.required));
        buf.writeVarInt(Math.max(0, msg.available));
        buf.writeVarInt(Math.max(0, msg.cooldownTicks));
    }

    public static AbilityFeedbackPacket decode(FriendlyByteBuf buf) {
        return new AbilityFeedbackPacket(buf.readUtf(MAX_ID_LENGTH), buf.readEnum(AbilityService.UseResult.class),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(AbilityFeedbackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ClientVoidData.pushFeedback(msg.abilityId, msg.result, msg.required, msg.available, msg.cooldownTicks);
    }
}
