package com.fieldofmemories.client;

import com.fieldofmemories.entity.PlayerEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class PlayerEchoRenderer extends MobRenderer<PlayerEchoEntity, PlayerModel<PlayerEchoEntity>> {
    public PlayerEchoRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerEchoEntity entity) {
        var uuid = entity.getSkinOwner();
        if (uuid == null) return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(uuid);
        return info == null ? DefaultPlayerSkin.getDefaultSkin(uuid) : info.getSkinLocation();
    }
}
