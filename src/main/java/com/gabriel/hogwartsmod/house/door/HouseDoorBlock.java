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
    public static final IntegerProperty PART = IntegerProperty.create("parte", 0, 29);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty PASS = BooleanProperty.create("passagem");
    private static final java.util.Map<Integer, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();
    public static final int OPEN_TICKS = 100;

    private final House house;
    private final int width;
    private final int height;

    public HouseDoorBlock(House house, Properties properties) {
        super(properties.lightLevel(state -> light(house, state.getValue(PART))));
        this.house = house;
        this.width = width(house);
        this.height = height(house);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, width / 2).setValue(OPEN, false).setValue(PASS, false));
    }

    private static int width(House house) {
        return switch (house) {
            case GRIFINORIA -> 3;
            case CORVINAL, SONSERINA, LUFA_LUFA -> 5;
        };
    }

    private static int height(House house) {
        return switch (house) {
            case GRIFINORIA -> 3;
            case SONSERINA -> 5;
            case LUFA_LUFA -> 4;
            case CORVINAL -> 6;
        };
    }

    private static int light(House house, int part) {
        int w = width(house);
        int col = part % w;
        int row = part / w;
        boolean edge = col == 0 || col == w - 1;
        return switch (house) {
            case SONSERINA -> 0;
            case CORVINAL -> 0;
            case LUFA_LUFA -> row == 3 && edge ? 13 : 0;
            case GRIFINORIA -> 0;
        };
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

    private int[] opening() {
        return switch (house) {
            case GRIFINORIA -> new int[]{-8, 8, 0, 32};
            case SONSERINA -> new int[]{-24, 24, 0, 48};
            case CORVINAL -> new int[]{-26, 26, 0, 76};
            case LUFA_LUFA -> new int[]{-18, 18, 8, 50};
        };
    }

    private int[] openingIn(int part) {
        int[] o = opening();
        int c = part % width - width / 2;
        int row = part / width;
        int u0 = Math.max(0, o[0] + 16 * c + 8);
        int u1 = Math.min(16, o[1] + 16 * c + 8);
        int y0 = Math.max(0, o[2] - 16 * row);
        int y1 = Math.min(16, o[3] - 16 * row);
        return u1 > u0 && y1 > y0 && y1 - y0 >= 4 ? new int[]{u0, u1, y0, y1} : null;
    }

    private boolean passage(int part) {
        return openingIn(part) != null;
    }

    private static VoxelShape box(Direction facing, double u0, double u1, double y0, double y1) {
        return switch (facing) {
            case SOUTH -> Block.box(u0, y0, 0, u1, y1, 16);
            case EAST -> Block.box(0, y0, 16 - u1, 16, y1, 16 - u0);
            case WEST -> Block.box(0, y0, u0, 16, y1, u1);
            default -> Block.box(16 - u1, y0, 0, 16 - u0, y1, 16);
        };
    }

    private VoxelShape openShape(Direction facing, int part) {
        return SHAPES.computeIfAbsent(house.ordinal() * 1000 + facing.ordinal() * 100 + part, k -> {
            int[] o = openingIn(part);
            VoxelShape shape = Shapes.empty();
            if (o[0] > 0) {
                shape = Shapes.or(shape, box(facing, 0, o[0], 0, 16));
            }
            if (o[1] < 16) {
                shape = Shapes.or(shape, box(facing, o[1], 16, 0, 16));
            }
            if (o[2] > 0) {
                shape = Shapes.or(shape, box(facing, o[0], o[1], 0, o[2]));
            }
            if (o[3] < 16) {
                shape = Shapes.or(shape, box(facing, o[0], o[1], o[3], 16));
            }
            return shape;
        });
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
        int part = state.getValue(PART);
        if (!state.getValue(PASS) || !passage(part)) {
            return Shapes.block();
        }
        return openShape(state.getValue(FACING), part);
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
            BlockPos master = master(state, pos);
            Direction facing = state.getValue(FACING);
            double side = (sp.getX() - master.getX() - 0.5) * facing.getStepX() + (sp.getZ() - master.getZ() - 0.5) * facing.getStepZ();
            if (side < 0) {
                setOpen(sp.serverLevel(), master, true);
                HouseDoors.playOpen(sp.serverLevel(), master, house);
            } else {
                HouseDoors.openScreen(sp, master, house);
            }
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
            case SONSERINA -> 62;
            case CORVINAL -> 56;
            case LUFA_LUFA -> 38;
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
        boolean inside = false;
        for (int part = 0; part < parts() && !inside; part++) {
            if (passage(part)) {
                inside = !level.getEntitiesOfClass(LivingEntity.class, new AABB(partPos(pos, state.getValue(FACING), part)).deflate(0.05)).isEmpty();
            }
        }
        if (inside) {
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
