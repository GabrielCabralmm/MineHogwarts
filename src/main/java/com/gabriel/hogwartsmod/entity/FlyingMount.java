package com.gabriel.hogwartsmod.entity;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface FlyingMount {
    float getStamina();

    float getMaxStamina();

    boolean isBoosting();

    boolean isBoostPose();

    void setBoostSynced(boolean value);

    float getRoll(float partialTick);

    @Nullable
    LivingEntity getControllingPassenger();

    default boolean showsTurnKeys() {
        return true;
    }

    default String dismountKey() {
        return "hud.hogwartsmod.broom_dismount";
    }

    default String upKey() {
        return "hud.hogwartsmod.broom_up";
    }
}
