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
        if (ticks >= 800) {
            finished = true;
            NetworkHandler.finishStoolMinigame(progress);
            if (minecraft != null) minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        gui.drawCenteredString(font, "НЕ ОТВОДИ РУКИ", width / 2, height / 2 - 55, 0xFFE5E5E5);
        gui.drawCenteredString(font, "Нажимай стрелку, которая появляется на экране", width / 2, height / 2 - 35, 0xFFAAAAAA);
        int current = Math.min(sequence.length() - 1, ticks / 40);
        String arrow = sequence.substring(current, current + 1);
        String symbol = switch (arrow) {
            case "U" -> "↑";
            case "D" -> "↓";
            case "L" -> "←";
            default -> "→";
        };
        gui.drawCenteredString(font, symbol, width / 2, height / 2 - 4, 0xFFFFFFFF);
        gui.drawCenteredString(font, "Успешно: " + progress + " / " + sequence.length(), width / 2, height / 2 + 30, 0xFFFFC078);
        gui.drawCenteredString(font, "Время: " + Math.max(0, 40 - ticks / 20) + " сек.", width / 2, height / 2 + 46, 0xFFAAAAAA);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (finished || ticks >= 800) return true;
        String pressed = switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> "U";
            case GLFW.GLFW_KEY_DOWN -> "D";
            case GLFW.GLFW_KEY_LEFT -> "L";
            case GLFW.GLFW_KEY_RIGHT -> "R";
            default -> "";
        };
        if (pressed.isEmpty()) return true;
        int current = Math.min(sequence.length() - 1, ticks / 40);
        if (pressed.equals(sequence.substring(current, current + 1)) && current == progress) progress++;
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
