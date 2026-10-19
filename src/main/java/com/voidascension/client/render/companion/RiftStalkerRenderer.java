package com.voidascension.client.render.companion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidascension.VoidAscension;
import com.voidascension.entity.companion.RiftStalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the Rift Stalker companion.
 */
public class RiftStalkerRenderer extends MobRenderer<RiftStalkerEntity, RiftStalkerModel<RiftStalkerEntity>> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(VoidAscension.MOD_ID, "textures/entity/companion/rift_stalker.png");

    public RiftStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new RiftStalkerModel<>(context.bakeLayer(CompanionModelLayers.RIFT_STALKER)), 0.35F);
    }

    @Override
    protected void scale(RiftStalkerEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.getStage().modelScale();
        poseStack.scale(s, s, s);
    }

    @Override
    protected int getBlockLightLevel(RiftStalkerEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(RiftStalkerEntity entity) {
        return TEXTURE;
    }
}
