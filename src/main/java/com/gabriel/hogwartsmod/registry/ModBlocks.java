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

    private ModBlocks() {
    }
}
