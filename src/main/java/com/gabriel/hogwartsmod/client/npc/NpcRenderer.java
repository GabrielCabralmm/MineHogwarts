package com.gabriel.hogwartsmod.client.npc;

import com.gabriel.hogwartsmod.npc.HogwartsNpc;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class NpcRenderer<T extends HogwartsNpc> extends GeoEntityRenderer<T> {

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new NpcModel<>());
        shadowRadius = 0.45f;
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, T npc) {
                return "item_mao".equals(bone.getName()) ? npc.getMainHandItem() : null;
            }

            @Override
            protected void renderStackForBone(com.mojang.blaze3d.vertex.PoseStack pose, GeoBone bone, ItemStack stack, T npc,
                                              net.minecraft.client.renderer.MultiBufferSource buffers, float partialTick, int light, int overlay) {
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90f));
                super.renderStackForBone(pose, bone, stack, npc, buffers, partialTick, light, overlay);
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T npc) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
        });
    }
}
