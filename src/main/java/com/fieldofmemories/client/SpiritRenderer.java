package com.fieldofmemories.client;

import com.fieldofmemories.entity.SpiritEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class SpiritRenderer extends MobRenderer<SpiritEntity, PlayerModel<SpiritEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("fieldofmemories", "textures/entity/evilsould.png");

    public SpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SpiritEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (!VideoCameraClient.isViewing()) return;
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritEntity entity) {
        return TEXTURE;
    }
}