package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.entity.BroomEntity;
import com.gabriel.hogwartsmod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BroomRenderer extends EntityRenderer<BroomEntity> {
    private final ItemRenderer items;

    public BroomRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
        shadowRadius = 0.3f;
    }

    @Override
    public void render(BroomEntity broom, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = new ItemStack(broom.getBroomType() == BroomType.SPORT ? ModItems.BROOM_SPORT.get() : ModItems.BROOM_TRANSPORT.get());
        pose.pushPose();
        pose.translate(0, 0.25, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.ZP.rotationDegrees(broom.getRoll(partialTick)));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(1.45f, 1.45f, 1.45f);
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, broom.level(), broom.getId());
        pose.popPose();
        super.render(broom, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BroomEntity broom) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
