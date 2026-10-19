package com.voidascension.network;

import com.voidascension.VoidAscension;
import com.voidascension.data.PlayerDataSerializer;
import com.voidascension.data.VoidCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network channel. Protocol "4" (0.2.0; "3" was 0.2.0-alpha.1; "2" was 0.1.0-alpha);
 * clients and servers with a different protocol refuse to connect.
 * <ul>
 *     <li>S2C {@link SyncVoidDataPacket} - full snapshot of the receiving player's own data (display only).</li>
 *     <li>C2S {@link RequestEvolutionPacket} - "evolve" button; fully re-validated by the server.</li>
 *     <li>C2S {@link UseAbilityPacket} - ability key; fully re-validated by the server.</li>
 *     <li>S2C {@link AbilityFeedbackPacket} - why an ability request was denied (HUD warning only).</li>
 *     <li>C2S {@link CompanionActionPacket} - companion UI and control actions (validated on server).</li>
 *     <li>S2C {@link CompanionFeedbackPacket} - companion action outcome notification.</li>
 * </ul>
 */
public final class VoidNetwork {
    public static final String PROTOCOL = "4";
    public static SimpleChannel CHANNEL;

    public static void register() {
        if (CHANNEL != null) return;
        CHANNEL = NetworkRegistry.newSimpleChannel(VoidAscension.id("main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        int id = 0;
        CHANNEL.messageBuilder(SyncVoidDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncVoidDataPacket::encode)
                .decoder(SyncVoidDataPacket::decode)
                .consumerMainThread(SyncVoidDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestEvolutionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestEvolutionPacket::encode)
                .decoder(RequestEvolutionPacket::decode)
                .consumerMainThread(RequestEvolutionPacket::handle)
                .add();
        CHANNEL.messageBuilder(UseAbilityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UseAbilityPacket::encode)
                .decoder(UseAbilityPacket::decode)
                .consumerMainThread(UseAbilityPacket::handle)
                .add();
        CHANNEL.messageBuilder(AbilityFeedbackPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilityFeedbackPacket::encode)
                .decoder(AbilityFeedbackPacket::decode)
                .consumerMainThread(AbilityFeedbackPacket::handle)
                .add();
        CHANNEL.messageBuilder(CompanionActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionActionPacket::encode)
                .decoder(CompanionActionPacket::decode)
                .consumerMainThread(CompanionActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(CompanionFeedbackPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CompanionFeedbackPacket::encode)
                .decoder(CompanionFeedbackPacket::decode)
                .consumerMainThread(CompanionFeedbackPacket::handle)
                .add();
    }

    /** Tells the player's client why an ability was denied. No-op for fake players or before registration. */
    public static void sendFeedback(ServerPlayer player, AbilityFeedbackPacket packet) {
        if (CHANNEL == null || player instanceof FakePlayer || player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Tells the player's client about companion action results. */
    public static void sendCompanionFeedback(ServerPlayer player, CompanionFeedbackPacket packet) {
        if (CHANNEL == null || player instanceof FakePlayer || player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Sends the player's current data to their own client. No-op for fake players or before registration. */
    public static void sync(ServerPlayer player) {
        if (CHANNEL == null || player instanceof FakePlayer || player.connection == null) return;
        VoidCapabilities.get(player).ifPresent(data -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncVoidDataPacket(PlayerDataSerializer.write(data))));
    }

    private VoidNetwork() {}
}
