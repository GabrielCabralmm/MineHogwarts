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
                        output.accept(ModItems.SORTING_HAT.get());
                        output.accept(ModItems.COIN_POUCH.get());
                        output.accept(ModItems.GALEAO.get());
                        output.accept(ModItems.SICLE.get());
                        output.accept(ModItems.NUQUE.get());
                        output.accept(ModItems.BOLSA_CRIATURAS.get());
                        output.accept(ModItems.OVO_HIPOGRIFO.get());
                        output.accept(ModItems.OVO_TESTRALIO.get());
                        output.accept(ModItems.OVO_CORUJA.get());
                        output.accept(ModItems.ENVELOPE.get());
                        output.accept(ModItems.PACOTE.get());
                        output.accept(ModItems.LICENCA_CRIATURAS.get());
                        output.accept(ModBlocks.CAULDRON_ITEM.get());
                        output.accept(ModItems.MUCO_VERMECEGO.get());
                        output.accept(ModItems.CASCA_WIGGENTREE.get());
                        output.accept(ModItems.PO_PEDRA_DA_LUA.get());
                        ModItems.POTION_BOOKS.forEach(book -> output.accept(book.get()));
                        output.accept(ModItems.POCAO_WIGGENWELD.get());
                        output.accept(ModItems.POCAO_REVIGORANTE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
