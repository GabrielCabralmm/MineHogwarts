package com.gabriel.hogwartsmod.duel;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelRecords extends SavedData {
    private static final String NAME = HogwartsMod.MOD_ID + "_duelos";
    private final Map<String, Long> last = new HashMap<>();

    public static DuelRecords get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DuelRecords::load, DuelRecords::new, NAME);
    }

    private static String key(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public long remainingMillis(UUID a, UUID b, long cooldownMillis) {
        Long t = last.get(key(a, b));
        return t == null ? 0 : Math.max(0, t + cooldownMillis - System.currentTimeMillis());
    }

    public void record(UUID a, UUID b) {
        last.put(key(a, b), System.currentTimeMillis());
        setDirty();
    }

    public static DuelRecords load(CompoundTag tag) {
        DuelRecords data = new DuelRecords();
        for (String k : tag.getAllKeys()) {
            data.last.put(k, tag.getLong(k));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        last.forEach(tag::putLong);
        return tag;
    }
}
