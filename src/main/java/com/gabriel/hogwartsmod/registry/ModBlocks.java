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

    private ModBlocks() {
    }
}
