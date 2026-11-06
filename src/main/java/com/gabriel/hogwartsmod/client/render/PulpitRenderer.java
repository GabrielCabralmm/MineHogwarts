package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import com.gabriel.hogwartsmod.client.gui.PulpitScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class PulpitRenderer implements BlockEntityRenderer<PulpitBlockEntity> {

    public PulpitRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PulpitBlockEntity pulpit, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || pulpit.getOwner() == null || !pulpit.getOwner().equals(mc.player.getUUID())) {
            return;
        }
        BlockPos pos = pulpit.getBlockPos();
        int[] live = PulpitScreen.liveExtents(pos);
        if (live == null && !pulpit.isShowArea()) {
            return;
        }
        AABB area = (live != null ? PulpitBlockEntity.area(pos, pulpit.facing(), live) : pulpit.area())
                .move(-pos.getX(), -pos.getY(), -pos.getZ()).inflate(0.004);
        float pulse = live != null ? 0.85f + 0.15f * (float) Math.sin((mc.level.getGameTime() + partialTick) * 0.25) : 0.75f;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, area, 0.95f, 0.76f, 0.25f, pulse);
        LevelRenderer.renderLineBox(pose, lines, area.deflate(0.02), 1.0f, 0.9f, 0.55f, pulse * 0.5f);
    }

    @Override
    public boolean shouldRenderOffScreen(PulpitBlockEntity pulpit) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
