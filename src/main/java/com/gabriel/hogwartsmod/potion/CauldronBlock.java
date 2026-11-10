package com.gabriel.hogwartsmod.potion;

import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CauldronBlock extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("cheio");
    public static final BooleanProperty FIRE = BooleanProperty.create("fogueira");
    private static final VoxelShape SHAPE_FIRE = Shapes.or(box(0, 0, 0, 16, 4, 16), box(2, 7, 2, 14, 16, 14), box(3, 4, 3, 13, 7, 13));
    private static final VoxelShape SHAPE_FLOOR = box(2, 0, 2, 14, 10, 14);

    public CauldronBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILLED, false).setValue(FIRE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED, FIRE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FIRE) ? SHAPE_FIRE : SHAPE_FLOOR;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState replaced = context.getLevel().getBlockState(context.getClickedPos());
        boolean fire = replaced.getBlock() instanceof PotionFireBlock || CauldronItem.comesWithFire(context.getItemInHand());
        net.minecraft.nbt.CompoundTag data = net.minecraft.world.item.BlockItem.getBlockEntityData(context.getItemInHand());
        boolean filled = data != null && !data.getString("Estado").isEmpty() && !data.getString("Estado").equals("EMPTY");
        return defaultBlockState().setValue(FIRE, fire).setValue(FILLED, filled);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(FIRE) && random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, net.minecraft.sounds.SoundEvents.CAMPFIRE_CRACKLE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.4f + random.nextFloat() * 0.5f, random.nextFloat() * 0.7f + 0.6f, false);
        }
        if (state.getValue(FIRE) && random.nextInt(4) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.3,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0, 0.005, 0);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof CauldronBlockEntity cauldron)) {
            return InteractionResult.PASS;
        }
        return cauldron.interact(player, player.getItemInHand(hand));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CAULDRON.get()) {
            return null;
        }
        return level.isClientSide
                ? (l, p, s, be) -> ((CauldronBlockEntity) be).clientTick()
                : (l, p, s, be) -> ((CauldronBlockEntity) be).serverTick();
    }
}
