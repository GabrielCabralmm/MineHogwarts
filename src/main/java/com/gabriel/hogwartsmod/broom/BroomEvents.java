package com.gabriel.hogwartsmod.broom;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.entity.BroomEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class BroomEvents {
    private BroomEvents() {
    }

    @SubscribeEvent
    public static void keepRiding(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof com.gabriel.hogwartsmod.entity.FlyingMount && event.getEntityBeingMounted().isAlive()
                && event.getEntityMounting() instanceof Player player && player.isAlive() && player.isShiftKeyDown()) {
            event.setCanceled(true);
        }
    }
}
