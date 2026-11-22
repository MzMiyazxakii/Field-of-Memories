package com.fieldofmemories.data;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;

public class StoolLootData extends SavedData {
    private static final String DATA_NAME = "fieldofmemories_stool_loot";
    private final List<Entry> entries = new ArrayList<>();
    private int successfulGamesWithoutHolyFather;

    public StoolLootData() {
        add("diamond",1,3,10); add("netherite",1,1,5); add("armor",1,1,7);
        add("iron",3,12,10); add("golden_apple",1,5,8); add("totem",1,1,2);
        add("nether_star",1,1,2); add("memory_disc",1,1,3); add("coal_block",1,6,8);
        add("gold",1,7,8); add("goat_horn",1,1,4); add("ender_pearl",1,5,8);
        add("blaze_rod",1,4,8);
    }

    private void add(String key,int min,int max,int weight){ entries.add(new Entry(key,min,max,weight)); }
    public static StoolLootData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(StoolLootData::load,StoolLootData::new,DATA_NAME);
    }
    public List<Entry> getEntries(){ return List.copyOf(entries); }
    public void edit(int index,int action) {
        if(index<0 || index>=entries.size()) return;
        Entry e=entries.get(index); int min=e.min(), max=e.max(), weight=e.weight();
        boolean single = e.key().equals("netherite") || e.key().equals("armor") || e.key().equals("totem") || e.key().equals("nether_star") || e.key().equals("memory_disc") || e.key().equals("goat_horn");
        switch(action){
            case 0 -> weight=Math.min(100,weight+1);
            case 1 -> weight=Math.max(0,weight-1);
            case 2 -> min=Math.min(max,min+1);
            case 3 -> min=Math.max(1,min-1);
            case 4 -> max=Math.min(64,max+1);
            case 5 -> max=Math.max(min,max-1);
            case 6 -> weight=0;
            default -> { return; }
        }
        entries.set(index,new Entry(e.key(),min,max,weight)); setDirty();
    }
    public ItemStack rollForStool(net.minecraft.util.RandomSource random) {
        if (successfulGamesWithoutHolyFather >= 12) {
            successfulGamesWithoutHolyFather = 0;
            setDirty();
            return new ItemStack(FieldOfMemories.MEMORY_DISC_6.get());
        }
        successfulGamesWithoutHolyFather++;
        ItemStack reward = roll(random);
        if (!reward.isEmpty() && reward.is(FieldOfMemories.MEMORY_DISC_6.get())) {
            successfulGamesWithoutHolyFather = 0;
        }
        setDirty();
        return reward;
    }

    public ItemStack roll(net.minecraft.util.RandomSource random) {
        int total=entries.stream().mapToInt(Entry::weight).sum();
        if(total<=0) return ItemStack.EMPTY;
        int roll=random.nextInt(total);
        for(Entry e:entries){
            roll-=e.weight(); if(roll<0) return create(e,random);
        }
        return ItemStack.EMPTY;
    }
    private ItemStack create(Entry e,net.minecraft.util.RandomSource random) {
        int count=e.min()+random.nextInt(e.max()-e.min()+1);
        return switch(e.key()){
            case "diamond" -> new ItemStack(Items.DIAMOND,count);
            case "netherite" -> new ItemStack(Items.NETHERITE_INGOT,count);
            case "armor" -> {
                ItemStack[] armor={new ItemStack(Items.IRON_HELMET),new ItemStack(Items.IRON_CHESTPLATE),new ItemStack(Items.IRON_LEGGINGS),new ItemStack(Items.IRON_BOOTS),
                    new ItemStack(Items.GOLDEN_HELMET),new ItemStack(Items.GOLDEN_CHESTPLATE),new ItemStack(Items.GOLDEN_LEGGINGS),new ItemStack(Items.GOLDEN_BOOTS),
                    new ItemStack(Items.DIAMOND_HELMET),new ItemStack(Items.DIAMOND_CHESTPLATE),new ItemStack(Items.DIAMOND_LEGGINGS),new ItemStack(Items.DIAMOND_BOOTS),
                    new ItemStack(Items.NETHERITE_HELMET),new ItemStack(Items.NETHERITE_CHESTPLATE),new ItemStack(Items.NETHERITE_LEGGINGS),new ItemStack(Items.NETHERITE_BOOTS)};
                yield armor[random.nextInt(armor.length)];
            }
            case "iron" -> new ItemStack(Items.IRON_INGOT,count);
            case "golden_apple" -> new ItemStack(Items.GOLDEN_APPLE,count);
            case "totem" -> new ItemStack(Items.TOTEM_OF_UNDYING,count);
            case "nether_star" -> new ItemStack(Items.NETHER_STAR,count);
            case "memory_disc" -> new ItemStack(FieldOfMemories.MEMORY_DISC_6.get(),count);
            case "coal_block" -> new ItemStack(Items.COAL_BLOCK,count);
            case "gold" -> new ItemStack(Items.GOLD_INGOT,count);
            case "goat_horn" -> new ItemStack(Items.GOAT_HORN,count);
            case "ender_pearl" -> new ItemStack(Items.ENDER_PEARL,count);
            default -> new ItemStack(Items.BLAZE_ROD,count);
        };
    }
    public String serializeForClient(){
        StringBuilder out=new StringBuilder();
        for(Entry e:entries){if(out.length()>0)out.append(';');out.append(e.key()).append(',').append(e.min()).append(',').append(e.max()).append(',').append(e.weight());}
        return out.toString();
    }
    private static StoolLootData load(CompoundTag tag){
        StoolLootData d=new StoolLootData();
        d.successfulGamesWithoutHolyFather = Math.max(0, tag.getInt("SuccessfulGamesWithoutHolyFather"));
        if(tag.contains("Entries",9)){
            d.entries.clear(); ListTag list=tag.getList("Entries",10);
            for(int i=0;i<list.size();i++){CompoundTag e=list.getCompound(i);String key=e.getString("Key");
                if(!List.of("diamond","netherite","armor","iron","golden_apple","totem","nether_star","memory_disc","coal_block","gold","goat_horn","ender_pearl","blaze_rod").contains(key))continue;
                int min=Math.max(1,Math.min(64,e.getInt("Min")));int max=Math.max(min,Math.min(64,e.getInt("Max")));int weight=Math.max(0,Math.min(100,e.getInt("Weight")));
                d.entries.add(new Entry(key,min,max,weight));
            }
        }
        return d;
    }
    @Override public CompoundTag save(CompoundTag tag){
        tag.putInt("SuccessfulGamesWithoutHolyFather", successfulGamesWithoutHolyFather);
        ListTag list=new ListTag();for(Entry e:entries){CompoundTag n=new CompoundTag();n.putString("Key",e.key());n.putInt("Min",e.min());n.putInt("Max",e.max());n.putInt("Weight",e.weight());list.add(n);}tag.put("Entries",list);return tag;
    }
    public record Entry(String key,int min,int max,int weight){}
}
