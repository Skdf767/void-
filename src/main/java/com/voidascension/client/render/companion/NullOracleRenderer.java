package com.voidascension.client.render.companion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidascension.VoidAscension;
import com.voidascension.entity.companion.NullOracleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the Null Oracle companion.
 */
public class NullOracleRenderer extends MobRenderer<NullOracleEntity, NullOracleModel<NullOracleEntity>> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(VoidAscension.MOD_ID, "textures/entity/companion/null_oracle.png");

    public NullOracleRenderer(EntityRendererProvider.Context context) {
        super(context, new NullOracleModel<>(context.bakeLayer(CompanionModelLayers.NULL_ORACLE)), 0.3F);
    }

    @Override
    protected void scale(NullOracleEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.getStage().modelScale();
        poseStack.scale(s, s, s);
    }

    @Override
    protected int getBlockLightLevel(NullOracleEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(NullOracleEntity entity) {
        return TEXTURE;
    }
}
