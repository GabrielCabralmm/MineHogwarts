package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlock;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class HouseDoorRenderer extends GeoBlockRenderer<HouseDoorBlockEntity> {
    private static final ResourceLocation TELA = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/block/porta/grifinoria_tela.png");
    private static final int FRAMES = 48;
    private static final float TICKS_PER_FRAME = 2.5f;

    public HouseDoorRenderer(BlockEntityRendererProvider.Context context) {
        super(new Model());
    }

    @Override
    public void renderFinal(PoseStack pose, HouseDoorBlockEntity door, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource buffers,
                            VertexConsumer buffer, float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        super.renderFinal(pose, door, model, buffers, buffer, partialTick, light, overlay, red, green, blue, alpha);
        if (buffers != null && door.getBlockState().getBlock() instanceof HouseDoorBlock block && block.getHouse() == House.SONSERINA) {
            SlytherinSerpent.render(door, partialTick, pose, buffers, light);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(HouseDoorBlockEntity door) {
        return true;
    }

    @Override
    public void renderRecursively(PoseStack pose, HouseDoorBlockEntity door, GeoBone bone, RenderType type, MultiBufferSource buffers,
                                  VertexConsumer buffer, boolean reRender, float partialTick, int light, int overlay,
                                  float red, float green, float blue, float alpha) {
        if (bone.getName().equals("tela")) {
            renderCanvas(pose, door, bone, buffers, partialTick, light, overlay);
            return;
        }
        int boneLight = bone.getName().startsWith("brilho") ? LightTexture.FULL_BRIGHT : light;
        super.renderRecursively(pose, door, bone, type, buffers, buffer, reRender, partialTick, boneLight, overlay, red, green, blue, alpha);
    }

    private static void renderCanvas(PoseStack pose, HouseDoorBlockEntity door, GeoBone bone, MultiBufferSource buffers,
                                     float partialTick, int light, int overlay) {
        if (bone.isHidden() || door.getLevel() == null) {
            return;
        }
        int frame = (int) ((door.getLevel().getGameTime() + partialTick) / TICKS_PER_FRAME) % FRAMES;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TELA));
        pose.pushPose();
        RenderUtils.prepMatrixForBone(pose, bone);
        for (GeoCube cube : bone.getCubes()) {
            pose.pushPose();
            RenderUtils.translateToPivotPoint(pose, cube);
            RenderUtils.rotateMatrixAroundCube(pose, cube);
            RenderUtils.translateAwayFromPivotPoint(pose, cube);
            Matrix4f matrix = new Matrix4f(pose.last().pose());
            Matrix3f normalMatrix = pose.last().normal();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null || quad.direction() != Direction.NORTH) {
                    continue;
                }
                float u0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v0 = Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
                for (GeoVertex vertex : quad.vertices()) {
                    u0 = Math.min(u0, vertex.texU());
                    u1 = Math.max(u1, vertex.texU());
                    v0 = Math.min(v0, vertex.texV());
                    v1 = Math.max(v1, vertex.texV());
                }
                Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
                for (GeoVertex vertex : quad.vertices()) {
                    float u = (vertex.texU() - u0) / Math.max(1e-6f, u1 - u0);
                    float v = (frame + (vertex.texV() - v0) / Math.max(1e-6f, v1 - v0)) / FRAMES;
                    Vector3f p = vertex.position();
                    Vector4f pos = matrix.transform(new Vector4f(p.x(), p.y(), p.z(), 1));
                    buffer.vertex(pos.x(), pos.y(), pos.z(), 1, 1, 1, 1, u, v, overlay, light, normal.x(), normal.y(), normal.z());
                }
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static final class Model extends GeoModel<HouseDoorBlockEntity> {
        private static String name(HouseDoorBlockEntity door) {
            return door.getBlockState().getBlock() instanceof HouseDoorBlock block ? block.getHouse().getId() : "grifinoria";
        }

        private static ResourceLocation res(String path) {
            return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, path);
        }

        @Override
        public ResourceLocation getModelResource(HouseDoorBlockEntity door) {
            return res("geo/porta/" + name(door) + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(HouseDoorBlockEntity door) {
            return res("textures/block/porta/" + name(door) + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(HouseDoorBlockEntity door) {
            return res("animations/porta/" + name(door) + ".animation.json");
        }
    }
}
