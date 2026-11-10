package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.creature.HippogriffEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HippogriffRenderer extends GeoEntityRenderer<HippogriffEntity> {

    public HippogriffRenderer(EntityRendererProvider.Context context) {
        super(context, new HippogriffModel());
        shadowRadius = 0.8f;
    }
}
