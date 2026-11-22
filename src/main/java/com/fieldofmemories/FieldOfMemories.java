package com.fieldofmemories;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.fieldofmemories.block.ScarecrowBlock;
import com.fieldofmemories.block.LootContainerBlock;
import com.fieldofmemories.block.TripleTallGrassBlock;
import com.fieldofmemories.worldgen.TripleTallGrassFeature;
import net.minecraft.world.item.BlockItem;
import com.fieldofmemories.item.AdminToolItem;
import com.fieldofmemories.item.MemoryDiscItem;
import com.fieldofmemories.network.NetworkHandler;
import com.fieldofmemories.worldgen.FieldDecorFeature;
import com.fieldofmemories.worldgen.AbandonedSettlementFeature;

@Mod(FieldOfMemories.MOD_ID)
public class FieldOfMemories {
    public static final String MOD_ID = "fieldofmemories";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final RegistryObject<Block> SCARECROW = BLOCKS.register("scarecrow", ScarecrowBlock::new);
    public static final RegistryObject<Block> TRIPLE_TALL_GRASS = BLOCKS.register("triple_tall_grass", TripleTallGrassBlock::new);
    public static final RegistryObject<Block> BACKPACK_BLOCK = BLOCKS.register("backpack", () -> new LootContainerBlock(true));
    public static final RegistryObject<Block> CARDBOARD_BOX_BLOCK = BLOCKS.register("cardboard_box", () -> new LootContainerBlock(false));
    public static final RegistryObject<Item> CARDBOARD_BOX_ITEM = ITEMS.register("cardboard_box", () -> new BlockItem(CARDBOARD_BOX_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ADMIN_TOOL = ITEMS.register("admin_tool", () -> new AdminToolItem(new Item.Properties()));
    public static final RegistryObject<Item> MEMORY_DISC_1 = registerMemoryDisc("memory_disc_1", 0);
    public static final RegistryObject<Item> MEMORY_DISC_2 = registerMemoryDisc("memory_disc_2", 1);
    public static final RegistryObject<Item> MEMORY_DISC_3 = registerMemoryDisc("memory_disc_3", 2);
    public static final RegistryObject<Item> MEMORY_DISC_4 = registerMemoryDisc("memory_disc_4", 3);
    public static final RegistryObject<Item> MEMORY_DISC_5 = registerMemoryDisc("memory_disc_5", 4);
    public static final RegistryObject<Item> MEMORY_DISC_6 = registerMemoryDisc("memory_disc_6", 5);
    public static final RegistryObject<Item> MEMORY_DISC_7 = registerMemoryDisc("memory_disc_7", 6);
    public static final RegistryObject<Item> MEMORY_DISC_8 = registerMemoryDisc("memory_disc_8", 7);
    public static final RegistryObject<Item>[] MEMORY_DISCS = new RegistryObject[] {
            MEMORY_DISC_1, MEMORY_DISC_2, MEMORY_DISC_3, MEMORY_DISC_4,
            MEMORY_DISC_5, MEMORY_DISC_6, MEMORY_DISC_7, MEMORY_DISC_8
    };

    private static RegistryObject<Item> registerMemoryDisc(String name, int discId) {
        return ITEMS.register(name, () -> new MemoryDiscItem(discId, new Item.Properties()));
    }

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    public static final RegistryObject<SoundEvent> DEATH_SCREEN_SOUND = SOUND_EVENTS.register(
            "death_screen",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "death_screen"))
    );
    public static final RegistryObject<SoundEvent> MEMORY_DISC_SOUND = SOUND_EVENTS.register(
            "diskmemorysound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "diskmemorysound"))
    );
    public static final RegistryObject<SoundEvent> OPEN_SOUNDISK = SOUND_EVENTS.register(
            "opensoundisk",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "opensoundisk"))
    );

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, MOD_ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TRIPLE_TALL_GRASS_FEATURE =
            FEATURES.register("triple_tall_grass", () -> new TripleTallGrassFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FIELD_DECOR =
            FEATURES.register("field_decor", () -> new FieldDecorFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ABANDONED_SETTLEMENT =
            FEATURES.register("abandoned_settlement", () -> new AbandonedSettlementFeature(NoneFeatureConfiguration.CODEC));

    public FieldOfMemories() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        SOUND_EVENTS.register(modBus);
        FEATURES.register(modBus);
        NetworkHandler.register();
    }
}
