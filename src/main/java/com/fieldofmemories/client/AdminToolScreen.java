package com.fieldofmemories.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;

public class AdminToolScreen extends Screen {
    private String page = "home";

    public AdminToolScreen() { super(Component.literal("Field of Memories — Admin Tool")); }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        int y = height / 2 - 65;
        if (page.equals("home")) {
            add("События", cx, y, () -> go("events")); y += 25;
            add("Движение", cx, y, () -> go("movement")); y += 25;
            add("Выносливость", cx, y, () -> go("stamina")); y += 25;
            add("Прыжки", cx, y, () -> go("jumps"));
        } else if (page.equals("events")) {
            add("Заставить упасть", cx, y, MovementClient::adminForceFall);
            add("Назад", cx, y + 52, () -> go("home"));
        } else if (page.equals("movement")) {
            add("Скорость бега −", cx - 82, y, () -> MovementClient.setAdminSpeed(MovementClient.getAdminSpeed() - 0.02F));
            add("Скорость бега +", cx + 82, y, () -> MovementClient.setAdminSpeed(MovementClient.getAdminSpeed() + 0.02F));
            add("Назад", cx, y + 52, () -> go("home"));
        } else if (page.equals("stamina")) {
            add("Макс. выносливость −", cx - 82, y, () -> MovementClient.setAdminMaxStamina(MovementClient.getAdminMaxStamina() - 10));
            add("Макс. выносливость +", cx + 82, y, () -> MovementClient.setAdminMaxStamina(MovementClient.getAdminMaxStamina() + 10));
            add("Восстановление −", cx - 82, y + 27, () -> MovementClient.setAdminRecovery(MovementClient.getAdminRecovery() - 0.05F));
            add("Восстановление +", cx + 82, y + 27, () -> MovementClient.setAdminRecovery(MovementClient.getAdminRecovery() + 0.05F));
            add("Назад", cx, y + 78, () -> go("home"));
        } else if (page.equals("jumps")) {
            add("Дальность −", cx - 82, y, () -> MovementClient.setAdminJump(MovementClient.getAdminJump() - 0.05F));
            add("Дальность +", cx + 82, y, () -> MovementClient.setAdminJump(MovementClient.getAdminJump() + 0.05F));
            add("Откат −", cx - 82, y + 27, () -> MovementClient.setAdminJumpCooldown(MovementClient.getAdminJumpCooldown() - 2));
            add("Откат +", cx + 82, y + 27, () -> MovementClient.setAdminJumpCooldown(MovementClient.getAdminJumpCooldown() + 2));
            add("Назад", cx, y + 78, () -> go("home"));
        }
    }

    private void add(String label, int centerX, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(centerX - 78, y, 156, 20).build());
    }

    private void go(String next) { page = next; init(); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        gui.drawCenteredString(font, Component.literal("АДМИН-ПАНЕЛЬ"), width / 2, height / 2 - 94, 0xFFFF9A38);
        gui.drawCenteredString(font, Component.literal(pageName()), width / 2, height / 2 - 81, 0xFFE0E0E0);
        if (page.equals("movement")) gui.drawCenteredString(font, Component.literal(String.format("Бонус скорости: %.2f", MovementClient.getAdminSpeed())), width / 2, height / 2 + 12, 0xFFFFFFFF);
        if (page.equals("stamina")) {
            gui.drawCenteredString(font, Component.literal("Максимум: " + (int) MovementClient.getAdminMaxStamina()), width / 2, height / 2 + 2, 0xFFFFFFFF);
            gui.drawCenteredString(font, Component.literal(String.format("Восстановление: %.2f / тик", MovementClient.getAdminRecovery())), width / 2, height / 2 + 14, 0xFFFFFFFF);
        }
        if (page.equals("jumps")) {
            gui.drawCenteredString(font, Component.literal(String.format("Множитель дальности: %.2f", MovementClient.getAdminJump())), width / 2, height / 2 + 2, 0xFFFFFFFF);
            gui.drawCenteredString(font, Component.literal("Откат: " + MovementClient.getAdminJumpCooldown() + " тиков"), width / 2, height / 2 + 14, 0xFFFFFFFF);
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private String pageName() {
        return switch (page) {
            case "events" -> "СОБЫТИЯ";
            case "movement" -> "ДВИЖЕНИЕ";
            case "stamina" -> "ВЫНОСЛИВОСТЬ";
            case "jumps" -> "ПРЫЖКИ";
            default -> "ГЛАВНОЕ МЕНЮ";
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}