package com.fieldofmemories.client;

import com.fieldofmemories.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public final class StoolMinigameScreen extends Screen {
    private static StoolMinigameScreen activeScreen;
    public static float cameraMadness() { return activeScreen == null || activeScreen.finished ? 0.0F : Math.min(1.0F, 0.25F + activeScreen.progress / (float)Math.max(1, activeScreen.sequence.length()) * 0.75F); }
    public static float cameraPhase(float partialTick) { return activeScreen == null ? 0.0F : activeScreen.ticks + partialTick; }
    private final String sequence;
    private int ticks;
    private int progress;
    private int attempts = 3;
    private int escapePresses;
    private boolean finished;
    private final Random hallucinationRandom = new Random();
    private int hallucinationTimer = 35;
    private String hallucination = "";
    private int hallucinationLife;

    public StoolMinigameScreen(String sequence) {
        super(Component.literal("Аномальный табурет"));
        this.sequence = sequence;
        activeScreen = this;
    }

    @Override
    public void tick() {
        super.tick();
        if (finished) return;
        ticks++;
        if (--hallucinationTimer <= 0) {
            String[] thoughts = {"ТЫ НЕ ОДИН", "НЕ ОБОРАЧИВАЙСЯ", "ВСТАНЬ. СЕЙЧАС ЖЕ.", "ЭТО НЕ ТВОЁ МЕСТО",
                    "ОН СМОТРИТ НА ТЕБЯ", "ТЫ УЖЕ СИДИШЬ СЛИШКОМ ДОЛГО", "НЕ СЛУШАЙ ЕГО"};
            hallucination = thoughts[hallucinationRandom.nextInt(thoughts.length)];
            hallucinationLife = 24 + hallucinationRandom.nextInt(24);
            hallucinationTimer = 45 + hallucinationRandom.nextInt(55);
        }
        if (hallucinationLife > 0) hallucinationLife--;
        if (ticks - progress * 40 >= 40) loseAttempt();
    }

    private void loseAttempt() {
        if (finished) return;
        attempts--;
        if (attempts <= 0) {
            finished = true;
            activeScreen = null;
            NetworkHandler.finishStoolMinigame(-1);
            if (minecraft != null) minecraft.setScreen(null);
        } else {
            ticks = progress * 40;
        }
    }

    private void succeed() {
        if (finished) return;
        finished = true;
        activeScreen = null;
        NetworkHandler.finishStoolMinigame(1);
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void cancel() {
        if (finished) return;
        finished = true;
        activeScreen = null;
        NetworkHandler.finishStoolMinigame(0);
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        float madness = Math.min(1.0F, 0.25F + progress / (float)Math.max(1, sequence.length()) * 0.75F);
        gui.pose().pushPose();
        gui.fill(0, 0, width, height, 0x22000000);
        if (ticks % 11 < 3) {
            int bandY = hallucinationRandom.nextInt(Math.max(1, height));
            gui.fill(0, bandY, width, bandY + 2, 0x44777777);
        }
        gui.drawCenteredString(font, "НЕ ОТВОДИ РУКИ", width / 2, height / 2 - 55, 0xFFE5E5E5);
        gui.drawCenteredString(font, "Нажми нужную стрелку за 2 секунды", width / 2, height / 2 - 35, 0xFFAAAAAA);
        String arrow = sequence.substring(Math.min(progress, sequence.length() - 1), Math.min(progress, sequence.length() - 1) + 1);
        String symbol = switch (arrow) {
            case "U" -> "↑";
            case "D" -> "↓";
            case "L" -> "←";
            default -> "→";
        };
        gui.drawCenteredString(font, symbol, width / 2, height / 2 - 4, 0xFFFFFFFF);
        gui.drawCenteredString(font, "Попытки: " + attempts + " / 3", width / 2, height / 2 + 30, 0xFFFF7777);
        gui.drawCenteredString(font, "Успешно: " + progress + " / " + sequence.length(), width / 2, height / 2 + 46, 0xFFFFC078);
        gui.drawCenteredString(font, "ESC × " + escapePresses + " / 5 — слезть", width / 2, height / 2 + 62, 0xFFAAAAAA);
        if (hallucinationLife > 0) {
            int hx = 12 + hallucinationRandom.nextInt(Math.max(1, width - 24));
            int hy = 12 + hallucinationRandom.nextInt(Math.max(1, height - 24));
            int color = ticks % 6 < 3 ? 0xFFFF3333 : 0xFFB00000;
            gui.drawString(font, hallucination, hx, hy, color, true);
        }
        if (attempts == 1 || progress >= sequence.length() - 3) {
            gui.fill(0, 0, width, height, 0x18000000);
            gui.drawCenteredString(font, "ОНО НЕ ОТПУСТИТ ТЕБЯ", width / 2, Math.max(8, height / 4), 0x99FF2222);
        }
        gui.pose().popPose();
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (finished) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            escapePresses++;
            if (escapePresses >= 5) cancel();
            return true;
        }
        String pressed = switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> "U";
            case GLFW.GLFW_KEY_DOWN -> "D";
            case GLFW.GLFW_KEY_LEFT -> "L";
            case GLFW.GLFW_KEY_RIGHT -> "R";
            default -> "";
        };
        if (pressed.isEmpty()) return true;
        if (pressed.equals(sequence.substring(progress, progress + 1))) {
            progress++;
            if (progress >= sequence.length()) succeed();
        } else {
            loseAttempt();
        }
        return true;
    }

    @Override public void removed() { if (activeScreen == this) activeScreen = null; super.removed(); }

    @Override public boolean isPauseScreen() { return false; }
}
