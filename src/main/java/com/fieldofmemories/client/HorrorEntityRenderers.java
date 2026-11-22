package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.client.model.ScarecrowModel;
import com.fieldofmemories.client.model.SmileFaceModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HorrorEntityRenderers {
    private HorrorEntityRenderers() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ScarecrowModel.LAYER_LOCATION, ScarecrowModel::createBodyLayer);
        event.registerLayerDefinition(SmileFaceModel.LAYER_LOCATION, SmileFaceModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FieldOfMemories.SCARECROW_ENTITY.get(), ScarecrowRenderer::new);
        event.registerEntityRenderer(FieldOfMemories.SMILE_FACE_ENTITY.get(), SmileFaceRenderer::new);
        event.registerEntityRenderer(FieldOfMemories.PLAYER_ECHO_ENTITY.get(), PlayerEchoRenderer::new);
        event.registerEntityRenderer(FieldOfMemories.SPIRIT_ENTITY.get(), SpiritRenderer::new);
        event.registerEntityRenderer(FieldOfMemories.RIFT_ENTITY.get(), RiftRenderer::new);
        event.registerEntityRenderer(FieldOfMemories.WINDOW_STALKER_ENTITY.get(), WindowStalkerRenderer::new);
    }
}