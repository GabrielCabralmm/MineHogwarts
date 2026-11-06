package com.gabriel.hogwartsmod.house;

import com.gabriel.hogwartsmod.network.HousePointsPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class HousePoints extends SavedData {
    private static final String ID = "hogwartsmod_pontos";

    public static int[] clientPoints = new int[House.values().length];
    public static int[] clientCups = new int[House.values().length];
    @Nullable
    public static House clientLastWinner;

    private final Map<House, Integer> points = new EnumMap<>(House.class);
    private final Map<House, Integer> cups = new EnumMap<>(House.class);
    @Nullable
    private House lastWinner;

    public static HousePoints get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HousePoints::load, HousePoints::new, ID);
    }

    private static HousePoints load(CompoundTag tag) {
        HousePoints data = new HousePoints();
        CompoundTag p = tag.getCompound("Points");
        CompoundTag c = tag.getCompound("Cups");
        for (House house : House.values()) {
            data.points.put(house, p.getInt(house.getId()));
            data.cups.put(house, c.getInt(house.getId()));
        }
        data.lastWinner = House.byId(tag.getString("LastWinner"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag p = new CompoundTag();
        CompoundTag c = new CompoundTag();
        for (House house : House.values()) {
            p.putInt(house.getId(), get(house));
            c.putInt(house.getId(), cups(house));
        }
        tag.put("Points", p);
        tag.put("Cups", c);
        if (lastWinner != null) {
            tag.putString("LastWinner", lastWinner.getId());
        }
        return tag;
    }

    public int get(House house) {
        return points.getOrDefault(house, 0);
    }

    public int cups(House house) {
        return cups.getOrDefault(house, 0);
    }

    @Nullable
    public House lastWinner() {
        return lastWinner;
    }

    public int add(House house, int amount) {
        int value = Math.max(0, get(house) + amount);
        points.put(house, value);
        changed();
        return value;
    }

    public void set(House house, int value) {
        points.put(house, Math.max(0, value));
        changed();
    }

    public void reset() {
        points.clear();
        changed();
    }

    @Nullable
    public House leader() {
        House best = null;
        int bestPoints = 0;
        boolean tie = false;
        for (House house : House.values()) {
            int value = get(house);
            if (value > bestPoints) {
                best = house;
                bestPoints = value;
                tie = false;
            } else if (value == bestPoints && value > 0) {
                tie = true;
            }
        }
        return tie ? null : best;
    }

    public void finish(House winner) {
        lastWinner = winner;
        cups.put(winner, cups(winner) + 1);
        points.clear();
        changed();
    }

    private void changed() {
        setDirty();
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet());
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet());
    }

    private HousePointsPacket packet() {
        int[] p = new int[House.values().length];
        int[] c = new int[House.values().length];
        for (House house : House.values()) {
            p[house.ordinal()] = get(house);
            c[house.ordinal()] = cups(house);
        }
        return new HousePointsPacket(p, c, lastWinner == null ? "" : lastWinner.getId());
    }
}
