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
import net.minecraft.core.Direction;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.world.phys.AABB;

public class PulpitRenderer implements BlockEntityRenderer<PulpitBlockEntity> {

    private final BookModel book;

    public PulpitRenderer(BlockEntityRendererProvider.Context context) {
        book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(PulpitBlockEntity pulpit, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderBook(pulpit, partialTick, pose, buffers, light, overlay);
        renderArea(pulpit, partialTick, pose, buffers);
    }

    private void renderBook(PulpitBlockEntity pulpit, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float open = pulpit.getOpen(partialTick);
        float ease = open * open * (3 - 2 * open);
        float time = pulpit.getTime(partialTick);
        Direction facing = pulpit.facing();
        float yaw = (float) Math.atan2(facing.getStepZ(), facing.getStepX());
        float bob = (1 - ease) * (0.06f + (float) Math.sin(time * 0.1f) * 0.04f);
        pose.pushPose();
        pose.translate(0.5, Mth.lerp(ease, 1.28f, 0.93f) + bob, 0.5);
        pose.mulPose(Axis.YP.rotation(-yaw));
        pose.translate(Mth.lerp(ease, 0f, 0.02f), 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(ease, 80f, 67.5f)));
        float turn = Mth.clamp(((time % 240f) - 200f) / 30f, 0f, 1f);
        float page = turn * turn * (3 - 2 * turn);
        book.setupAnim(time, page, 1f, ease * 1.22f);
        VertexConsumer consumer = EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entitySolid);
        book.render(pose, consumer, light, overlay, 1f, 1f, 1f, 1f);
        pose.popPose();
    }

    private void renderArea(PulpitBlockEntity pulpit, float partialTick, PoseStack pose, MultiBufferSource buffers) {
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
