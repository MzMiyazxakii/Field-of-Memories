package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.entity.SpiritEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class SpiritSoundClient {
    private static SpiritPresenceSound activeSound;

    private SpiritSoundClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopSound(mc);
            return;
        }

        SpiritEntity nearest = mc.level.getEntitiesOfClass(
                SpiritEntity.class, mc.player.getBoundingBox().inflate(64.0D))
                .stream()
                .min((a, b) -> Double.compare(mc.player.distanceToSqr(a), mc.player.distanceToSqr(b)))
                .orElse(null);

        if (activeSound != null && (nearest == null || activeSound.spirit != nearest)) {
            stopSound(mc);
        }
        if (nearest != null && activeSound == null) {
            activeSound = new SpiritPresenceSound(nearest);
            mc.getSoundManager().play(activeSound);
        }
    }

    private static void stopSound(Minecraft mc) {
        if (activeSound != null) {
            mc.getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }

    private static final class SpiritPresenceSound extends AbstractTickableSoundInstance {
        private final SpiritEntity spirit;

        private SpiritPresenceSound(SpiritEntity spirit) {
            super(FieldOfMemories.SPIRIT_PRESENCE_SOUND.get(), SoundSource.AMBIENT, RandomSource.create());
            this.spirit = spirit;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            this.attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (spirit.isRemoved() || !spirit.isAlive() || mc.player == null || mc.level != spirit.level()) {
                stop();
                return;
            }

            this.x = spirit.getX();
            this.y = spirit.getY();
            this.z = spirit.getZ();
            double distance = mc.player.distanceTo(spirit);
            double proximity = Math.max(0.0D, Math.min(1.0D, 1.0D - distance / 64.0D));
            this.volume = (float) Math.pow(proximity, 1.35D);
        }
    }
}