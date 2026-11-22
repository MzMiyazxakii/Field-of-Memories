package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeItemEvent {
    private static final String FAKE_TAG = "FieldOfMemoriesFakeItem";
    private static final String[] GLYPHS = {
            "⟊⍜⟟⟒ ⌖⍜⟟⟒", "⸸⟟⍜⟒ ⌇⍜⟟⟒", "⍜⟒⌖⟊ ⟟⸸⍜", "⌇⟒⸸⍜ ⌖⟟⟊",
            "⟟⟊⸸ ⍜⌖⟒", "⌖⍜⸸⟟ ⟒⌇⟊", "⍜⌇⟊⸸ ⟟⟒⌖"
    };

    private FakeItemEvent() {}

    public static void trySpawn(ServerLevel level, List<ServerPlayer> players, RandomSource random,
                                int danger, int activity) {
        if (players.isEmpty()) return;

        // Rare world event; higher danger and activity make it somewhat more likely.
        int chance = Math.min(4, 1 + danger / 50 + Math.max(0, activity - 100) / 150);
        if (random.nextInt(100) >= chance) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 7.0D + random.nextDouble() * 13.0D;
        int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (!level.getBlockState(new net.minecraft.core.BlockPos(x, y - 1, z)).isSolidRender(level,
                new net.minecraft.core.BlockPos(x, y - 1, z))) return;
        if (!level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir()) return;

        ItemStack fake = createFakeStack(random);
        ItemEntity entity = new ItemEntity(level, x + 0.5D, y + 0.15D, z + 0.5D, fake);
        entity.setDefaultPickUpDelay();
        entity.setUnlimitedLifetime();
        level.addFreshEntity(entity);
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().getBoolean(FAKE_TAG)) continue;

            // Remove the counterfeit before applying the scare, so it cannot trigger twice.
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            triggerForPlayer(player);
            return;
        }
    }

    private static void triggerForPlayer(ServerPlayer player) {
        SoundEvent scream = player.serverLevel().random.nextBoolean()
                ? FieldOfMemories.SMILE_FACE_SCREAM_1.get()
                : FieldOfMemories.SMILE_FACE_SCREAM_2.get();

        // playNotifySound sends the sound only to this player, not to nearby players.
        player.playNotifySound(scream, SoundSource.MASTER, 1.0F, 1.0F);
        player.hurt(player.damageSources().magic(), 6.0F);
        player.sendSystemMessage(Component.literal("это мои вещи"));
    }
}
