package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.entity.SpiritEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritSoundClient {
    private static PresenceSound activeSound;

    private SpiritSoundClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopPresenceSound(mc);
            return;
        }

        SpiritEntity nearest = mc.level.getEntitiesOfClass(
                SpiritEntity.class, mc.player.getBoundingBox().inflate(24.0D))
                .stream()
                .filter(spirit -> spirit.isAlive() && !spirit.isRemoved())
                .min((a, b) -> Double.compare(mc.player.distanceToSqr(a), mc.player.distanceToSqr(b)))
                .orElse(null);

        if (nearest == null) {
            stopPresenceSound(mc);
            return;
        }

        if (activeSound == null || activeSound.getSpiritId() != nearest.getId() || activeSound.isStopped()) {
            stopPresenceSound(mc);
            activeSound = new PresenceSound(nearest.getId());
            mc.getSoundManager().play(activeSound);
        }
        activeSound.updateVolume(mc.player.distanceTo(nearest));
    }

    private static void stopPresenceSound(Minecraft mc) {
        if (activeSound != null) {
            mc.getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }

    private static final class PresenceSound extends AbstractTickableSoundInstance {
        private final int spiritId;

        private PresenceSound(int spiritId) {
            super(FieldOfMemories.SPIRIT_PRESENCE_SOUND.get(), SoundSource.MASTER, RandomSource.create());
            this.spiritId = spiritId;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.55F;
            this.pitch = 1.0F;
        }

        private int getSpiritId() {
            return spiritId;
        }

        private void updateVolume(double distance) {
            float proximity = (float) Math.max(0.0D, Math.min(1.0D, 1.0D - distance / 24.0D));
            this.volume = 0.55F + proximity * 0.30F;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                stop();
                return;
            }

            var entity = mc.level.getEntity(spiritId);
            if (!(entity instanceof SpiritEntity spirit) || !spirit.isAlive() || spirit.isRemoved()
                    || mc.player.distanceToSqr(spirit) > 24.0D * 24.0D) {
                stop();
                return;
            }

            this.x = mc.player.getX();
            this.y = mc.player.getY();
            this.z = mc.player.getZ();
        }
    }
}
