package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.creature.OwlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlRenderer extends GeoEntityRenderer<OwlEntity> {

    public OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0.25f;
    }

    private static final class Model extends GeoModel<OwlEntity> {
        private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "geo/criatura/coruja.geo.json");
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/entity/criatura/coruja.png");
        private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "animations/criatura/coruja.animation.json");

        @Override
        public ResourceLocation getModelResource(OwlEntity entity) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(OwlEntity entity) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(OwlEntity entity) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(OwlEntity entity, long instanceId, AnimationState<OwlEntity> state) {
            CoreGeoBone head = getAnimationProcessor().getBone("cabeca");
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (head != null && data != null) {
                head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * 0.7f);
                head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -90f, 90f) * Mth.DEG_TO_RAD);
            }
        }
    }
}
