package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record HousePacket(UUID player, String house) {

    public static void encode(HousePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.player);
        buf.writeUtf(packet.house, 32);
    }

    public static HousePacket decode(FriendlyByteBuf buf) {
        return new HousePacket(buf.readUUID(), buf.readUtf(32));
    }

    public static void handle(HousePacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleHouse(packet.player, packet.house));
        ctx.get().setPacketHandled(true);
    }
}
