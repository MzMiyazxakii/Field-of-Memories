package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.client.MemoryDiscClient;
import com.fieldofmemories.client.StoolMinigameScreen;
import com.fieldofmemories.network.NetworkHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.common.ForgeMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
    private static float longJumpDistance = 1.12F;
    private static float longJumpHeight = 0.30F;
    private static float longJumpStaminaCost = 32.0F;
    private static int sprintJumpCooldown = 0;
    private static final float SPRINT_UNLOCK_STAMINA = 20.0F;
    private static final UUID RUN_SPEED_MODIFIER = UUID.fromString("7d51a5d1-5f3b-4b2e-9b7b-1c2f7d8a9e31");
    private static final UUID EXHAUSTED_STEP_MODIFIER = UUID.fromString("9c1d7b2e-6a4f-4d3b-8f2a-1e5c7b9d0a42");
    private static final AttributeModifier EXHAUSTED_STEP_BOOST = new AttributeModifier(
            EXHAUSTED_STEP_MODIFIER, "Field of Memories exhausted step height", 0.4D,
            AttributeModifier.Operation.ADDITION);
    private static AttributeModifier RUN_SPEED_BOOST =
            new AttributeModifier(RUN_SPEED_MODIFIER, "Field of Memories running speed", sprintSpeedBoost,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static float stamina = maxStamina;
    private static int recoveryDelay = 0;
    private static int sprintTicks = 0;
    // Prevent vanilla's held-sprint logic from immediately re-enabling sprint
    // after the player presses the sprint key to switch back to walking.
    private static boolean sprintCancelUntilRelease = false;
    private static int runParticleTicks = 0;
    private static boolean exhausted = false;
    private static boolean prone = false;
    private static boolean waitingForSpace = false;
    private static int fallRecoveryTicks = 0;
    private static boolean wasJumping = false;
    private static boolean pendingLowJump = false;
    private static boolean jumpHeld = false;
    private static boolean jumpInputWasDown = false;
    private static boolean longJumpInputSuppressed = false;
    private static boolean emptySprintAttempt = false;
    private static int shakeTicks = 0;
    private static int fallNoticeTicks = 0;
    private static float spawnLockedYaw = 0.0F;
    private static float spawnLockedPitch = 0.0F;
    private static float savedLeftArmX, savedLeftArmY, savedLeftArmZ;

    private MovementClient() {}

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) { enterProne(); }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) { enterProne(); }

    private static void enterProne() {
        prone = true;
        waitingForSpace = true;
        fallRecoveryTicks = 0;
        fallNoticeTicks = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            spawnLockedYaw = player.getYRot();
            spawnLockedPitch = player.getXRot();
            player.setPose(Pose.SWIMMING);
            player.setSprinting(false);
            removeRunBoost(player);
        }
        NetworkHandler.sendProneState(true);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer keyPlayer = mc.player;
        if (mc.options.keySprint.matches(event.getKey(), event.getScanCode())) {
            if (event.getAction() == GLFW.GLFW_PRESS && keyPlayer != null && keyPlayer.isSprinting()) {
                // A second press while running means "walk", not "keep sprinting".
                // Keep the cancellation latched until release so vanilla cannot
                // turn sprint back on while the key is still held.
                sprintCancelUntilRelease = true;
                keyPlayer.setSprinting(false);
                removeRunBoost(keyPlayer);
                return;
            }
            if (event.getAction() == GLFW.GLFW_RELEASE) {
                sprintCancelUntilRelease = false;
            }
        }
        if (event.getAction() != GLFW.GLFW_PRESS
                || !mc.options.keyJump.matches(event.getKey(), event.getScanCode())) return;
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (prone && waitingForSpace) {
            prone = false;
            waitingForSpace = false;
            player.setPose(Pose.STANDING);
            NetworkHandler.sendProneState(false);
            player.setDeltaMovement(0, 0, 0);
            // The jump binding used to get up must not also count as a jump.
            jumpInputWasDown = true;
        }
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        LocalPlayer player = (LocalPlayer) event.getEntity();
        longJumpInputSuppressed = false;
        if (sprintCancelUntilRelease) {
            player.setSprinting(false);
            removeRunBoost(player);
        }
        if (prone) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
            jumpInputWasDown = false;
            player.setSprinting(false);
            return;
        }
        boolean jumpDown = event.getInput().jumping;
        boolean jumpPressed = jumpDown && !jumpInputWasDown;
        jumpInputWasDown = jumpDown;
        boolean shiftDown = Minecraft.getInstance().options.keyShift.isDown();
        boolean sprintJumpBlocked = player.isSprinting() && sprintJumpCooldown > 0;
        if (sprintJumpBlocked) event.getInput().jumping = false;
        // Shift+Space is handled manually below so vanilla cannot launch a
        // normal jump before the long-jump impulse is applied.
        if (player.onGround() && shiftDown && stamina >= longJumpStaminaCost) {
            event.getInput().jumping = false;
            longJumpInputSuppressed = true;
        } else if (jumpPressed && player.onGround() && !shiftDown && !sprintJumpBlocked) {
            // Charge stamina on the input edge, before vanilla launches the
            // player. Waiting until ClientTick END misses the first jump
            // because the player is already airborne by then.
            float staminaBeforeJump = stamina;
            stamina = Math.max(0.0F, stamina - 7.0F);
            recoveryDelay = stamina <= 0.0F ? 60 : 20;
            pendingLowJump = staminaBeforeJump < 7.0F;
            if (stamina <= 0.0F) exhausted = true;
        }
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
        if (fallNoticeTicks > 0) fallNoticeTicks--;
        if (waitingForSpace) {
            player.setYRot(spawnLockedYaw);
            player.setXRot(spawnLockedPitch);
            player.yRotO = spawnLockedYaw;
            player.xRotO = spawnLockedPitch;
        }
        if (prone) {
            if (!waitingForSpace && --fallRecoveryTicks <= 0) {
                prone = false;
                    player.setPose(Pose.STANDING);
                NetworkHandler.sendProneState(false);
                player.setDeltaMovement(0, 0, 0);
                return;
            }
            // Vanilla recalculates the local player's pose during its tick and
            // can replace the forced prone pose with STANDING. Re-apply SWIMMING
            // at END phase so rendering sees one stable pose in both camera modes.
            // Do not rewrite position or velocity here: that would create a
            // separate client/server correction loop.
            player.setPose(Pose.SWIMMING);
            // A prone player is stationary: do not let residual velocity or
            // swimming travel move the player across the ground.
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.zza = 0.0F;
            player.xxa = 0.0F;
            player.setSprinting(false);
            removeRunBoost(player);
            return;
        }

        if (sprintJumpCooldown > 0) sprintJumpCooldown--;
        if (sprintCancelUntilRelease) {
            player.setSprinting(false);
            removeRunBoost(player);
        }
        boolean sprintKeyDown = mc.options.keySprint.isDown();
        emptySprintAttempt = exhausted && stamina < SPRINT_UNLOCK_STAMINA
                && sprintKeyDown && (player.zza > 0.0F || mc.options.keyUp.isDown());
        if (emptySprintAttempt) shakeTicks++;
        else shakeTicks = 0;

        boolean sprintAllowed = stamina > 0.0F && (!exhausted || stamina >= SPRINT_UNLOCK_STAMINA);
        if (exhausted && stamina >= SPRINT_UNLOCK_STAMINA) exhausted = false;
        if (!sprintAllowed) player.setSprinting(false);

        boolean sprinting = sprintAllowed && player.isSprinting() && player.zza > 0.0F;
        jumpHeld = mc.options.keyJump.isDown();
        boolean jumping = jumpHeld && player.onGround()
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
            boolean crouchJump = longJumpInputSuppressed && mc.options.keyShift.isDown();
            longJumpInputSuppressed = false;
            if (crouchJump) {
                pendingLowJump = false;
                // Trigger the base jump here (vanilla input was suppressed),
                // then add the long-jump height and horizontal impulse.
                player.jumpFromGround();
                var motion = player.getDeltaMovement();
                var look = player.getLookAngle();
                double lookLength = Math.sqrt(look.x * look.x + look.z * look.z);
                if (lookLength > 1.0E-4D) {
                    double boost = longJumpDistance;
                    player.setDeltaMovement(
                            motion.x + (look.x / lookLength) * boost,
                            motion.y + longJumpHeight,
                            motion.z + (look.z / lookLength) * boost);
                }
                stamina = Math.max(0, stamina - longJumpStaminaCost);
                recoveryDelay = 45;
            } else {
                // Stamina was already charged on the jump input edge in onInput.
                if (player.isSprinting() && sprintJumpCooldown <= 0) {
                    sprintJumpCooldown = sprintJumpCooldownLength;
                    var motion = player.getDeltaMovement();
                    player.setDeltaMovement(motion.x * jumpDistanceMultiplier, motion.y, motion.z * jumpDistanceMultiplier);
                }
            }
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
        updateExhaustedStepHeight(player);

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

        // After a sustained sprint, stumbling must be realistically reachable
        // before the stamina bar is depleted. Keep it probabilistic, but not
        // so rare that repeated long runs almost never trigger it.
        if (sprinting && sprintTicks >= 20 * 4 && player.onGround()
                && mc.level.random.nextDouble() < 0.012D) {
            startFall(player);
            sprintTicks = 0;
        }
    }

    public static void beginForcedFallFromServer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) startFall(player, false);
    }

    private static void startFall(LocalPlayer player) {
        startFall(player, true);
    }

    private static void startFall(LocalPlayer player, boolean notifyServer) {
        // Play directly through the client sound manager so the stumble cue
        // is not dependent on positional attenuation or server sound packets.
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forLocalAmbience(SoundEvents.PLAYER_SMALL_FALL, 0.85F, 0.7F));
        fallNoticeTicks = 40;
        // A short, subtle dust puff at the feet when the sprint stumble begins.
        for (int i = 0; i < 7; i++) {
            double angle = (Math.PI * 2.0D * i) / 7.0D;
            double speed = 0.035D + player.level().random.nextDouble() * 0.025D;
            player.level().addParticle(ParticleTypes.CLOUD,
                    player.getX() + Math.cos(angle) * 0.22D,
                    player.getY() + 0.06D,
                    player.getZ() + Math.sin(angle) * 0.22D,
                    Math.cos(angle) * speed, 0.025D, Math.sin(angle) * speed);
        }
        for (int i = 0; i < 3; i++) {
            double offsetX = (player.level().random.nextDouble() - 0.5D) * 0.45D;
            double offsetZ = (player.level().random.nextDouble() - 0.5D) * 0.45D;
            player.level().addParticle(ParticleTypes.POOF,
                    player.getX() + offsetX, player.getY() + 0.12D, player.getZ() + offsetZ,
                    0.0D, 0.015D, 0.0D);
        }
        prone = true;
        waitingForSpace = false;
        shakeTicks = 0;
        player.setSprinting(false);
        removeRunBoost(player);
        if (notifyServer) NetworkHandler.sendProneState(true);
        fallRecoveryTicks = 40;
        player.setPose(Pose.SWIMMING);
        player.setDeltaMovement(0, 0, 0);
        player.setSprinting(false);
        removeRunBoost(player);
    }


    public static float getAdminMaxStamina() { return maxStamina; }
    public static float getAdminRecovery() { return staminaRecovery; }
    public static float getAdminSpeed() { return sprintSpeedBoost; }
    public static float getAdminJump() { return jumpDistanceMultiplier; }
    public static int getAdminJumpCooldown() { return sprintJumpCooldownLength; }
    public static float getAdminLongJumpDistance() { return longJumpDistance; }
    public static float getAdminLongJumpHeight() { return longJumpHeight; }
    public static float getAdminLongJumpCost() { return longJumpStaminaCost; }
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
    public static void setAdminLongJumpDistance(float value) { longJumpDistance = Math.max(0.2F, Math.min(2.5F, value)); }
    public static void setAdminLongJumpHeight(float value) { longJumpHeight = Math.max(0.0F, Math.min(0.8F, value)); }
    public static void setAdminLongJumpCost(float value) { longJumpStaminaCost = Math.max(0.0F, Math.min(100.0F, value)); }
    public static void adminForceFall() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !prone) startFall(player);
    }

    private static void updateExhaustedStepHeight(LocalPlayer player) {
        var attribute = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attribute == null) return;
        if (stamina <= 0.0F) {
            if (attribute.getModifier(EXHAUSTED_STEP_MODIFIER) == null) {
                attribute.addTransientModifier(EXHAUSTED_STEP_BOOST);
            }
        } else {
            attribute.removeModifier(EXHAUSTED_STEP_MODIFIER);
        }
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

        float madness = StoolMinigameScreen.cameraMadness();
        if (madness > 0.0F) {
            float t = (float) StoolMinigameScreen.cameraPhase((float) event.getPartialTick());
            float fast = 1.0F + madness * 3.5F;
            event.setYaw(event.getYaw() + (float)Math.sin(t * 0.83F) * 0.75F * fast);
            event.setPitch(event.getPitch() + (float)Math.cos(t * 1.17F) * 0.55F * fast);
            event.setRoll(event.getRoll() + (float)Math.sin(t * 1.43F) * 0.9F * fast);
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (waitingForSpace) {
            event.setYaw(spawnLockedYaw);
            event.setPitch(spawnLockedPitch);
            event.setRoll(0.0F);
            return;
        }
        if (player == null || prone || !player.onGround() || player.zza <= 0.0F) return;

        double horizontalSpeedSqr = player.getDeltaMovement().horizontalDistanceSqr();
        if (horizontalSpeedSqr < 0.0025D) return;

        float phase;
        float rollAmount;
        float pitchAmount;
        if (player.isSprinting()) {
            // Running has a more pronounced, heavier camera sway.
            phase = player.tickCount * 0.85F;
            rollAmount = 0.65F;
            pitchAmount = 0.29F;
        } else {
            // A light step rhythm while walking; faster movement increases
            // both the rhythm and the sway slightly without reaching run intensity.
            boolean fastSteps = horizontalSpeedSqr > 0.012D;
            phase = player.tickCount * (fastSteps ? 0.72F : 0.52F);
            rollAmount = fastSteps ? 0.28F : 0.16F;
            pitchAmount = fastSteps ? 0.11F : 0.07F;
        }
        event.setRoll(event.getRoll() + (float) Math.sin(phase) * rollAmount);
        event.setPitch(event.getPitch() + (float) Math.sin(phase * 2.0F) * pitchAmount);
    }

    @SubscribeEvent
    public static void onRenderPronePlayer(RenderPlayerEvent.Pre event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (!prone || localPlayer == null || event.getEntity() != localPlayer) return;
        var model = event.getRenderer().getModel();
        // Pose.SWIMMING supplies the horizontal prone silhouette, but its
        // vanilla swim cycle paddles the limbs. Freeze the limbs in one pose.
        model.leftArm.xRot = -0.18F;
        model.rightArm.xRot = -0.18F;
        model.leftArm.yRot = 0.0F;
        model.rightArm.yRot = 0.0F;
        model.leftArm.zRot = 0.0F;
        model.rightArm.zRot = 0.0F;
        model.leftLeg.xRot = 0.08F;
        model.rightLeg.xRot = 0.08F;
        model.leftLeg.yRot = 0.0F;
        model.rightLeg.yRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
        model.rightLeg.zRot = 0.0F;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null || event.getEntity() != localPlayer
                || !Minecraft.getInstance().options.keyShift.isDown()
                || !localPlayer.onGround() || prone) return;

        float readiness = Math.min(1.0F, stamina / Math.max(1.0F, longJumpStaminaCost));
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, localPlayer.getBbHeight() + 0.34D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        MultiBufferSource buffers = event.getMultiBufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        int halfWidth = 22;
        float barHeight = 4.0F;
        drawBillboardQuad(consumer, poseStack, -halfWidth - 1, -1, halfWidth + 1, barHeight + 1, 0, 0, 0, 190);
        int fillWidth = Math.round(halfWidth * 2.0F * readiness);
        int red = readiness >= 1.0F ? 70 : 235;
        int green = readiness >= 1.0F ? 220 : 145;
        drawBillboardQuad(consumer, poseStack, -halfWidth, 0, -halfWidth + fillWidth, barHeight,
                red, green, 45, 240);
        poseStack.popPose();
    }

    private static void drawBillboardQuad(VertexConsumer consumer, PoseStack poseStack,
                                          float left, float top, float right, float bottom,
                                          int red, int green, int blue, int alpha) {
        var matrix = poseStack.last().pose();
        consumer.vertex(matrix, left, top, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, left, bottom, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, right, bottom, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, right, top, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            event.setNewScreen(new DeathScreen());
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        // A restrained darkness pass before the HUD: the world is a little dimmer
        // at both day and night, while the HUD and CRT pass remain clearly visible.
        if (event.getOverlay() == VanillaGuiOverlay.VIGNETTE.type()
                && mc.player != null && mc.screen == null && !prone && !waitingForSpace) {
            GuiGraphics gui = event.getGuiGraphics();
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), 0x0C000000);
        }
        // During the initial spawn/get-up sequence, keep the vanilla hotbar
        // and crosshair hidden until the player presses Space.
        if (prone && waitingForSpace
                && (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()
                || event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()
                || event.getOverlay() == VanillaGuiOverlay.CHAT_PANEL.type())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderScreenEffectsWhileHudHidden(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui || mc.player == null || mc.screen != null || event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;
        GuiGraphics gui = event.getGuiGraphics(); int w = gui.guiWidth(), h = gui.guiHeight();
        if (MemoryDiscClient.shouldDarkenScreen()) gui.fill(0, 0, w, h, 0xEA000000);
        if (MemoryDiscClient.isReading() && !MemoryDiscClient.shouldDarkenScreen()) gui.fill(0, 0, w, h, 0x16A00020);
        if (!waitingForSpace) { gui.fill(0, 0, w, h, 0x12000000); if (prone) gui.fill(0, 0, w, h, 0x40000000);
            for (int y = 0; y < h; y += 2) gui.fill(0, y, w, Math.min(h, y + 1), 0x1B000000);
            int band = (mc.player.tickCount * 3) % Math.max(1, h); gui.fill(0, band, w, Math.min(h, band + 3), 0x18D5E0E5);
            int phase = mc.player.tickCount / 2; for (int i = 0; i < 13; i++) { int seed = phase * 1664525 + i * 1013904223; int x = Math.floorMod(seed ^ (seed >>> 15), Math.max(1, w)); int y = Math.floorMod(seed * 17 + i * 7919, Math.max(1, h)); int sw = 2 + Math.floorMod(seed >>> 5, 9); gui.fill(x, y, Math.min(w, x + sw), Math.min(h, y + 1), 0x24DDE5E8); }
        }
        if (prone && !waitingForSpace) { int a = Math.min(72, Math.max(0, Math.round(72.0F * fallRecoveryTicks / 40.0F))); if (a > 0) gui.fill(0, 0, w, h, a << 24); }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics gui = event.getGuiGraphics();

        // Darken the complete frame for the two atmospheric discs first.
        // The CRT pass below is deliberately drawn afterward so its scanlines,
        // interference band and static remain visible over the darkness.
        if (MemoryDiscClient.shouldDarkenScreen()) {
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), 0xEA000000);
        }
        // A restrained crimson tint while any memory disc is being read.
        // Draw it before the CRT pass so scanlines and interference stay crisp.
        if (MemoryDiscClient.isReading() && !MemoryDiscClient.shouldDarkenScreen()) {
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), 0x16A00020);
        }

        // Alternate, slightly darker CRT look: dense scanlines, a drifting
        // interference band and sparse static streaks. The vignette is drawn
        // separately below so it remains visible while the player is prone.
        if (!waitingForSpace) {
            int screenWidth = gui.guiWidth();
            int screenHeight = gui.guiHeight();
            gui.fill(0, 0, screenWidth, screenHeight, 0x12000000);
            // Keep the exact same CRT elements during a fall, but darken the
            // entire image underneath them so the interference remains visible.
            if (prone) {
                gui.fill(0, 0, screenWidth, screenHeight, 0x40000000);
            }
            for (int lineY = 0; lineY < screenHeight; lineY += 2) {
                gui.fill(0, lineY, screenWidth, Math.min(screenHeight, lineY + 1), 0x1B000000);
            }

            int bandY = (mc.player.tickCount * 3) % Math.max(1, screenHeight);
            gui.fill(0, bandY, screenWidth, Math.min(screenHeight, bandY + 3), 0x18D5E0E5);
            int phase = mc.player.tickCount / 2;
            for (int i = 0; i < 13; i++) {
                int seed = phase * 1664525 + i * 1013904223;
                int noiseX = Math.floorMod(seed ^ (seed >>> 15), Math.max(1, screenWidth));
                int noiseY = Math.floorMod(seed * 17 + i * 7919, Math.max(1, screenHeight));
                int streakWidth = 2 + Math.floorMod(seed >>> 5, 9);
                gui.fill(noiseX, noiseY, Math.min(screenWidth, noiseX + streakWidth),
                        Math.min(screenHeight, noiseY + 1), 0x24DDE5E8);
            }

        }
        if (prone && !waitingForSpace) {
            int w = gui.guiWidth(), h = gui.guiHeight();
            int alpha = Math.min(72, Math.max(0, Math.round(72.0F * fallRecoveryTicks / 40.0F)));
            if (alpha > 0) gui.fill(0, 0, w, h, (alpha << 24));
        }
        if (fallNoticeTicks > 0) {
            int textAlpha = Math.min(255, Math.round(255.0F * fallNoticeTicks / 40.0F));
            gui.drawCenteredString(mc.font, Component.literal("Вы упали"), gui.guiWidth() / 2,
                    gui.guiHeight() - 58, (textAlpha << 24) | 0x00E6E6E6);
        }
        int width = 104, height = 6;
        int x = 10;
        int y = gui.guiHeight() - 14;
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
            gui.fill(0, 0, w, h, 0xE6000000);
            gui.drawCenteredString(mc.font, Component.literal("Нажмите ПРОБЕЛ, чтобы встать"), w / 2, h / 2, 0xFFE6E6E6);
        }
        // Keep the vignette hidden during the spawn lock and prone recovery.
        if (!prone && !waitingForSpace) {
            int screenWidth = gui.guiWidth();
            int screenHeight = gui.guiHeight();
            gui.fill(0, 0, 12, screenHeight, 0x28000000);
            gui.fill(Math.max(0, screenWidth - 12), 0, screenWidth, screenHeight, 0x28000000);
            gui.fill(0, 0, screenWidth, 3, 0x28000000);
            gui.fill(0, Math.max(0, screenHeight - 3), screenWidth, screenHeight, 0x28000000);
        }
    }
}
