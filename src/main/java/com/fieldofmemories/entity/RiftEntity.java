package com.fieldofmemories.entity;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RiftEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BLACK =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PINK =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifetime = 400;
    private ItemStack examining = ItemStack.EMPTY;
    private int examineTicks;
    private ItemEntity heldVisual;

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData() {
        entityData.define(BLACK, false);
        entityData.define(PINK, false);
    }
    public void setBlackRift(boolean value) { entityData.set(BLACK, value); }
    public boolean isBlackRift() { return entityData.get(BLACK); }
    public void setPinkRift(boolean value) { entityData.set(PINK, value); }
    public boolean isPinkRift() { return entityData.get(PINK); }
    public void setLifetime(int ticks) { lifetime = Math.max(40, ticks); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vector3f color = isPinkRift() ? new Vector3f(1.0F, 0.12F, 0.55F)
                    : new Vector3f(0.005F, 0.005F, 0.005F);
            for (int i = 0; i < 8; i++) {
                double ox = (random.nextDouble() - 0.5D) * 2.2D;
                double oy = (random.nextDouble() - 0.5D) * 2.2D;
                double oz = (random.nextDouble() - 0.5D) * 2.2D;
                level().addParticle(new DustParticleOptions(color, 1.35F),
                        getX() + ox, getY() + oy, getZ() + oz, 0.0D, 0.015D, 0.0D);
            }
            return;
        }
        if (--lifetime <= 0) {
            clearExamining();
            discard();
            return;
        }
        if (isPinkRift()) {
            tickPinkRift();
            return;
        }
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(8.0D))) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            Vec3 pull = position().subtract(player.position());
            double distance = Math.max(0.2D, pull.length());
            if (distance < 8.0D) {
                Vec3 force = pull.normalize().scale((8.0D - distance) * 0.012D);
                player.setDeltaMovement(player.getDeltaMovement().add(force.x, force.y * 0.25D, force.z));
                player.hurtMarked = true;
            }
            if (distance < 1.35D) {
                if (com.fieldofmemories.item.CrossItem.tryProtect(player, true)) { discard(); return; }
                if (isBlackRift()) {
                    player.kill();
                } else {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double x = getX() + Math.cos(angle) * (30.0D + random.nextDouble() * 18.0D);
                    double z = getZ() + Math.sin(angle) * (30.0D + random.nextDouble() * 18.0D);
                    int y = player.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            (int) Math.floor(x), (int) Math.floor(z));
                    player.teleportTo(x, y, z);
                }
                discard();
                return;
            }
        }
    }

    private void tickPinkRift() {
        if (!examining.isEmpty()) {
            if (heldVisual == null || !heldVisual.isAlive()) createHeldVisual();
            if (heldVisual != null) {
                heldVisual.setPos(getX(), getY() + 0.85D, getZ());
                heldVisual.setDeltaMovement(Vec3.ZERO);
            }
            repelIncomingItems();
            if (--examineTicks <= 0) {
                ItemStack result = process(examining);
                clearExamining();
                if (!result.isEmpty()) {
                    double spitAngle = random.nextDouble() * Math.PI * 2.0D;
                    ItemEntity output = new ItemEntity(level(), getX() + Math.cos(spitAngle) * 3.0D,
                            getY() + 0.55D, getZ() + Math.sin(spitAngle) * 3.0D, result);
                    output.setDefaultPickUpDelay();
                    output.setDeltaMovement(0.0D, 0.18D, 0.0D);
                    level().addFreshEntity(output);
                }
            }
            return;
        }
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(1.8D))) {
            if (!item.isAlive() || item.getItem().isEmpty()) continue;
            ItemStack offered = item.getItem().copy();
            item.discard();
            examining = offered;
            examineTicks = 60;
            createHeldVisual();
            return;
        }
    }

    private void repelIncomingItems() {
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(2.4D))) {
            if (item == heldVisual || !item.isAlive()) continue;
            Vec3 away = item.position().subtract(position());
            if (away.lengthSqr() < 0.01D) away = new Vec3(0.0D, 0.2D, 0.0D);
            item.setDeltaMovement(away.normalize().scale(0.32D).add(0.0D, 0.12D, 0.0D));
            item.setPickUpDelay(10);
            item.hurtMarked = true;
        }
    }

    private void createHeldVisual() {
        if (level().isClientSide || examining.isEmpty()) return;
        heldVisual = new ItemEntity(level(), getX(), getY() + 0.85D, getZ(), examining.copy());
        heldVisual.setNoGravity(true);
        heldVisual.setPickUpDelay(32767);
        heldVisual.setDeltaMovement(Vec3.ZERO);
        level().addFreshEntity(heldVisual);
    }

    private void clearExamining() {
        if (heldVisual != null && heldVisual.isAlive()) heldVisual.discard();
        heldVisual = null;
        examining = ItemStack.EMPTY;
        examineTicks = 0;
    }

    private ItemStack process(ItemStack input) {
        if (input.is(Items.CARVED_PUMPKIN)) {
            return new ItemStack(FieldOfMemories.MEMORY_DISC_7.get());
        }
        if (level().isClientSide || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return input.copy();
        }
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(12.0D))) {
            CraftingContainer grid = new CraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
            for (int i = 0; i < 9; i++) grid.setItem(i, ItemStack.EMPTY);
            boolean valid = true;
            List<net.minecraft.world.item.crafting.Ingredient> ingredients = null;
            for (net.minecraft.world.item.crafting.Recipe<?> candidate : serverLevel.getRecipeManager().getRecipes()) {
                if (!(candidate instanceof CraftingRecipe recipe)) continue;
                List<net.minecraft.world.item.crafting.Ingredient> recipeIngredients = recipe.getIngredients();
                if (recipeIngredients.isEmpty() || recipeIngredients.size() > 9) continue;
                valid = true;
                for (int i = 0; i < recipeIngredients.size(); i++) {
                    net.minecraft.world.item.crafting.Ingredient ingredient = recipeIngredients.get(i);
                    if (ingredient.isEmpty()) continue;
                    if (!ingredient.test(input)) { valid = false; break; }
                    grid.setItem(i, input.copyWithCount(1));
                }
                if (valid && recipe.matches(grid, serverLevel)) {
                    ingredients = recipeIngredients;
                    break;
                }
                for (int i = 0; i < 9; i++) grid.setItem(i, ItemStack.EMPTY);
            }
            if (ingredients != null) {
                int count = 0;
                for (net.minecraft.world.item.crafting.Ingredient ingredient : ingredients) {
                    if (!ingredient.isEmpty() && ingredient.test(input)) count++;
                }
                int returned = count / 2;
                if (returned <= 0) return input.copy();
                ItemStack material = ItemStack.EMPTY;
                for (net.minecraft.world.item.crafting.Ingredient ingredient : ingredients) {
                    if (!ingredient.isEmpty() && ingredient.test(input)) {
                        ItemStack[] choices = ingredient.getItems();
                        if (choices.length > 0) { material = choices[0].copy(); break; }
                    }
                }
                if (material.isEmpty()) return input.copy();
                material.setCount(returned);
                return material;
            }
        }
        return input.copy();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        lifetime = Math.max(1, tag.getInt("Lifetime"));
        setBlackRift(tag.getBoolean("Black"));
        setPinkRift(tag.getBoolean("Pink"));
        if (tag.contains("Examining")) examining = ItemStack.of(tag.getCompound("Examining"));
        examineTicks = tag.getInt("ExamineTicks");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", lifetime);
        tag.putBoolean("Black", isBlackRift());
        tag.putBoolean("Pink", isPinkRift());
        if (!examining.isEmpty()) tag.put("Examining", examining.save(new CompoundTag()));
        tag.putInt("ExamineTicks", examineTicks);
    }
    @Override public boolean isPickable() { return false; }
}
