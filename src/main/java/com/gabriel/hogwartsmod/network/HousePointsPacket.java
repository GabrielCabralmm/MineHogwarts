package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.house.HousePoints;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HousePointsPacket(int[] points, int[] cups, String lastWinner) {

    public static void encode(HousePointsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.points);
        buf.writeVarIntArray(packet.cups);
        buf.writeUtf(packet.lastWinner, 32);
    }

    public static HousePointsPacket decode(FriendlyByteBuf buf) {
        return new HousePointsPacket(buf.readVarIntArray(), buf.readVarIntArray(), buf.readUtf(32));
    }

    public static void handle(HousePointsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        int n = House.values().length;
        if (packet.points.length == n && packet.cups.length == n) {
            HousePoints.clientPoints = packet.points.clone();
            HousePoints.clientCups = packet.cups.clone();
            HousePoints.clientLastWinner = House.byId(packet.lastWinner);
        }
        ctx.get().setPacketHandled(true);
    }
}
