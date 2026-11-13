package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.creature.MountCreature;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class MountModel<T extends MountCreature> extends GeoModel<T> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public MountModel(String name) {
        model = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "geo/criatura/" + name + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/entity/criatura/" + name + ".png");
        animation = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "animations/criatura/" + name + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        return animation;
    }

    @Override
    public void setCustomAnimations(T entity, long instanceId, AnimationState<T> state) {
        CoreGeoBone neck = getAnimationProcessor().getBone("pescoco");
        if (neck != null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            boolean firstPersonRider = mc.player != null && mc.player.getVehicle() == entity && mc.options.getCameraType().isFirstPerson();
            neck.setHidden(firstPersonRider);
        }
    }
}
