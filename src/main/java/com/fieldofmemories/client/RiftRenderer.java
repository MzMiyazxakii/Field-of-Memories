package com.fieldofmemories.client;

import com.fieldofmemories.entity.RiftEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RiftRenderer extends EntityRenderer<RiftEntity> {
    public RiftRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(RiftEntity entity) {
        return new ResourceLocation("minecraft", "textures/particle/smoke.png");
    }
}