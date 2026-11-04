package com.gabriel.hogwartsmod;

import com.gabriel.hogwartsmod.registry.ModCreativeTabs;
import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HogwartsMod.MOD_ID)
public class HogwartsMod {
    public static final String MOD_ID = "hogwartsmod";

    public HogwartsMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }
}
