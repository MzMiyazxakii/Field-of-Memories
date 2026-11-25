package com.fieldofmemories.entity;

import com.fieldofmemories.data.BookProgress;

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
import net.minecraft.world.inventory.TransientCraftingContainer;
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
import java.util.Collections;
import java.util.List;

public class RiftEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BLACK =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PINK =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifetime = 12000;
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
    public void setLifetime(int ticks) { lifetime = Math.max(12000, ticks); }

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
            discard();
            return;
        }
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(12.0D))) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            BookProgress.discover(player, isPinkRift() ? BookProgress.PINK_RIFT : BookProgress.DARK_RIFT);
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
                if (com.fieldofmemories.item.CrossItem.tryProtect(player, true)) {
                    BookProgress.discover(player, BookProgress.RIFT_CROSS_CLUE);
                    discard();
                    return;
                }
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
                List<ItemStack> results = process(examining);
                clearExamining();
                for (ItemStack result : results) {
                    if (result.isEmpty()) continue;
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

    private void returnExaminingItem() {
        if (examining.isEmpty() || level().isClientSide) return;
        ItemEntity returned = new ItemEntity(level(), getX(), getY() + 0.45D, getZ(), examining.copy());
        returned.setDefaultPickUpDelay();
        returned.setDeltaMovement(0.0D, 0.16D, 0.0D);
        level().addFreshEntity(returned);
        clearExamining();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && isPinkRift() && !examining.isEmpty()) {
            if (reason == RemovalReason.UNLOADED_TO_CHUNK) {
                if (heldVisual != null && heldVisual.isAlive()) heldVisual.discard();
                heldVisual = null;
            } else {
                returnExaminingItem();
            }
        }
        super.remove(reason);
    }

    private List<ItemStack> process(ItemStack input) {
        List<ItemStack> outputs = new ArrayList<>();
        if (input.isEmpty()) return outputs;
        if (input.is(Items.CARVED_PUMPKIN)) {
            outputs.add(new ItemStack(FieldOfMemories.MEMORY_DISC_7.get(), input.getCount()));
            return outputs;
        }
        if (level().isClientSide || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            outputs.add(input.copy());
            return outputs;
        }

        // Reverse the matching crafting recipe, preserving ingredient TYPES and their slot counts.
        for (CraftingRecipe recipe : serverLevel.getRecipeManager().getRecipes().stream()
                .filter(CraftingRecipe.class::isInstance)
                .map(CraftingRecipe.class::cast).toList()) {
            ItemStack crafted = recipe.getResultItem(serverLevel.registryAccess());
            if (crafted.isEmpty() || !ItemStack.isSameItemSameTags(crafted, input)) continue;

            List<net.minecraft.world.item.crafting.Ingredient> groups = new ArrayList<>();
            List<Integer> groupSizes = new ArrayList<>();
            for (var ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty() || ingredient.getItems().length == 0) continue;

                int group = -1;
                for (int i = 0; i < groups.size(); i++) {
                    if (sameIngredientChoices(groups.get(i), ingredient)) {
                        group = i;
                        break;
                    }
                }
                if (group < 0) {
                    groups.add(ingredient);
                    groupSizes.add(1);
                } else {
                    groupSizes.set(group, groupSizes.get(group) + 1);
                }
            }

            if (groups.isEmpty()) break;

            // A recipe can only be reversed for complete output batches.
            // For example, one plank is not enough to exchange for a log:
            // one log crafts into four planks, so four planks are required.
            int outputPerCraft = Math.max(1, crafted.getCount());
            int completeCrafts = input.getCount() / outputPerCraft;
            int remainder = input.getCount() % outputPerCraft;

            if (completeCrafts == 0) {
                outputs.add(input.copy());
                return outputs;
            }

            for (int i = 0; i < groups.size(); i++) {
                ItemStack material = chooseIngredientItem(groups.get(i), recipe, crafted);
                if (material.isEmpty()) continue;
                int totalCount = groupSizes.get(i) * completeCrafts;
                while (totalCount > 0) {
                    ItemStack stack = material.copy();
                    int amount = Math.min(totalCount, stack.getMaxStackSize());
                    stack.setCount(amount);
                    outputs.add(stack);
                    totalCount -= amount;
                }
            }

            // Keep any input that did not complete another full recipe.
            if (remainder > 0) {
                ItemStack leftover = input.copy();
                leftover.setCount(remainder);
                outputs.add(leftover);
            }
            return outputs;
        }

        // Items without a crafting recipe are returned unchanged.
        outputs.add(input.copy());
        return outputs;
    }

    private boolean sameIngredientChoices(net.minecraft.world.item.crafting.Ingredient first,
                                          net.minecraft.world.item.crafting.Ingredient second) {
        ItemStack[] a = first.getItems();
        ItemStack[] b = second.getItems();
        if (a.length != b.length) return false;
        for (ItemStack stackA : a) {
            boolean found = false;
            for (ItemStack stackB : b) {
                if (ItemStack.isSameItemSameTags(stackA, stackB)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private ItemStack chooseIngredientItem(net.minecraft.world.item.crafting.Ingredient ingredient,
                                           CraftingRecipe recipe, ItemStack crafted) {
        ItemStack[] choices = ingredient.getItems();
        if (choices.length == 0) return ItemStack.EMPTY;

        // For tagged ingredients (for example #minecraft:planks), prefer the variant
        // that corresponds to the recipe output (oak door -> oak planks).
        String outputPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(crafted.getItem()).getPath();
        String recipePath = recipe.getId().getPath();
        String hint = recipePath.length() > outputPath.length() ? recipePath : outputPath;
        String[] parts = hint.split("_");
        List<ItemStack> preferred = new ArrayList<>();
        for (ItemStack choice : choices) {
            String choicePath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(choice.getItem()).getPath();
            for (String part : parts) {
                if (part.length() >= 3 && choicePath.startsWith(part + "_")) {
                    preferred.add(choice);
                    break;
                }
            }
        }
        ItemStack selected = preferred.isEmpty()
                ? choices[random.nextInt(choices.length)]
                : preferred.get(random.nextInt(preferred.size()));
        return selected.copy();
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
