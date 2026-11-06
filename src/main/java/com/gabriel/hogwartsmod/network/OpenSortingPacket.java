package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record OpenSortingPacket(UUID target, String name, String house) {

    public static void encode(OpenSortingPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.target);
        buf.writeUtf(packet.name, 64);
        buf.writeUtf(packet.house, 32);
    }

    public static OpenSortingPacket decode(FriendlyByteBuf buf) {
        return new OpenSortingPacket(buf.readUUID(), buf.readUtf(64), buf.readUtf(32));
    }

    public static void handle(OpenSortingPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenSorting(packet.target, packet.name, packet.house));
        ctx.get().setPacketHandled(true);
    }
}
