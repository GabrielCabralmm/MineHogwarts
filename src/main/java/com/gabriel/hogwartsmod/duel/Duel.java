package com.gabriel.hogwartsmod.duel;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Duel {
    public final UUID a;
    public final UUID b;
    public final String nameA;
    public final String nameB;
    public final ResourceKey<Level> dimension;
    public final Vec3 center;
    public final double radius;
    public final long startTick;
    public final long endTick;
    public final long betEach;
    public final ServerBossEvent bar;
    public final Map<UUID, Set<String>> spells = new HashMap<>();
    public int lastCount = -1;
    public boolean ended;

    Duel(UUID a, UUID b, String nameA, String nameB, ResourceKey<Level> dimension, Vec3 center, double radius, long startTick, long endTick,
         long betEach, ServerBossEvent bar) {
        this.a = a;
        this.b = b;
        this.nameA = nameA;
        this.nameB = nameB;
        this.dimension = dimension;
        this.center = center;
        this.radius = radius;
        this.startTick = startTick;
        this.endTick = endTick;
        this.betEach = betEach;
        this.bar = bar;
        spells.put(a, new HashSet<>());
        spells.put(b, new HashSet<>());
    }

    public boolean has(UUID id) {
        return a.equals(id) || b.equals(id);
    }

    public UUID other(UUID id) {
        return a.equals(id) ? b : a;
    }

    public String name(UUID id) {
        return a.equals(id) ? nameA : nameB;
    }

    public boolean fighting(long now) {
        return now >= startTick && !ended;
    }
}
