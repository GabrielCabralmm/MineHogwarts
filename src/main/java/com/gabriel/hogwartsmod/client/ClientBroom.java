package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.entity.BroomEntity;
import com.gabriel.hogwartsmod.network.BroomDismountPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientBroom {
    private static boolean wasRiding;
    private static float fovBoost;
    private static float fovBoostOld;

    private ClientBroom() {
    }

    public static void pilot(BroomEntity broom) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || broom.getControllingPassenger() != player) {
            return;
        }
        float forward = Math.signum(player.input.forwardImpulse);
        float vertical = (player.input.jumping ? 1f : 0f) - (player.input.shiftKeyDown ? 1f : 0f);
        broom.fly(forward, vertical, ModKeys.isModifierDown(), player.getYRot(), player.getXRot());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null) {
            return;
        }
        while (ModKeys.BROOM_DISMOUNT.consumeClick()) {
            if (mc.player.getVehicle() instanceof BroomEntity) {
                ModNetwork.CHANNEL.sendToServer(new BroomDismountPacket());
            }
        }
        boolean riding = mc.player.getVehicle() instanceof BroomEntity;
        if (riding && !wasRiding) {
            mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.translatable("message.hogwartsmod.broom_mounted",
                    ModKeys.BROOM_DISMOUNT.getTranslatedKeyMessage(), mc.options.keyJump.getTranslatedKeyMessage(),
                    mc.options.keyShift.getTranslatedKeyMessage()), false);
        }
        wasRiding = riding;
        fovBoostOld = fovBoost;
        boolean boosting = mc.player.getVehicle() instanceof BroomEntity broom && broom.isBoosting();
        fovBoost = Mth.lerp(0.15f, fovBoost, boosting ? 1f : 0f);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        float boost = Mth.lerp((float) event.getPartialTick(), fovBoostOld, fovBoost);
        if (boost > 0.01f) {
            event.setFOV(event.getFOV() * (1.0 + 0.18 * boost));
        }
    }

    @SubscribeEvent
    public static void roll(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof BroomEntity broom && broom.getControllingPassenger() == mc.player) {
            event.setRoll(event.getRoll() + broom.getRoll((float) event.getPartialTick()) * 0.35f);
        }
    }
}
