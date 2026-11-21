package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.MemoryDiscItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class MemoryDiscClient {
    private static final String WARNING_LINE_1 = "Диск самолично лезет вам в лицо";
    private static final String WARNING_LINE_2 = "В вашей голове играют чужие воспоминания";

    private MemoryDiscClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // The reading timeline is derived from the active use duration. Releasing
        // the mouse button naturally stops the sequence without leaving stale text.
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isUsingItem()
                || !(mc.player.getUseItem().getItem() instanceof MemoryDiscItem disc)) return;

        int elapsed = mc.player.getTicksUsingItem();
        GuiGraphics gui = event.getGuiGraphics();
        int centerX = gui.guiWidth() / 2;
        int baseY = gui.guiHeight() - 48;
        int color = 0xFFE8E8E8;

        if (elapsed < MemoryDiscItem.WARNING_TICKS) {
            if (elapsed < 10) return;
            gui.drawCenteredString(mc.font, WARNING_LINE_1, centerX, baseY, color);
            gui.drawCenteredString(mc.font, WARNING_LINE_2, centerX, baseY + 10, color);
            return;
        }

        List<String> fragments = MemoryDiscItem.getFragments(disc.getDiscId());
        int fragmentIndex = (elapsed - MemoryDiscItem.WARNING_TICKS) / MemoryDiscItem.FRAGMENT_TICKS;
        if (fragmentIndex < 0 || fragmentIndex >= fragments.size()) return;

        var message = Component.literal(fragments.get(fragmentIndex));
        if (disc.getDiscId() == 6) {
            message = message.setStyle(Style.EMPTY.withFont(new ResourceLocation("minecraft", "uniform")));
        }
        int maxWidth = Math.max(80, gui.guiWidth() - 32);
        List<net.minecraft.util.FormattedCharSequence> lines = mc.font.split(message, maxWidth);
        int y = baseY - Math.max(0, lines.size() - 1) * 9;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            gui.drawCenteredString(mc.font, line, centerX, y, color);
            y += 9;
        }
    }
}
