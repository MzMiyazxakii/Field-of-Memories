package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.entity.SpiritEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritSoundClient {
    private static int soundCooldown;
    private SpiritSoundClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            soundCooldown = 0;
            return;
        }

        SpiritEntity nearest = mc.level.getEntitiesOfClass(
                SpiritEntity.class, mc.player.getBoundingBox().inflate(32.0D))
                .stream()
                .filter(spirit -> spirit.isAlive() && !spirit.isRemoved())
                .min((a, b) -> Double.compare(mc.player.distanceToSqr(a), mc.player.distanceToSqr(b)))
                .orElse(null);

        if (nearest == null) {
            soundCooldown = 0;
            return;
        }

        if (soundCooldown > 0) {
            soundCooldown--;
            return;
        }

        double distance = mc.player.distanceTo(nearest);
        float proximity = (float)Math.max(0.0D, Math.min(1.0D, 1.0D - distance / 32.0D));
        float volume = 0.25F + proximity * 0.75F;
        float pitch = 0.88F + mc.level.random.nextFloat() * 0.24F;
        mc.level.playLocalSound(nearest.getX(), nearest.getY(), nearest.getZ(),
                FieldOfMemories.SPIRIT_PRESENCE_SOUND.get(), SoundSource.AMBIENT,
                volume, pitch, false);
        soundCooldown = 80 + mc.level.random.nextInt(41);
    }
}
