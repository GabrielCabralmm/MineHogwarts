package com.gabriel.hogwartsmod.house.door;

import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HouseDoorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("parte", 0, 24);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty PASS = BooleanProperty.create("passagem");
    public static final int OPEN_TICKS = 100;

    private final House house;
    private final int width;
    private final int height;

    public HouseDoorBlock(House house, Properties properties) {
        super(properties.lightLevel(state -> light(house, state.getValue(PART))));
        this.house = house;
        this.width = house == House.SONSERINA ? 5 : 3;
        this.height = house == House.SONSERINA ? 4 : 3;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, width / 2).setValue(OPEN, false).setValue(PASS, false));
    }

    private static int light(House house, int part) {
        return house == House.SONSERINA && part / 5 >= 2 && (part % 5 == 0 || part % 5 == 4) ? 12 : 0;
    }

    public int master() {
        return width / 2;
    }

    public int parts() {
        return width * height;
    }

    public House getHouse() {
        return house;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN, PASS);
    }

    // -- Estrutura

    private BlockPos partPos(BlockPos master, Direction facing, int part) {
        return master.relative(facing.getClockWise(), part % width - width / 2).above(part / width);
    }

    public BlockPos master(BlockState state, BlockPos pos) {
        int part = state.getValue(PART);
        return pos.relative(state.getValue(FACING).getClockWise(), width / 2 - part % width).below(part / width);
    }

    private boolean passage(int part) {
        int col = part % width - width / 2;
        int row = part / width;
        return house == House.SONSERINA ? row < 2 && Math.abs(col) <= 1 : row < 2 && col == 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (pos.getY() + height - 1 >= level.getMaxBuildHeight()) {
            return null;
        }
        for (int part = 0; part < parts(); part++) {
            if (!level.getBlockState(partPos(pos, facing, part)).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        for (int part = 0; part < parts(); part++) {
            if (part != master()) {
                level.setBlock(partPos(pos, state.getValue(FACING), part), state.setValue(PART, part), 3);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this)) {
            BlockPos master = master(state, pos);
            for (int part = 0; part < parts(); part++) {
                BlockPos p = partPos(master, state.getValue(FACING), part);
                BlockState other = level.getBlockState(p);
                if (!p.equals(pos) && other.is(this) && other.getValue(PART) == part) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == master() ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PASS) && passage(state.getValue(PART)) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player.hasPermissions(2) ? super.getDestroyProgress(state, player, level, pos) : 0f;
    }

    // -- Interação

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(OPEN) && isMember(sp)) {
            HouseDoors.openScreen(sp, master(state, pos), house);
        }
        return InteractionResult.CONSUME;
    }

    public boolean isMember(Player player) {
        MagicData data = MagicData.get(player);
        return data != null && data.getHouse() == house;
    }

    private static int openDelay(House house) {
        return switch (house) {
            case GRIFINORIA -> 22;
            case SONSERINA -> 48;
            case CORVINAL -> 36;
            case LUFA_LUFA -> 26;
        };
    }

    private void setParts(ServerLevel level, BlockPos master, BlockState state, boolean open, boolean pass) {
        for (int part = 0; part < parts(); part++) {
            BlockPos p = partPos(master, state.getValue(FACING), part);
            BlockState other = level.getBlockState(p);
            if (other.is(state.getBlock())) {
                level.setBlock(p, other.setValue(OPEN, open).setValue(PASS, pass), 10);
            }
        }
    }

    public static void setOpen(ServerLevel level, BlockPos master, boolean open) {
        BlockState state = level.getBlockState(master);
        if (!(state.getBlock() instanceof HouseDoorBlock door) || state.getValue(PART) != door.master() || state.getValue(OPEN) == open) {
            return;
        }
        door.setParts(level, master, state, open, false);
        if (open) {
            level.scheduleTick(master, state.getBlock(), openDelay(door.house));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != master() || !state.getValue(OPEN)) {
            return;
        }
        if (!state.getValue(PASS)) {
            setParts(level, pos, state, true, true);
            level.scheduleTick(pos, this, OPEN_TICKS);
            return;
        }
        Direction side = state.getValue(FACING).getClockWise();
        double wide = house == House.SONSERINA ? 1 : 0;
        AABB passage = new AABB(pos).inflate(Math.abs(side.getStepX()) * wide, 0, Math.abs(side.getStepZ()) * wide).expandTowards(0, 1, 0).deflate(0.05);
        if (!level.getEntitiesOfClass(LivingEntity.class, passage).isEmpty()) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        setOpen(level, pos, false);
        HouseDoors.playClose(level, pos, house);
    }

    // -- Entidade

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                                                         net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide && type == com.gabriel.hogwartsmod.registry.ModBlockEntities.HOUSE_DOOR.get()
                ? (l, p, st, be) -> ((HouseDoorBlockEntity) be).clientTick() : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == master() ? new HouseDoorBlockEntity(pos, state) : null;
    }
}
