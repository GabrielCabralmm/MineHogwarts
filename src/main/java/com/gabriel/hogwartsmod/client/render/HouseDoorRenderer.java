package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlock;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
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
