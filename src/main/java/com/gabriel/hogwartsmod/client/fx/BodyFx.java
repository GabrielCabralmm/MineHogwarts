package com.gabriel.hogwartsmod.client.fx;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class BodyFx {
    private static final Field SHADOW = ObfuscationReflectionHelper.findField(EntityRenderer.class, "f_114477_");
    private static final Map<EntityRenderer<?>, Float> SHADOWS = new IdentityHashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();

    private BodyFx() {
    }

    private record Shape(float spin, float width, float height, float orbit) {
    }

    private static Shape shape(int entityId, float partialTick) {
        float departure = ClientFxState.apparateProgress(entityId, partialTick);
        if (departure >= 0) {
            float k = Mth.clamp((departure - 0.75f) / 0.25f, 0f, 1f);
            if (k <= 0f) {
                return null;
            }
            float spin = 1600f * k * k;
            float out = Mth.clamp(k / 0.3f, 0f, 1f);
            float orbit = 0.55f * out * out * (3 - 2 * out) * (1f - k) * (1f - k);
            float shrink = k * k;
            float width = (1f - 0.7f * shrink) * (1f - Mth.clamp((k - 0.75f) / 0.25f, 0f, 1f));
            float height = (1f + 0.15f * Mth.clamp(k / 0.5f, 0f, 1f)) * (1f - 0.97f * shrink);
            return new Shape(spin, Math.max(0.001f, width), Math.max(0.01f, height), orbit);
        }
        float arrival = ClientFxState.arrivalProgress(entityId, partialTick, 8f);
        if (arrival >= 0) {
            float ease = 1 - (1 - arrival) * (1 - arrival) * (1 - arrival);
            float back = 1f - ease;
            float orbit = 0.55f * back * (1f - back) * 2.2f;
            return new Shape(-1600f * back * back, 0.03f + 0.97f * ease, 0.04f + 0.96f * ease, orbit);
        }
        return null;
    }

    private static void setShadow(EntityRenderer<?> renderer, boolean hidden) {
        try {
            Float original = SHADOWS.computeIfAbsent(renderer, r -> {
                try {
                    return SHADOW.getFloat(r);
                } catch (IllegalAccessException e) {
                    return 0.5f;
                }
            });
            SHADOW.setFloat(renderer, hidden ? 0f : original);
        } catch (IllegalAccessException ignored) {
        }
    }

    @SubscribeEvent
    public static void before(RenderPlayerEvent.Pre event) {
        int id = event.getEntity().getId();
        boolean dodging = ClientFxState.isDodging(id);
        Shape shape = dodging ? null : shape(id, event.getPartialTick());
        setShadow(event.getRenderer(), dodging || shape != null && shape.width < 0.6f);
        if (dodging) {
            event.setCanceled(true);
            return;
        }
        if (shape == null) {
            return;
        }
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, 1.0, 0);
        pose.mulPose(Axis.YP.rotationDegrees(shape.spin));
        pose.translate(shape.orbit, 0, 0);
        pose.mulPose(Axis.YP.rotationDegrees(shape.spin * 0.5f));
        pose.scale(shape.width, shape.height, shape.width);
        pose.translate(0, -1.0, 0);
        PUSHED.add(id);
    }

    @SubscribeEvent
    public static void after(RenderPlayerEvent.Post event) {
        if (PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void hideHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float departure = ClientFxState.apparateProgress(player.getId(), event.getPartialTick());
        if (ClientFxState.isDodging(player.getId()) || departure > 0.75f) {
            event.setCanceled(true);
        }
    }

    private static float firstPersonCollapse(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return 0f;
        }
        float departure = ClientFxState.apparateProgress(mc.player.getId(), partialTick);
        if (departure >= 0) {
            return Mth.clamp((departure - 0.55f) / 0.45f, 0f, 1f);
        }
        float arrival = ClientFxState.arrivalProgress(mc.player.getId(), partialTick, 10f);
        return arrival >= 0 ? 1f - arrival : 0f;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        float c = firstPersonCollapse((float) event.getPartialTick());
        if (c > 0) {
            event.setRoll(event.getRoll() + 220f * c * c);
        }
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        float c = firstPersonCollapse((float) event.getPartialTick());
        if (c > 0) {
            event.setFOV(event.getFOV() * (1.0 - 0.55 * c * c));
        }
    }
}
