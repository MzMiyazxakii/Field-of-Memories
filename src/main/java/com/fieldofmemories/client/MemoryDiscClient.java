package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.MemoryDiscItem;
import com.fieldofmemories.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
    private static int promptShakeTicks;
    private static int promptShakeAge;
    private static int postReadCooldownTicks;
    private static int eveningInitialWaitTicks;
    private static MemoryDiscSound activeSound;

    private MemoryDiscClient() {}

    public static boolean isReading() {
        return reading;
    }

    public static boolean shouldDarkenScreen() {
        return reading && (discId == 6 || discId == 7);
    }

    public static void handleServerState(int state, int receivedDiscId, int receivedFragmentIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (state == 0) {
            stopMemorySound(mc);
            reading = true;
            firstFragmentRequested = false;
            discId = receivedDiscId;
            fragmentIndex = -1;
            warningTicks = 0;
            promptShakeTicks = 0;
            promptShakeAge = 0;
            eveningInitialWaitTicks = 0;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(FieldOfMemories.OPEN_SOUNDISK.get(), 1.0F));
        } else if (state == 1 && reading) {
            if (discId == 7 && fragmentIndex < 0) {
                eveningInitialWaitTicks = 40;
            }
            fragmentIndex = receivedFragmentIndex;
        } else if (state == 2) {
            stopMemorySound(mc);
            reading = false;
            firstFragmentRequested = false;
            discId = -1;
            fragmentIndex = -1;
            warningTicks = 0;
            postReadCooldownTicks = 52;
            eveningInitialWaitTicks = 0;
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

        if (promptShakeTicks > 0) promptShakeTicks--;
        if (postReadCooldownTicks > 0) postReadCooldownTicks--;
        if (eveningInitialWaitTicks > 0) eveningInitialWaitTicks--;

        if (reading) {
            if (fragmentIndex < 0) {
                warningTicks++;
                if (warningTicks >= MemoryDiscItem.WARNING_TICKS && !firstFragmentRequested) {
                    firstFragmentRequested = true;
                    startMemorySound(mc);
                    NetworkHandler.requestMemoryDiscNext(-1);
                }
            }

            while (MemoryDiscKeybinds.READ_MEMORY.consumeClick()) {
                if (fragmentIndex >= 0 && eveningInitialWaitTicks <= 0) {
                    promptShakeTicks = 6;
                    promptShakeAge = 0;
                    NetworkHandler.requestMemoryDiscNext(fragmentIndex);
                }
            }
            if (promptShakeTicks > 0) promptShakeAge++;
        } else {
            boolean pressedRead = MemoryDiscKeybinds.READ_MEMORY.consumeClick();
            if (pressedRead && postReadCooldownTicks <= 0
                    && (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof MemoryDiscItem
                    || mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof MemoryDiscItem)) {
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
            if (postReadCooldownTicks > 0) return;
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
            gui.drawCenteredString(mc.font, WARNING_LINE_1, centerX, baseY, 0xFFFFD36A);
            gui.drawCenteredString(mc.font, WARNING_LINE_2, centerX, baseY + 12, 0xFFFFD36A);
            return;
        }

        List<String> fragments = MemoryDiscItem.getFragments(discId);
        if (fragmentIndex >= 0 && fragmentIndex < fragments.size()) {
            MutableComponent message = Component.literal(fragments.get(fragmentIndex));
            if (discId == 6) {
                message = message.withStyle(Style.EMPTY.withFont(new ResourceLocation("minecraft", "uniform")));
            }
            int maxWidth = Math.max(80, width - 32);
            List<FormattedCharSequence> lines = mc.font.split(message, maxWidth);
            int y = baseY - Math.max(0, lines.size() - 1) * 9;
            for (FormattedCharSequence line : lines) {
                gui.drawCenteredString(mc.font, line, centerX, y, 0xFFFFFFFF);
                y += 9;
            }
        }

        if (eveningInitialWaitTicks > 0) return;

        String prompt = "Нажмите U чтоб продолжить";
        float shakeX = promptShakeTicks > 0 ? (float) Math.sin(promptShakeAge * 2.8D) * 0.8F : 0.0F;
        float shakeY = promptShakeTicks > 0 ? (float) Math.cos(promptShakeAge * 3.1D) * 0.65F : 0.0F;
        gui.pose().pushPose();
        gui.pose().translate(width - 8.0D, 0.0D, 0.0D);
        gui.pose().scale(0.88F, 1.0F, 1.0F);
        int promptX = -mc.font.width(prompt) + Math.round(shakeX);
        int promptY = height - 18 + Math.round(shakeY);
        gui.drawString(mc.font, prompt, promptX, promptY, 0xFFFFFFFF, true);
        gui.pose().popPose();
    }
}
