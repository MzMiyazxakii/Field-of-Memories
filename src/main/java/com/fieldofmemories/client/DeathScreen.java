package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.lwjgl.glfw.GLFW;

/**
 * A black death screen with an intense, unstable CRT television effect.
 * The respawn text is the only interactive element.
 */
public final class DeathScreen extends Screen {
    private int age;
    private boolean respawnHovered;
    private SoundInstance deathScreenSound;
    private boolean respawnRequested;

    private static final int BUTTON_WIDTH = 184;
    private static final int BUTTON_HEIGHT = 30;

    public DeathScreen() {
        super(Component.literal("Смерть"));
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            this.deathScreenSound = SimpleSoundInstance.forUI(
                    FieldOfMemories.DEATH_SCREEN_SOUND.get(), 1.0F
            );
            this.minecraft.getSoundManager().play(this.deathScreenSound);
        }
    }

    @Override
    public void tick() {
        age++;
        // Wait for the server's respawn response instead of closing the screen
        // immediately; otherwise the client can reopen it and replay its sound.
        if (respawnRequested && this.minecraft != null && this.minecraft.player != null
                && !this.minecraft.player.isDeadOrDying()) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int w = this.width;
        int h = this.height;

        // Keep the image almost completely black; the CRT interference provides
        // the only visible detail behind the respawn control.
        gui.fill(0, 0, w, h, 0xFF000000);

        int flicker = 5 + Math.floorMod(age * 17, 7);
        gui.fill(0, 0, w, h, (flicker << 24) | 0x0010181A);

        // Dense horizontal scanlines.
        for (int y = 0; y < h; y += 2) {
            int alpha = (y + age) % 8 == 0 ? 42 : 25;
            gui.fill(0, y, w, Math.min(h, y + 1), (alpha << 24) | 0x00000000);
        }

        // Fine vertical interference and short bursts of analog static.
        for (int x = 0; x < w; x += 4) {
            int alpha = 7 + Math.floorMod(x * 19 + age * 11, 15);
            gui.fill(x, 0, Math.min(w, x + 1), h, (alpha << 24) | 0x004A5558);
        }
        int phase = age / 2;
        for (int i = 0; i < 105; i++) {
            int seed = phase * 1664525 + i * 1013904223;
            int x = Math.floorMod(seed ^ (seed >>> 13), Math.max(1, w));
            int y = Math.floorMod(seed * 31 + i * 7919, Math.max(1, h));
            int width = 2 + Math.floorMod(seed >>> 7, Math.max(3, Math.min(75, w)));
            int alpha = 12 + Math.floorMod(seed >>> 11, 48);
            int color = (alpha << 24) | ((i % 3 == 0) ? 0x00A9B8BA : 0x00FFFFFF);
            gui.fill(x, y, Math.min(w, x + width), Math.min(h, y + 1), color);
        }

        // Heavy rolling tracking bars, with occasional horizontal signal tears.
        int bandY = Math.floorMod(age * 4, Math.max(1, h + 90)) - 45;
        gui.fill(0, bandY, w, Math.min(h, bandY + 5), 0x506D7779);
        gui.fill(0, bandY + 9, w, Math.min(h, bandY + 11), 0x286D7779);
        if (age % 3 == 0) {
            int tearY = Math.floorMod(age * 13, Math.max(1, h));
            gui.fill(0, tearY, w, Math.min(h, tearY + 3), 0x55FFFFFF);
            int offset = Math.floorMod(age * 7, Math.max(1, w / 3));
            gui.fill(offset, tearY + 3, Math.min(w, offset + w / 2), Math.min(h, tearY + 5), 0x388B999B);
        }

        // Dark CRT edges and corners.
        gui.fill(0, 0, w, Math.max(1, h / 12), 0xB9000000);
        gui.fill(0, h - Math.max(1, h / 12), w, h, 0xB9000000);
        gui.fill(0, 0, Math.max(1, w / 12), h, 0xB9000000);
        gui.fill(w - Math.max(1, w / 12), 0, w, h, 0xB9000000);
        gui.fill(0, 0, w / 5, h / 5, 0x35000000);
        gui.fill(w - w / 5, 0, w, h / 5, 0x35000000);
        gui.fill(0, h - h / 5, w / 5, h, 0x35000000);
        gui.fill(w - w / 5, h - h / 5, w, h, 0x35000000);

        // The only visible control: a bright white respawn button.
        int centerX = w / 2;
        int buttonY = h / 2 - BUTTON_HEIGHT / 2;
        int left = centerX - BUTTON_WIDTH / 2;
        int right = centerX + BUTTON_WIDTH / 2;
        respawnHovered = mouseX >= left && mouseX <= right
                && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;

        int pulse = (int) (Math.sin((age + partialTick) * 0.11D) * 13.0D + 242.0D);
        int white = (pulse << 24) | 0x00FFFFFF;
        int textWidth = this.font.width("Возродиться");
        int textX = centerX - textWidth / 2;
        int textY = buttonY + (BUTTON_HEIGHT - this.font.lineHeight) / 2;

        if (respawnHovered) {
            gui.fill(left - 5, buttonY - 4, right + 5, buttonY + BUTTON_HEIGHT + 4, 0x30FFFFFF);
        }
        gui.drawCenteredString(this.font, Component.literal("Возродиться"),
                centerX, textY, respawnHovered ? 0xFFFFFFFF : white);

        // Final interference pass overlays both background and button, like a
        // damaged television signal rather than a clean UI layer.
        for (int y = 1; y < h; y += 4) {
            gui.fill(0, y, w, Math.min(h, y + 1), 0x1E000000);
        }
        if (age % 5 == 0) {
            int flashY = Math.floorMod(age * 23, Math.max(1, h));
            gui.fill(0, flashY, w, Math.min(h, flashY + 1), 0x38FFFFFF);
        }
    }

    private void respawn() {
        if (this.minecraft == null || this.minecraft.player == null || respawnRequested) return;
        respawnRequested = true;
        if (this.deathScreenSound != null) {
            this.minecraft.getSoundManager().stop(this.deathScreenSound);
            this.deathScreenSound = null;
        }
        this.minecraft.player.connection.send(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int centerX = this.width / 2;
            int buttonY = this.height / 2 - BUTTON_HEIGHT / 2;
            int left = centerX - BUTTON_WIDTH / 2;
            int right = centerX + BUTTON_WIDTH / 2;
            if (mouseX >= left && mouseX <= right
                    && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                respawn();
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
