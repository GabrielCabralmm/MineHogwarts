package com.gabriel.hogwartsmod.potion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PotionFireBlock extends Block {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 7, 16);

    public PotionFireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().getItem() instanceof CauldronItem;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        emberFx(level, pos, random, 0.5);
    }

    static void emberFx(Level level, BlockPos pos, RandomSource random, double height) {
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.25,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5, pos.getY() + height,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
        }
    }
}
