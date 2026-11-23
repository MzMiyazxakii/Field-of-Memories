package com.fieldofmemories.data;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.CrossItem;
import com.fieldofmemories.item.MemoryDiscItem;
import com.fieldofmemories.item.OldCameraItem;
import com.fieldofmemories.item.VideoCameraItem;
import com.fieldofmemories.network.BookNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BookProgress {
    private static final String ENTRIES_TAG = "FOMBookEntries";
    private static final String OLD_BOOK_GIVEN_TAG = "FOMOldBookGiven";
    public static final int MIMIC = 0;
    public static final int MIMIC_ITEM = 1;
    public static final int SMILE_FACE = 2;
    public static final int SCARECROW = 3;
    public static final int WINDOW_STALKER = 4;
    public static final int SPIRIT = 5;
    public static final int OLD_CAMERA = 6;
    public static final int VIDEO_CAMERA = 7;
    public static final int CROSS = 8;
    public static final int EMPTY_CROSS = 9;
    public static final int MEMORY_DISCS = 10;
    public static final int STOOL = 11;
    public static final int DARK_RIFT = 12;
    public static final int PINK_RIFT = 13;
    public static final int CROSS_SAVED = 14;
    public static final int FARM_CLUE = 15;
    public static final int FACE_SHARDS = 16;
    public static final int BOX_CLUE = 17;
    public static final int RIFT_CROSS_CLUE = 18;

    private BookProgress() {}

    public static void discover(ServerPlayer player, int entry) {
        if (entry < 0 || entry >= 19) return;
        int mask = player.getPersistentData().getInt(ENTRIES_TAG);
        int updated = mask | (1 << entry);
        if (updated == mask) return;
        player.getPersistentData().putInt(ENTRIES_TAG, updated);
        player.sendSystemMessage(Component.literal("Вы о чём-то задумались...")
                .withStyle(style -> style.withFont(new ResourceLocation("minecraft", "uniform"))));
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.18F, 0.78F);
        BookNetwork.sync(player, updated);
    }

    public static int getMask(Player player) {
        return player.getPersistentData().getInt(ENTRIES_TAG);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getPersistentData().getBoolean(OLD_BOOK_GIVEN_TAG)) return;
        ItemStack book = new ItemStack(FieldOfMemories.OLD_BOOK.get());
        if (!player.getInventory().add(book)) player.drop(book, false);
        player.getPersistentData().putBoolean(OLD_BOOK_GIVEN_TAG, true);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem().getItem();
        if (stack.is(FieldOfMemories.CROSS.get()) && CrossItem.getState(stack) == CrossItem.EMPTY) {
            discover(player, EMPTY_CROSS);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof MemoryDiscItem) {
                discover(player, MEMORY_DISCS);
                return;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof MemoryDiscItem) {
                discover(player, MEMORY_DISCS);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof OldCameraItem) discover(player, OLD_CAMERA);
        if (stack.getItem() instanceof VideoCameraItem) discover(player, VIDEO_CAMERA);
        if (stack.is(FieldOfMemories.CROSS.get())) {
            if (CrossItem.getState(stack) == CrossItem.EMPTY) discover(player, EMPTY_CROSS);
            else discover(player, CROSS);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        int mask = event.getOriginal().getPersistentData().getInt(ENTRIES_TAG);
        if (mask != 0) event.getEntity().getPersistentData().putInt(ENTRIES_TAG, mask);
        if (event.getOriginal().getPersistentData().getBoolean(OLD_BOOK_GIVEN_TAG)) event.getEntity().getPersistentData().putBoolean(OLD_BOOK_GIVEN_TAG, true);
    }
}
