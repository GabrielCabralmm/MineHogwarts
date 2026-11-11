package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.mail.PackageMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, HogwartsMod.MOD_ID);

    public static final RegistryObject<MenuType<PackageMenu>> PACOTE = MENUS.register("pacote",
            () -> IForgeMenuType.create(PackageMenu::new));

    public static final RegistryObject<MenuType<com.gabriel.hogwartsmod.wand.WandBenchMenu>> BANCADA_VARINHAS = MENUS.register("bancada_varinhas",
            () -> IForgeMenuType.create(com.gabriel.hogwartsmod.wand.WandBenchMenu::new));

    public static final RegistryObject<MenuType<com.gabriel.hogwartsmod.broom.BroomBenchMenu>> BANCADA_VASSOURAS = MENUS.register("bancada_vassouras",
            () -> IForgeMenuType.create(com.gabriel.hogwartsmod.broom.BroomBenchMenu::new));

    private ModMenus() {
    }
}
