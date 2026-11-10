package com.gabriel.hogwartsmod.client.potion;

import com.gabriel.hogwartsmod.potion.CauldronCarry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class CauldronItemClient implements IClientItemExtensions {
    public static final CauldronItemClient INSTANCE = new CauldronItemClient();
    private static final HumanoidModel.ArmPose CARRY = HumanoidModel.ArmPose.create("HOGWARTSMOD_CARRY", true, (model, entity, arm) -> {
        model.rightArm.xRot = -1.05f;
        model.rightArm.yRot = -0.28f;
        model.rightArm.zRot = 0f;
        model.leftArm.xRot = -1.05f;
        model.leftArm.yRot = 0.28f;
        model.leftArm.zRot = 0f;
    });

    private CauldronItemClient() {
    }

    @Override
    public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return CauldronCarry.isCarried(stack) ? CARRY : null;
    }
}
