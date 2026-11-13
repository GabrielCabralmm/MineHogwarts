package com.gabriel.hogwartsmod.client.house;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class SortingHatItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static SortingHatItemRenderer instance;

    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (instance == null) {
                Minecraft mc = Minecraft.getInstance();
                instance = new SortingHatItemRenderer(mc);
            }
            return instance;
        }
    };

    private SortingHatItemRenderer(Minecraft mc) {
        super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        SortingHatRenderer.draw(pose, buffers, light, 0, 0, true);
        pose.popPose();
    }
}
