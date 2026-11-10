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

    public static final RegistryObject<Block> POTION_FIRE = BLOCKS.register("fogueira_pocoes",
            () -> new com.gabriel.hogwartsmod.potion.PotionFireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL)
                    .strength(1.5f).sound(SoundType.WOOD).noOcclusion().lightLevel(state -> 13)));

    public static final RegistryObject<Item> POTION_FIRE_ITEM = ModItems.ITEMS.register("fogueira_pocoes",
            () -> new BlockItem(POTION_FIRE.get(), new Item.Properties()));

    private ModBlocks() {
    }
}
