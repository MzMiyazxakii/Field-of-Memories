package com.fieldofmemories.client;

import com.fieldofmemories.client.model.SmileFaceModel;
import com.fieldofmemories.entity.SmileFaceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class SmileFaceRenderer extends EntityRenderer<SmileFaceEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("fieldofmemories", "textures/entity/smile_face.png");
    private final SmileFaceModel<SmileFaceEntity> model;

    public SmileFaceRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SmileFaceModel<>(context.bakeLayer(SmileFaceModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SmileFaceEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        // The Blockbench face mesh is vertically mirrored in model space.
        // Flip it around its vertical midpoint without changing its position.
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.scale(1.2F, -1.2F, 1.2F);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override public ResourceLocation getTextureLocation(SmileFaceEntity entity) { return TEXTURE; }
}
