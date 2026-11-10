package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.creature.MountCreature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

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
        CoreGeoBone head = getAnimationProcessor().getBone("cabeca");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null && !entity.isVehicle()) {
            head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * 0.6f);
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -50f, 50f) * Mth.DEG_TO_RAD * 0.6f);
        }
        CoreGeoBone neck = getAnimationProcessor().getBone("pescoco");
        if (neck != null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            boolean firstPersonRider = mc.player != null && mc.player.getVehicle() == entity && mc.options.getCameraType().isFirstPerson();
            neck.setHidden(firstPersonRider);
        }
        CoreGeoBone body = getAnimationProcessor().getBone("raiz");
        if (body != null && entity.isVehicle()) {
            body.setRotZ(body.getRotZ() + entity.getRoll(state.getPartialTick()) * Mth.DEG_TO_RAD * 0.6f);
        }
    }
}
