package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.entity.ProfessorNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ProfessorNpcRenderer extends HumanoidMobRenderer<ProfessorNpcEntity, PlayerModel<ProfessorNpcEntity>> {
    private static final ResourceLocation SKIN =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/entity/professor_npc.png");

    public ProfessorNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ProfessorNpcEntity entity) {
        return SKIN;
    }
}
