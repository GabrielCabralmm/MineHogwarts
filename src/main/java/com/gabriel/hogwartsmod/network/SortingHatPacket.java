package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SortingHatPacket(int entity, boolean on) {

    public static void encode(SortingHatPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeBoolean(packet.on);
    }

    public static SortingHatPacket decode(FriendlyByteBuf buf) {
        return new SortingHatPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SortingHatPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSortingHat(packet.entity, packet.on));
        ctx.get().setPacketHandled(true);
    }
}
