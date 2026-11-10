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

    public static final RegistryObject<Item> GALEAO = ITEMS.register("galeao", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SICLE = ITEMS.register("sicle", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NUQUE = ITEMS.register("nuque", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COIN_POUCH = ITEMS.register("bolsa_moedas",
            () -> new com.gabriel.hogwartsmod.money.CoinPouchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MUCO_VERMECEGO = ITEMS.register("muco_vermecego", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CASCA_WIGGENTREE = ITEMS.register("casca_wiggentree", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PO_PEDRA_DA_LUA = ITEMS.register("po_pedra_da_lua", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> POCAO_WIGGENWELD = ITEMS.register("pocao_wiggenweld",
            () -> new com.gabriel.hogwartsmod.potion.BrewItem(com.gabriel.hogwartsmod.potion.Brew.WIGGENWELD, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> POCAO_REVIGORANTE = ITEMS.register("pocao_revigorante",
            () -> new com.gabriel.hogwartsmod.potion.BrewItem(com.gabriel.hogwartsmod.potion.Brew.REVIGORANTE, new Item.Properties().stacksTo(16)));

    public static final java.util.List<RegistryObject<Item>> POTION_BOOKS = java.util.stream.IntStream.rangeClosed(1, 5)
            .mapToObj(n -> ITEMS.register("livro_pocoes_" + n,
                    () -> (Item) new com.gabriel.hogwartsmod.potion.PotionBookItem(n, new Item.Properties().stacksTo(1))))
            .toList();

    public static final RegistryObject<Item> BOLSA_CRIATURAS = ITEMS.register("bolsa_criaturas",
            () -> new com.gabriel.hogwartsmod.creature.CreatureBagItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> OVO_HIPOGRIFO = ITEMS.register("ovo_hipogrifo",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.HIPPOGRIFF, 0x8C8E94, 0xB7895A, new Item.Properties()));

    public static Item potion(com.gabriel.hogwartsmod.potion.Brew brew) {
        return switch (brew) {
            case WIGGENWELD -> POCAO_WIGGENWELD.get();
            case REVIGORANTE -> POCAO_REVIGORANTE.get();
        };
    }

    private ModItems() {
    }
}
