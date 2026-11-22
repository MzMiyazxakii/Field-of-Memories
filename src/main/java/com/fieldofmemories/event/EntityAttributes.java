package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EntityAttributes {
    private EntityAttributes() {}

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier.Builder hostileAttributes = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);

        event.put(FieldOfMemories.SPIRIT_ENTITY.get(), hostileAttributes.build());

        AttributeSupplier.Builder echoAttributes = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

        event.put(FieldOfMemories.PLAYER_ECHO_ENTITY.get(), echoAttributes.build());

        AttributeSupplier.Builder stalkerAttributes = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
        event.put(FieldOfMemories.WINDOW_STALKER_ENTITY.get(), stalkerAttributes.build());
    }
}
