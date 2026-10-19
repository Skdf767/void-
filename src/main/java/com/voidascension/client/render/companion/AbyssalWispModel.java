package com.voidascension.client.render.companion;

import com.voidascension.entity.companion.AbyssalWispEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Animated model for the Abyssal Wisp companion.
 * Features an undulating central core and four rotating energy tendrils.
 */
public class AbyssalWispModel<T extends AbyssalWispEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart tendril1;
    private final ModelPart tendril2;
    private final ModelPart tendril3;
    private final ModelPart tendril4;

    public AbyssalWispModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        this.tendril1 = root.getChild("tendril1");
        this.tendril2 = root.getChild("tendril2");
        this.tendril3 = root.getChild("tendril3");
        this.tendril4 = root.getChild("tendril4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("tendril1", CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(-1.0F, -4.0F, 4.0F, 2.0F, 8.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("tendril2", CubeListBuilder.create()
                .texOffs(6, 12)
                .addBox(4.0F, -4.0F, -1.0F, 1.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("tendril3", CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(-1.0F, -4.0F, -5.0F, 2.0F, 8.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("tendril4", CubeListBuilder.create()
                .texOffs(6, 12)
                .addBox(-5.0F, -4.0F, -1.0F, 1.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float bob = (float) Math.sin(ageInTicks * 0.1F) * 1.5F;
        this.core.y = 16.0F + bob;

        float rot = ageInTicks * 0.06F;
        this.tendril1.yRot = rot;
        this.tendril2.yRot = rot + (float) Math.PI * 0.5F;
        this.tendril3.yRot = rot + (float) Math.PI;
        this.tendril4.yRot = rot + (float) Math.PI * 1.5F;

        this.tendril1.y = 16.0F + bob * 0.5F;
        this.tendril2.y = 16.0F - bob * 0.5F;
        this.tendril3.y = 16.0F + bob * 0.5F;
        this.tendril4.y = 16.0F - bob * 0.5F;
    }
}
