package com.fieldofmemories.client;

import com.fieldofmemories.item.OldCameraItem;
import com.fieldofmemories.item.VideoCameraItem;
import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class BatterySelectionScreen extends Screen {
    private final InteractionHand batteryHand;
    private boolean hasVideoCamera;
    private boolean hasOldCamera;

    public BatterySelectionScreen(InteractionHand batteryHand) {
        super(Component.translatable("screen.fieldofmemories.battery.title"));
        this.batteryHand = batteryHand;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        hasVideoCamera = false;
        hasOldCamera = false;

        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof VideoCameraItem
                    && !VideoCameraItem.hasInsertedBattery(stack)
                    && VideoCameraItem.getBattery(stack) < VideoCameraItem.MAX_BATTERY) hasVideoCamera = true;
            if (stack.getItem() instanceof OldCameraItem
                    && !OldCameraItem.hasInsertedBattery(stack)
                    && OldCameraItem.getCharge(stack) < OldCameraItem.MAX_CHARGE) hasOldCamera = true;
        }
        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand.getItem() instanceof VideoCameraItem
                && !VideoCameraItem.hasInsertedBattery(offhand)
                && VideoCameraItem.getBattery(offhand) < VideoCameraItem.MAX_BATTERY) hasVideoCamera = true;
        if (offhand.getItem() instanceof OldCameraItem
                && !OldCameraItem.hasInsertedBattery(offhand)
                && OldCameraItem.getCharge(offhand) < OldCameraItem.MAX_CHARGE) hasOldCamera = true;

        int optionCount = (hasVideoCamera ? 1 : 0) + (hasOldCamera ? 1 : 0);
        int buttonWidth = 170;
        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 2 - (optionCount * 24 + 22) / 2;

        if (hasVideoCamera) {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.fieldofmemories.battery.video_camera"),
                    button -> insertBattery(true))
                    .bounds(x, y, buttonWidth, 20).build());
            y += 24;
        }
        if (hasOldCamera) {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.fieldofmemories.battery.old_camera"),
                    button -> insertBattery(false))
                    .bounds(x, y, buttonWidth, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x, y + 2, buttonWidth, 20).build());
    }

    private void insertBattery(boolean videoCamera) {
        HorrorNetwork.requestBatteryInsert(videoCamera, batteryHand);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 55, 0xFFFFFF);
        if (!hasVideoCamera && !hasOldCamera) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.fieldofmemories.battery.none"),
                    width / 2, height / 2 - 10, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
