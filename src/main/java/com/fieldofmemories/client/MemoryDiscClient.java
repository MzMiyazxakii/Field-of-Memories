package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.MemoryDiscItem;
import com.fieldofmemories.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class MemoryDiscClient {
    private static final String WARNING_LINE_1 = "Диск лезет вам прямо в лицо";
    private static final String WARNING_LINE_2 = "В вашей голове начинают звучать чужие воспоминания";

    private static boolean reading;
    private static boolean firstFragmentRequested;
    private static int discId = -1;
    private static int fragmentIndex = -1;
    private static int warningTicks;
    private static MemoryDiscSound activeSound;

    private MemoryDiscClient() {}

    public static void handleServerState(int state, int receivedDiscId, int receivedFragmentIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (state == 0) {
            stopMemorySound(mc);
            reading = true;
            firstFragmentRequested = false;
            discId = receivedDiscId;
            fragmentIndex = -1;
            warningTicks = 0;
        } else if (state == 1 && reading) {
            fragmentIndex = receivedFragmentIndex;
        } else if (state == 2) {
            stopMemorySound(mc);
            reading = false;
            firstFragmentRequested = false;
            discId = -1;
            fragmentIndex = -1;
            warningTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            stopMemorySound(mc);
            reading = false;
            return;
        }

        if (reading) {
            if (fragmentIndex < 0) {
                warningTicks++;
                if (warningTicks >= MemoryDiscItem.WARNING_TICKS && !firstFragmentRequested) {
                    firstFragmentRequested = true;
                    startMemorySound(mc);
                    NetworkHandler.requestMemoryDiscNext();
                }
            }

            while (MemoryDiscKeybinds.READ_MEMORY.consumeClick()) {
                if (fragmentIndex >= 0) NetworkHandler.requestMemoryDiscNext();
            }
        } else if (MemoryDiscKeybinds.READ_MEMORY.consumeClick()) {
            if (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof MemoryDiscItem
                    || mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof MemoryDiscItem) {
                NetworkHandler.requestMemoryDiscStart();
            }
        }
    }

    private static void startMemorySound(Minecraft mc) {
        stopMemorySound(mc);
        activeSound = new MemoryDiscSound();
        mc.getSoundManager().play(activeSound);
    }

    private static void stopMemorySound(Minecraft mc) {
        if (activeSound != null) {
            mc.getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }

    private static final class MemoryDiscSound extends AbstractTickableSoundInstance {
        private MemoryDiscSound() {
            super(FieldOfMemories.MEMORY_DISC_SOUND.get(), SoundSource.PLAYERS, RandomSource.create());
            this.looping = true;
            this.relative = true;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {}
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics gui = event.getGuiGraphics();
        int width = gui.guiWidth();
        int height = gui.guiHeight();

        if (!reading) {
            boolean hasDisc = mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof MemoryDiscItem
                    || mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof MemoryDiscItem;
            if (hasDisc) {
                String prompt = "Нажмите U чтоб прочитать";
                gui.drawString(mc.font, prompt, width - mc.font.width(prompt) - 8, height - 18, 0xFFFFFFFF, true);
            }
            return;
        }

        int centerX = width / 2;
        int baseY = height - 48;
        if (fragmentIndex < 0) {
            gui.drawCenteredString(mc.font, WARNING_LINE_1, centerX, baseY, 0xFF55FF55);
            gui.drawCenteredString(mc.font, WARNING_LINE_2, centerX, baseY + 12, 0xFF55FF55);
            return;
        }

        List<String> fragments = MemoryDiscItem.getFragments(discId);
        if (fragmentIndex >= 0 && fragmentIndex < fragments.size()) {
            Component message = Component.literal(fragments.get(fragmentIndex));
            if (discId == 6) {
                message = message.setStyle(Style.EMPTY.withFont(new ResourceLocation("minecraft", "uniform")));
            }
            int maxWidth = Math.max(80, width - 32);
            List<FormattedCharSequence> lines = mc.font.split(message, maxWidth);
            int y = baseY - Math.max(0, lines.size() - 1) * 9;
            for (FormattedCharSequence line : lines) {
                gui.drawCenteredString(mc.font, line, centerX, y, 0xFFFFFFFF);
                y += 9;
            }
        }

        String prompt = "Нажмите U чтоб продолжить";
        gui.drawString(mc.font, prompt, width - mc.font.width(prompt) - 8, height - 18, 0xFFFFFFFF, true);
    }
}
