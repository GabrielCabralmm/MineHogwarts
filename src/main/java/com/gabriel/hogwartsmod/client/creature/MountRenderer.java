package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.creature.MountCreature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
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

    @Override
    public void renderRecursively(PoseStack pose, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource buffers,
                                  VertexConsumer buffer, boolean reRender, float partialTick, int light, int overlay, float r, float g, float b, float a) {
        if (bone.getName().startsWith("brilho")) {
            light = LightTexture.FULL_BRIGHT;
        }
        super.renderRecursively(pose, animatable, bone, renderType, buffers, buffer, reRender, partialTick, light, overlay, r, g, b, a);
    }
}
