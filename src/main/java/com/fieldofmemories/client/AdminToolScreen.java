package com.fieldofmemories.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import com.fieldofmemories.network.NetworkHandler;
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
            add("Заставить игрока упасть", cx, y, () -> go("players"));
            add("Назад", cx, y + 52, () -> go("home"));
        } else if (page.equals("players")) {
            int row = 0;
            if (Minecraft.getInstance().level != null) {
                for (var player : Minecraft.getInstance().level.players()) {
                    if (row >= 6) break;
                    final var targetId = player.getUUID();
                    add(player.getGameProfile().getName(), cx, y + row * 24, () -> {
                        NetworkHandler.sendForceFall(targetId);
                        go("events");
                    });
                    row++;
                }
            }
            add("Назад", cx, y + 6 * 24, () -> go("events"));
        } else if (page.equals("movement")) {
            add("Скорость бега −", cx - 82, y, () -> MovementClient.setAdminSpeed(MovementClient.getAdminSpeed() - 0.02F));
            add("Скорость бега +", cx + 82, y, () -> MovementClient.setAdminSpeed(MovementClient.getAdminSpeed() + 0.02F));
            add("Назад", cx, y + 52, () -> go("home"));
        } else if (page.equals("stamina")) {
            add("Макс. выносливость −", cx - 82, y, () -> MovementClient.setAdminMaxStamina(MovementClient.getAdminMaxStamina() - 10));
            add("Макс. выносливость +", cx + 82, y, () -> MovementClient.setAdminMaxStamina(MovementClient.getAdminMaxStamina() + 10));
            add("Восстановление −", cx - 82, y + 27, () -> MovementClient.setAdminRecovery(MovementClient.getAdminRecovery() - 0.05F));
            add("Восстановление +", cx + 82, y + 27, () -> MovementClient.setAdminRecovery(MovementClient.getAdminRecovery() + 0.05F));
            add("Назад", cx, y + 100, () -> go("home"));
        } else if (page.equals("jumps")) {
            int jumpY = height / 2 - 12;
            add("Дальность −", cx - 82, jumpY, () -> MovementClient.setAdminJump(MovementClient.getAdminJump() - 0.05F));
            add("Дальность +", cx + 82, jumpY, () -> MovementClient.setAdminJump(MovementClient.getAdminJump() + 0.05F));
            add("Откат −", cx - 82, jumpY + 22, () -> MovementClient.setAdminJumpCooldown(MovementClient.getAdminJumpCooldown() - 2));
            add("Откат +", cx + 82, jumpY + 22, () -> MovementClient.setAdminJumpCooldown(MovementClient.getAdminJumpCooldown() + 2));
            add("Длинный прыжок −", cx - 82, jumpY + 44, () -> MovementClient.setAdminLongJumpDistance(MovementClient.getAdminLongJumpDistance() - 0.1F));
            add("Длинный прыжок +", cx + 82, jumpY + 44, () -> MovementClient.setAdminLongJumpDistance(MovementClient.getAdminLongJumpDistance() + 0.1F));
            add("Высота −", cx - 82, jumpY + 66, () -> MovementClient.setAdminLongJumpHeight(MovementClient.getAdminLongJumpHeight() - 0.05F));
            add("Высота +", cx + 82, jumpY + 66, () -> MovementClient.setAdminLongJumpHeight(MovementClient.getAdminLongJumpHeight() + 0.05F));
            add("Цена выносливости −", cx - 82, jumpY + 88, () -> MovementClient.setAdminLongJumpCost(MovementClient.getAdminLongJumpCost() - 2));
            add("Цена выносливости +", cx + 82, jumpY + 88, () -> MovementClient.setAdminLongJumpCost(MovementClient.getAdminLongJumpCost() + 2));
            add("Назад", cx, jumpY + 110, () -> go("home"));
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
            gui.drawCenteredString(font, Component.literal(String.format("Дальность: %.2f | Откат: %d", MovementClient.getAdminJump(), MovementClient.getAdminJumpCooldown())), width / 2, height / 2 - 64, 0xFFFFFFFF);
            gui.drawCenteredString(font, Component.literal(String.format("Длинный прыжок: %.2f | Высота: %.2f", MovementClient.getAdminLongJumpDistance(), MovementClient.getAdminLongJumpHeight())), width / 2, height / 2 - 52, 0xFFFFFFFF);
            gui.drawCenteredString(font, Component.literal(String.format("Цена выносливости: %.0f", MovementClient.getAdminLongJumpCost())), width / 2, height / 2 - 40, 0xFFFFFFFF);
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private String pageName() {
        return switch (page) {
            case "events" -> "СОБЫТИЯ";
            case "players" -> "ВЫБЕРИТЕ ИГРОКА";
            case "movement" -> "ДВИЖЕНИЕ";
            case "stamina" -> "ВЫНОСЛИВОСТЬ";
            case "jumps" -> "ПРЫЖКИ";
            default -> "ГЛАВНОЕ МЕНЮ";
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}