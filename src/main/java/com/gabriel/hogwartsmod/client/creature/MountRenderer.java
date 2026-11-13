package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.creature.MountCreature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
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
        String name = bone.getName();
        boolean flying = animatable.isWingsOpen();
        if (name.startsWith("asa_fechada")) {
            bone.setHidden(flying);
        } else if (name.equals("asa_esq") || name.equals("asa_dir")) {
            bone.setHidden(!flying);
        }
        if (name.startsWith("brilho")) {
            light = LightTexture.FULL_BRIGHT;
        }
        float rx = 0, ry = 0, rz = 0;
        if (name.equals("raiz") && animatable.isVehicle()) {
            rz = animatable.getRoll(partialTick) * 0.6f;
        } else if (name.equals("cabeca") && !animatable.isVehicle()) {
            float yaw = Mth.wrapDegrees(Mth.rotLerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot)
                    - Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot));
            ry = -Mth.clamp(yaw, -35f, 35f) * 0.6f;
            rx = -Mth.clamp(animatable.getViewXRot(partialTick), -25f, 25f) * 0.5f;
        }
        if (rx == 0 && ry == 0 && rz == 0) {
            super.renderRecursively(pose, animatable, bone, renderType, buffers, buffer, reRender, partialTick, light, overlay, r, g, b, a);
            return;
        }
        pose.pushPose();
        pose.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
        pose.mulPose(Axis.ZP.rotationDegrees(rz));
        pose.mulPose(Axis.YP.rotationDegrees(ry));
        pose.mulPose(Axis.XP.rotationDegrees(rx));
        pose.translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f);
        super.renderRecursively(pose, animatable, bone, renderType, buffers, buffer, reRender, partialTick, light, overlay, r, g, b, a);
        pose.popPose();
    }
}
