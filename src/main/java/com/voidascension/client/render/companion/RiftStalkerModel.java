package com.voidascension.client.render.companion;

import com.voidascension.entity.companion.RiftStalkerEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Animated model for the Rift Stalker companion.
 * Features an angular crystal core and hovering predatory blade-wings.
 */
public class RiftStalkerModel<T extends RiftStalkerEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart crest;

    public RiftStalkerModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.crest = root.getChild("crest");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.5F, -3.5F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(0.0F, -4.0F, -1.0F, 1.0F, 8.0F, 5.0F), PartPose.offset(3.5F, 16.0F, 0.0F));

        root.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(12, 12)
                .addBox(-1.0F, -4.0F, -1.0F, 1.0F, 8.0F, 5.0F), PartPose.offset(-3.5F, 16.0F, 0.0F));

        root.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(20, 0)
                .addBox(-1.5F, -6.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float bob = (float) Math.sin(ageInTicks * 0.12F) * 1.2F;
        this.core.y = 16.0F + bob;
        this.crest.y = 16.0F + bob;

        float wingAngle = (float) Math.sin(ageInTicks * 0.15F) * 0.2F;
        this.leftWing.yRot = -0.35F + wingAngle;
        this.rightWing.yRot = 0.35F - wingAngle;

        this.leftWing.y = 16.0F + bob;
        this.rightWing.y = 16.0F + bob;
    }
}
