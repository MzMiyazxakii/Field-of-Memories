package com.fieldofmemories.entity;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

public class HolyWaterProjectile extends ThrowableItemProjectile {
    public HolyWaterProjectile(EntityType<? extends HolyWaterProjectile> type, Level level) { super(type, level); }
    public HolyWaterProjectile(Level level, LivingEntity owner) { super(FieldOfMemories.HOLY_WATER_PROJECTILE.get(), owner, level); }

    @Override protected Item getDefaultItem() { return FieldOfMemories.HOLY_WATER.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().sendParticles(new DustParticleOptions(new Vector3f(0.12F, 0.55F, 1.0F), 1.15F),
                    getX(), getY(), getZ(), 24, 0.24D, 0.18D, 0.24D, 0.025D);
            discard();
        }
    }
}