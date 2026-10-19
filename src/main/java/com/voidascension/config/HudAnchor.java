package com.voidascension.config;

/** Screen corner the Void HUD is anchored to (client config {@code hud.anchor}). */
public enum HudAnchor {
    TOP_LEFT(false, false), TOP_RIGHT(true, false), BOTTOM_LEFT(false, true), BOTTOM_RIGHT(true, true);

    public final boolean right;
    public final boolean bottom;

    HudAnchor(boolean right, boolean bottom) {
        this.right = right;
        this.bottom = bottom;
    }
}
