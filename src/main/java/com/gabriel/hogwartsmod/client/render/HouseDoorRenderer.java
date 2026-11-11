package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlock;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class HouseDoorRenderer implements BlockEntityRenderer<HouseDoorBlockEntity> {
    public static final ResourceLocation GRIFINORIA = model("grifinoria");
    public static final ResourceLocation SONSERINA_LEFT = model("sonserina_esq");
    public static final ResourceLocation SONSERINA_RIGHT = model("sonserina_dir");
    public static final ResourceLocation CORVINAL = model("corvinal");
    public static final ResourceLocation LUFA_LUFA = model("lufa_lufa");
    public static final ResourceLocation[] ALL = {GRIFINORIA, SONSERINA_LEFT, SONSERINA_RIGHT, CORVINAL, LUFA_LUFA};

    public HouseDoorRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "block/porta/" + name);
    }

    @Override
    public void render(HouseDoorBlockEntity door, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = door.getBlockState();
        if (!(state.getBlock() instanceof HouseDoorBlock block)) {
            return;
        }
        Direction facing = state.getValue(HouseDoorBlock.FACING);
        float open = door.openness(partialTick);
        House house = block.getHouse();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        switch (house) {
            case GRIFINORIA -> swing(pose, buffers, state, light, overlay, GRIFINORIA, open, 0);
            case CORVINAL -> swing(pose, buffers, state, light, overlay, CORVINAL, open, 0.1875f);
            case SONSERINA -> {
                pose.pushPose();
                pose.translate(-0.48 * open, 0, 0);
                draw(pose, buffers, state, light, overlay, SONSERINA_LEFT);
                pose.popPose();
                pose.pushPose();
                pose.translate(0.48 * open, 0, 0);
                draw(pose, buffers, state, light, overlay, SONSERINA_RIGHT);
                pose.popPose();
            }
            case LUFA_LUFA -> {
                pose.translate(0, 2, 0);
                pose.mulPose(Axis.XP.rotationDegrees(-95f * open));
                pose.translate(0, -2, 0);
                draw(pose, buffers, state, light, overlay, LUFA_LUFA);
            }
        }
        pose.popPose();
    }

    private static void swing(PoseStack pose, MultiBufferSource buffers, BlockState state, int light, int overlay, ResourceLocation model, float open, float depth) {
        pose.mulPose(Axis.YP.rotationDegrees(-100f * open));
        draw(pose, buffers, state, light, overlay, model);
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, BlockState state, int light, int overlay, ResourceLocation id) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    @Override
    public boolean shouldRenderOffScreen(HouseDoorBlockEntity door) {
        return true;
    }
}
