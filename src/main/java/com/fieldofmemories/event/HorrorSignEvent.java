package com.fieldofmemories.event;

import com.fieldofmemories.worldgen.HomeMemoryTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public final class HorrorSignEvent {
    private static final String[] WHITE_MESSAGES = {
            "Leave",
            "LEAVE LEAVE LEAVE LEAVE LEAVE LEAVE LEAVE",
            "I won’t let you \"live\"",
            "Nothing dies twice",
            "Turn around",
            "You stink",
            "This is MY spot",
            "You are just a matter of time",
            "I hate you",
            "fag",
            "A nonentity",
            "DIE",
            "eight",
            "Center of the cemetery"
    };

    private static final String[] BLUE_MESSAGES = {
            "Mother?",
            "I will not give myself to her",
            "These are MY memories",
            "No one has the right to tell me what to do",
            "She is looking for you",
            "Videocamera",
            "Doesn't see the eyes",
            "Don't look into the darkness"
    };

    private HorrorSignEvent() {}

    public static void tryTrigger(ServerLevel level, List<ServerPlayer> players,
                                  RandomSource random, int danger, int activity) {
        if (players.isEmpty()) return;

        int chance = Math.min(90, (28 + danger / 2) * activity / 100);
        if (random.nextInt(100) >= chance) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        boolean blue = random.nextInt(100) < 32;
        String message = blue
                ? BLUE_MESSAGES[random.nextInt(BLUE_MESSAGES.length)]
                : WHITE_MESSAGES[random.nextInt(WHITE_MESSAGES.length)];

        if (random.nextInt(100) < 48 && replaceNearbySign(level, target, random, message, blue, 24)) {
            return;
        }

        BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
        BlockPos anchor = home != null ? home : target.blockPosition();
        int minDistance = home != null ? 4 : 9;
        int maxDistance = home != null ? 9 : 15;
        if (placeNewSign(level, anchor, random, message, blue, minDistance, maxDistance)) {
            return;
        }

        replaceNearbySign(level, target, random, message, blue, 28);
    }

    private static boolean replaceNearbySign(ServerLevel level, ServerPlayer player,
                                             RandomSource random, String message,
                                             boolean blue, int radius) {
        List<BlockPos> signs = new ArrayList<>();
        BlockPos center = player.blockPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                for (int y = -5; y <= 7; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    if (level.getBlockEntity(pos) instanceof SignBlockEntity) {
                        signs.add(pos.immutable());
                    }
                }
            }
        }

        if (signs.isEmpty()) return false;
        BlockPos chosen = signs.get(random.nextInt(signs.size()));
        if (level.getBlockEntity(chosen) instanceof SignBlockEntity sign) {
            writeMessage(sign, message, blue);
            level.sendBlockUpdated(chosen, level.getBlockState(chosen),
                    level.getBlockState(chosen), 3);
            return true;
        }
        return false;
    }

    private static boolean placeNewSign(ServerLevel level, BlockPos anchor, RandomSource random,
                                        String message, boolean blue, int minDistance,
                                        int maxDistance) {
        for (int attempt = 0; attempt < 28; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            int x = anchor.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = anchor.getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos ground = pos.below();

            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                    || !level.canSeeSky(pos)) {
                continue;
            }
            if (hasNearbySign(level, pos, 5)) continue;

            BlockState state = Blocks.OAK_SIGN.defaultBlockState()
                    .setValue(StandingSignBlock.ROTATION, random.nextInt(16));
            if (!level.setBlock(pos, state, 3)) continue;

            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                writeMessage(sign, message, blue);
                level.sendBlockUpdated(pos, state, state, 3);
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbySign(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 3; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof SignBlockEntity) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void writeMessage(SignBlockEntity sign, String message, boolean blue) {
        String[] lines = wrap(message);
        SignText front = sign.getFrontText();
        SignText back = sign.getBackText();
        for (int i = 0; i < 4; i++) {
            Component line = Component.literal(i < lines.length ? lines[i] : "");
            front = front.setMessage(i, line);
            back = back.setMessage(i, line);
        }
        DyeColor color = blue ? DyeColor.BLUE : DyeColor.BLACK;
        sign.setText(front.setColor(color), true);
        sign.setText(back.setColor(color), false);
        sign.setChanged();
    }

    private static String[] wrap(String message) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : message.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > 15) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            if (word.length() <= 15) {
                line.append(word);
            } else {
                for (int i = 0; i < word.length(); i += 15) {
                    if (line.length() > 0) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    int end = Math.min(word.length(), i + 15);
                    lines.add(word.substring(i, end));
                }
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines.stream().limit(4).toArray(String[]::new);
    }
}
