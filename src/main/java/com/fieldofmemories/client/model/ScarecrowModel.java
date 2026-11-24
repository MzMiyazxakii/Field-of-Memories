package com.fieldofmemories.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.animation.KeyframeAnimations;
import com.fieldofmemories.entity.ScarecrowEntity;
import org.joml.Vector3f;

// Model exported from the supplied Blockbench Pugalo.java and adapted for Field of Memories.
public class ScarecrowModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("fieldofmemories", "scarecrow"), "main");

    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public ScarecrowModel(ModelPart root) {
        this.waist = root.getChild("Waist");
        this.head = this.waist.getChild("Head");
        this.body = this.waist.getChild("Body");
        this.rightArm = this.waist.getChild("Right Arm");
        this.leftArm = this.waist.getChild("Left Arm");
    }

    @Override
    public ModelPart root() {
        return waist;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // Give the paper-thin hat brim a tiny thickness so its faces have stable normals.
        Head.addOrReplaceChild("Head_r1", CubeListBuilder.create()
                .texOffs(28, 31).addBox(-4.0F, -9.0F, -3.0F, 8.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-7.0F, -8.0F, -7.0F, 14.0F, 0.05F, 15.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -3.0F, -0.5F, 0.2618F, 0.0F, 0.0F));

        // Thin side panels are given minimal depth to prevent angle-dependent flicker.
        Head.addOrReplaceChild("Head_r2", CubeListBuilder.create()
                .texOffs(46, 40).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -3.0F, -0.25F, 0.2618F, 0.0F, 0.0F));

        Head.addOrReplaceChild("Head_r3", CubeListBuilder.create()
                .texOffs(48, 41).addBox(5.0F, -8.0F, -4.0F, 0.05F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(48, 41).addBox(13.225F, -8.0F, -4.0F, 0.05F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(4, 15).addBox(5.1F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-9.1F, -3.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

        Head.addOrReplaceChild("Head_r4", CubeListBuilder.create()
                .texOffs(31, 49).addBox(-4.0F, -8.0F, 5.0F, 8.0F, 8.0F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.75F, -0.875F, 0.2618F, 0.0F, 0.0F));

        PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create()
                .texOffs(0, 15).addBox(-0.5F, -21.0F, -0.5F, 1.0F, 45.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(4, 31).addBox(-3.5F, -2.0F, -2.5F, 7.0F, 9.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(36, 15).addBox(3.5F, -2.0F, -2.5F, 7.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(28, 39).addBox(10.5F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(28, 39).addBox(-15.5F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 15).addBox(-10.5F, -2.0F, -2.5F, 7.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -11.25F, 0.0F));

        Body.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(36, 23).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 9.75F, 0.0F, 0.0F, 0.7418F, 0.0F));

        Waist.addOrReplaceChild("Right Arm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.0F, -10.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
        Waist.addOrReplaceChild("Left Arm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.0F, -10.0F, 0.0F, 0.2094F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        waist.getAllParts().forEach(ModelPart::resetPose);
        if (entity instanceof ScarecrowEntity scarecrow) {
            switch (scarecrow.getPoseVariant()) {
                case 1 -> KeyframeAnimations.animate(this, ScarecrowAnimation.idle2, 0L, 1.0F, new Vector3f());
                case 2 -> KeyframeAnimations.animate(this, ScarecrowAnimation.idle3, 0L, 1.0F, new Vector3f());
                default -> KeyframeAnimations.animate(this, ScarecrowAnimation.idle, 0L, 1.0F, new Vector3f());
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}