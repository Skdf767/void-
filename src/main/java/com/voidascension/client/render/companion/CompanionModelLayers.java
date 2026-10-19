package com.voidascension.client.render.companion;

import com.voidascension.VoidAscension;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class CompanionModelLayers {
    public static final ModelLayerLocation ABYSSAL_WISP =
            new ModelLayerLocation(VoidAscension.id("abyssal_wisp"), "main");
    public static final ModelLayerLocation RIFT_STALKER =
            new ModelLayerLocation(VoidAscension.id("rift_stalker"), "main");
    public static final ModelLayerLocation NULL_ORACLE =
            new ModelLayerLocation(VoidAscension.id("null_oracle"), "main");

    private CompanionModelLayers() {}
}
