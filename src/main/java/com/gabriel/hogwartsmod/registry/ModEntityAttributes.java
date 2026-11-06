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
    }

    private ModEntityAttributes() {
    }
}
