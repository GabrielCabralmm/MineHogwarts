package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.creature.HippogriffEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class HippogriffModel extends GeoModel<HippogriffEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "geo/criatura/hipogrifo.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/entity/criatura/hipogrifo.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "animations/criatura/hipogrifo.animation.json");

    @Override
    public ResourceLocation getModelResource(HippogriffEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HippogriffEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HippogriffEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(HippogriffEntity entity, long instanceId, AnimationState<HippogriffEntity> state) {
        CoreGeoBone head = getAnimationProcessor().getBone("cabeca");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null && !entity.isVehicle()) {
            head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * 0.6f);
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -50f, 50f) * Mth.DEG_TO_RAD * 0.6f);
        }
        CoreGeoBone body = getAnimationProcessor().getBone("raiz");
        if (body != null && entity.isVehicle()) {
            body.setRotZ(body.getRotZ() + entity.getRoll(state.getPartialTick()) * Mth.DEG_TO_RAD * 0.6f);
        }
    }
}
