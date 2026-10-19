package com.voidascension.network;

import com.voidascension.companion.CompanionService;
import com.voidascension.companion.CompanionSpecies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C packet: reports the outcome of a companion action to the client for UI/HUD effects.
 */
public record CompanionFeedbackPacket(CompanionService.ActionResult result, CompanionSpecies species, int abilityIndex) {

    public static void encode(CompanionFeedbackPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.result);
        buf.writeEnum(msg.species);
        buf.writeVarInt(msg.abilityIndex);
    }

    public static CompanionFeedbackPacket decode(FriendlyByteBuf buf) {
        CompanionService.ActionResult result = buf.readEnum(CompanionService.ActionResult.class);
        CompanionSpecies species = buf.readEnum(CompanionSpecies.class);
        int abilityIndex = buf.readVarInt();
        return new CompanionFeedbackPacket(result, species, abilityIndex);
    }

    public static void handle(CompanionFeedbackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side visual or HUD notification hooks
        });
        ctx.get().setPacketHandled(true);
    }
}
