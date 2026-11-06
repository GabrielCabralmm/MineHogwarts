package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.item.BroomItem;
import com.gabriel.hogwartsmod.item.WandItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HogwartsMod.MOD_ID);

    public static final RegistryObject<Item> WAND = ITEMS.register("wand",
            () -> new WandItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BROOM_SPORT = ITEMS.register("vassoura_esportiva",
            () -> new BroomItem(BroomType.SPORT, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BROOM_TRANSPORT = ITEMS.register("vassoura_transporte",
            () -> new BroomItem(BroomType.TRANSPORT, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ZONE_WAND = ITEMS.register("varinha_zonas",
            () -> new com.gabriel.hogwartsmod.item.ZoneWandItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SORTING_HAT = ITEMS.register("chapeu_seletor",
            () -> new com.gabriel.hogwartsmod.item.SortingHatItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
