package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.network.AmbientHorrorNetwork;
import com.fieldofmemories.worldgen.HomeMemoryTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmbientHorrorEvents {
    private static final String JAM_ACTIVE = "FOMDoorJamActive";
    private static final String JAM_X = "FOMDoorJamX";
    private static final String JAM_Y = "FOMDoorJamY";
    private static final String JAM_Z = "FOMDoorJamZ";
    private static final String JAM_COUNT = "FOMDoorJamCount";
    private static final String JAM_UNTIL = "FOMDoorJamUntil";

    private static final String[] DYE_NAMES = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private static final Map<ServerLevel, List<PendingFootstep>> PENDING_STEPS = new HashMap<>();

    private AmbientHorrorEvents() {}

    @SubscribeEvent
    public static void onFootstepSequenceTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<PendingFootstep> pending = PENDING_STEPS.get(level);
        if (pending == null || pending.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<PendingFootstep> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingFootstep step = iterator.next();
            if (step.tick() > now) continue;
            level.playSound(null, step.pos(), step.sound(), SoundSource.PLAYERS, 1.5F, step.pitch());
            iterator.remove();
        }
        if (pending.isEmpty()) PENDING_STEPS.remove(level);
    }

    private record PendingFootstep(long tick, BlockPos pos, SoundEvent sound, float pitch) {}

    public static void tryTrigger(ServerLevel level, List<ServerPlayer> players,
                                  net.minecraft.util.RandomSource random, int danger, int activity) {
        int chance = Math.min(42, 12 + danger / 5 + activity / 20);
        if (random.nextInt(100) >= chance) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        switch (random.nextInt(11)) {
            case 0 -> playKidLaugh(level, target, random);
            case 1 -> {
                if (level.isNight()) playClock(level, target, random);
            }
            case 2 -> playHorrorPipi(level, target, random);
            case 3 -> startDarkPeriod(level);
            case 4 -> playRunningSteps(level, target, random);
            case 5 -> AmbientHorrorNetwork.sendToLevel(level,
                    com.fieldofmemories.client.AmbientHorrorClient.EFFECT_BLACKOUT, 55 + random.nextInt(35));
            case 6 -> breakNearbyWindow(level, target, random);
            case 7 -> AmbientHorrorNetwork.sendToLevel(level,
                    com.fieldofmemories.client.AmbientHorrorClient.EFFECT_SILENCE, 55 + random.nextInt(45));
            case 8 -> placeCakeOutsideHome(level, target, random);
            case 9 -> recolorHomeItem(level, target, random);
            case 10 -> {
                if (level.isNight()) playKidLaugh(level, target, random);
                else playHorrorPipi(level, target, random);
            }
        }
    }

    private static void playKidLaugh(ServerLevel level, ServerPlayer target,
                                     net.minecraft.util.RandomSource random) {
        SoundEvent sound = FieldOfMemories.KID_LAUGH.get();
        playSomewhereNearby(level, target, random, sound, 1.45F, 0.82F + random.nextFloat() * 0.35F);
    }

    private static void playClock(ServerLevel level, ServerPlayer target,
                                  net.minecraft.util.RandomSource random) {
        playSomewhereNearby(level, target, random, FieldOfMemories.CLOCK_CLOCK.get(),
                1.25F, 0.92F + random.nextFloat() * 0.16F);
    }

    private static void playHorrorPipi(ServerLevel level, ServerPlayer target,
                                       net.minecraft.util.RandomSource random) {
        playSomewhereNearby(level, target, random, FieldOfMemories.HORROR_PIPI.get(),
                1.55F, 0.85F + random.nextFloat() * 0.3F);
    }

    private static void playSomewhereNearby(ServerLevel level, ServerPlayer target,
                                            net.minecraft.util.RandomSource random, SoundEvent sound,
                                            float volume, float pitch) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 9.0D + random.nextDouble() * 13.0D;
        BlockPos pos = BlockPos.containing(target.getX() + Math.cos(angle) * distance,
                target.getY() + random.nextInt(5) - 2,
                target.getZ() + Math.sin(angle) * distance);
        level.playSound(null, pos, sound, SoundSource.AMBIENT, volume, pitch);
    }

    private static void playRunningSteps(ServerLevel level, ServerPlayer target,
                                          net.minecraft.util.RandomSource random) {
        Vec3 look = target.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (back.lengthSqr() < 0.001D) back = new Vec3(0.0D, 0.0D, 1.0D);
        back = back.normalize();
        Vec3 side = new Vec3(-back.z, 0.0D, back.x);
        Vec3 start = target.position().add(back.scale(2.2D));
        List<PendingFootstep> sequence = PENDING_STEPS.computeIfAbsent(level, ignored -> new ArrayList<>());
        long startTick = level.getGameTime() + 1L;
        for (int i = 0; i < 5; i++) {
            Vec3 foot = start.add(back.scale(i * 0.48D)).add(side.scale((i % 2 == 0 ? -1 : 1) * 0.18D));
            BlockPos pos = BlockPos.containing(foot.x, foot.y, foot.z);
            BlockState ground = level.getBlockState(pos.below());
            SoundEvent step = ground.getSoundType(level, pos.below(), target).getStepSound();
            sequence.add(new PendingFootstep(startTick + i * 3L, pos, step,
                    0.78F + random.nextFloat() * 0.22F));
        }
    }

    private static void startDarkPeriod(ServerLevel level) {
        AmbientHorrorNetwork.sendToLevel(level,
                com.fieldofmemories.client.AmbientHorrorClient.EFFECT_DARK_PERIOD, 12000);
    }

    private static void breakNearbyWindow(ServerLevel level, ServerPlayer target,
                                          net.minecraft.util.RandomSource random) {
        BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
        BlockPos center = home == null ? target.blockPosition() : home;
        List<BlockPos> glass = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-9, -5, -9), center.offset(9, 5, 9))) {
            if (!level.isLoaded(pos)) continue;
            String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
            if (path.equals("glass") || path.equals("glass_pane")
                    || path.endsWith("_stained_glass") || path.endsWith("_stained_glass_pane")) {
                glass.add(pos.immutable());
            }
        }
        if (glass.isEmpty()) return;
        BlockPos broken = glass.get(random.nextInt(glass.size()));
        level.destroyBlock(broken, false);
        level.playSound(null, broken, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.3F,
                0.78F + random.nextFloat() * 0.25F);
    }

    private static void placeCakeOutsideHome(ServerLevel level, ServerPlayer target,
                                             net.minecraft.util.RandomSource random) {
        BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
        if (home == null) return;
        BlockPos doorPos = BlockPos.findClosestMatch(home, 14, 6,
                pos -> level.getBlockState(pos).getBlock() instanceof DoorBlock).orElse(null);
        if (doorPos == null) return;
        BlockState doorState = level.getBlockState(doorPos);
        if (!(doorState.getBlock() instanceof DoorBlock)) return;
        Direction facing = doorState.getValue(DoorBlock.FACING);
        BlockPos first = doorPos.relative(facing);
        BlockPos second = doorPos.relative(facing.getOpposite());
        BlockPos[] candidates = random.nextBoolean()
                ? new BlockPos[]{first, second} : new BlockPos[]{second, first};
        for (BlockPos pos : candidates) {
            if (level.isLoaded(pos) && level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                level.setBlock(pos, Blocks.CAKE.defaultBlockState(), 3);
                return;
            }
        }
    }

    private static void recolorHomeItem(ServerLevel level, ServerPlayer target,
                                        net.minecraft.util.RandomSource random) {
        BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
        if (home == null) return;
        List<BlockPos> beds = new ArrayList<>();
        List<BlockPos> carpets = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-10, -5, -10), home.offset(10, 5, 10))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock) beds.add(pos.immutable());
            else if (state.getBlock() instanceof CarpetBlock) carpets.add(pos.immutable());
        }

        boolean changeBed = !beds.isEmpty() && (carpets.isEmpty() || random.nextBoolean());
        if (changeBed) {
            BlockPos pos = beds.get(random.nextInt(beds.size()));
            BlockState oldState = level.getBlockState(pos);
            String path = BuiltInRegistries.BLOCK.getKey(oldState.getBlock()).getPath();
            if (!path.endsWith("_bed")) return;
            String current = path.substring(0, path.length() - 4);
            String color = differentColor(current, random);
            Block newBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", color + "_bed"));
            if (!(newBlock instanceof BedBlock)) return;
            Direction facing = oldState.getValue(BedBlock.FACING);
            BedPart part = oldState.getValue(BedBlock.PART);
            boolean occupied = oldState.getValue(BedBlock.OCCUPIED);
            BlockPos otherPos = pos.relative(part == BedPart.FOOT ? facing : facing.getOpposite());
            BlockState otherOld = level.getBlockState(otherPos);
            BlockState newState = newBlock.defaultBlockState()
                    .setValue(BedBlock.FACING, facing)
                    .setValue(BedBlock.PART, part)
                    .setValue(BedBlock.OCCUPIED, occupied);
            level.setBlock(pos, newState, 3);
            if (otherOld.getBlock() instanceof BedBlock) {
                BlockState otherNew = newBlock.defaultBlockState()
                        .setValue(BedBlock.FACING, otherOld.getValue(BedBlock.FACING))
                        .setValue(BedBlock.PART, otherOld.getValue(BedBlock.PART))
                        .setValue(BedBlock.OCCUPIED, otherOld.getValue(BedBlock.OCCUPIED));
                level.setBlock(otherPos, otherNew, 3);
            }
            return;
        }

        if (!carpets.isEmpty()) {
            BlockPos pos = carpets.get(random.nextInt(carpets.size()));
            String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
            if (!path.endsWith("_carpet")) return;
            String current = path.substring(0, path.length() - 7);
            String color = differentColor(current, random);
            Block newBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", color + "_carpet"));
            if (newBlock instanceof CarpetBlock) level.setBlock(pos, newBlock.defaultBlockState(), 3);
        }
    }

    private static String differentColor(String current, net.minecraft.util.RandomSource random) {
        String color = current;
        for (int attempt = 0; attempt < 8 && color.equals(current); attempt++) {
            color = DYE_NAMES[random.nextInt(DYE_NAMES.length)];
        }
        return color.equals(current) ? "pink" : color;
    }

    @SubscribeEvent
    public static void onDoorRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.OPEN)) return;

        var data = player.getPersistentData();
        long now = level.getGameTime();
        boolean active = data.getBoolean(JAM_ACTIVE) && now <= data.getLong(JAM_UNTIL)
                && data.getInt(JAM_X) == pos.getX() && data.getInt(JAM_Y) == pos.getY()
                && data.getInt(JAM_Z) == pos.getZ();

        if (!active) {
            data.remove(JAM_ACTIVE);
            if (level.random.nextInt(220) != 0) return;
            data.putBoolean(JAM_ACTIVE, true);
            data.putInt(JAM_X, pos.getX());
            data.putInt(JAM_Y, pos.getY());
            data.putInt(JAM_Z, pos.getZ());
            data.putInt(JAM_COUNT, 1);
            data.putLong(JAM_UNTIL, now + 100L);
            player.displayClientMessage(Component.literal("Дверь не открывается"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        int clicks = data.getInt(JAM_COUNT) + 1;
        data.putInt(JAM_COUNT, clicks);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (clicks >= 7) {
            if (level.getBlockState(pos).getBlock() instanceof DoorBlock door) {
                door.setOpen(player, level, level.getBlockState(pos), pos, true);
                level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            data.remove(JAM_ACTIVE);
            data.remove(JAM_COUNT);
        } else {
            data.putLong(JAM_UNTIL, now + 100L);
        }
    }
}
