package com.fieldofmemories.client;

import com.fieldofmemories.entity.WindowStalkerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WindowStalkerRenderer extends MobRenderer<WindowStalkerEntity, PlayerModel<WindowStalkerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("fieldofmemories", "textures/entity/window_stalker.png");

    public WindowStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.35F);
    }

    @Override public ResourceLocation getTextureLocation(WindowStalkerEntity entity) { return TEXTURE; }
}
