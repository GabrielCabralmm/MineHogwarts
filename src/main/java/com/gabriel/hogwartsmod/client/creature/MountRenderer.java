package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.creature.MountCreature;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MountRenderer<T extends MountCreature> extends GeoEntityRenderer<T> {

    public MountRenderer(EntityRendererProvider.Context context, String name, float scale, float shadow) {
        super(context, new MountModel<>(name));
        shadowRadius = shadow;
        withScale(scale);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        return entity.isVisibleToClient() && super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (entity.isVisibleToClient()) {
            super.render(entity, yaw, partialTick, poseStack, buffers, light);
        }
    }
}
