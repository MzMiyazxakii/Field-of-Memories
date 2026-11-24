package com.fieldofmemories.client;

import com.fieldofmemories.client.model.ScarecrowModel;
import com.fieldofmemories.entity.ScarecrowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;

public class ScarecrowRenderer extends EntityRenderer<ScarecrowEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("fieldofmemories", "textures/entity/scarecrow.png");
    private final ScarecrowModel<ScarecrowEntity> model;

    public ScarecrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ScarecrowModel<>(context.bakeLayer(ScarecrowModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ScarecrowEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Match vanilla's upright model coordinate system: model pixels use +Y downward,
        // while world space uses +Y upward. EntityRenderer does not apply MobRenderer's flip.
        poseStack.translate(0.0D, 1.5D, 0.0D);
        // Blockbench model coordinates have the head at negative Y and legs at
        // positive Y. Flip the model vertically exactly once so the head is up.
        poseStack.scale(1.0F, -1.0F, 1.0F);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCullZOffset(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override public ResourceLocation getTextureLocation(ScarecrowEntity entity) { return TEXTURE; }
}
