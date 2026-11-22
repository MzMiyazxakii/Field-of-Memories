package com.fieldofmemories.data;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class BoxLootData extends SavedData {
    private static final String DATA_NAME = "fieldofmemories_box_loot";
    private final List<Entry> entries = new ArrayList<>();
    private int brokenBoxes;
    private boolean memoryDiscFound;

    public BoxLootData() {
        addDefault(Items.IRON_INGOT, 1, 3, 10);
        addDefault(Items.IRON_NUGGET, 5, 15, 12);
        addDefault(Items.GOLD_INGOT, 1, 3, 7);
        addDefault(Items.CARROT, 1, 5, 9);
        addDefault(Items.POTATO, 1, 5, 9);
        addDefault(Items.REDSTONE, 2, 8, 8);
        addDefault(Items.LAPIS_LAZULI, 2, 8, 6);
        addDefault(Items.AMETHYST_SHARD, 2, 8, 6);
        addDefault(Items.COPPER_INGOT, 2, 8, 8);
        addDefault(Items.BONE, 1, 4, 7);
        addDefault(Items.STICK, 1, 5, 10);
        addDefault(Items.SPRUCE_PLANKS, 1, 5, 8);
        addDefault(Items.STRING, 1, 5, 6);
        addDefault(Items.FEATHER, 1, 5, 5);
        addDefault(Items.LEATHER, 1, 5, 5);
        addDefault(Items.FLINT, 1, 5, 5);
        addDefault(Items.BEEF, 1, 7, 3);
        addDefault(Items.PORKCHOP, 1, 7, 3);
        addDefault(Items.CHICKEN, 1, 7, 3);
        addDefault(Items.MUTTON, 1, 7, 3);
        addDefault(Items.RABBIT, 1, 7, 2);
        addDefault(Items.BREAD, 1, 3, 5);
        addDefault(Items.APPLE, 1, 3, 4);
        addDefault(Items.COAL, 2, 6, 5);
        addDefault(Items.TORCH, 2, 8, 4);
        addDefault(Items.EMERALD, 1, 2, 1);
        addDefault(Items.DIAMOND, 1, 1, 1);
        addDefault(Items.ARROW, 2, 8, 4);
        addDefault(Items.GOLDEN_CARROT, 1, 2, 1);
    }

    private void addDefault(Item item, int min, int max, int weight) {
        if (item != Items.AIR) entries.add(new Entry(item, min, max, weight));
    }

    public static BoxLootData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(BoxLootData::load, BoxLootData::new, DATA_NAME);
    }

    /**
     * Records one broken cardboard box for the entire world.
     * Returns a memory disc when the random chance succeeds or the 40-box guarantee is reached.
     */
    public ItemStack rollMemoryDisc(net.minecraft.util.RandomSource random) {
        brokenBoxes++;
        if (!memoryDiscFound && (brokenBoxes >= 40 || random.nextFloat() < 0.012F)) {
            memoryDiscFound = true;
            setDirty();
            return new ItemStack(FieldOfMemories.MEMORY_DISCS[random.nextBoolean() ? 0 : 1].get());
        }
        setDirty();
        return ItemStack.EMPTY;
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public Entry find(Item item) {
        for (Entry entry : entries) if (entry.item() == item) return entry;
        return null;
    }

    public void edit(String itemId, int action) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return;
        Entry current = find(item);
        if (action == 0 && current == null) {
            entries.add(new Entry(item, 1, 3, 5));
        } else if (action == 1 && current != null) {
            entries.remove(current);
        } else if (current != null) {
            int index = entries.indexOf(current);
            int min = current.min();
            int max = current.max();
            int weight = current.weight();
            switch (action) {
                case 2 -> weight = Math.min(100, weight + 1);
                case 3 -> weight = Math.max(1, weight - 1);
                case 4 -> { min = Math.min(max, min + 1); }
                case 5 -> { min = Math.max(1, min - 1); }
                case 6 -> { max = Math.min(64, max + 1); }
                case 7 -> { max = Math.max(min, max - 1); }
                default -> { return; }
            }
            entries.set(index, new Entry(item, min, max, weight));
        }
        setDirty();
    }

    public ItemStack roll(net.minecraft.util.RandomSource random) {
        if (entries.isEmpty()) return ItemStack.EMPTY;
        int totalWeight = entries.stream().mapToInt(Entry::weight).sum();
        if (totalWeight <= 0) return ItemStack.EMPTY;
        int value = random.nextInt(totalWeight);
        for (Entry entry : entries) {
            value -= entry.weight();
            if (value < 0) {
                int count = entry.min() + random.nextInt(entry.max() - entry.min() + 1);
                return new ItemStack(entry.item(), count);
            }
        }
        return ItemStack.EMPTY;
    }

    public String serializeForClient() {
        StringBuilder out = new StringBuilder();
        for (Entry e : entries) {
            if (out.length() > 0) out.append(';');
            out.append(BuiltInRegistries.ITEM.getKey(e.item())).append(',')
                    .append(e.min()).append(',').append(e.max()).append(',').append(e.weight());
        }
        return out.toString();
    }

    private static BoxLootData load(CompoundTag tag) {
        BoxLootData data = new BoxLootData();
        if (tag.contains("Entries", 9)) {
            data.entries.clear();
            ListTag list = tag.getList("Entries", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(e.getString("Item"));
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) continue;
                int min = Math.max(1, Math.min(64, e.getInt("Min")));
                int max = Math.max(min, Math.min(64, e.getInt("Max")));
                int weight = Math.max(1, Math.min(100, e.getInt("Weight")));
                data.entries.add(new Entry(item, min, max, weight));
            }
        }
        data.brokenBoxes = Math.max(0, tag.getInt("BrokenBoxes"));
        data.memoryDiscFound = tag.getBoolean("MemoryDiscFound");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(e.item()).toString());
            entry.putInt("Min", e.min());
            entry.putInt("Max", e.max());
            entry.putInt("Weight", e.weight());
            list.add(entry);
        }
        tag.put("Entries", list);
        tag.putInt("BrokenBoxes", brokenBoxes);
        tag.putBoolean("MemoryDiscFound", memoryDiscFound);
        return tag;
    }

    public record Entry(Item item, int min, int max, int weight) {}
}
