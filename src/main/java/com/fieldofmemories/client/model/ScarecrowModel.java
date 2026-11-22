package com.fieldofmemories.client.model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.animation.KeyframeAnimations;
import com.fieldofmemories.entity.ScarecrowEntity;
import org.joml.Vector3f;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class ScarecrowModel<T extends Entity> extends HierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("fieldofmemories", "scarecrow"), "main");
	private final ModelPart root;
	private final ModelPart Pug;
	private final ModelPart Tors;
	private final ModelPart LeftArm;
	private final ModelPart LeftH;
	private final ModelPart Head;
	private final ModelPart LeftLeg;
	private final ModelPart RightLeg;
	private final ModelPart RightArm;
	private final ModelPart RightH;

	public ScarecrowModel(ModelPart root) {
		this.root = root;
		this.Pug = root.getChild("Pug");
		this.Tors = this.Pug.getChild("Tors");
		this.LeftArm = this.Pug.getChild("LeftArm");
		this.LeftH = this.LeftArm.getChild("LeftH");
		this.Head = this.Pug.getChild("Head");
		this.LeftLeg = this.Pug.getChild("LeftLeg");
		this.RightLeg = this.Pug.getChild("RightLeg");
		this.RightArm = this.Pug.getChild("RightArm");
		this.RightH = this.RightArm.getChild("RightH");
	}

	@Override
	public ModelPart root() {
		return Pug;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Pug = partdefinition.addOrReplaceChild("Pug", CubeListBuilder.create(), PartPose.offset(0.6585F, 4.1421F, 1.4857F));

		PartDefinition Tors = Pug.addOrReplaceChild("Tors", CubeListBuilder.create(), PartPose.offset(0.75F, 4.5F, -0.25F));

		PartDefinition cube_r1 = Tors.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(12, 36).addBox(-1.5F, -12.0F, -1.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(56, 40).addBox(-0.5F, -19.0F, 0.0F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 12.0F, 0.0F, 0.0F, -0.7418F, 0.0F));

		PartDefinition LeftArm = Pug.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(38, 44).addBox(-0.6689F, -0.5F, -0.6843F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 0.0F, 0.5F));

		PartDefinition LeftH = LeftArm.addOrReplaceChild("LeftH", CubeListBuilder.create().texOffs(39, 49).addBox(-0.6689F, -0.5F, -0.4343F, 7.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 0.0F, -0.25F));

		PartDefinition Head = Pug.addOrReplaceChild("Head", CubeListBuilder.create(), PartPose.offset(0.5F, -2.5F, 0.0F));

		PartDefinition cube_r2 = Head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 13).addBox(-3.5F, -18.0F, -3.0F, 7.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 10.0F, -0.25F, 0.0F, -0.7418F, 0.0F));

		PartDefinition LeftLeg = Pug.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offset(2.0F, 6.5F, 0.0F));

		PartDefinition cube_r3 = LeftLeg.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(24, 28).addBox(-1.5F, -10.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6331F, 16.2431F, -2.167F, -0.0436F, -0.7418F, 0.0F));

		PartDefinition cube_r4 = LeftLeg.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 36).addBox(-1.5F, -17.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 16.0F, -4.75F, -0.3927F, -0.7418F, 0.0F));

		PartDefinition RightLeg = Pug.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offset(-1.0F, 6.5F, 0.5F));

		PartDefinition cube_r5 = RightLeg.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(28, 13).addBox(-7.8271F, -9.8307F, -1.4516F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(36, 37).addBox(-7.8271F, 3.1693F, -1.4516F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(28, 38).addBox(-6.8271F, -0.8307F, -0.4516F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(36, 33).addBox(-7.8271F, -0.8307F, -1.4516F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1585F, 8.3579F, -6.9857F, -2.0858F, 1.4382F, -1.7134F));

		PartDefinition RightArm = Pug.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(39, 46).addBox(-6.6689F, -0.5F, -0.1843F, 7.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightH = RightArm.addOrReplaceChild("RightH", CubeListBuilder.create().texOffs(39, 47).addBox(-6.9189F, -0.75F, -0.6843F, 7.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.75F, 0.25F, 0.5F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (entity instanceof ScarecrowEntity scarecrow) {
            switch (scarecrow.getPoseVariant()) {
                case 1 -> KeyframeAnimations.animate(this, ScarecrowAnimation.stalking,
                        (long) (ageInTicks * 50.0F), 1.0F, new Vector3f());
                case 2 -> KeyframeAnimations.animate(this, ScarecrowAnimation.stalking2,
                        0L, 1.0F, new Vector3f());
                default -> { }
            }
        }
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Pug.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}