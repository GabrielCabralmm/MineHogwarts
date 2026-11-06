package com.gabriel.hogwartsmod.client.npc;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.npc.HogwartsNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashMap;
import java.util.Map;

public class NpcModel<T extends HogwartsNpc> extends GeoModel<T> {
    public static final String BASE = "humano";
    private static final Map<String, Boolean> EXISTS = new HashMap<>();

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, path);
    }

    private static boolean exists(ResourceLocation location) {
        return EXISTS.computeIfAbsent(location.toString(),
                k -> Minecraft.getInstance().getResourceManager().getResource(location).isPresent());
    }

    public static void clearCache() {
        EXISTS.clear();
    }

    @Override
    public ResourceLocation getModelResource(T npc) {
        ResourceLocation own = id("geo/npc/" + npc.getModel() + ".geo.json");
        return exists(own) ? own : id("geo/npc/" + BASE + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T npc) {
        ResourceLocation own = id("textures/entity/npc/" + npc.getModel() + ".png");
        return exists(own) ? own : id("textures/entity/npc/" + BASE + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T npc) {
        ResourceLocation own = id("animations/npc/" + npc.getModel() + ".animation.json");
        return exists(own) ? own : id("animations/npc/" + BASE + ".animation.json");
    }

    @Override
    public void setCustomAnimations(T npc, long instanceId, AnimationState<T> state) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null) {
            head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
