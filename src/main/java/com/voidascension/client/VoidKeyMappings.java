package com.voidascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Rebindable key mappings (Options > Controls > Key Binds > VOID: ASCENSION). */
public final class VoidKeyMappings {
    public static final String CATEGORY = "key.categories.void_ascension";

    public static final KeyMapping OPEN_PROGRESSION = new KeyMapping("key.void_ascension.open_progression",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping VOID_DASH = new KeyMapping("key.void_ascension.void_dash",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    // Companion controls (0.2.0)
    public static final KeyMapping OPEN_COMPANIONS = new KeyMapping("key.void_ascension.open_companions",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping COMPANION_ABILITY = new KeyMapping("key.void_ascension.companion_ability",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping CYCLE_COMPANION = new KeyMapping("key.void_ascension.cycle_companion",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    private VoidKeyMappings() {}
}
