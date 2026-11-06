package com.gabriel.hogwartsmod.zone;

import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.ZonesPacket;
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

public class ZoneData extends SavedData {
    private static final String ID = "hogwartsmod_zonas";
    private final List<Zone> zones = new ArrayList<>();

    public static ZoneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ZoneData::load, ZoneData::new, ID);
    }

    private static ZoneData load(CompoundTag tag) {
        ZoneData data = new ZoneData();
        ListTag list = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.zones.add(Zone.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        zones.forEach(z -> list.add(z.save()));
        tag.put("Zones", list);
        return tag;
    }

    public List<Zone> all() {
        return List.copyOf(zones);
    }

    @Nullable
    public Zone find(String name) {
        return zones.stream().filter(z -> z.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void put(Zone zone) {
        zones.removeIf(z -> z.name().equalsIgnoreCase(zone.name()));
        zones.add(zone);
        zones.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        setDirty();
        sendToAll();
    }

    public boolean remove(String name) {
        boolean removed = zones.removeIf(z -> z.name().equalsIgnoreCase(name));
        if (removed) {
            setDirty();
            sendToAll();
        }
        return removed;
    }

    public void sendTo(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZonesPacket(all()));
    }

    public void sendToAll() {
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ZonesPacket(all()));
    }
}
