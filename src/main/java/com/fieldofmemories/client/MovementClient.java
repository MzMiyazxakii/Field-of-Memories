package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class MovementClient {
    private static final float MAX_STAMINA = 100.0F;
    private static float stamina = MAX_STAMINA;
    private static int recoveryDelay = 0;
    private static int sprintTicks = 0;
    private static boolean exhausted = false;
    private static boolean prone = false;
    private static boolean wasJumping = false;

    private MovementClient() {}

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) { enterProne(); }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) { enterProne(); }

    private static void enterProne() { prone = true; }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!prone || event.getKey() != GLFW.GLFW_KEY_SPACE || event.getAction() != GLFW.GLFW_PRESS) return;
        prone = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setPose(Pose.STANDING);
            player.setDeltaMovement(0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (prone) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (prone) {
            player.setPose(Pose.SWIMMING);
            player.setSprinting(false);
            player.setDeltaMovement(0, 0, 0);
            return;
        }

        boolean sprinting = player.isSprinting() && player.zza > 0.0F;
        boolean jumping = mc.options.keyJump.isDown() && player.onGround();
        if (sprinting) {
            stamina = Math.max(0, stamina - 0.42F);
            recoveryDelay = stamina <= 0 ? 60 : 20;
            sprintTicks++;
            if (stamina <= 0) { exhausted = true; player.setSprinting(false); }
        } else sprintTicks = 0;

        if (jumping && !wasJumping) {
            stamina = Math.max(0, stamina - 7.0F);
            recoveryDelay = stamina <= 0 ? 60 : 20;
            if (stamina <= 0) exhausted = true;
        }
        wasJumping = jumping;
        if (exhausted) player.setSprinting(false);
        if (recoveryDelay > 0) recoveryDelay--;
        else if (!sprinting && !jumping) {
            stamina = Math.min(MAX_STAMINA, stamina + 0.38F);
            if (stamina > 0) exhausted = false;
        }

        if (sprintTicks >= 20 * 12 && player.onGround()) {
            double chance = jumping ? 0.00055D : 0.00016D;
            if (mc.level.random.nextDouble() < chance) {
                enterProne();
                player.setPose(Pose.SWIMMING);
                player.setSprinting(false);
                sprintTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics gui = event.getGuiGraphics();
        int x = 10, y = 10, width = 104, height = 8;
        gui.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xCC101010);
        int filled = Math.round(width * (stamina / MAX_STAMINA));
        int color = stamina > 35 ? 0xFFB8C6B0 : 0xFFB56A62;
        gui.fill(x, y, x + filled, y + height, color);
        gui.fill(x + filled, y, x + width, y + height, 0xAA292929);
        if (prone) {
            int w = gui.guiWidth(), h = gui.guiHeight();
            gui.fill(0, 0, w, h, 0x88000000);
            gui.drawCenteredString(mc.font, Component.literal("Нажмите ПРОБЕЛ, чтобы встать"), w / 2, h / 2, 0xFFE6E6E6);
        }
    }
}
