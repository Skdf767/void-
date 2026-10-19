package com.voidascension.gametest;

import com.mojang.authlib.GameProfile;
import com.voidascension.core.CoreService;
import com.voidascension.data.VoidCapabilities;
import com.voidascension.data.VoidPlayerData;
import com.voidascension.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.UUID;

/** Shared helpers for the GameTests. */
final class TestSupport {
    static final String TEMPLATE = "empty";

    /**
     * A real ServerPlayer (Forge FakePlayer) with capabilities attached. Vanilla makeMockServerPlayerInLevel()
     * cannot be used on Forge 1.20.1 (the patched PlayerList dereferences the mock connection's null channel).
     * Note FakePlayer is invulnerable, so damage handlers are tested by posting the Forge events directly.
     */
    static ServerPlayer newPlayer(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "va_" + id.toString().substring(0, 8)));
    }

    static VoidPlayerData data(Player player) {
        return VoidCapabilities.get(player).orElseThrow(() -> new IllegalStateException("void data capability missing"));
    }

    /** New player with an activated Core in their inventory. */
    static ServerPlayer activatedPlayer(GameTestHelper helper) {
        ServerPlayer p = newPlayer(helper);
        ItemStack core = new ItemStack(ModItems.VOID_CORE.get());
        CoreService.bind(p, core, false);
        p.setItemInHand(InteractionHand.OFF_HAND, core); // keep the main hand free for test items
        return p;
    }

    static void cleanup(ServerPlayer... players) {
        for (ServerPlayer p : players) p.discard();
    }

    static AbstractContainerMenu dummyMenu() {
        return new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

            @Override
            public boolean stillValid(Player player) { return true; }
        };
    }

    private TestSupport() {}
}
