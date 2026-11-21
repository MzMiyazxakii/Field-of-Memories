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

        // Dim phosphor glow, subtle vertical interference and analog picture noise.
        for (int x = 0; x < w; x += 3) {
            int alpha = 8 + Math.floorMod(x * 13 + age * 5, 9);
            gui.fill(x, 0, Math.min(w, x + 1), h, (alpha << 24) | 0x003D5960);
        }
        int phase = age / 2;
        for (int i = 0; i < 52; i++) {
            int seed = phase * 1664525 + i * 1013904223;
            int x = Math.floorMod(seed ^ (seed >>> 15), Math.max(1, w));
            int y = Math.floorMod(seed * 17 + i * 7919, Math.max(1, h));
            int streakWidth = 3 + Math.floorMod(seed >>> 5, Math.max(4, Math.min(42, w)));
            int alpha = 12 + Math.floorMod(seed >>> 9, 29);
            gui.fill(x, y, Math.min(w, x + streakWidth), Math.min(h, y + 1),
                    (alpha << 24) | 0x00B9D5D8);
        }

        // Uneven rolling tracking bands, like a damaged analog signal.
        int bandY = Math.floorMod(age * 3, Math.max(1, h + 36)) - 18;
        gui.fill(0, bandY, w, Math.min(h, bandY + 2), 0x387FADB0);
        gui.fill(0, bandY + 5, w, Math.min(h, bandY + 7), 0x1685AEB3);

        // Heavy dark edges and fine scanlines finish the CRT image.
        for (int y = 0; y < h; y += 3) {
            gui.fill(0, y, w, Math.min(h, y + 1), 0x26000000);
        }
        gui.fill(0, 0, w, 18, 0x85000000);
        gui.fill(0, h - 18, w, h, 0x85000000);
        gui.fill(0, 0, 18, h, 0x85000000);
        gui.fill(w - 18, 0, w, h, 0x85000000);

        int titleY = centerY - 76;
        int titleColor = (age % 53 < 2) ? 0xFFB5C7C5 : 0xFFE7D8B8;
        gui.pose().pushPose();
        gui.pose().translate(centerX, titleY + 5, 0);
        // Slightly wider lettering, now a pale, sickly phosphor rather than bright red.
        gui.pose().scale(2.65F, 2.35F, 1.0F);
        gui.drawCenteredString(this.font, Component.literal("СМЕРТЬ"), 1, 1, 0xFF241D19);
        gui.drawCenteredString(this.font, Component.literal("СМЕРТЬ"), 0, 0, titleColor);
        gui.pose().popPose();

        int heartY = centerY + 55;
        heartHovered = mouseX >= centerX - 50 && mouseX <= centerX + 50
                && mouseY >= heartY - 38 && mouseY <= heartY + 38;
        float beat = 1.0F + (float) Math.sin((age + partialTick) * 0.16D) * 0.035F;
        int heartBase = heartHovered ? 0xFFFF6A68 : 0xFFB91525;
        gui.pose().pushPose();
        gui.pose().translate(centerX - 45, heartY - 31, 0);
        gui.pose().scale(beat, beat, 1.0F);
        // A custom pixel heart replaces the font glyph with a clean, unmistakable silhouette.
        int[] heartRows = {
                0b0001100001100,
                0b0011110011110,
                0b0111111111111,
                0b0111111111111,
                0b0011111111110,
                0b0001111111100,
                0b0000111111000,
                0b0000011110000,
                0b0000001100000
        };
        for (int row = 0; row < heartRows.length; row++) {
            for (int col = 0; col < 13; col++) {
                if ((heartRows[row] & (1 << (12 - col))) != 0) {
                    int x = col * 7;
                    int y = row * 7;
                    gui.fill(x + 2, y + 2, x + 9, y + 9, 0xFF26050A);
                    int shade = row < 3 ? heartBase : (heartHovered ? 0xFFFF8A78 : 0xFFE43A3B);
                    gui.fill(x, y, x + 7, y + 7, shade);
                }
            }
        }
        gui.fill(17, 9, 31, 13, heartHovered ? 0xFFFFD0B9 : 0xFFFFA39A);
        gui.pose().popPose();

        // Final CRT pass deliberately crosses the foreground elements.
        for (int y = 0; y < h; y += 4) {
            gui.fill(0, y, w, Math.min(h, y + 1), 0x1C9AAEB0);
        }
        int tearY = Math.floorMod(age * 5, Math.max(1, h));
        gui.fill(0, tearY, w, Math.min(h, tearY + 2), 0x3094B9BC);
        if (age % 4 == 0) {
            int noiseY = Math.floorMod(age * 11, Math.max(1, h));
            gui.fill(0, noiseY, w, Math.min(h, noiseY + 1), 0x25FFFFFF);
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
            int heartY = this.height / 2 + 55;
            if (mouseX >= centerX - 50 && mouseX <= centerX + 50
                    && mouseY >= heartY - 38 && mouseY <= heartY + 38) {
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
