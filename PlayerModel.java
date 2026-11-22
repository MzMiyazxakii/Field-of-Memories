// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class CustomModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "custommodel"), "main");
	private final ModelPart Player;
	private final ModelPart Waist;
	private final ModelPart Head;
	private final ModelPart Body;
	private final ModelPart Right Arm;
	private final ModelPart Left Arm;
	private final ModelPart Right Leg;
	private final ModelPart Right Lower;
	private final ModelPart Left Leg;
	private final ModelPart Left Lower;

	public CustomModel(ModelPart root) {
		this.Player = root.getChild("Player");
		this.Waist = this.Player.getChild("Waist");
		this.Head = this.Waist.getChild("Head");
		this.Body = this.Waist.getChild("Body");
		this.Right Arm = this.Waist.getChild("Right Arm");
		this.Left Arm = this.Waist.getChild("Left Arm");
		this.Right Leg = this.Player.getChild("Right Leg");
		this.Right Lower = this.Right Leg.getChild("Right Lower");
		this.Left Leg = this.Player.getChild("Left Leg");
		this.Left Lower = this.Left Leg.getChild("Left Lower");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Player = partdefinition.addOrReplaceChild("Player", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Waist = Player.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition Right Arm = Waist.addOrReplaceChild("Right Arm", CubeListBuilder.create().texOffs(24, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

		PartDefinition Left Arm = Waist.addOrReplaceChild("Left Arm", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -10.0F, 0.0F));

		PartDefinition Right Leg = Player.addOrReplaceChild("Right Leg", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, -12.0F, 0.0F));

		PartDefinition Right Lower = Right Leg.addOrReplaceChild("Right Lower", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition Left Leg = Player.addOrReplaceChild("Left Leg", CubeListBuilder.create().texOffs(16, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, -12.0F, 0.0F));

		PartDefinition Left Lower = Left Leg.addOrReplaceChild("Left Lower", CubeListBuilder.create().texOffs(16, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Player.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}