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

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, com.mojang.blaze3d.vertex.PoseStack poseStack, OwlEntity animatable,
                                    software.bernie.geckolib.cache.object.BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float s = animatable.isBaby() ? 0.5f : 1f;
        super.scaleModelForRender(widthScale * s, heightScale * s, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    private static final class Model extends GeoModel<OwlEntity> {
        private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "geo/criatura/coruja.geo.json");
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[OwlEntity.COLORS.length];

        static {
            for (int i = 0; i < TEXTURES.length; i++) {
                TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID,
                        i == 0 ? "textures/entity/criatura/coruja.png" : "textures/entity/criatura/coruja_" + OwlEntity.COLORS[i] + ".png");
            }
        }
        private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "animations/criatura/coruja.animation.json");

        @Override
        public ResourceLocation getModelResource(OwlEntity entity) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(OwlEntity entity) {
            return TEXTURES[Math.floorMod(entity.getColor(), TEXTURES.length)];
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
