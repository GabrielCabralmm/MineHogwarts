package com.gabriel.hogwartsmod.block;

import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PulpitBlockEntity extends BlockEntity {
    public static final int MAX_EXTENT = 32;

    public enum Side { LEFT, RIGHT, FRONT, BACK, UP, DOWN }

    private final int[] extents = {5, 5, 10, 2, 4, 1};
    @Nullable
    private UUID owner;
    private boolean showArea = true;
    private boolean bookOpen;
    private float open;
    private float openOld;
    private int time;

    public PulpitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULPIT.get(), pos, state);
    }

    // -- Dono e área

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID value) {
        owner = value;
        changed();
    }

    public boolean canEdit(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    public int get(Side side) {
        return extents[side.ordinal()];
    }

    public int[] getExtents() {
        return extents.clone();
    }

    public void setExtents(int[] values, boolean show) {
        for (int i = 0; i < extents.length && i < values.length; i++) {
            extents[i] = Math.max(0, Math.min(MAX_EXTENT, values[i]));
        }
        showArea = show;
        changed();
    }

    public boolean isShowArea() {
        return showArea;
    }

    public Direction facing() {
        return getBlockState().hasProperty(PulpitBlock.FACING) ? getBlockState().getValue(PulpitBlock.FACING) : Direction.NORTH;
    }

    public AABB area() {
        return area(worldPosition, facing(), extents);
    }

    public static AABB area(BlockPos pos, Direction facing, int[] e) {
        Direction front = facing.getOpposite();
        Direction right = front.getClockWise();
        BlockPos a = pos.relative(front, e[Side.FRONT.ordinal()]).relative(right, e[Side.RIGHT.ordinal()]).above(e[Side.UP.ordinal()]);
        BlockPos b = pos.relative(facing, e[Side.BACK.ordinal()]).relative(right.getOpposite(), e[Side.LEFT.ordinal()]).below(e[Side.DOWN.ordinal()]);
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -- Livro

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, PulpitBlockEntity pulpit) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        boolean near = false;
        for (Player player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(pos.getCenter()) <= 5.0 * 5.0) {
                com.gabriel.hogwartsmod.magic.MagicData data = com.gabriel.hogwartsmod.magic.MagicData.get(player);
                if (data != null && data.isProfessor()) {
                    near = true;
                    break;
                }
            }
        }
        if (near != pulpit.bookOpen) {
            pulpit.bookOpen = near;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, PulpitBlockEntity pulpit) {
        pulpit.time++;
        pulpit.openOld = pulpit.open;
        float target = pulpit.bookOpen ? 1f : 0f;
        pulpit.open += Math.max(-0.08f, Math.min(0.08f, target - pulpit.open));
    }

    public float getOpen(float partialTick) {
        return openOld + (open - openOld) * partialTick;
    }

    public float getTime(float partialTick) {
        return time + partialTick;
    }

    // -- Salvamento e sincronização

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("Area", extents);
        tag.putBoolean("ShowArea", showArea);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int[] saved = tag.getIntArray("Area");
        if (saved.length == extents.length) {
            System.arraycopy(saved, 0, extents, 0, extents.length);
        }
        showArea = !tag.contains("ShowArea") || tag.getBoolean("ShowArea");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        bookOpen = tag.getBoolean("BookOpen");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.putBoolean("BookOpen", bookOpen);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
