package com.fieldofmemories.client;

import com.fieldofmemories.entity.RunningShadowEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RunningShadowRenderer extends MobRenderer<RunningShadowEntity, PlayerModel<RunningShadowEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("fieldofmemories", "textures/entity/running_shadow.png");

    public RunningShadowRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(RunningShadowEntity entity) {
        return TEXTURE;
    }
}
