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

    private record Shape(float spin, float width, float height) {
    }

    private static Shape shape(int entityId, float partialTick) {
        float departure = ClientFxState.apparateProgress(entityId, partialTick);
        if (departure >= 0) {
            float k = Mth.clamp((departure - 0.75f) / 0.25f, 0f, 1f);
            if (k <= 0f) {
                return null;
            }
            float spin = 1800f * k * k;
            float squeeze = Mth.clamp(k / 0.8f, 0f, 1f);
            float width = 1f - 0.94f * squeeze * squeeze * (3 - 2 * squeeze);
            float stretch = 1f + 0.22f * squeeze;
            float vanish = Mth.clamp((k - 0.8f) / 0.2f, 0f, 1f);
            width *= 1f - vanish;
            float height = stretch * (1f - 0.97f * vanish * vanish);
            return new Shape(spin, Math.max(0.001f, width), Math.max(0.01f, height));
        }
        float arrival = ClientFxState.arrivalProgress(entityId, partialTick, 7f);
        if (arrival >= 0) {
            float appear = Mth.clamp(arrival / 0.25f, 0f, 1f);
            float open = Mth.clamp((arrival - 0.15f) / 0.85f, 0f, 1f);
            float ease = 1 - (1 - open) * (1 - open) * (1 - open);
            float height = (0.05f + 1.17f * appear) * (1f - 0.18f * ease);
            float width = 0.04f + 0.96f * ease;
            return new Shape(-1440f * (1 - ease) * (1 - ease), width, height);
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
