package com.gabriel.hogwartsmod.house.door;

import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HouseDoorBlockEntity extends BlockEntity {
    public static final int ANIM_TICKS = 12;
    private int progress;
    private int lastProgress;

    public HouseDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOUSE_DOOR.get(), pos, state);
    }

    public void clientTick() {
        lastProgress = progress;
        boolean open = getBlockState().getValue(HouseDoorBlock.OPEN);
        progress = Math.max(0, Math.min(ANIM_TICKS, progress + (open ? 1 : -1)));
    }

    public float openness(float partialTick) {
        float t = (lastProgress + (progress - lastProgress) * partialTick) / ANIM_TICKS;
        return t * t * (3 - 2 * t);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.5).expandTowards(0, 1, 0);
    }
}
