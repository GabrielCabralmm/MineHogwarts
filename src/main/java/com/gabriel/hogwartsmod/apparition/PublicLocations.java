package com.gabriel.hogwartsmod.apparition;

import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PublicLocationsPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PublicLocations extends SavedData {
    private static final String ID = "hogwartsmod_aparatacao";
    private final List<ApparitionPoint> points = new ArrayList<>();

    public static PublicLocations get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PublicLocations::load, PublicLocations::new, ID);
    }

    private static PublicLocations load(CompoundTag tag) {
        PublicLocations data = new PublicLocations();
        ListTag list = tag.getList("Points", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.points.add(ApparitionPoint.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        points.forEach(p -> list.add(p.save()));
        tag.put("Points", list);
        return tag;
    }

    public List<ApparitionPoint> all() {
        return List.copyOf(points);
    }

    @Nullable
    public ApparitionPoint find(String name) {
        return points.stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void put(ApparitionPoint point) {
        points.removeIf(p -> p.name().equalsIgnoreCase(point.name()));
        points.add(point);
        points.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        setDirty();
    }

    public boolean remove(String name) {
        boolean removed = points.removeIf(p -> p.name().equalsIgnoreCase(name));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PublicLocationsPacket(all()));
    }

    public void sendToAll() {
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PublicLocationsPacket(all()));
    }
}
