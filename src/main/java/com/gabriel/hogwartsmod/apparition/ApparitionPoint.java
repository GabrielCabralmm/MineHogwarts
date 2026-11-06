package com.gabriel.hogwartsmod.apparition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record ApparitionPoint(String name, String dimension, double x, double y, double z, float yaw) {

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public ApparitionPoint withName(String value) {
        return new ApparitionPoint(value, dimension, x, y, z, yaw);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Dim", dimension);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        return tag;
    }

    public static ApparitionPoint load(CompoundTag tag) {
        return new ApparitionPoint(tag.getString("Name"), tag.getString("Dim"), tag.getDouble("X"), tag.getDouble("Y"),
                tag.getDouble("Z"), tag.getFloat("Yaw"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name, 64);
        buf.writeUtf(dimension, 128);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
    }

    public static ApparitionPoint read(FriendlyByteBuf buf) {
        return new ApparitionPoint(buf.readUtf(64), buf.readUtf(128), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
    }
}
