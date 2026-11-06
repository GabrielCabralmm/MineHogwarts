package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HogwartsMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hogwartsmod"))
                    .icon(() -> new ItemStack(ModItems.WAND.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WAND.get());
                        output.accept(ModBlocks.PULPIT_ITEM.get());
                        output.accept(ModItems.BROOM_SPORT.get());
                        output.accept(ModItems.BROOM_TRANSPORT.get());
                        output.accept(ModItems.ZONE_WAND.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
