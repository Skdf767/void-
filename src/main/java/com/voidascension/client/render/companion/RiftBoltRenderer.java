package com.voidascension.client.render.companion;

import com.voidascension.entity.companion.RiftBoltEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Renderer for the Rift Bolt projectile.
 */
public class RiftBoltRenderer extends ThrownItemRenderer<RiftBoltEntity> {

    public RiftBoltRenderer(EntityRendererProvider.Context context) {
        super(context, 1.2F, true);
    }
}
