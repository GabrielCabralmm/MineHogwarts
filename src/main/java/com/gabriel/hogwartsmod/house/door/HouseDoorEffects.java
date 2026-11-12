package com.gabriel.hogwartsmod.house.door;

import com.gabriel.hogwartsmod.house.House;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class HouseDoorEffects {
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.3f, 1f, 0.45f), 1f);

    private HouseDoorEffects() {
    }

    public static void opening(HouseDoorBlockEntity door, int tick) {
        Level level = door.getLevel();
        if (level == null || !(door.getBlockState().getBlock() instanceof HouseDoorBlock block)) {
            return;
        }
        if (block.getHouse() == House.SONSERINA) {
            slytherin(door, level, tick);
        }
    }

    private static void slytherin(HouseDoorBlockEntity door, Level level, int tick) {
        RandomSource rnd = level.random;
        if (tick == 0) {
            sound(door, level, 0, 32, SoundEvents.CAT_HISS, 0.9f, 0.55f);
            sound(door, level, 0, 32, SoundEvents.CAT_HISS, 0.7f, 0.45f);
        }
        if (tick == 4) {
            sound(door, level, 0, 24, SoundEvents.SAND_STEP, 0.6f, 0.5f);
        }
        if (tick < 16) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                Vec3 p = at(door, sgn * 34 + rnd.nextGaussian() * 1.5, 36 + rnd.nextGaussian(), -26 + rnd.nextGaussian());
                level.addParticle(GREEN, p.x, p.y, p.z, 0, 0.01, 0);
            }
        }
        if (tick == 19) {
            sound(door, level, 0, 16, SoundEvents.GRINDSTONE_USE, 0.8f, 0.5f);
        }
        if (tick == 30) {
            sound(door, level, 0, 16, SoundEvents.PISTON_CONTRACT, 0.7f, 0.6f);
        }
        if (tick >= 19 && tick < 50) {
            BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState());
            for (int i = 0; i < 3; i++) {
                Vec3 p = at(door, (rnd.nextFloat() - 0.5f) * 48, rnd.nextFloat() * 42, -9);
                level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
            }
            if (tick % 2 == 0) {
                Vec3 p = at(door, (rnd.nextFloat() - 0.5f) * 44, rnd.nextFloat() * 36, -10);
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }

    private static void sound(HouseDoorBlockEntity door, Level level, double x, double y, SoundEvent sound, float volume, float pitch) {
        Vec3 p = at(door, x, y, -8);
        level.playLocalSound(p.x, p.y, p.z, sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    public static Vec3 at(HouseDoorBlockEntity door, double x, double y, double z) {
        Direction facing = door.getBlockState().getValue(HouseDoorBlock.FACING);
        Direction side = facing.getClockWise();
        BlockPos pos = door.getBlockPos();
        double f = -z / 16;
        double s = -x / 16;
        return new Vec3(pos.getX() + 0.5 + facing.getStepX() * f + side.getStepX() * s, pos.getY() + y / 16,
                pos.getZ() + 0.5 + facing.getStepZ() * f + side.getStepZ() * s);
    }
}
