package com.fieldofmemories.client;

import com.fieldofmemories.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class StoolMinigameScreen extends Screen {
    private final String sequence;
    private int ticks;
    private int progress;
    private int attempts = 3;
    private int escapePresses;
    private boolean finished;

    public StoolMinigameScreen(String sequence) {
        super(Component.literal("Аномальный табурет"));
        this.sequence = sequence;
    }

    @Override
    public void tick() {
        super.tick();
        if (finished) return;
        ticks++;
        if (ticks - progress * 40 >= 40) loseAttempt();
    }

    private void loseAttempt() {
        if (finished) return;
        attempts--;
        if (attempts <= 0) {
            finished = true;
            NetworkHandler.finishStoolMinigame(-1);
            if (minecraft != null) minecraft.setScreen(null);
        } else {
            ticks = progress * 40;
        }
    }

    private void succeed() {
        if (finished) return;
        finished = true;
        NetworkHandler.finishStoolMinigame(1);
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void cancel() {
        if (finished) return;
        finished = true;
        NetworkHandler.finishStoolMinigame(0);
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
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

    @Override public boolean isPauseScreen() { return false; }
}
