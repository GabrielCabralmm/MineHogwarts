package com.gabriel.hogwartsmod.client.fx;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Map;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ShieldRenderer {
    private static final int SEGMENTS = 32;
    private static final int RINGS = 8;
    private static final double SPHERE_RADIUS = 1.5;
    private static final double MAX_ANGLE = Math.toRadians(42);
    private static final float R = 0.62f;
    private static final float G = 0.8f;
    private static final float B = 1.0f;

    private ShieldRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;
        for (Map.Entry<Integer, ClientFxState.State> entry : ClientFxState.states().entrySet()) {
            ClientFxState.State state = entry.getValue();
            if (!state.shield) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Player player) || !player.isAlive()) {
                continue;
            }
            float time = level.getGameTime() + partial;
            drawShield(event.getPoseStack(), buffers.getBuffer(RenderType.lightning()), player, state, cam, partial, time);
            drew = true;
        }
        if (drew) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static void drawShield(PoseStack poseStack, VertexConsumer vc, Player player, ClientFxState.State state,
                                   Vec3 cam, float partial, float time) {
        Vec3 look = player.getViewVector(partial);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 front = player.getEyePosition(partial).add(look.scale(1.15)).add(0, -0.15, 0);
        Vec3 sphereCenter = front.subtract(look.scale(SPHERE_RADIUS)).subtract(cam);

        float grow = Mth.clamp((time - state.shieldStart) / 4f, 0f, 1f);
        float hitAge = time - state.lastHit;
        float hitWave = hitAge >= 0 && hitAge < 10 ? hitAge / 10f : -1f;
        float hitStrength = state.lastHitPerfect ? 1.4f : 0.9f;

        Matrix4f m = poseStack.last().pose();
        Vec3[][] grid = new Vec3[RINGS + 1][SEGMENTS + 1];
        float[][][] colors = new float[RINGS + 1][SEGMENTS + 1][];
        for (int i = 0; i <= RINGS; i++) {
            float t = i / (float) RINGS;
            double theta = t * MAX_ANGLE * grow;
            for (int j = 0; j <= SEGMENTS; j++) {
                double phi = j * Math.PI * 2 / SEGMENTS;
                Vec3 radial = right.scale(Math.cos(phi)).add(up.scale(Math.sin(phi)));
                grid[i][j] = sphereCenter.add(look.scale(Math.cos(theta) * SPHERE_RADIUS)).add(radial.scale(Math.sin(theta) * SPHERE_RADIUS));
                float fresnel = 0.06f + 0.42f * t * t * t;
                float shimmer = 0.05f * Mth.sin(time * 0.35f + (float) phi * 3f + t * 7f)
                        + 0.03f * Mth.sin(time * 0.6f - (float) phi * 5f);
                float wave = 0f;
                if (hitWave >= 0) {
                    float d = (t - hitWave) / 0.12f;
                    wave = (float) Math.exp(-d * d) * (1f - hitWave) * hitStrength;
                }
                float edge = i == RINGS ? 0.35f : 0f;
                float alpha = Mth.clamp((fresnel + shimmer + wave + edge) * grow, 0f, 1f);
                float white = Mth.clamp(wave * 0.8f + edge, 0f, 1f);
                colors[i][j] = new float[]{Mth.lerp(white, R, 1f), Mth.lerp(white, G, 1f), Mth.lerp(white, B, 1f), alpha};
            }
        }
        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < SEGMENTS; j++) {
                GlowDraw.quad(vc, m, grid[i][j], grid[i][j + 1], grid[i + 1][j + 1], grid[i + 1][j],
                        colors[i][j], colors[i][j + 1], colors[i + 1][j + 1], colors[i + 1][j]);
            }
        }
    }
}
