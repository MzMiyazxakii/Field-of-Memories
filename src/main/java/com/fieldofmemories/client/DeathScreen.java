package com.fieldofmemories.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.lwjgl.glfw.GLFW;

/**
 * A custom death screen with a heavily darkened CRT/static treatment.
 * The heart is the sole respawn control.
 */
public final class DeathScreen extends Screen {
    private int age;
    private boolean heartHovered;

    public DeathScreen() {
        super(Component.literal("Смерть"));
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int w = this.width;
        int h = this.height;
        int centerX = w / 2;
        int centerY = h / 2;

        // Almost-black picture with a faint, irregular red pulse.
        gui.fill(0, 0, w, h, 0xFF020203);
        int pulse = (int) (Math.sin((age + partialTick) * 0.075D) * 5.0D + 8.0D);
        gui.fill(0, 0, w, h, (pulse << 24) | 0x00300000);

        // Heavy CRT scanlines and a slowly moving interference band.
        for (int y = 0; y < h; y += 2) {
            gui.fill(0, y, w, Math.min(h, y + 1), 0x38000000);
        }
        int bandY = Math.floorMod(age * 4, Math.max(1, h));
        gui.fill(0, bandY, w, Math.min(h, bandY + 4), 0x20B7C2C8);

        // Deterministic animated static: short horizontal streaks.
        int phase = age / 2;
        for (int i = 0; i < 34; i++) {
            int seed = phase * 1664525 + i * 1013904223;
            int x = Math.floorMod(seed ^ (seed >>> 15), Math.max(1, w));
            int y = Math.floorMod(seed * 17 + i * 7919, Math.max(1, h));
            int streakWidth = 2 + Math.floorMod(seed >>> 5, Math.max(3, Math.min(24, w)));
            int alpha = 18 + Math.floorMod(seed >>> 9, 35);
            gui.fill(x, y, Math.min(w, x + streakWidth), Math.min(h, y + 1),
                    (alpha << 24) | 0x00D5DADD);
        }

        // Dark vignette around the image edges.
        gui.fill(0, 0, w, 12, 0x70000000);
        gui.fill(0, h - 12, w, h, 0x70000000);
        gui.fill(0, 0, 12, h, 0x70000000);
        gui.fill(w - 12, 0, w, h, 0x70000000);

        int titleY = centerY - 62;
        int flicker = (age % 47 < 2) ? 0xFF650000 : 0xFFFF2020;
        gui.drawCenteredString(this.font, Component.literal("СМЕРТЬ"), centerX + 2, titleY + 1, 0xFF4A0000);
        gui.drawCenteredString(this.font, Component.literal("СМЕРТЬ"), centerX, titleY, flicker);
        gui.drawCenteredString(this.font, Component.literal("СИГНАЛ ПОТЕРЯН"), centerX, centerY - 28, 0xFF8B3434);

        int heartY = centerY + 48;
        heartHovered = mouseX >= centerX - 34 && mouseX <= centerX + 34
                && mouseY >= heartY - 30 && mouseY <= heartY + 28;
        float beat = 1.0F + (float) Math.sin((age + partialTick) * 0.16D) * 0.055F;
        int heartColor = heartHovered ? 0xFFFF7777 : 0xFFB50918;
        gui.pose().pushPose();
        gui.pose().translate(centerX, heartY, 0);
        gui.pose().scale(beat, beat, 1.0F);
        gui.drawCenteredString(this.font, Component.literal("♥"), 0, -18, 0xFF3A0000);
        gui.drawCenteredString(this.font, Component.literal("♥"), 0, -19, heartColor);
        gui.pose().popPose();

        gui.drawCenteredString(this.font, Component.literal("ВОЗРОДИТЬСЯ"), centerX, heartY + 27,
                heartHovered ? 0xFFFFAAAA : 0xFFB86B6B);
        gui.drawCenteredString(this.font, Component.literal("НАЖМИ НА СЕРДЦЕ"), centerX, h - 28, 0xFF6B5555);
    }

    private void respawn() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        this.minecraft.player.connection.send(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int centerX = this.width / 2;
            int heartY = this.height / 2 + 48;
            if (mouseX >= centerX - 34 && mouseX <= centerX + 34
                    && mouseY >= heartY - 30 && mouseY <= heartY + 28) {
                respawn();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            respawn();
            return true;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
