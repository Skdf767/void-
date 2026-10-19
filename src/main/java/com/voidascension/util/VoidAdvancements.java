package com.voidascension.util;

import com.voidascension.VoidAscension;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

/** Awards code-triggered advancements (those using the minecraft:impossible trigger with a "code" criterion). */
public final class VoidAdvancements {
    public static final String BIND_CORE = "bind_core";
    public static final String EVOLVE = "evolve";
    public static final String ASCEND = "ascend";
    public static final String VOID_RESCUE = "void_rescue";

    public static void award(ServerPlayer player, String path) {
        if (player.server == null) return;
        Advancement adv = player.server.getAdvancements().getAdvancement(VoidAscension.id(path));
        if (adv != null) {
            player.getAdvancements().award(adv, "code");
        }
    }

    private VoidAdvancements() {}
}
