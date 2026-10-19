package com.voidascension.client.render.companion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidascension.VoidAscension;
import com.voidascension.entity.companion.AbyssalWispEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the Abyssal Wisp companion.
 */
public class AbyssalWispRenderer extends MobRenderer<AbyssalWispEntity, AbyssalWispModel<AbyssalWispEntity>> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(VoidAscension.MOD_ID, "textures/entity/companion/abyssal_wisp.png");

    public AbyssalWispRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalWispModel<>(context.bakeLayer(CompanionModelLayers.ABYSSAL_WISP)), 0.3F);
    }

    @Override
    protected void scale(AbyssalWispEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.getStage().modelScale();
        poseStack.scale(s, s, s);
    }

    @Override
    protected int getBlockLightLevel(AbyssalWispEntity entity, BlockPos pos) {
        return 15; // Natural glowing aura
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalWispEntity entity) {
        return TEXTURE;
    }
}
