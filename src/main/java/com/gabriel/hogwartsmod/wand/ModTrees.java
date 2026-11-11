package com.gabriel.hogwartsmod.wand;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.registry.ModBlocks;
import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class ModTrees {
    public static final Map<WandWood, RegistryObject<Block>> LOGS = new EnumMap<>(WandWood.class);
    public static final Map<WandWood, RegistryObject<Block>> LEAVES = new EnumMap<>(WandWood.class);
    public static final Map<WandWood, RegistryObject<Block>> SAPLINGS = new EnumMap<>(WandWood.class);
    public static final Map<WandWood, RegistryObject<Block>> PLANKS = new EnumMap<>(WandWood.class);
    public static final Map<WandWood, RegistryObject<Item>> BRANCHES = new EnumMap<>(WandWood.class);

    private ModTrees() {
    }

    public static void init() {
        for (WandWood wood : WandWood.values()) {
            BRANCHES.put(wood, ModItems.ITEMS.register("galho_" + wood.getId(), () -> new BranchItem(wood, new Item.Properties())));
            if (wood.isVanilla()) {
                continue;
            }
            String id = wood.getId();
            RegistryObject<Block> log = ModBlocks.BLOCKS.register("tronco_" + id,
                    () -> new FlammableLog(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));
            RegistryObject<Block> leaves = ModBlocks.BLOCKS.register("folhas_" + id,
                    () -> new FlammableLeaves(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
            RegistryObject<Block> planks = ModBlocks.BLOCKS.register("tabuas_" + id,
                    () -> new FlammablePlanks(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
            RegistryObject<Block> sapling = ModBlocks.BLOCKS.register("muda_" + id,
                    () -> new SaplingBlock(new Grower(id), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
            LOGS.put(wood, log);
            LEAVES.put(wood, leaves);
            PLANKS.put(wood, planks);
            SAPLINGS.put(wood, sapling);
            ModItems.ITEMS.register("tronco_" + id, () -> new BlockItem(log.get(), new Item.Properties()));
            ModItems.ITEMS.register("folhas_" + id, () -> new BlockItem(leaves.get(), new Item.Properties()));
            ModItems.ITEMS.register("tabuas_" + id, () -> new BlockItem(planks.get(), new Item.Properties()));
            ModItems.ITEMS.register("muda_" + id, () -> new BlockItem(sapling.get(), new Item.Properties()));
        }
    }

    // -- Blocos

    private static final class Grower extends AbstractTreeGrower {
        private final ResourceKey<ConfiguredFeature<?, ?>> key;

        Grower(String id) {
            key = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "arvore_" + id));
        }

        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) {
            return key;
        }
    }

    private static final class FlammableLog extends RotatedPillarBlock {
        FlammableLog(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return true;
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 5;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 5;
        }
    }

    private static final class FlammableLeaves extends LeavesBlock {
        FlammableLeaves(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return true;
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 60;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 30;
        }
    }

    private static final class FlammablePlanks extends Block {
        FlammablePlanks(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return true;
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 20;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 5;
        }
    }
}
