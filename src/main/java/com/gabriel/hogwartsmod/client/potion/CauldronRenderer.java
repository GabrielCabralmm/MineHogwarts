package com.gabriel.hogwartsmod.client.potion;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.potion.CauldronBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public class CauldronRenderer implements BlockEntityRenderer<CauldronBlockEntity> {
    public static final ResourceLocation SPOON = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "block/colher_caldeirao");

    public CauldronRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CauldronBlockEntity cauldron, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(SPOON);
        pose.pushPose();
        pose.translate(0.5, cauldron.liquidY() - 0.2, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-cauldron.spoonAngle(partialTick)));
        pose.translate(0.2, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-22f));
        pose.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                cauldron.getBlockState(), model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }
}
