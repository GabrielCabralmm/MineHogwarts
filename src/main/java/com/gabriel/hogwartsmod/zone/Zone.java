package com.gabriel.hogwartsmod.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record Zone(String name, String dimension, List<Box> boxes, int rules) {
    public static final int MAX_PARTS = 64;

    public record Box(BlockPos min, BlockPos max, boolean cut) {

        public static Box of(BlockPos a, BlockPos b) {
            return of(a, b, false);
        }

        public static Box of(BlockPos a, BlockPos b, boolean cut) {
            return new Box(new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                    new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())), cut);
        }

        public boolean contains(Vec3 pos) {
            return pos.x >= min.getX() && pos.x < max.getX() + 1
                    && pos.y >= min.getY() && pos.y < max.getY() + 1
                    && pos.z >= min.getZ() && pos.z < max.getZ() + 1;
        }

        public boolean contains(BlockPos pos) {
            return contains(pos.getCenter());
        }

        public AABB aabb() {
            return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }

        public long volume() {
            return (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }
    }

    public Zone {
        boxes = List.copyOf(boxes);
    }

    public static Zone of(String name, String dimension, BlockPos a, BlockPos b, int rules) {
        return new Zone(name, dimension, List.of(Box.of(a, b)), rules);
    }

    public boolean has(ZoneRule rule) {
        return (rules & rule.bit()) != 0;
    }

    public Zone with(ZoneRule rule, boolean on) {
        return new Zone(name, dimension, boxes, on ? rules | rule.bit() : rules & ~rule.bit());
    }

    public Zone withArea(BlockPos a, BlockPos b) {
        return new Zone(name, dimension, List.of(Box.of(a, b)), rules);
    }

    public Zone plus(BlockPos a, BlockPos b) {
        return plus(a, b, false);
    }

    public Zone plus(BlockPos a, BlockPos b, boolean cut) {
        List<Box> list = new ArrayList<>(boxes);
        list.add(Box.of(a, b, cut));
        return new Zone(name, dimension, list, rules);
    }

    public Zone withoutLast() {
        return boxes.size() <= 1 ? this : new Zone(name, dimension, boxes.subList(0, boxes.size() - 1), rules);
    }

    public boolean contains(String dim, Vec3 pos) {
        if (!dimension.equals(dim)) {
            return false;
        }
        return inside(boxes, pos);
    }

    public static boolean inside(List<Box> boxes, Vec3 pos) {
        boolean inside = false;
        for (Box box : boxes) {
            if (box.contains(pos)) {
                inside = !box.cut();
            }
        }
        return inside;
    }

    public boolean touches(String dim, Box area) {
        if (!dimension.equals(dim)) {
            return false;
        }
        AABB a = area.aabb();
        for (Box box : boxes) {
            if (!box.cut() && box.aabb().intersects(a)) {
                return true;
            }
        }
        return false;
    }

    public int cuts() {
        int n = 0;
        for (Box box : boxes) {
            if (box.cut()) {
                n++;
            }
        }
        return n;
    }

    public boolean contains(String dim, BlockPos pos) {
        return contains(dim, pos.getCenter());
    }

    public AABB bounds() {
        AABB all = boxes.get(0).aabb();
        for (Box box : boxes) {
            if (!box.cut()) {
                all = all.minmax(box.aabb());
            }
        }
        return all;
    }

    public long volume() {
        return ZoneShape.of(this).volume();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Dimension", dimension);
        ListTag list = new ListTag();
        for (Box box : boxes) {
            CompoundTag b = new CompoundTag();
            b.putLong("Min", box.min().asLong());
            b.putLong("Max", box.max().asLong());
            if (box.cut()) {
                b.putBoolean("Cut", true);
            }
            list.add(b);
        }
        tag.put("Boxes", list);
        tag.putInt("Rules", rules);
        return tag;
    }

    public static Zone load(CompoundTag tag) {
        List<Box> boxes = new ArrayList<>();
        if (tag.contains("Boxes")) {
            ListTag list = tag.getList("Boxes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag b = list.getCompound(i);
                boxes.add(new Box(BlockPos.of(b.getLong("Min")), BlockPos.of(b.getLong("Max")), b.getBoolean("Cut")));
            }
        } else {
            boxes.add(new Box(BlockPos.of(tag.getLong("Min")), BlockPos.of(tag.getLong("Max")), false));
        }
        return new Zone(tag.getString("Name"), tag.getString("Dimension"), boxes, tag.getInt("Rules"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(dimension);
        buf.writeCollection(boxes, (b, box) -> {
            b.writeBlockPos(box.min());
            b.writeBlockPos(box.max());
            b.writeBoolean(box.cut());
        });
        buf.writeVarInt(rules);
    }

    public static Zone read(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String dim = buf.readUtf();
        List<Box> boxes = buf.readList(b -> new Box(b.readBlockPos(), b.readBlockPos(), b.readBoolean()));
        return new Zone(name, dim, boxes, buf.readVarInt());
    }
}
