package com.voidascension.client.render.companion;

import com.voidascension.entity.companion.NullOracleEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Animated model for the Null Oracle companion.
 * Features a floating mystic prism surrounded by dual gyroscopic rings.
 */
public class NullOracleModel<T extends NullOracleEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart prism;
    private final ModelPart innerRing;
    private final ModelPart outerRing;

    public NullOracleModel(ModelPart root) {
        this.root = root;
        this.prism = root.getChild("prism");
        this.innerRing = root.getChild("inner_ring");
        this.outerRing = root.getChild("outer_ring");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("prism", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("inner_ring", CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(-5.0F, -0.5F, -5.0F, 10.0F, 1.0F, 10.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("outer_ring", CubeListBuilder.create()
                .texOffs(0, 23)
                .addBox(-7.0F, -7.0F, -0.5F, 14.0F, 14.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 48, 48);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float bob = (float) Math.sin(ageInTicks * 0.08F) * 1.5F;
        this.prism.y = 16.0F + bob;
        this.innerRing.y = 16.0F + bob;
        this.outerRing.y = 16.0F + bob;

        // Gyroscopic rotation
        this.prism.yRot = -ageInTicks * 0.04F;
        this.innerRing.yRot = ageInTicks * 0.06F;
        this.outerRing.xRot = ageInTicks * 0.04F;
        this.outerRing.zRot = ageInTicks * 0.03F;
    }
}
