package com.gabriel.hogwartsmod;

import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import com.gabriel.hogwartsmod.registry.ModBlocks;
import com.gabriel.hogwartsmod.registry.ModCreativeTabs;
import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HogwartsMod.MOD_ID)
public class HogwartsMod {
    public static final String MOD_ID = "hogwartsmod";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public HogwartsMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        com.gabriel.hogwartsmod.registry.ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        com.gabriel.hogwartsmod.registry.ModLootModifiers.SERIALIZERS.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, MagicConfig.SPEC);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
