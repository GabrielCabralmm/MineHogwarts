package com.gabriel.hogwartsmod.client.house;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.model.DefaultedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SortingHatRenderer extends GeoObjectRenderer<SortingHatRenderer.Hat> {
    private static final Hat HAT = new Hat();
    private static SortingHatRenderer instance;
    private float talk;
    private float sway;
    private boolean item;

    private SortingHatRenderer() {
        super(new DefaultedGeoModel<>(ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "chapeu_seletor")) {
            @Override
            protected String subtype() {
                return "entity";
            }

            @Override
            public ResourceLocation getModelResource(Hat animatable) {
                return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "geo/chapeu_seletor.geo.json");
            }

            @Override
            public ResourceLocation getAnimationResource(Hat animatable) {
                return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "animations/chapeu_seletor.animation.json");
            }
        });
    }

    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, float talk, float sway) {
        draw(pose, buffers, light, talk, sway, false);
    }

    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, float talk, float sway, boolean item) {
        if (instance == null) {
            instance = new SortingHatRenderer();
        }
        instance.item = item;
        instance.talk = talk;
        instance.sway = sway;
        instance.render(pose, HAT, buffers, null, null, light);
    }

    @Override
    public void preRender(PoseStack pose, Hat animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource buffers,
                          VertexConsumer buffer, boolean reRender, float partialTick, int light, int overlay, float r, float g, float b, float a) {
        super.preRender(pose, animatable, model, buffers, buffer, reRender, partialTick, light, overlay, r, g, b, a);
        pose.translate(-0.5f, -0.51f, -0.5f);
    }

    @Override
    public void renderRecursively(PoseStack pose, Hat animatable, GeoBone bone, RenderType renderType, MultiBufferSource buffers,
                                  VertexConsumer buffer, boolean reRender, float partialTick, int light, int overlay, float r, float g, float b, float a) {
        float open = Math.max(0, talk);
        switch (bone.getName()) {
            case "boca" -> bone.setRotX(open * 0.55f);
            case "sobrancelha" -> bone.setPosY(open * 0.4f);
            case "topo" -> bone.setRotZ(sway * 0.06f);
            case "ponta" -> bone.setRotZ(-0.14f + sway * 0.18f);
            case "testa" -> bone.setScaleY(1f + open * 0.06f);
            case "faixa_esq", "faixa_dir" -> {
                bone.setRotZ(-sway * 0.03f);
                bone.setHidden(item);
            }
            default -> {
            }
        }
        super.renderRecursively(pose, animatable, bone, renderType, buffers, buffer, reRender, partialTick, light, overlay, r, g, b, a);
    }

    public static final class Hat implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return 0;
        }
    }
}
