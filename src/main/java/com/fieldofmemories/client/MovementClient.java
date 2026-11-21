package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class MovementClient {
    private static float maxStamina = 100.0F;
    private static float staminaRecovery = 0.38F;
    private static float sprintSpeedBoost = 0.16F;
    private static float jumpDistanceMultiplier = 1.20F;
    private static int sprintJumpCooldownLength = 12;
    private static int sprintJumpCooldown = 0;
    private static final float SPRINT_UNLOCK_STAMINA = 20.0F;
    private static final UUID RUN_SPEED_MODIFIER = UUID.fromString("7d51a5d1-5f3b-4b2e-9b7b-1c2f7d8a9e31");
    private static AttributeModifier RUN_SPEED_BOOST =
            new AttributeModifier(RUN_SPEED_MODIFIER, "Field of Memories running speed", sprintSpeedBoost,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static float stamina = maxStamina;
    private static int recoveryDelay = 0;
    private static int sprintTicks = 0;
    private static int runParticleTicks = 0;
    private static boolean exhausted = false;
    private static boolean prone = false;
    private static boolean waitingForSpace = false;
    private static int fallRecoveryTicks = 0;
    private static boolean wasJumping = false;
    private static boolean pendingLowJump = false;
    private static float proneYaw;
    private static float pronePitch;
    private static float proneHeadYaw;
    private static float proneBodyYaw;
    private static boolean proneAnglesCaptured = false;
    private static boolean emptySprintAttempt = false;
    private static int shakeTicks = 0;

    private MovementClient() {}

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) { enterProne(); }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) { enterProne(); }

    private static void enterProne() {
        prone = true;
        waitingForSpace = true;
        fallRecoveryTicks = 0;
        proneAnglesCaptured = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) captureProneAngles(player);
    }

    private static void captureProneAngles(LocalPlayer player) {
        proneYaw = player.getYRot();
        pronePitch = player.getXRot();
        proneHeadYaw = player.yHeadRot;
        proneBodyYaw = player.yBodyRot;
        proneAnglesCaptured = true;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!prone || !waitingForSpace || event.getKey() != GLFW.GLFW_KEY_SPACE || event.getAction() != GLFW.GLFW_PRESS) return;
        prone = false;
        waitingForSpace = false;
        proneAnglesCaptured = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setPose(Pose.STANDING);
            player.setDeltaMovement(0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        LocalPlayer player = (LocalPlayer) event.getEntity();
        if (prone) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
            player.setSprinting(false);
            return;
        }
        if (player.isSprinting() && sprintJumpCooldown > 0) event.getInput().jumping = false;
        if (stamina <= 0.0F || (exhausted && stamina < SPRINT_UNLOCK_STAMINA)) {
            player.setSprinting(false);
            removeRunBoost(player);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (prone) {
            if (!waitingForSpace && --fallRecoveryTicks <= 0) {
                prone = false;
                proneAnglesCaptured = false;
                player.setPose(Pose.STANDING);
                player.setDeltaMovement(0, 0, 0);
                return;
            }
            if (!proneAnglesCaptured) captureProneAngles(player);
            player.setYRot(proneYaw);
            player.setXRot(pronePitch);
            player.yRotO = proneYaw;
            player.xRotO = pronePitch;
            player.yHeadRot = proneHeadYaw;
            player.yBodyRot = proneBodyYaw;
            player.setPose(Pose.SWIMMING);
            player.setSprinting(false);
            player.setDeltaMovement(0, 0, 0);
            removeRunBoost(player);
            return;
        }

        if (sprintJumpCooldown > 0) sprintJumpCooldown--;
        boolean sprintKeyDown = mc.options.keySprint.isDown();
        emptySprintAttempt = exhausted && stamina < SPRINT_UNLOCK_STAMINA
                && sprintKeyDown && (player.zza > 0.0F || mc.options.keyUp.isDown());
        if (emptySprintAttempt) shakeTicks++;
        else shakeTicks = 0;

        boolean sprintAllowed = stamina > 0.0F && (!exhausted || stamina >= SPRINT_UNLOCK_STAMINA);
        if (exhausted && stamina >= SPRINT_UNLOCK_STAMINA) exhausted = false;
        if (!sprintAllowed) player.setSprinting(false);

        boolean sprinting = sprintAllowed && player.isSprinting() && player.zza > 0.0F;
        boolean jumping = mc.options.keyJump.isDown() && player.onGround()
                && (!player.isSprinting() || sprintJumpCooldown <= 0);
        if (sprinting) {
            stamina = Math.max(0, stamina - 0.42F);
            recoveryDelay = stamina <= 0 ? 60 : 20;
            sprintTicks++;
            if (stamina <= 0) {
                exhausted = true;
                player.setSprinting(false);
            }
        } else {
            sprintTicks = 0;
        }

        if (jumping && !wasJumping) {
            pendingLowJump = stamina < 7.0F;
            if (player.isSprinting() && stamina >= 7.0F && sprintJumpCooldown <= 0) {
                sprintJumpCooldown = sprintJumpCooldownLength;
                var motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x * jumpDistanceMultiplier, motion.y, motion.z * jumpDistanceMultiplier);
            }
            stamina = Math.max(0, stamina - 7.0F);
            recoveryDelay = stamina <= 0 ? 60 : 20;
            if (stamina <= 0) exhausted = true;
        }
        wasJumping = jumping;

        if (pendingLowJump && !player.onGround() && player.getDeltaMovement().y > 0.0D) {
            player.setDeltaMovement(player.getDeltaMovement().x,
                    player.getDeltaMovement().y * 0.35D, player.getDeltaMovement().z);
            pendingLowJump = false;
        }

        if (recoveryDelay > 0) recoveryDelay--;
        else if (!sprinting && !jumping) {
            stamina = Math.min(maxStamina, stamina + staminaRecovery);
            if (exhausted && stamina >= SPRINT_UNLOCK_STAMINA) exhausted = false;
        }

        if (sprinting && stamina > 0.0F && !exhausted && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
            addRunBoost(player);
            if (++runParticleTicks >= 2) {
                runParticleTicks = 0;
                double side = (mc.level.random.nextDouble() - 0.5D) * 0.55D;
                for (int i = 0; i < 2; i++) {
                    double offsetX = (mc.level.random.nextDouble() - 0.5D) * 0.5D;
                    double offsetZ = (mc.level.random.nextDouble() - 0.5D) * 0.5D;
                    mc.level.addParticle(ParticleTypes.CLOUD,
                            player.getX() + offsetX, player.getY() + 0.08D, player.getZ() + offsetZ,
                            side * 0.08D, 0.025D, side * 0.08D);
                }
            }
        } else {
            removeRunBoost(player);
            runParticleTicks = 0;
        }

        if (sprinting && sprintTicks >= 20 * 4 && player.onGround()
                && mc.level.random.nextDouble() < 0.0018D) {
            startFall(player);
            sprintTicks = 0;
        }
    }

    private static void startFall(LocalPlayer player) {
        player.playSound(SoundEvents.PLAYER_SMALL_FALL, 0.7F, 0.85F);
        prone = true;
        waitingForSpace = false;
        fallRecoveryTicks = 40;
        proneAnglesCaptured = false;
        captureProneAngles(player);
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
        removeRunBoost(player);
    }


    public static float getAdminMaxStamina() { return maxStamina; }
    public static float getAdminRecovery() { return staminaRecovery; }
    public static float getAdminSpeed() { return sprintSpeedBoost; }
    public static float getAdminJump() { return jumpDistanceMultiplier; }
    public static int getAdminJumpCooldown() { return sprintJumpCooldownLength; }
    public static void setAdminMaxStamina(float value) {
        maxStamina = Math.max(20.0F, Math.min(300.0F, value));
        stamina = Math.min(stamina, maxStamina);
    }
    public static void setAdminRecovery(float value) { staminaRecovery = Math.max(0.05F, Math.min(2.0F, value)); }
    public static void setAdminSpeed(float value) {
        sprintSpeedBoost = Math.max(0.0F, Math.min(0.6F, value));
        RUN_SPEED_BOOST = new AttributeModifier(RUN_SPEED_MODIFIER, "Field of Memories running speed",
                sprintSpeedBoost, AttributeModifier.Operation.MULTIPLY_TOTAL);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isSprinting()) {
            removeRunBoost(player);
            addRunBoost(player);
        }
    }
    public static void setAdminJump(float value) { jumpDistanceMultiplier = Math.max(1.0F, Math.min(2.0F, value)); }
    public static void setAdminJumpCooldown(int value) { sprintJumpCooldownLength = Math.max(0, Math.min(40, value)); }
    public static void adminForceFall() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !prone) startFall(player);
    }

    private static void addRunBoost(LocalPlayer player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(RUN_SPEED_MODIFIER) == null) {
            attribute.addTransientModifier(RUN_SPEED_BOOST);
        }
    }

    private static void removeRunBoost(LocalPlayer player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) attribute.removeModifier(RUN_SPEED_MODIFIER);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isSprinting() || !player.onGround() || player.zza <= 0.0F) return;
        float phase = player.tickCount * 0.85F;
        event.setRoll(event.getRoll() + (float) Math.sin(phase) * 0.42F);
        event.setPitch(event.getPitch() + (float) Math.sin(phase * 2.0F) * 0.19F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics gui = event.getGuiGraphics();
        if (prone && !waitingForSpace) {
            int w = gui.guiWidth(), h = gui.guiHeight();
            int fadeTicks = Math.min(fallRecoveryTicks, 40);
            int alpha = Math.min(42, Math.max(0, Math.round(42.0F * fadeTicks / 40.0F)));
            if (alpha > 0) gui.fill(0, 0, w, h, (alpha << 24));
            int textAlpha = Math.min(255, Math.round(255.0F * fadeTicks / 12.0F));
            if (textAlpha > 0) {
                int textY = h - 58;
                gui.drawCenteredString(mc.font, Component.literal("Вы упали"), w / 2, textY,
                        (textAlpha << 24) | 0x00E6E6E6);
            }
        }
        int width = 104, height = 8;
        int x = 10;
        int y = gui.guiHeight() - 16;
        if (emptySprintAttempt) {
            x += Math.round((float) Math.sin(shakeTicks * 1.7D) * 2.0F);
        }
        gui.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xCC101010);
        int filled = Math.round(width * (stamina / maxStamina));
        int color = 0xFFFF8A24;
        gui.fill(x, y, x + filled, y + height, color);
        gui.fill(x + filled, y, x + width, y + height, 0xAA292929);
        if (prone && waitingForSpace) {
            int w = gui.guiWidth(), h = gui.guiHeight();
            gui.fill(0, 0, w, h, 0xAA000000);
            gui.drawCenteredString(mc.font, Component.literal("Нажмите ПРОБЕЛ, чтобы встать"), w / 2, h / 2, 0xFFE6E6E6);
        }
    }
}
