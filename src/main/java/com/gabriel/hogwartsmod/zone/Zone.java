package com.gabriel.hogwartsmod.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record Zone(String name, String dimension, BlockPos min, BlockPos max, int rules) {

    public static Zone of(String name, String dimension, BlockPos a, BlockPos b, int rules) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        return new Zone(name, dimension, min, max, rules);
    }

    public boolean has(ZoneRule rule) {
        return (rules & rule.bit()) != 0;
    }

    public Zone with(ZoneRule rule, boolean on) {
        return new Zone(name, dimension, min, max, on ? rules | rule.bit() : rules & ~rule.bit());
    }

    public Zone withArea(BlockPos a, BlockPos b) {
        return of(name, dimension, a, b, rules);
    }

    public boolean contains(String dim, Vec3 pos) {
        return dimension.equals(dim)
                && pos.x >= min.getX() && pos.x < max.getX() + 1
                && pos.y >= min.getY() && pos.y < max.getY() + 1
                && pos.z >= min.getZ() && pos.z < max.getZ() + 1;
    }

    public AABB box() {
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    public int sizeX() {
        return max.getX() - min.getX() + 1;
    }

    public int sizeY() {
        return max.getY() - min.getY() + 1;
    }

    public int sizeZ() {
        return max.getZ() - min.getZ() + 1;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Dimension", dimension);
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        tag.putInt("Rules", rules);
        return tag;
    }

    public static Zone load(CompoundTag tag) {
        return new Zone(tag.getString("Name"), tag.getString("Dimension"), BlockPos.of(tag.getLong("Min")),
                BlockPos.of(tag.getLong("Max")), tag.getInt("Rules"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(dimension);
        buf.writeBlockPos(min);
        buf.writeBlockPos(max);
        buf.writeVarInt(rules);
    }

    public static Zone read(FriendlyByteBuf buf) {
        return new Zone(buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt());
    }
}
