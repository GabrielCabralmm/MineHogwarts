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
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
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
