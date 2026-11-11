package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.block.PulpitBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HogwartsMod.MOD_ID);

    public static final RegistryObject<Block> PULPIT = BLOCKS.register("pulpito_professor",
            () -> new PulpitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(2.5f).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Item> PULPIT_ITEM = ModItems.ITEMS.register("pulpito_professor",
            () -> new BlockItem(PULPIT.get(), new Item.Properties()));

    public static final RegistryObject<Block> CAULDRON = BLOCKS.register("caldeirao",
            () -> new com.gabriel.hogwartsmod.potion.CauldronBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops().strength(2.0f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(com.gabriel.hogwartsmod.potion.CauldronBlock.FIRE) ? 12 : 0)));

    public static final RegistryObject<Item> CAULDRON_ITEM = ModItems.ITEMS.register("caldeirao",
            () -> new com.gabriel.hogwartsmod.potion.CauldronItem(CAULDRON.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> BANCADA_VARINHAS = BLOCKS.register("bancada_varinhas",
            () -> new com.gabriel.hogwartsmod.wand.WandBenchBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE).noOcclusion()));

    public static final RegistryObject<Item> BANCADA_VARINHAS_ITEM = ModItems.ITEMS.register("bancada_varinhas",
            () -> new net.minecraft.world.item.BlockItem(BANCADA_VARINHAS.get(), new Item.Properties()));

    public static final RegistryObject<Block> BANCADA_VASSOURAS = BLOCKS.register("bancada_vassouras",
            () -> new com.gabriel.hogwartsmod.broom.BroomBenchBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE).noOcclusion()));

    public static final RegistryObject<Item> BANCADA_VASSOURAS_ITEM = ModItems.ITEMS.register("bancada_vassouras",
            () -> new net.minecraft.world.item.BlockItem(BANCADA_VASSOURAS.get(), new Item.Properties()));

    public static final java.util.Map<com.gabriel.hogwartsmod.house.House, RegistryObject<Block>> HOUSE_DOORS = new java.util.EnumMap<>(com.gabriel.hogwartsmod.house.House.class);

    static {
        for (com.gabriel.hogwartsmod.house.House house : com.gabriel.hogwartsmod.house.House.values()) {
            RegistryObject<Block> door = BLOCKS.register("porta_" + house.getId(), () -> new com.gabriel.hogwartsmod.house.door.HouseDoorBlock(house,
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.STONE)
                            .strength(3f, 3600000f).noOcclusion().noLootTable()));
            HOUSE_DOORS.put(house, door);
            ModItems.ITEMS.register("porta_" + house.getId(), () -> new net.minecraft.world.item.DoubleHighBlockItem(door.get(), new Item.Properties()));
        }
    }

    private ModBlocks() {
    }
}
