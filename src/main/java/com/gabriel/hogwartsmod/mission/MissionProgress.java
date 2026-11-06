package com.gabriel.hogwartsmod.mission;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MissionProgress {
    public static final int MAX_ACTIVE = 5;

    private final List<ActiveMission> active = new ArrayList<>();
    private final Map<String, Long> completed = new HashMap<>();

    public static String key(UUID npc, String mission) {
        return npc + "/" + mission;
    }

    public List<ActiveMission> active() {
        return List.copyOf(active);
    }

    @Nullable
    public ActiveMission find(UUID npc, String mission) {
        String key = key(npc, mission);
        return active.stream().filter(a -> a.key().equals(key)).findFirst().orElse(null);
    }

    public void start(ActiveMission mission) {
        active.removeIf(a -> a.key().equals(mission.key()));
        active.add(mission);
    }

    public void update(ActiveMission mission) {
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).key().equals(mission.key())) {
                active.set(i, mission);
            }
        }
    }

    public void remove(UUID npc, String mission) {
        String key = key(npc, mission);
        active.removeIf(a -> a.key().equals(key));
    }

    public void complete(UUID npc, String mission, long now) {
        remove(npc, mission);
        completed.put(key(npc, mission), now);
    }

    public long completedAt(UUID npc, String mission) {
        return completed.getOrDefault(key(npc, mission), -1L);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        active.forEach(a -> list.add(a.save()));
        tag.put("Ativas", list);
        CompoundTag done = new CompoundTag();
        completed.forEach(done::putLong);
        tag.put("Concluidas", done);
        return tag;
    }

    public void load(CompoundTag tag) {
        active.clear();
        completed.clear();
        ListTag list = tag.getList("Ativas", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            active.add(ActiveMission.load(list.getCompound(i)));
        }
        CompoundTag done = tag.getCompound("Concluidas");
        done.getAllKeys().forEach(k -> completed.put(k, done.getLong(k)));
    }
}
