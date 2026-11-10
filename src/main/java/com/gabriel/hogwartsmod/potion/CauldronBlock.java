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
    private static final VoxelShape SHAPE_FIRE = box(1.5, 0, 1.5, 14.5, 10, 14.5);
    private static final VoxelShape SHAPE_FLOOR = box(1.5, 0, 1.5, 14.5, 10.7, 14.5);

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
        boolean fire = isCampfire(context.getLevel().getBlockState(context.getClickedPos().below()));
        net.minecraft.nbt.CompoundTag data = net.minecraft.world.item.BlockItem.getBlockEntityData(context.getItemInHand());
        boolean filled = data != null && !data.getString("Estado").isEmpty() && !data.getString("Estado").equals("EMPTY");
        return defaultBlockState().setValue(FIRE, fire).setValue(FILLED, filled);
    }

    public static boolean isCampfire(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.CAMPFIRES);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == net.minecraft.core.Direction.DOWN) {
            return state.setValue(FIRE, isCampfire(neighbor));
        }
        return state;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
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
