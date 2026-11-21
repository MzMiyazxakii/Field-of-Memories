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

        int titleY = centerY - 76;
        int flicker = (age % 47 < 2) ? 0xFF9A2525 : 0xFFFF3B3B;
        gui.pose().pushPose();
        gui.pose().translate(centerX, titleY + 5, 0);
        gui.pose().scale(2.35F, 2.35F, 1.0F);
        // A dark offset keeps the enlarged title readable against the CRT image.
        gui.drawCenteredString(this.font, Component.literal("СМЕРТЬ"), 1, 1, 0xFF260000);
        gui.drawCenteredString(this.font, Component.literal("СМЕРТЬ"), 0, 0, flicker);
        gui.pose().popPose();

        int heartY = centerY + 55;
        heartHovered = mouseX >= centerX - 48 && mouseX <= centerX + 48
                && mouseY >= heartY - 44 && mouseY <= heartY + 42;
        float beat = 1.0F + (float) Math.sin((age + partialTick) * 0.16D) * 0.045F;
        int heartColor = heartHovered ? 0xFFFF7777 : 0xFFE52B43;
        gui.pose().pushPose();
        gui.pose().translate(centerX, heartY, 0);
        gui.pose().scale(beat * 2.25F, beat * 2.25F, 1.0F);
        // Layered heart gives the respawn control a cleaner, fuller silhouette.
        gui.drawCenteredString(this.font, Component.literal("♥"), 0, -9, 0xFF260006);
        gui.drawCenteredString(this.font, Component.literal("♥"), 0, -10, heartColor);
        gui.pose().popPose();

        // Faint interference streaks pass over the title and heart like CRT noise.
        int overlayY = Math.floorMod(age * 3, Math.max(1, h));
        gui.fill(0, overlayY, w, Math.min(h, overlayY + 2), 0x249AA5AA);
        if (age % 5 == 0) {
            int noiseY = Math.floorMod(age * 11, Math.max(1, h));
            gui.fill(0, noiseY, w, Math.min(h, noiseY + 1), 0x1EFFFFFF);
        }
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
