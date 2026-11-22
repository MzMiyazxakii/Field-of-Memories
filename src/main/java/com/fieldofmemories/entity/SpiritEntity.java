package com.fieldofmemories.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.EnumSet;

public class SpiritEntity extends PathfinderMob {
    public SpiritEntity(EntityType<? extends SpiritEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new Goal() {
            private Player target;
            private int ticks;

            {
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                target = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))
                        .stream()
                        .filter(player -> player.isAlive() && !player.isSpectator() && !player.isCreative())
                        .min(Comparator.comparingDouble(SpiritEntity.this::distanceToSqr))
                        .orElse(null);
                return target != null && distanceToSqr(target) > 64.0D;
            }

            @Override
            public boolean canContinueToUse() {
                return target != null && target.isAlive() && !target.isSpectator()
                        && distanceToSqr(target) > 36.0D && ticks < 240;
            }

            @Override
            public void start() {
                ticks = 0;
                navigation.moveTo(target, 0.46D);
            }

            @Override
            public void tick() {
                ticks++;
                if (target != null) {
                    lookControl.setLookAt(target, 30.0F, 30.0F);
                    if (ticks % 20 == 0) navigation.moveTo(target, 0.46D);
                }
            }

            @Override
            public void stop() {
                target = null;
                navigation.stop();
            }
        });
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 28.0F, 0.015F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.32D, 70));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // The spirit uses its own distance-based sound instead of block footsteps.
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 2400) {
            discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}