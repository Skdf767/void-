package com.voidascension.network;

import com.voidascension.data.ClientVoidData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: full snapshot of the receiving player's Void data. */
public record SyncVoidDataPacket(CompoundTag data) {

    public static void encode(SyncVoidDataPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncVoidDataPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncVoidDataPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncVoidDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // consumerMainThread already enqueues onto the client thread and marks the packet handled.
        ClientVoidData.apply(msg.data);
    }
}
