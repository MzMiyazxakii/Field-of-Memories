package com.fieldofmemories;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import com.fieldofmemories.item.AdminToolItem;
import com.fieldofmemories.network.NetworkHandler;

@Mod(FieldOfMemories.MOD_ID)
public class FieldOfMemories {
    public static final String MOD_ID = "fieldofmemories";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> ADMIN_TOOL = ITEMS.register("admin_tool", () -> new AdminToolItem(new Item.Properties()));

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    public static final RegistryObject<SoundEvent> DEATH_SCREEN_SOUND = SOUND_EVENTS.register(
            "death_screen",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "death_screen"))
    );

    public FieldOfMemories() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        SOUND_EVENTS.register(modBus);
        NetworkHandler.register();
    }
}
