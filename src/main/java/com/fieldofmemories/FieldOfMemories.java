package com.fieldofmemories;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.fieldofmemories.block.LootContainerBlock;
import com.fieldofmemories.block.TreeLeafletBlock;
import com.fieldofmemories.block.AnomalousStoolBlock;
import net.minecraft.world.item.BlockItem;
import com.fieldofmemories.item.AdminToolItem;
import com.fieldofmemories.item.MemoryDiscItem;
import com.fieldofmemories.item.VideoCameraItem;
import com.fieldofmemories.item.OldCameraItem;
import com.fieldofmemories.item.BatteryItem;
import com.fieldofmemories.item.CrossItem;
import com.fieldofmemories.network.NetworkHandler;
import com.fieldofmemories.network.HorrorNetwork;
import com.fieldofmemories.entity.ScarecrowEntity;
import com.fieldofmemories.entity.SmileFaceEntity;
import com.fieldofmemories.entity.PlayerEchoEntity;
import com.fieldofmemories.entity.SpiritEntity;
import com.fieldofmemories.entity.RiftEntity;
import com.fieldofmemories.entity.WindowStalkerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.fieldofmemories.worldgen.FieldDecorFeature;
import com.fieldofmemories.worldgen.AbandonedSettlementFeature;

@Mod(FieldOfMemories.MOD_ID)
public class FieldOfMemories {
    public static final String MOD_ID = "fieldofmemories";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<EntityType<ScarecrowEntity>> SCARECROW_ENTITY = ENTITY_TYPES.register(
            "scarecrow", () -> EntityType.Builder.of(ScarecrowEntity::new, MobCategory.MISC)
                    .sized(0.9F, 2.1F).clientTrackingRange(64).updateInterval(2).build("scarecrow"));
    public static final RegistryObject<EntityType<SmileFaceEntity>> SMILE_FACE_ENTITY = ENTITY_TYPES.register(
            "smile_face", () -> EntityType.Builder.of(SmileFaceEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.2F).clientTrackingRange(64).updateInterval(2).build("smile_face"));
    public static final RegistryObject<EntityType<PlayerEchoEntity>> PLAYER_ECHO_ENTITY = ENTITY_TYPES.register(
            "player_echo", () -> EntityType.Builder.of(PlayerEchoEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(2).build("player_echo"));
    public static final RegistryObject<EntityType<SpiritEntity>> SPIRIT_ENTITY = ENTITY_TYPES.register(
            "spirit", () -> EntityType.Builder.of(SpiritEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(2).build("spirit"));
    public static final RegistryObject<EntityType<RiftEntity>> RIFT_ENTITY = ENTITY_TYPES.register(
            "rift", () -> EntityType.Builder.of(RiftEntity::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(64).updateInterval(1).build("rift"));
    public static final RegistryObject<EntityType<WindowStalkerEntity>> WINDOW_STALKER_ENTITY = ENTITY_TYPES.register(
            "window_stalker", () -> EntityType.Builder.of(WindowStalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(2).build("window_stalker"));
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final RegistryObject<Block> BACKPACK_BLOCK = BLOCKS.register("backpack", () -> new LootContainerBlock(true));
    public static final RegistryObject<Block> CARDBOARD_BOX_BLOCK = BLOCKS.register("cardboard_box", () -> new LootContainerBlock(false));
    public static final RegistryObject<Block> TREE_LEAFLET_BLOCK = BLOCKS.register("tree_leaflet", () -> new TreeLeafletBlock(BlockBehaviour.Properties.of().strength(0.1F).sound(net.minecraft.world.level.block.SoundType.WOOL).noOcclusion().noCollission()));
    public static final RegistryObject<Block> ANOMALOUS_STOOL_BLOCK = BLOCKS.register("anomalous_stool", AnomalousStoolBlock::new);
    public static final RegistryObject<Item> ANOMALOUS_STOOL = ITEMS.register("anomalous_stool", () -> new BlockItem(ANOMALOUS_STOOL_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CARDBOARD_BOX_ITEM = ITEMS.register("cardboard_box", () -> new BlockItem(CARDBOARD_BOX_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery", () -> new BatteryItem(new Item.Properties()));
    public static final RegistryObject<Item> CROSS = ITEMS.register("cross", () -> new CrossItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> VIDEO_CAMERA = ITEMS.register("video_camera", () -> new VideoCameraItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OLD_CAMERA = ITEMS.register("old_camera", () -> new OldCameraItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OLD_BOOK = ITEMS.register("old_book", () -> new Item(new Item.Properties().stacksTo(1)));
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
    public static final RegistryObject<SoundEvent> CAMERA_OPEN_SOUND = SOUND_EVENTS.register(
            "camera_open",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "camera_open"))
    );
    public static final RegistryObject<SoundEvent> CAMERA_MOVEMENT_SOUND = SOUND_EVENTS.register(
            "camera_movement",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "camera_movement"))
    );
    public static final RegistryObject<SoundEvent> KNOCK_1 = SOUND_EVENTS.register(
            "knock_1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "knock_1"))
    );
    public static final RegistryObject<SoundEvent> KNOCK_2 = SOUND_EVENTS.register(
            "knock_2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "knock_2"))
    );
    public static final RegistryObject<SoundEvent> SMILE_FACE_SCREAM_1 = SOUND_EVENTS.register(
            "smile_face_scream_1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "smile_face_scream_1"))
    );
    public static final RegistryObject<SoundEvent> SMILE_FACE_SCREAM_2 = SOUND_EVENTS.register(
            "smile_face_scream_2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "smile_face_scream_2"))
    );
    public static final RegistryObject<SoundEvent> SPIRIT_PRESENCE_SOUND = SOUND_EVENTS.register(
            "spirit_presence",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "spirit_presence"))
    );
    public static final RegistryObject<SoundEvent> HEARTBOUND_SOUND = SOUND_EVENTS.register(
            "heartbound",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "heartbound"))
    );
    public static final RegistryObject<SoundEvent> SCREAMSOUL_SOUND = SOUND_EVENTS.register(
            "screamsoul",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "screamsoul"))
    );

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, MOD_ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FIELD_DECOR =
            FEATURES.register("field_decor", () -> new FieldDecorFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ABANDONED_SETTLEMENT =
            FEATURES.register("abandoned_settlement", () -> new AbandonedSettlementFeature(NoneFeatureConfiguration.CODEC));

    public FieldOfMemories() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        SOUND_EVENTS.register(modBus);
        FEATURES.register(modBus);
        NetworkHandler.register();
        HorrorNetwork.register();
    }
}