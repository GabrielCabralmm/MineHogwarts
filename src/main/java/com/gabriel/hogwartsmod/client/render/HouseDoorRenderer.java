package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlock;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HouseDoorRenderer extends GeoBlockRenderer<HouseDoorBlockEntity> {
    public HouseDoorRenderer(BlockEntityRendererProvider.Context context) {
        super(new Model());
    }

    @Override
    public boolean shouldRenderOffScreen(HouseDoorBlockEntity door) {
        return true;
    }

    @Override
    public void renderRecursively(PoseStack pose, HouseDoorBlockEntity door, GeoBone bone, RenderType type, MultiBufferSource buffers,
                                  VertexConsumer buffer, boolean reRender, float partialTick, int light, int overlay,
                                  float red, float green, float blue, float alpha) {
        int boneLight = bone.getName().startsWith("brilho") ? LightTexture.FULL_BRIGHT : light;
        super.renderRecursively(pose, door, bone, type, buffers, buffer, reRender, partialTick, boneLight, overlay, red, green, blue, alpha);
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
