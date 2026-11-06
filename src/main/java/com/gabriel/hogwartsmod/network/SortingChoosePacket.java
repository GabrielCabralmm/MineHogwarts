package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.house.SortingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SortingChoosePacket(UUID target, String house) {

    public static void encode(SortingChoosePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.target);
        buf.writeUtf(packet.house, 32);
    }

    public static SortingChoosePacket decode(FriendlyByteBuf buf) {
        return new SortingChoosePacket(buf.readUUID(), buf.readUtf(32));
    }

    public static void handle(SortingChoosePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            SortingManager.choose(player, packet.target, House.byId(packet.house));
        }
        ctx.get().setPacketHandled(true);
    }
}
