package com.gabriel.hogwartsmod.client.fx;

import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity> {

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpellProjectileEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        Spell spell = entity.getSpell();
        Vec3 velocity = entity.getDeltaMovement();
        if (spell == null || velocity.lengthSqr() < 1.0E-4) {
            return;
        }
        boolean thin = spell == Spells.EXPELLIARMUS;
        float length = thin ? 3.2f : 2.2f;
        float scale = thin ? 0.7f : 1.0f;
        Vector3f color = spell.getColorVector();
        Vec3 dir = velocity.normalize();
        Vec3 cam = entityRenderDispatcher.camera.getPosition();
        Vec3 worldPos = entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2, 0);
        Vec3 view = worldPos.subtract(cam).normalize();
        Vec3 side = dir.cross(view);
        if (side.lengthSqr() < 1.0E-6) {
            side = dir.cross(new Vec3(0, 1, 0));
        }
        side = side.normalize();
        Vec3 head = new Vec3(0, entity.getBbHeight() / 2, 0);
        Vec3 tail = head.subtract(dir.scale(length));

        Matrix4f m = poseStack.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float r = color.x();
        float g = color.y();
        float b = color.z();
        GlowDraw.ribbon(vc, m, head, tail, side, 0.42f * scale, r, g, b, 0.16f, 0f);
        GlowDraw.ribbon(vc, m, head, tail, side, 0.24f * scale, r, g, b, 0.35f, 0f);
        GlowDraw.ribbon(vc, m, head, tail, side, 0.11f * scale, (r + 1) / 2, (g + 1) / 2, (b + 1) / 2, 0.8f, 0.05f);
        GlowDraw.ribbon(vc, m, head, tail.add(dir.scale(length * 0.35)), side, 0.045f * scale, 1f, 1f, 1f, 1f, 0.1f);

        Quaternionf rot = entityRenderDispatcher.cameraOrientation();
        Vector3f left = rot.transform(new Vector3f(1, 0, 0));
        Vector3f up = rot.transform(new Vector3f(0, 1, 0));
        Vec3 l = new Vec3(left.x(), left.y(), left.z());
        Vec3 u = new Vec3(up.x(), up.y(), up.z());
        GlowDraw.disc(vc, m, head, l, u, 0.7f * scale, r, g, b, 0.45f);
        GlowDraw.disc(vc, m, head, l, u, 0.28f * scale, 1f, 1f, 1f, 0.9f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectileEntity entity) {
        return TextureAtlas.LOCATION_PARTICLES;
    }
}
