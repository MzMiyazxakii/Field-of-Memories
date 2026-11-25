package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.data.BookProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeItemEvent {
    public static final String FAKE_TAG = "FieldOfMemoriesFakeItem";
    private static final String CHEST_CHECKED_TAG = "FieldOfMemoriesFakeItemChecked";
    private static final String[] GLYPHS = {
            "⟊⍜⟟⟒ ⌖⍜⟟⟒", "⸸⟟⍜⟒ ⌇⍜⟟⟒", "⍜⟒⌖⟊ ⟟⸸⍜", "⌇⟒⸸⍜ ⌖⟟⟊",
            "⟟⟊⸸ ⍜⌖⟒", "⌖⍜⸸⟟ ⟒⌇⟊", "⍜⌇⟊⸸ ⟟⟒⌖"
    };

    private FakeItemEvent() {}

    public static void trySpawn(ServerLevel level, List<ServerPlayer> players, RandomSource random,
                                int danger, int activity) {
        if (players.isEmpty()) return;

        // Check loaded chest inventories independently from the ground-spawn roll.
        tryPopulateNearbyChests(level, players, random, danger, activity);

        double chance = Math.min(1.0D, 0.35D + danger / 250.0D + Math.max(0, activity - 100) / 500.0D);
        if (!com.fieldofmemories.data.HorrorWorldData.rollEventChance(level, random, chance)) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 7.0D + random.nextDouble() * 13.0D;
        int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(x, y - 1, z);
        net.minecraft.core.BlockPos spawn = new net.minecraft.core.BlockPos(x, y, z);
        if (!level.getBlockState(ground).isSolidRender(level, ground)) return;
        if (!level.getBlockState(spawn).isAir()) return;

        ItemStack fake = createFakeStack(random);
        if (fake.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, x + 0.5D, y + 0.15D, z + 0.5D, fake);
        entity.setDefaultPickUpDelay();
        entity.setUnlimitedLifetime();
        level.addFreshEntity(entity);
    }

    private static void tryPopulateNearbyChests(ServerLevel level, List<ServerPlayer> players,
                                                RandomSource random, int danger, int activity) {
        double chance = Math.min(1.0D, 0.45D + danger / 250.0D + Math.max(0, activity - 100) / 500.0D);
        for (ServerPlayer player : players) {
            ChunkPos center = new ChunkPos(player.blockPosition());
            // Only inspect already-loaded chunks around active players; never force-load terrain.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                    if (chunk == null) continue;
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (!(blockEntity instanceof ChestBlockEntity chest) || chest.isRemoved()) continue;
                        CompoundTag persistent = chest.getPersistentData();
                        if (persistent.getBoolean(CHEST_CHECKED_TAG)) continue;

                        // Wait until a loot-table chest has been opened and vanilla has filled it.
                        if (chest.isEmpty()) continue;

                        persistent.putBoolean(CHEST_CHECKED_TAG, true);
                        chest.setChanged();
                        if (!com.fieldofmemories.data.HorrorWorldData.rollEventChance(level, random, chance)) continue;

                        int size = chest.getContainerSize();
                        int start = random.nextInt(Math.max(1, size));
                        for (int i = 0; i < size; i++) {
                            int slot = (start + i) % size;
                            if (chest.getItem(slot).isEmpty()) {
                                ItemStack fake = createFakeStack(random);
                                if (!fake.isEmpty()) {
                                    chest.setItem(slot, fake);
                                    chest.setChanged();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }


    public static boolean spawnForAdmin(ServerLevel level, ServerPlayer target) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 4.0D + random.nextDouble() * 7.0D;
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(x, y - 1, z);
            net.minecraft.core.BlockPos spawn = new net.minecraft.core.BlockPos(x, y, z);
            if (!level.isLoaded(spawn) || !level.getBlockState(ground).isSolidRender(level, ground)
                    || !level.getBlockState(spawn).isAir()) continue;
            ItemStack fake = createFakeStack(random);
            if (fake.isEmpty()) return false;
            ItemEntity entity = new ItemEntity(level, x + 0.5D, y + 0.15D, z + 0.5D, fake);
            entity.setDefaultPickUpDelay();
            entity.setUnlimitedLifetime();
            return level.addFreshEntity(entity);
        }
        return false;
    }

    private static ItemStack createFakeStack(RandomSource random) {
        List<Item> candidates = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item != Items.AIR && item != FieldOfMemories.ADMIN_TOOL.get()) {
                candidates.add(item);
            }
        }
        if (candidates.isEmpty()) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(candidates.get(random.nextInt(candidates.size())));
        stack.getOrCreateTag().putBoolean(FAKE_TAG, true);

        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        String glyphs = GLYPHS[random.nextInt(GLYPHS.length)];
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(glyphs))));
        display.put("Lore", lore);
        return stack;
    }

    public static boolean isFakeItem(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(FAKE_TAG);
    }

    public static int purgeNearby(ServerLevel level, net.minecraft.world.phys.Vec3 center, double radius) {
        int removed = 0;
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (isFakeItem(item.getItem())) {
                item.discard();
                removed++;
            }
        }
        net.minecraft.core.BlockPos min = net.minecraft.core.BlockPos.containing(
                center.x - radius, center.y - radius, center.z - radius);
        net.minecraft.core.BlockPos max = net.minecraft.core.BlockPos.containing(
                center.x + radius, center.y + radius, center.z + radius);
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) continue;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof ChestBlockEntity chest)) continue;
            boolean changed = false;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                if (isFakeItem(chest.getItem(slot))) {
                    chest.setItem(slot, ItemStack.EMPTY);
                    removed++;
                    changed = true;
                }
            }
            if (changed) chest.setChanged();
        }
        return removed;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().getBoolean(FAKE_TAG)) continue;

            BookProgress.discover(player, BookProgress.MIMIC_ITEM);
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            if (!com.fieldofmemories.item.CrossItem.tryProtect(player, true)) triggerForPlayer(player);
            return;
        }
    }

    private static void triggerForPlayer(ServerPlayer player) {
        SoundEvent scream = player.serverLevel().random.nextBoolean()
                ? FieldOfMemories.SMILE_FACE_SCREAM_1.get()
                : FieldOfMemories.SMILE_FACE_SCREAM_2.get();

        player.playNotifySound(scream, SoundSource.MASTER, 1.0F, 1.0F);
        player.hurt(player.damageSources().magic(), 6.0F);
        player.sendSystemMessage(Component.literal("это мои вещи").withStyle(net.minecraft.ChatFormatting.RED));
    }
}
