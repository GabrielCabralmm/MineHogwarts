package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record ZonesPacket(List<Zone> zones) {

    public static void encode(ZonesPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.zones, (b, z) -> z.write(b));
    }

    public static ZonesPacket decode(FriendlyByteBuf buf) {
        return new ZonesPacket(buf.readList(Zone::read));
    }

    public static void handle(ZonesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        Zones.clientZones = List.copyOf(packet.zones);
        ctx.get().setPacketHandled(true);
    }
}
