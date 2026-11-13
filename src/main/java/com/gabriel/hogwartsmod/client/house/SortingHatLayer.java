package com.gabriel.hogwartsmod.client.house;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

public class SortingHatLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public SortingHatLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientSorting.isWearing(player.getId())) {
            return;
        }
        float age = ClientSorting.age(player.getId(), partialTick);
        float drop = Math.min(1f, age / 6f);
        float settle = 1f - (1f - drop) * (1f - drop);
        float talk = Mth.sin(age * 0.55f) * Math.min(1f, age / 12f);
        pose.pushPose();
        getParentModel().getHead().translateAndRotate(pose);
        pose.translate(0, -0.33 - (1 - settle) * 0.6, 0);
        pose.scale(1.15f, 1.15f, 1.15f);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        SortingHatRenderer.draw(pose, buffers, light, talk, Mth.sin(age * 0.13f));
        pose.popPose();
    }
}
