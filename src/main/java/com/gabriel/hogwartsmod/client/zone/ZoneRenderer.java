package com.gabriel.hogwartsmod.client.zone;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.client.gui.ZoneScreen;
import com.gabriel.hogwartsmod.item.ZoneWandItem;
import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.ZoneShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ZoneRenderer {

    private ZoneRenderer() {
    }

    public static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return mc.screen instanceof ZoneScreen
                || mc.player.getMainHandItem().getItem() instanceof ZoneWandItem
                || mc.player.getOffhandItem().getItem() instanceof ZoneWandItem;
    }

    private static void line(PoseStack.Pose pose, VertexConsumer lines, double[] e, float r, float g, float b, float a) {
        float nx = (float) (e[3] - e[0]);
        float ny = (float) (e[4] - e[1]);
        float nz = (float) (e[5] - e[2]);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len;
        ny /= len;
        nz /= len;
        lines.vertex(pose.pose(), (float) e[0], (float) e[1], (float) e[2]).color(r, g, b, a).normal(pose.normal(), nx, ny, nz).endVertex();
        lines.vertex(pose.pose(), (float) e[3], (float) e[4], (float) e[5]).color(r, g, b, a).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !visible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        float pulse = 0.65f + 0.35f * (float) Math.sin((mc.level.getGameTime() + event.getPartialTick()) * 0.15);
        String selected = mc.screen instanceof ZoneScreen screen ? screen.selectedName() : null;
        for (Zone zone : ClientZones.inDimension()) {
            boolean active = zone.name().equals(selected);
            float r = active ? 1.0f : 0.45f;
            float gr = active ? 0.85f : 0.7f;
            float b = active ? 0.4f : 1.0f;
            float alpha = active ? pulse : 0.8f;
            PoseStack.Pose last = pose.last();
            for (double[] e : ZoneShape.of(zone).edges()) {
                line(last, lines, e, r, gr, b, alpha);
            }
        }
        BlockPos a = ClientZones.first;
        BlockPos b = ClientZones.second;
        if (a != null && b != null) {
            AABB box = new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                    Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
            LevelRenderer.renderLineBox(pose, lines, box.inflate(0.02), 0.95f, 0.76f, 0.25f, pulse);
        }
        if (a != null) {
            LevelRenderer.renderLineBox(pose, lines, new AABB(a).inflate(0.03), 0.4f, 1.0f, 0.5f, 1f);
        }
        if (b != null) {
            LevelRenderer.renderLineBox(pose, lines, new AABB(b).inflate(0.03), 1.0f, 0.45f, 0.35f, 1f);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
