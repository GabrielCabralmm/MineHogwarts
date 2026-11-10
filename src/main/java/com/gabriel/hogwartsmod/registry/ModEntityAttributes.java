package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.entity.ProfessorNpcEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {

    @SubscribeEvent
    public static void register(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PROFESSOR_NPC.get(), ProfessorNpcEntity.createAttributes().build());
        event.put(ModEntities.MISSION_NPC.get(), com.gabriel.hogwartsmod.npc.HogwartsNpc.createAttributes().build());
        event.put(ModEntities.SHOP_NPC.get(), com.gabriel.hogwartsmod.npc.HogwartsNpc.createAttributes().build());
        event.put(ModEntities.HIPPOGRIFF.get(), com.gabriel.hogwartsmod.creature.HippogriffEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.HIPPOGRIFF.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                        && level.getRawBrightness(pos, 0) > 8,
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private ModEntityAttributes() {
    }
}
